/*
 * Copyright (c) 2008-2021, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.android.ui.fragments.maps;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentGeocodeAddressBinding;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingAddressViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.constants.Events.GEOCODE_ZOOM;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;

@AndroidEntryPoint
public class GeocodingAddressFragment extends GeocodingPageFragment {

    private GeocodingAddressViewModel mViewModel;
    private FragmentGeocodeAddressBinding mBinding;

    public static GeocodingAddressFragment newInstance() {
        return new GeocodingAddressFragment();
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_geocode_address.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_geocode_address, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link GeocodingAddressViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get reference to the shared view model that holds the marker.
        mSharedViewModel = new ViewModelProvider(requireParentFragment()).get(GeocodingViewModel.class);

        mViewModel = new ViewModelProvider(this).get(GeocodingAddressViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    /**
     * Search for the provided address and move the marker to that point if it's found.
     */
    public void search() {
        updateMap();
    }

    /**
     * Locate the address from the provided marker point.
     */
    public void locate() {
        updateText(mSharedViewModel.getPoint().getValue());
    }

    @Override
    protected void updateText(EnhancedLatLng point) {
        try {
            LatLng coordinate = point.getLatLng();

            if (coordinate == null) {
                throw new IllegalArgumentException();
            }

            // Geolocation takes place over the network, so processing it in a background thread.
            subscribeToWorker(mNetworkRepository.geocodeCoordinate(coordinate), new WorkerCallback() {
                @Override
                public void onSuccess(@NonNull @NotNull WorkInfo workInfo) {
                    // Get the resulting geolocated address if there is one.
                    String address = workInfo.getOutputData().getString("address");

                    if (emptyCheck(address)) {
                        Snackbar.make(requireView(), "Could not find address for the provided marker.", Snackbar.LENGTH_SHORT).show();
                    } else {
                        mViewModel.setAddress(address);
                    }
                    mViewModel.setLoading(false);
                }

                @Override
                public void onFailure(@NonNull @NotNull WorkInfo workInfo) {
                    Snackbar.make(requireView(), "Could not find address for the provided marker.", Snackbar.LENGTH_SHORT).show();
                    mViewModel.setLoading(false);

                }

                @Override
                public void onWorking() {
                    mViewModel.setLoading(true);
                }
            });
        } catch (Exception e) {
            Snackbar.make(requireView(), "Please place a marker on the map to geolocate.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void updateMap() {
        try {
            // Gather the text field values.
            String addressText = Objects.requireNonNull(mViewModel.getAddress().getValue());

            // If the text is empty, the whole coordinate with be invalid, so throw an exception.
            if (emptyCheck(addressText)) {
                throw new IllegalArgumentException();
            }

            // Geolocation takes place over the network, so processing it in a background thread.
            subscribeToWorker(mNetworkRepository.geocodeAddress(addressText), new WorkerCallback() {
                @Override
                public void onSuccess(@NonNull @NotNull WorkInfo workInfo) {
                    // Get the resulting geolocated LatLng if there is one.
                    double latitude = workInfo.getOutputData().getDouble("latitude", -1D);
                    double longitude = workInfo.getOutputData().getDouble("longitude", -1D);

                    if (latitude != -1D && longitude != -1D) {
                        // Add/move the marker on the map depending on the new coordinate value.
                        LatLng point = new LatLng(latitude, longitude);
                        setPoint(point, INPUT);
                        LiveDataBus.publish(GEOCODE_ZOOM);
                    } else {
                        Snackbar.make(requireView(), "Could not geolocate the provided address: " + addressText, Snackbar.LENGTH_SHORT).show();
                    }
                    mViewModel.setLoading(false);
                }

                @Override
                public void onFailure(@NonNull @NotNull WorkInfo workInfo) {
                    Snackbar.make(requireView(), "Could not geolocate the provided address: " + addressText, Snackbar.LENGTH_SHORT).show();
                    mViewModel.setLoading(false);
                }

                @Override
                public void onWorking() {
                    mViewModel.setLoading(true);
                }
            });
        } catch (Exception e) {
            Snackbar.make(requireView(), "Please provide an address to geolocate.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void refresh() {
        updateText(mSharedViewModel.getPoint().getValue());
    }

    /**
     * Get the title to use for this tab fragment. Need to pass in a context, because the fragment
     * isn't attached to by the time that the {@link TabLayoutMediator} tries to get the tab title.
     *
     * @param context The context where this tab will be used.
     * @return String The tab's title.
     */
    @Override
    public String getTabTitle(Context context) {
        return context.getString(R.string.address_placemark);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.address_placemark);
    }
}
