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

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentDistanceCalculatorBinding;
import edu.mit.ll.nics.android.maps.EnhancedLocation;
import edu.mit.ll.nics.android.ui.viewmodel.maps.DistanceCalculatorViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.DistanceCalculatorViewModel.DistanceCalculatorViewModelFactory;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;

import static edu.mit.ll.nics.android.utils.GeoUtils.latLngToLocation;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.multiEmptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.popBackStack;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

@AndroidEntryPoint
public class DistanceCalculatorFragment extends MapBaseFragment {

    private DistanceCalculatorViewModel mViewModel;
    private FragmentDistanceCalculatorBinding mBinding;

    @Inject
    DistanceCalculatorViewModelFactory mViewModelFactory;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register on back pressed callback.
        mActivity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_distance_calculator.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_distance_calculator, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link DistanceCalculatorViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the view model with the point from the shared view model to initialize the text fields.
        DistanceCalculatorViewModel.Factory factory = new DistanceCalculatorViewModel.Factory(mViewModelFactory, mMapViewModel.getDistancePoint());
        mViewModel = new ViewModelProvider(this, factory).get(DistanceCalculatorViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setMapViewModel(mMapViewModel);
        mBinding.setFragment(this);
        mBinding.setMapFragment(mMapFragment);

        subscribeToModel();
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mMapFragment.getElevation();
        super.onDestroy();
    }

    private void subscribeToModel() {
        // Observe when the map marker has changed and update the text fields accordingly.
        mMapViewModel.getDistancePointLiveData().observe(mLifecycleOwner, point -> {
            if (point == null) {
                mViewModel.clearTextFields();
            } else {
                updateText(point);
            }
        });

        // Observe the latitude text field for changes and update the map accordingly.
        mViewModel.getLatitude().observe(mLifecycleOwner, lat -> {
            // Only update the map if the text field change was from user input.
            if (lat != null && lat.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the longitude text field for changes and update the map accordingly.
        mViewModel.getLongitude().observe(mLifecycleOwner, lon -> {
            // Only update the map if the text field change was from user input.
            if (lon != null && lon.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });
    }

    protected void updateText(EnhancedLocation location) {
        if (location.getTrigger().equals(MAP)) {
            mViewModel.setTextFields(location);
        }
    }

    protected void updateMap() {
        try {
            // Gather the text field values.
            String latitude = Objects.requireNonNull(mViewModel.getLatitude().getValue()).getData();
            String longitude = Objects.requireNonNull(mViewModel.getLongitude().getValue()).getData();

            // If the text is empty, the whole coordinate with be invalid, so throw an exception.
            if (multiEmptyCheck(latitude, longitude)) {
                throw new IllegalArgumentException();
            }

            // Create a location object from the text fields.
            Location location = new Location(EMPTY);
            location.setLatitude(Double.parseDouble(latitude));
            location.setLongitude(Double.parseDouble(longitude));

            // Add/move the marker on the map depending on the new coordinate value.
            setPoint(location, INPUT);
        } catch (Exception e) {
            // If any exception occurs, the text fields are invalid, so remove the marker from the map.
            setPoint(null, INPUT);
        }
    }

    protected void setPoint(Location location, LiveDataTrigger trigger) {
        if (mMapViewModel.getDistancePoint() == null) {
            mMapViewModel.setDistancePoint(new EnhancedLocation(UUID.randomUUID(), location, MAP));
        } else {
            EnhancedLocation l = mMapViewModel.getDistancePoint();
            l.setLocation(location);
            l.setTrigger(trigger);
            mMapViewModel.setDistancePoint(l);
        }
    }

    public void refresh() {
        mViewModel.refresh(mMapViewModel.getDistancePoint());
    }

    public void clear() {
        mMapViewModel.setDistancePoint(null);
    }

    /**
     * Override the onBackPressedCallback so that the user can go back to whatever edit panel they were working on.
     */
    final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            popBackStack(mNavController);
        }
    };

    @Override
    public void onMapClick(@NotNull LatLng point) {
        setPoint(latLngToLocation(point), MAP);
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        setPoint(latLngToLocation(marker.getPosition()), MAP);
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        setPoint(latLngToLocation(marker.getPosition()), MAP);
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        setPoint(latLngToLocation(marker.getPosition()), MAP);
    }
}
