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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.data.DMSCoordinates;
import edu.mit.ll.nics.android.databinding.FragmentGeocodeDegMinSecBinding;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingDegMinSecViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingDegMinSecViewModel.GeocodingDegMinSecViewModelFactory;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingViewModel;

import static edu.mit.ll.nics.android.utils.Utils.multiEmptyCheck;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;

@AndroidEntryPoint
public class GeocodingDegMinSecFragment extends GeocodingPageFragment {

    private GeocodingDegMinSecViewModel mViewModel;
    private FragmentGeocodeDegMinSecBinding mBinding;

    @Inject
    GeocodingDegMinSecViewModelFactory mViewModelFactory;

    public static GeocodingDegMinSecFragment newInstance() {
        return new GeocodingDegMinSecFragment();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_geocode_deg_min.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_geocode_deg_min_sec, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link GeocodingDegMinSecViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get reference to the shared view model that holds the marker.
        mSharedViewModel = new ViewModelProvider(requireParentFragment()).get(GeocodingViewModel.class);

        // Initialize the view model with the point from the shared view model to initialize the text fields.
        GeocodingDegMinSecViewModel.Factory factory = new GeocodingDegMinSecViewModel.Factory(mViewModelFactory, getSharedPoint());
        mViewModel = new ViewModelProvider(this, factory).get(GeocodingDegMinSecViewModel.class);

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
     * Subscribe and observe the properties from the {@link GeocodingViewModel} and the
     * {@link GeocodingDegMinSecViewModel}.
     */
    private void subscribeToModel() {
        // Observe when the map marker has changed and update the text fields accordingly.
        mSharedViewModel.getPoint().observe(mLifecycleOwner, this::updateText);

        // Observe the latitude degrees text field for changes and update the map accordingly.
        mViewModel.getLatDegrees().observe(mLifecycleOwner, latDegrees -> {
            // Only update the map if the text field change was from user input.
            if (latDegrees != null && latDegrees.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the latitude minutes text field for changes and update the map accordingly.
        mViewModel.getLatMinutes().observe(mLifecycleOwner, latMinutes -> {
            // Only update the map if the text field change was from user input.
            if (latMinutes != null && latMinutes.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the latitude seconds text field for changes and update the map accordingly.
        mViewModel.getLatSeconds().observe(mLifecycleOwner, latSeconds -> {
            // Only update the map if the text field change was from user input.
            if (latSeconds != null && latSeconds.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the longitude degrees text field for changes and update the map accordingly.
        mViewModel.getLonDegrees().observe(mLifecycleOwner, lonDegrees -> {
            // Only update the map if the text field change was from user input.
            if (lonDegrees != null && lonDegrees.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the longitude minutes text field for changes and update the map accordingly.
        mViewModel.getLonMinutes().observe(mLifecycleOwner, lonMinutes -> {
            // Only update the map if the text field change was from user input.
            if (lonMinutes != null && lonMinutes.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the longitude seconds text field for changes and update the map accordingly.
        mViewModel.getLonSeconds().observe(mLifecycleOwner, lonSeconds -> {
            // Only update the map if the text field change was from user input.
            if (lonSeconds != null && lonSeconds.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });
    }

    @Override
    protected void updateText(EnhancedLatLng point) {
        mViewModel.setTextFields(point);
    }

    @Override
    protected void updateMap() {
        try {
            // Gather the text field values.
            String latDeg = Objects.requireNonNull(mViewModel.getLatDegrees().getValue()).getData();
            String latMin = Objects.requireNonNull(mViewModel.getLatMinutes().getValue()).getData();
            String latSec = Objects.requireNonNull(mViewModel.getLatSeconds().getValue()).getData();
            String lonDeg = Objects.requireNonNull(mViewModel.getLonDegrees().getValue()).getData();
            String lonMin = Objects.requireNonNull(mViewModel.getLonMinutes().getValue()).getData();
            String lonSec = Objects.requireNonNull(mViewModel.getLonSeconds().getValue()).getData();

            // If the text is empty, the whole coordinate with be invalid, so throw an exception.
            if (multiEmptyCheck(latDeg, latMin, latSec, lonDeg, lonMin, lonSec)) {
                throw new IllegalArgumentException();
            }

            // Convert the text field values to a DMSCoordinates object.
            DMSCoordinates dms = new DMSCoordinates(latDeg, latMin, latSec, lonDeg, lonMin, lonSec);

            // Add/move the marker on the map depending on the new coordinate value.
            setPoint(dms.toLatLng(), INPUT);
        } catch (Exception e) {
            // If any exception occurs, the text fields are invalid, so remove the marker from the map.
            setPoint(null, INPUT);
        }
    }

    @Override
    public void refresh() {
        mViewModel.refresh(getSharedPoint());
    }

    @Override
    public String getTabTitle(Context context) {
        return context.getString(R.string.dms);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.degrees_minutes_seconds);
    }
}
