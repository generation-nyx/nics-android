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
package edu.mit.ll.nics.android.ui.fragments;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.WorkInfo;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.MapPanelNavigationDirections;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.databinding.FragmentMapBinding;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.maps.EnhancedLocation;
import edu.mit.ll.nics.android.maps.LocationSegment;
import edu.mit.ll.nics.android.maps.MapMarkupInfoWindowAdapter;
import edu.mit.ll.nics.android.maps.MapStyle;
import edu.mit.ll.nics.android.maps.MapType;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.services.LocationService;
import edu.mit.ll.nics.android.ui.adapters.MapAdapter;
import edu.mit.ll.nics.android.ui.adapters.MapAdapter.MapAdapterFactory;
import edu.mit.ll.nics.android.ui.dialogs.MapStylePickerDialog;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import timber.log.Timber;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NONE;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN;
import static edu.mit.ll.nics.android.utils.CheckPermissions.hasLocationPermissions;
import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToStorage;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.setGraph;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_LOCATION_CHANGED;
import static edu.mit.ll.nics.android.utils.constants.Map.SAVED_CAMERA_POSITION;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

// TODO list:
// - editing panels.
// - long click delete.

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MapFragment extends AppFragment implements OnMapReadyCallback,
        GoogleMap.OnCameraMoveStartedListener {

    public GoogleMap mMap;
    private Marker mInfoMarker;
    private SupportMapFragment mMapFragment;
    private MapViewModel mViewModel;
    private FragmentMapBinding mBinding;
    private MapAdapter mMapAdapter;

    private boolean mEventsRegistered = false;

    private Sensor mRotationSensor;
    private SensorEventListener mSensorEventListener;

    private float mDeclination;
    private float mLastDeclination;
    private final float[] mRotationMatrix = new float[16];

    private LocationService mLocationService;

    private CameraPosition mStartingPosition;

    @Inject
    MapAdapterFactory mMapAdapterFactory;

    @Inject
    SensorManager mSensorManager;

    @Inject
    TrackingLayerRepository mTrackingRepository;

    @Inject
    GeneralMessageRepository mGeneralMessageRepository;

    @Inject
    EODReportRepository mEodReportRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the nav controller for this fragment.
        mNavController = mNavHostFragment.getNavController();

        // Request an update of the most recent map features.
        refresh();

        initSensors();

        initCameraPosition(savedInstanceState);

        Timber.tag(DEBUG).d("Created %s", MapFragment.class.getSimpleName());
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_map.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MapViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setFragment(this);
        mBinding.setViewModel(mViewModel);

        initMap();
    }

    /**
     * Subscribe to the {@link MapViewModel}'s updates provided by the {@link LiveData} by observing
     * the changes.
     */
    private void subscribeToModel() {
        // Update the NICS markup features on the map.
        mViewModel.getMarkupFeatures().observe(mLifecycleOwner, features -> mMapAdapter.setMarkupFeatures(features));

        // Update the hazards on the map.
        mViewModel.getHazards().observe(mLifecycleOwner, hazards -> mMapAdapter.setHazards(hazards));

        // Update the incident room layers on the map.
        mViewModel.getOverlappingRoomLayers().observe(mLifecycleOwner, layers -> mMapAdapter.setOverlappingRoomLayers(layers));

        // Update the tracking layers on the map.
        mViewModel.getTrackingLayers().observe(mLifecycleOwner, trackingLayers -> mMapAdapter.setTrackingLayers(trackingLayers));

        // Update the collabroom layers on the map.
        mViewModel.getCollabroomLayers().observe(mLifecycleOwner, collabroomLayers -> mMapAdapter.setCollabroomLayers(collabroomLayers));

        // Sets whether or not the user is currently editing features.
        mViewModel.isEditing().observe(mLifecycleOwner, isEditing -> mMapAdapter.setEditing(isEditing));

        // Sets what feature the user is editing.
        mViewModel.getEditingMarkupId().observe(mLifecycleOwner, id -> mMapAdapter.setEditingFeature(id));

        // Toggle compass mode.
        mViewModel.isCompassMode().observe(mLifecycleOwner, this::toggleCompassMode);

        // Set the user's selected map style.
        mViewModel.getMapStyle().observe(mLifecycleOwner, this::setMapStyle);

        // Set the user's selected map type.
        mViewModel.getMapType().observe(mLifecycleOwner, this::setMapType);

        // Sets whether or not traffic should be enabled on the map.
        mViewModel.isTrafficEnabled().observe(mLifecycleOwner, this::setIsTrafficEnabled);

        // Sets whether or not the indoor feature is enabled on the map.
        mViewModel.isIndoorEnabled().observe(mLifecycleOwner, this::setIsIndoorEnabled);

        // Sets whether or not buildings are visible on the map.
        mViewModel.isBuildingsEnabled().observe(mLifecycleOwner, this::setIsBuildingsEnabled);

        mViewModel.getDistance().observe(mLifecycleOwner, distance -> Timber.tag(DEBUG).d("Distance changed."));

        mViewModel.getElevation().observe(mLifecycleOwner, elevation -> Timber.tag(DEBUG).d("Elevation changed."));

        mViewModel.getCourse().observe(mLifecycleOwner, course -> Timber.tag(DEBUG).d("Course changed."));

        mViewModel.getDistancePointLiveData().observe(mLifecycleOwner, point -> Timber.tag(DEBUG).d("Distance point changed."));

        mViewModel.getDistancePolyline().observe(mLifecycleOwner, this::setDistancePolyline);

        mViewModel.isShowDistanceCalculatorLiveData().observe(mLifecycleOwner, show -> setDistancePolyline(mViewModel.getDistancePolyline().getValue()));
    }

    private void setDistancePolyline(LocationSegment distancePolyline) {
        if (mViewModel.isShowDistanceCalculator()) {
            mMapAdapter.setDistancePolyline(distancePolyline);
        } else {
            mMapAdapter.removeDistancePolyline();
        }
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mEventsRegistered) {
            registerEvents();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mEventsRegistered) {
            unregisterEvents();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.map, menu);

        AppCompatCheckBox checkBox = (AppCompatCheckBox) menu.findItem(R.id.showDistanceCalculator).getActionView();
        checkBox.setOnCheckedChangeListener((v, isChecked) -> {
            mPreferences.setShowDistanceCalculator(isChecked);
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull @NotNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.showDistanceCalculator).setChecked(mViewModel.isShowDistanceCalculator());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mapStyleOption) {
            openMapStylesPicker();
        } else if (id == R.id.mapOptions) {
            openMapOptionsPicker();
        } else if (id == R.id.mapTrackingLayersOption) {
            openTrackingLayersPicker();
        } else if (id == R.id.mapDataLayersOption) {
            openCollabroomLayersPicker();
        } else if(id == R.id.incidentRoomsOption) {
            openOverlappingRoomLayersPicker();
        } else if (id == R.id.geocodeLocationOption) {
            openGeocodingMenu();
        } else if (id == R.id.refreshMapOption) {
            refresh();
        } else if (id == R.id.snapshot) {
            takeMapSnapshot();
        } else if (id == R.id.distanceCalculator) {
            openDistanceCalculator();
        } else if (id == R.id.showDistanceCalculator) {
            item.setChecked(!item.isChecked());
            mPreferences.setShowDistanceCalculator(item.isChecked());
            removeInfoWindowMarker();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        if (mMap != null) {
            CameraPosition position = mMap.getCameraPosition();
            outState.putParcelable(SAVED_CAMERA_POSITION, position);
        }
        super.onSaveInstanceState(outState);
    }

    private void refresh() {
        mNetworkRepository.refreshMapContent();
        mServiceManager.forceLocationUpdate();
    }

    /**
     * Register all events for this fragment by subscribing them to the {@link LiveDataBus}.
     */
    private void registerEvents() {
        LiveDataBus.subscribe(NICS_LOCATION_CHANGED, mLifecycleOwner, data -> onLocationChanged((Location) data));
        mEventsRegistered = true;
    }

    /**
     * Unregister all events from the {@link LiveDataBus}.
     */
    private void unregisterEvents() {
        LiveDataBus.unregister(NICS_LOCATION_CHANGED);
        mEventsRegistered = false;
    }

    @Override
    public void onCameraMoveStarted(int i) {
        if (i == REASON_GESTURE && mViewModel.getCompassMode()) {
            mSensorManager.unregisterListener(mSensorEventListener);
            mBinding.mapRecenterButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Initializes the map if it hasn't been already. This will call
     * {@link SupportMapFragment#getMapAsync(OnMapReadyCallback)} which will
     * build initialize the map asynchronously.
     *
     * After the map is setup, our OnMapReady callback method will be called to setup everything
     * since it needs to wait for the map to actually be ready before adding to it.
     */
    private void initMap() {
        if (mMapFragment == null || mMap == null) {
            mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.markupMapFragment);
            try {
                if (mMapFragment != null) {
                    mMapFragment.getMapAsync(this);
                }
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to get map async. ");
            }
        }
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
        // Share the map instance with the other fragments using the shared view model.
        mMap = googleMap;
        mViewModel.setMap(mMap);

        mMapAdapter = mMapAdapterFactory.create(mActivity, mLifecycleOwner, mMap, mBinding.getRoot());
        subscribeToModel();

        // Set the navigation for the map panels after the map is ready to make sure the map instance is available.
        initMapPanels();

        // Init map style with the user's last selected style.
        setMapStyle(mPreferences.getMapStyle());

        // Init map type with the user's last selected type.
        setMapType(mPreferences.getMapType());

        // Set location source for MDT tracking and geofencing.
        initLocationSource();
        initMapListeners();
        initMapSettings();
        initCompass();
        initInfoWindowAdapter();
        initZoom();
        initDistancePoint();
    }

    private void initDistancePoint() {
        LatLng incidentCoord = mPreferences.getSelectedIncidentLocation();
        Location incidentLocation = new Location(EMPTY);
        incidentLocation.setLatitude(incidentCoord.latitude);
        incidentLocation.setLongitude(incidentCoord.longitude);
        mViewModel.setDistancePoint(new EnhancedLocation(UUID.randomUUID(), incidentLocation, MAP));
        getElevation();
    }

    private void initCameraPosition(Bundle savedInstanceState) {
        try {
            mStartingPosition = savedInstanceState.getParcelable(SAVED_CAMERA_POSITION);
        } catch (Exception ignored) {
            try {
                Incident incident = mPreferences.getSelectedIncident();
                LatLng pos = new LatLng(incident.getLatitude(), incident.getLongitude());
                mStartingPosition = new CameraPosition.Builder()
                        .target(pos)
                        .zoom(8)
                        .build();
            } catch (Exception e) {
                Timber.tag(DEBUG).d(e, "Couldn't set incident as starting position.");
            }
        }
    }

    private void initZoom() {
        zoomToStartingPosition();
        zoomToHazardBounds();
        zoomToReport();
    }

    private void zoomToStartingPosition() {
        try {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mStartingPosition));
        } catch (Exception e) {
            Timber.tag(DEBUG).d("Failed to zoom to starting position.");
        }
    }

    private void zoomToHazardBounds() {
        try {
            LatLngBounds bounds = MapFragmentArgs.fromBundle(getArguments()).getHazardBounds();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(Objects.requireNonNull(bounds), 200));
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to zoom to hazard bounds.");
        }
    }

    private void zoomToReport() {
        try {
            long generalMessageId = MapFragmentArgs.fromBundle(getArguments()).getGeneralMessageId();
            if (generalMessageId != -1L) {
                mTrackingRepository.setLayerActive(Tracking.GENERAL_MESSAGE);
                GeneralMessage report = mGeneralMessageRepository.getGeneralMessageById(generalMessageId);
                LatLng point = new LatLng(report.getLatitude(), report.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 8));
                return;
            }

            long eodReportId = MapFragmentArgs.fromBundle(getArguments()).getEodReportId();
            if (eodReportId != -1L) {
                mTrackingRepository.setLayerActive(Tracking.EOD);
                EODReport report = mEodReportRepository.getEODReportById(eodReportId);
                LatLng point = new LatLng(report.getLatitude(), report.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 8));
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d(e, "Failed to zoom to report.");
        }
    }

    private void initMapPanels() {
        try {
            boolean isSelectionMode = MapFragmentArgs.fromBundle(getArguments()).getSelectionMode();
            mViewModel.setIsSelectingLocation(isSelectionMode);
            if (isSelectionMode) {
                LatLng point = MapFragmentArgs.fromBundle(getArguments()).getSelectionPoint();
                NavHostFragment mapPanelHost = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.navigation_map_panel);
                setGraph(mapPanelHost, R.navigation.map_panel_navigation, R.id.geocodingFragment, getArguments());
            } else {
                NavHostFragment mapPanelHost = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.navigation_map_panel);
                setGraph(mapPanelHost, R.navigation.map_panel_navigation);
            }
        } catch (NullPointerException e) {
            Timber.tag(DEBUG).e(e, "Failed to load map panels navigation graph.");
        }
    }

    private void initMapListeners() {
        mMap.setOnCameraMoveStartedListener(this);
    }

    private void initInfoWindowAdapter() {
        mMap.setInfoWindowAdapter(new MapMarkupInfoWindowAdapter(mActivity, mLifecycleOwner, mSettings));
    }

    @SuppressLint("MissingPermission")
    private void initLocationSource() {
        if (mSettings.isMDTEnabled() || mSettings.isGeofencingEnabled()) {
            mLocationService = mServiceManager.getLocationService();
            if (mLocationService != null && hasLocationPermissions(mActivity)) {
                mLocationService.setUpdateRate(0);
                mMap.setLocationSource(mLocationService);
                mMap.setMyLocationEnabled(true);
            }
            mServiceManager.forceLocationUpdate();
        }
    }

    /**
     * Register the {@link Sensor#TYPE_ROTATION_VECTOR} sensor with this fragment and start listening
     * to changes in the sensor's value to update the camera view if compass mode is active.
     */
    private void initSensors() {
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR &&
                        mViewModel.getCompassMode() && mLastDeclination != mDeclination) {
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorEvent.values);
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(mRotationMatrix, orientation);
                    float bearing = (float) Math.toDegrees(orientation[0]) + mDeclination;
                    updateCamera(bearing);
                }
            }

            /**
             * Update the map camera's position using the bearing value from the sensor changed event.
             *
             * @param bearing The bearing (the angle in degrees measure from the north) value that
             *                was calculated in the sensor changed callback.
             */
            private void updateCamera(float bearing) {
                if (mMap != null) {
                    mMap.getCameraPosition();
                    CameraPosition oldPos = mMap.getCameraPosition();
                    CameraPosition position = CameraPosition.builder(oldPos).bearing(bearing).build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }

    private void openMapStylesPicker() {
        navigateSafe(mNavController, MapFragmentDirections.mapStylesPicker());
    }

    private void openTrackingLayersPicker() {
        navigateSafe(mNavController, MapFragmentDirections.trackingLayersPicker());
    }

    private void openCollabroomLayersPicker() {
        navigateSafe(mNavController, MapFragmentDirections.collabroomLayersPicker());
    }

    private void openOverlappingRoomLayersPicker() {
        navigateSafe(mNavController, MapFragmentDirections.overlappingRoomLayersPicker());
    }

    private void openGeocodingMenu() {
        try {
            // Open up the geocoding fragment. If it's already open, just skip.
            NavController controller = Navigation.findNavController(mBinding.navigationMapPanel);
            NavDestination destination = controller.getCurrentDestination();
            if (destination == null || destination.getId() != R.id.geocodingFragment) {
                navigateSafe(controller, MapPanelNavigationDirections.geocoding());
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to navigate to geocoding fragment.");
        }
    }

    private void openDistanceCalculator() {
        try {
            // Open up the distance calculator fragment. If it's already open, just skip.
            NavController controller = Navigation.findNavController(mBinding.navigationMapPanel);
            NavDestination destination = controller.getCurrentDestination();
            if (destination == null || destination.getId() != R.id.distanceCalculatorFragment) {
                navigateSafe(controller, MapPanelNavigationDirections.distanceCalculator());
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to navigate to distance calculator fragment.");
        }
    }

    //TODO have these as settings in viewmodel
    private void initMapSettings() {
        mMap.setIndoorEnabled(mViewModel.isIndoorEnabled().getValue());
        mMap.setBuildingsEnabled(mViewModel.isBuildingsEnabled().getValue());
        mMap.setTrafficEnabled(mViewModel.isTrafficEnabled().getValue());
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    public void toggleShowHazards() {
        mPreferences.setShowHazards(!mPreferences.isShowHazards());
    }

    public void copyLocation() {
        Marker selectedMarker = mViewModel.getSelectedMarker().getValue();

        if (selectedMarker != null) {
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            LatLng pos = selectedMarker.getPosition();
            ClipData clip = ClipData.newPlainText("selected_location", pos.latitude + "," + pos.longitude);

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Snackbar.make(requireView(), "Copied Location to Clipboard", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(requireView(), "Failed to copy location.", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Snackbar.make(requireView(), "No Location to Copy", Snackbar.LENGTH_SHORT).show();
        }
    }

    public void incidentFocus() {
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mPreferences.getSelectedIncident().getLatitude(), mPreferences.getSelectedIncident().getLongitude()), 12));
        } catch (Exception e) {
            Snackbar.make(requireView(), "No active incident.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void takeMapSnapshot() {
        if (mMap != null) {
            mMap.snapshot(bitmap -> saveImageToStorage(bitmap, mContext.getContentResolver()));
        } else {
            Snackbar.make(requireView(), "Map is not ready.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initCompass() {
        ImageView compass = mBinding.getRoot().findViewWithTag("GoogleMapCompass");

        GestureDetector gestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;
            }
        });

        compass.setOnTouchListener((v, event) -> {
             if (gestureDetector.onTouchEvent(event)) {
                 mViewModel.toggleCompassMode();
             }
             return true;
        });
    }

    private void toggleCompassMode(boolean isCompassMode) {
        if (isCompassMode) startCompassMode(); else stopCompassMode();
    }

    public void startCompassMode() {
        try {
            mBinding.mapRecenterButton.setVisibility(View.GONE);
            Snackbar.make(requireView(), "Compass Mode Enabled.", Snackbar.LENGTH_SHORT).show();

            Location location = mLocationService.getLastLocation();
            LatLng lastLocation = new LatLng(location.getLatitude(), location.getLongitude());

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLocation, 18);

            mMap.animateCamera(cameraUpdate, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mSensorManager.registerListener(mSensorEventListener,
                            mRotationSensor,
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW);
                }

                @Override
                public void onCancel() {
                    mSensorManager.registerListener(mSensorEventListener,
                            mRotationSensor,
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW);
                }
            });
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Error starting compass mode. ");
        }
    }

    private void stopCompassMode() {
        try {
            mBinding.mapRecenterButton.setVisibility(View.GONE);
            mSensorManager.unregisterListener(mSensorEventListener);
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Stopping compass mode has failed. ");
        }
    }

    private void openMapOptionsPicker() {
        // Create a new popup menu and inflate it with the map options.
        PopupMenu popupMenu = new PopupMenu(mActivity, mActivity.findViewById(R.id.mapOptions));
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.map_options, popupMenu.getMenu());

        // Initialize the current checked map type.
        Menu menu = popupMenu.getMenu();
        switch (mViewModel.getMapTypeValue()) {
            case MAP_TYPE_SATELLITE:
                menu.findItem(R.id.satelliteMapOption).setChecked(true);
                break;
            case MAP_TYPE_HYBRID:
                menu.findItem(R.id.hybridMapOption).setChecked(true);
                break;
            case MAP_TYPE_TERRAIN:
                menu.findItem(R.id.terrainMapOption).setChecked(true);
                break;
            case MAP_TYPE_NONE:
                menu.findItem(R.id.offlineMapOption).setChecked(true);
                break;
            default:
                menu.findItem(R.id.normalMapOption).setChecked(true);
                break;
        }

        // Initialize the map options to be checked or unchecked.
        menu.findItem(R.id.trafficMapOption).setChecked(mViewModel.isTrafficEnabled().getValue());
        menu.findItem(R.id.indoorMapOption).setChecked(mViewModel.isIndoorEnabled().getValue());
        menu.findItem(R.id.buildingsMapOption).setChecked(mViewModel.isBuildingsEnabled().getValue());
        menu.findItem(R.id.mapFullscreen).setChecked(mViewModel.isMapFullscreen().getValue());

        // Set the item click listener to update the view model with the selection.
        popupMenu.setOnMenuItemClickListener(item -> {
            // Update the menu item to toggle the checked option.
            item.setChecked(!item.isChecked());

            // Set the map type depending on what value the user checked.
            if (item.getGroupId() == R.id.mapType) {
                mPreferences.setMapType(MapType.lookUpById(item.getItemId()).getType());
            } else if (item.getGroupId() == R.id.mapOptions) {
                if (item.getItemId() == R.id.trafficMapOption) {
                    mPreferences.setTrafficEnabled(item.isChecked());
                } else if (item.getItemId() == R.id.indoorMapOption) {
                    mPreferences.setIndoorEnabled(item.isChecked());
                } else if (item.getItemId() == R.id.buildingsMapOption) {
                    mPreferences.setBuildingsEnabled(item.isChecked());
                } else if (item.getItemId() == R.id.mapFullscreen) {
                    mPreferences.setMapFullscreen(item.isChecked());
                }
            }

            // Don't close the popup menu when the user makes a selection.
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(getContext()));

            return false;
        });

        popupMenu.show();
    }

    private void setIsTrafficEnabled(Boolean enabled) {
        if (mMap != null && enabled != null) {
            mMap.setTrafficEnabled(enabled);
        }
    }

    private void setIsIndoorEnabled(Boolean enabled) {
        if (mMap != null && enabled != null) {
            mMap.setIndoorEnabled(enabled);
        }
    }

    private void setIsBuildingsEnabled(Boolean enabled) {
        if (mMap != null && enabled != null) {
            mMap.setBuildingsEnabled(enabled);
        }
    }

    private void setMapType(Integer mapType) {
        if (mMap != null && mapType != null) {
            mMap.setMapType(mapType);
        }
    }

    /**
     * Sets the maps style to be the selected style from the {@link MapStylePickerDialog}.
     *
     * The resource will be loaded from the nics/src/main/res/raw/ directory. The styles are the default styles
     * from the Google Maps Styling Wizard at https://mapstyle.withgoogle.com/.
     *
     * @param mapStyle The map style string to use that is of type {@link MapStyle}.
     */
    private void setMapStyle(String mapStyle) {
        if (mMap != null) {
            try {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(mContext, MapStyle.lookUp(mapStyle).getResourceId()));
            } catch (Resources.NotFoundException | NullPointerException e) {
                mMap.setMapStyle(null);
                Timber.tag(DEBUG).i("Failed to load map style resource, loading the default one.");
            }
        }
    }

    public void getElevation() {
        if (mViewModel.getDistanceLocation() != null) {
            LatLng coordinate = new LatLng(mViewModel.getDistanceLocation().getLatitude(), mViewModel.getDistanceLocation().getLongitude());
            subscribeToWorker(mNetworkRepository.getElevation(coordinate), new WorkerCallback() {
                @Override
                public void onSuccess(@NonNull @NotNull WorkInfo workInfo) {
                    // Get the resulting geolocated LatLng if there is one.
                    double elevation = workInfo.getOutputData().getDouble("elevation", -1D);

                    if (elevation != -1D) {
                        try {
                            EnhancedLocation location = mViewModel.getDistancePoint();
                            location.getLocation().setAltitude(elevation);
                            mViewModel.setDistancePoint(location);
                        } catch (Exception ignored) {
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull @NotNull WorkInfo workInfo) {
                }

                @Override
                public void onWorking() {
                }
            });
        }
    }

    public Marker getInfoMarker() {
        return mInfoMarker;
    }

    public void removeInfoWindowMarker() {
        if (mInfoMarker != null) {
            try {
                mInfoMarker.remove();
            } catch (Exception ignored) {
            }
            mInfoMarker = null;
        }
    }

    public void setInfoMarker(MarkerOptions options) {
        removeInfoWindowMarker();
        mInfoMarker = mMap.addMarker(options);
        Objects.requireNonNull(mInfoMarker).showInfoWindow();
    }

    /**
     * Updates the declination values from the location that is passed from the {@link LocationService}.
     *
     * This is called when the location changed event is triggered.
     *
     * @param location The {@link Location} that is passed from the {@link LocationService}.
     */
    private void onLocationChanged(Location location) {
        try {
            if (location != null) {
                GeomagneticField field = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    System.currentTimeMillis()
                );
                mLastDeclination = mDeclination;
                mDeclination = field.getDeclination();

                mViewModel.setUserLocation(location);
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to parse declination in onLocationChangedReceiver.");
        }
    }
}