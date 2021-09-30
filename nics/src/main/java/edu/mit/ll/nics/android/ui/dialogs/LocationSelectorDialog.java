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
package edu.mit.ll.nics.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.DialogLocationSelectorBinding;
import edu.mit.ll.nics.android.services.ServiceManager;
import edu.mit.ll.nics.android.ui.viewmodel.SymbolPickerViewModel;
import edu.mit.ll.nics.android.utils.ExtensionsKt;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.CheckPermissions.hasLocationPermissions;
import static edu.mit.ll.nics.android.utils.MapUtils.createMarker;
import static edu.mit.ll.nics.android.utils.Utils.popBackStack;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_LOCATION_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class LocationSelectorDialog extends AppDialog implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerDragListener, GoogleMap.OnMyLocationButtonClickListener {

    private GoogleMap mMap;
    private Marker mMarker;
    private NavController mNavController;
    private SupportMapFragment mMapFragment;
    private DialogLocationSelectorBinding mBinding;

    @Inject
    ServiceManager mServiceManager;

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
     * nics/src/main/res/layout/dialog_location_selector.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link SymbolPickerViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = NavHostFragment.findNavController(this);
        mBinding.setLifecycleOwner(mLifecycleOwner);

        initMap();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_location_selector, null, false);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.select_location))
                .setIcon(R.drawable.nics_logo)
                .setPositiveButton(R.string.button_submit, (dialog, which) -> submit())
                .setNegativeButton(R.string.cancel, (dialog, which) -> cancel())
                .setView(mBinding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        mMapFragment = (SupportMapFragment) mActivity.getSupportFragmentManager().findFragmentById(R.id.map);
        if (mMapFragment != null) {
            mActivity.getSupportFragmentManager().beginTransaction().remove(mMapFragment).commit();
        }
        super.onDestroyView();
    }

    private void initMap() {
        if (mMapFragment == null || mMap == null) {
            mMapFragment = (SupportMapFragment) mActivity.getSupportFragmentManager().findFragmentById(R.id.map);
            try {
                if (mMapFragment != null) {
                    mMapFragment.getMapAsync(this);
                }
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to get map async. ");
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull @NotNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (hasLocationPermissions(mActivity)) {
            mMap.setMyLocationEnabled(true);
        }

        LatLng point = LocationSelectorDialogArgs.fromBundle(getArguments()).getPoint();
        if (point != null) {
            initMarker(point);
        }
    }

    @Override
    public void onMapClick(@NonNull @NotNull LatLng latLng) {
        if (mMarker == null) {
            initMarker(latLng);
        } else {
            mMarker.setPosition(latLng);
        }
    }

    private void initMarker(LatLng latLng) {
        mMarker = mMap.addMarker(createMarker(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
    }

    private void submit() {
        if (mMarker != null) {
            LatLng position = mMarker.getPosition();
            ExtensionsKt.setNavigationResult(mNavController, PICK_LOCATION_REQUEST, position);
            dismiss();
        } else {
            cancel();
        }
    }

    private void cancel() {
        ExtensionsKt.setNavigationResult(mNavController, PICK_LOCATION_REQUEST, null);
        dismiss();
    }

    final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            cancel();
            popBackStack(mNavController);
        }
    };

    @Override
    public void onCancel(@NonNull @NotNull DialogInterface dialog) {
        ExtensionsKt.setNavigationResult(mNavController, PICK_LOCATION_REQUEST, null);
        super.onCancel(dialog);
    }

    @Override
    public void onMarkerDragStart(@NonNull @NotNull Marker marker) {
        mMarker.setPosition(marker.getPosition());
    }

    @Override
    public void onMarkerDrag(@NonNull @NotNull Marker marker) {
        mMarker.setPosition(marker.getPosition());
    }

    @Override
    public void onMarkerDragEnd(@NonNull @NotNull Marker marker) {
        mMarker.setPosition(marker.getPosition());
    }

    @Override
    public boolean onMyLocationButtonClick() {
        try {
            Location location = mServiceManager.getLocationService().getLastLocation();
            mMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        } catch (Exception e) {
            Timber.tag(DEBUG).w("Failed to set marker to location.");
        }
        return false;
    }
}
