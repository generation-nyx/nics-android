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
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.tabs.TabLayoutMediator;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentGeocodeCrsBinding;
import edu.mit.ll.nics.android.enums.EPSG;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingCrsViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingCrsViewModel.GeocodingCrsViewModelFactory;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;

import static edu.mit.ll.nics.android.utils.GeoUtils.projectionTransformation;
import static edu.mit.ll.nics.android.utils.Utils.multiEmptyCheck;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;

@AndroidEntryPoint
public class GeocodingCrsFragment extends GeocodingPageFragment {

    private GeocodingCrsViewModel mViewModel;
    private FragmentGeocodeCrsBinding mBinding;

    @Inject
    GeocodingCrsViewModelFactory mViewModelFactory;

    public static GeocodingCrsFragment newInstance() {
        return new GeocodingCrsFragment();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_geocode_crs.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_geocode_crs, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link GeocodingCrsViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Initialize the dropdown adapter with the EPSG options.
        initEPSGAdapter();

        // Get reference to the shared view model that holds the marker.
        mSharedViewModel = new ViewModelProvider(requireParentFragment()).get(GeocodingViewModel.class);

        // Initialize the view model with the point from the shared view model to initialize the text fields.
        GeocodingCrsViewModel.Factory factory = new GeocodingCrsViewModel.Factory(mViewModelFactory, getSharedPoint());
        mViewModel = new ViewModelProvider(this, factory).get(GeocodingCrsViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);

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

    /**
     * Initialize the EPSG dropdown with the different options. Options are from the {@link EPSG}
     * enum and are used to transform the coordinates between the different coordinate systems.
     */
    private void initEPSGAdapter() {
        // Gather the EPSG titles from the EPSG enum to use as options in the dropdown.
        List<String> options = (List<String>) CollectionUtils.collect(Arrays.asList(EPSG.values()), EPSG::getTitle);

        // Set up the adapter with the EPSG options and bind it to the xml.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, R.layout.dropdown_item, options);
        mBinding.setAdapter(adapter);
        mBinding.executePendingBindings();
    }

    /**
     * Subscribe and observe the properties from the {@link GeocodingViewModel} and the
     * {@link GeocodingCrsViewModel}.
     */
    private void subscribeToModel() {
        // Observe when the map marker has changed and update the text fields accordingly.
        mSharedViewModel.getPoint().observe(mLifecycleOwner, this::updateText);

        // Observe when the user changes the selected EPSG and refreshModel the map and text fields.
        mViewModel.getSelectedEpsg().observe(mLifecycleOwner, epsg -> refreshModels());

        // Observe the latitude text field for changes and update the map accordingly.
        mViewModel.getLatitude().observe(mLifecycleOwner, lat -> {
            // Only update the map if the text field change was from user input.
            if (lat != null && lat.getTrigger().equals(LiveDataTrigger.INPUT)) {
                updateMap();
            }
        });

        // Observe the longitude text field for changes and update the map accordingly.
        mViewModel.getLongitude().observe(mLifecycleOwner, lon -> {
            // Only update the map if the text field change was from user input.
            if (lon != null && lon.getTrigger().equals(LiveDataTrigger.INPUT)) {
                updateMap();
            }
        });
    }

    /**
     * Refreshes the view model values so that they can be updated when the EPSG selection has changed.
     */
    private void refreshModels() {
        mSharedViewModel.refresh();
        mViewModel.refreshModel();
    }

    @Override
    protected void updateText(EnhancedLatLng point) {
        mViewModel.setTextFields(point);
    }

    @Override
    protected void updateMap() {
        try {
            // Gather the text field values.
            String latitude = Objects.requireNonNull(mViewModel.getLatitude().getValue()).getData();
            String longitude = Objects.requireNonNull(mViewModel.getLongitude().getValue()).getData();

            // If the text is empty, the whole coordinate with be invalid, so throw an exception.
            if (multiEmptyCheck(latitude, longitude)) {
                throw new IllegalArgumentException();
            }

            // Get the EPSG selection from the view model.
            EPSG selection = EPSG.getByTitle(mViewModel.getSelectedEpsg().getValue());
            double lat = Double.parseDouble(latitude);
            double lon = Double.parseDouble(longitude);

            LatLng point;
            if (selection == EPSG.EPSG_4326) {
                // If the EPSG is EPSG:4326, then we can just use the coordinates as is.
                point = new LatLng(lat, lon);
            } else {
                // Otherwise, transform the coordinates to EPSG:4326 to use on the map.
                point = projectionTransformation(selection, lat, lon);
            }

            // Add/move the marker on the map depending on the new coordinate value.
            setPoint(point, INPUT);
        } catch (Exception e) {
            // If any exception occurs, the text fields are invalid, so remove the marker from the map.
            setPoint(null, INPUT);
        }
    }

    @Override
    public void refresh() {
        mViewModel.refresh(getSharedPoint());
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
        return context.getString(R.string.crs);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.coordinate_reference_system);
    }
}
