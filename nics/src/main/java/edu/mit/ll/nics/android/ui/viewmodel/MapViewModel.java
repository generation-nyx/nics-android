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
package edu.mit.ll.nics.android.ui.viewmodel;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.maps.EnhancedLocation;
import edu.mit.ll.nics.android.maps.LocationSegment;
import edu.mit.ll.nics.android.repository.CollabroomLayerRepository;
import edu.mit.ll.nics.android.repository.HazardRepository;
import edu.mit.ll.nics.android.repository.MapRepository;
import edu.mit.ll.nics.android.repository.OverlappingRoomLayerRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;
import static edu.mit.ll.nics.android.utils.GeoUtils.areLocationsEqual;
import static edu.mit.ll.nics.android.utils.GeoUtils.computeHeading;
import static edu.mit.ll.nics.android.utils.GeoUtils.locationToLatLng;
import static edu.mit.ll.nics.android.utils.MapUtils.computeDistance;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;

@HiltViewModel
public class MapViewModel extends ViewModel {

    private GoogleMap mMap;
    private final LiveData<String> mMapStyle;
    private final LiveData<Integer> mMapType;
    private final NonNullMutableLiveData<Boolean> mTrafficEnabled;
    private final NonNullMutableLiveData<Boolean> mIndoorEnabled;
    private final NonNullMutableLiveData<Boolean> mBuildingsEnabled;
    private final NonNullMutableLiveData<Boolean> mFullscreenMap;
    private final LiveData<List<Hazard>> mHazards;
    private final LiveData<List<Tracking>> mTrackingLayers;
    private final LiveData<List<CollabroomDataLayer>> mCollabroomLayers;
    private final LiveData<List<OverlappingRoomLayer>> mOverlappingRoomLayers;
    private final MutableLiveData<List<MarkupFeature>> mMarkupFeatures;
    private final MutableLiveData<Marker> mSelectedMarker = new MutableLiveData<>(null);
    private final NonNullMutableLiveData<Boolean> mShowHazards;
    private final NonNullMutableLiveData<Boolean> mIsCompassMode = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Boolean> mIsEditing = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Boolean> mIsSelectingLocation = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Long> mEditingMarkupId = new NonNullMutableLiveData<>(-1L);
    private final MediatorLiveData<Float> mMapHeightPercentage = new MediatorLiveData<>();
    private final MediatorLiveData<List<Hazard>> mHazardMediator = new MediatorLiveData<>();
    private final NonNullMutableLiveData<Boolean> mShowDistanceCalculator;
    private final MutableLiveData<Location> mUserLocation = new MutableLiveData<>(null);
    private final MutableLiveData<EnhancedLocation> mDistancePoint = new MutableLiveData<>(null);
    private final MediatorLiveData<LocationSegment> mDistancePolyline = new MediatorLiveData<>();
    private final MediatorLiveData<Double> mDistance = new MediatorLiveData<>();
    private final MediatorLiveData<Double> mElevation = new MediatorLiveData<>();
    private final MediatorLiveData<Double> mCourse = new MediatorLiveData<>();

    @Inject
    public MapViewModel(MapRepository mapRepository,
                        HazardRepository hazardRepository,
                        PreferencesRepository preferences,
                        TrackingLayerRepository trackingLayerRepository,
                        OverlappingRoomLayerRepository overlappingRoomLayerRepository,
                        CollabroomLayerRepository collabroomLayerRepository) {
        // TODO need mediator live data for if the selected collabroom changes ?
        long collabroomId = preferences.getSelectedCollabroomId();
        long incidentId = preferences.getSelectedIncidentId();

        mMarkupFeatures = mapRepository.getMarkupFeaturesLiveData(collabroomId);
        mHazards = hazardRepository.getHazardsLiveData(collabroomId);
        mTrackingLayers = trackingLayerRepository.getTrackingLayers();
        mCollabroomLayers = collabroomLayerRepository.getCollabroomLayersLiveData(collabroomId);
        mOverlappingRoomLayers = overlappingRoomLayerRepository.getOverlappingLayersLiveData(incidentId, collabroomId);
        mMapStyle = preferences.getMapStyleLiveData();
        mMapType = preferences.getMapTypeLiveData();
        mIndoorEnabled = preferences.isIndoorEnabledLiveData();
        mTrafficEnabled = preferences.isTrafficEnabledLiveData();
        mBuildingsEnabled = preferences.isBuildingsEnabledLiveData();
        mFullscreenMap = preferences.isMapFullscreenLiveData();
        mShowHazards = preferences.isShowHazardsLiveData();
        mShowDistanceCalculator = preferences.isShowDistanceCalculatorLiveData();

        mMapHeightPercentage.addSource(mFullscreenMap, isFullscreen -> mMapHeightPercentage.postValue(isFullscreen ? 0.99999f : 0.55f));

        mHazardMediator.addSource(mHazards, hazards -> refreshHazards());
        mHazardMediator.addSource(mShowHazards, isShowHazards -> refreshHazards());

        // Mediator to update the distance polyline if the user's location or the distance point changes.
        mDistancePolyline.addSource(mUserLocation, userLocation -> refreshDistancePolyline());
        mDistancePolyline.addSource(mDistancePoint, distancePoint -> refreshDistancePolyline());

        // Maintain a distance value from the distance polyline.
        mDistance.addSource(mDistancePolyline, this::calculateDistance);

        // Maintain a elevation value from the distance polyline.
        mElevation.addSource(mDistancePolyline, this::calculateElevation);

        // Maintain a course value from the distance polyline.
        mCourse.addSource(mDistancePolyline, this::calculateCourse);
    }

    private void calculateDistance(LocationSegment distancePolyline) {
        try {
            LatLng start = locationToLatLng(distancePolyline.getStartLocation());
            LatLng end = locationToLatLng(distancePolyline.getEndLocation());
            mDistance.setValue(computeDistance(Arrays.asList(start, end)));
        } catch (Exception e) {
            mDistance.setValue(null);
        }
    }

    private void calculateElevation(LocationSegment distancePolyline) {
        try {
            double startAltitude = distancePolyline.getStartLocation().getAltitude();
            double endAltitude = distancePolyline.getEndLocation().getAltitude();

            double diff = Math.abs(startAltitude - endAltitude);
            mElevation.setValue(diff);
        } catch (Exception e) {
            mElevation.setValue(null);
        }
    }

    private void calculateCourse(LocationSegment distancePolyline) {
        try {
            LatLng from = locationToLatLng(distancePolyline.getStartLocation());
            LatLng to = locationToLatLng(distancePolyline.getEndLocation());
            double course = computeHeading(from, to);
            mCourse.setValue(course);
        } catch (Exception e) {
            mCourse.setValue(null);
        }
    }

    private void refreshHazards() {
        boolean isShowHazards = mShowHazards.getValue();
        List<Hazard> hazards = mHazards.getValue();

        if (isShowHazards) {
            mHazardMediator.postValue(hazards);
        } else {
            mHazardMediator.postValue(null);
        }
    }

    private void refreshDistancePolyline() {
        Location userLocation = mUserLocation.getValue();
        EnhancedLocation distancePoint = mDistancePoint.getValue();

        // If both points are set, update the distance polyline on the map. Otherwise, remove it from the map.
        if (userLocation != null && distancePoint != null && distancePoint.getLocation() != null) {
            mDistancePolyline.setValue(new LocationSegment(userLocation, distancePoint.getLocation()));
        } else {
            mDistancePolyline.setValue(null);
        }
    }

    public GoogleMap getMap() {
        return mMap;
    }

    public void setMap(GoogleMap map) {
        mMap = map;
    }

    public LiveData<List<MarkupFeature>> getMarkupFeatures() {
        return mMarkupFeatures;
    }

    public LiveData<List<Hazard>> getHazards() {
        return mHazardMediator;
    }

    public LiveData<LocationSegment> getDistancePolyline() {
        return mDistancePolyline;
    }

    public LiveData<List<Hazard>> getHazardsList() {
        return mHazards;
    }

    public LiveData<Marker> getSelectedMarker() {
        return mSelectedMarker;
    }

    public void setSelectedMarker(Marker marker) {
        mSelectedMarker.postValue(marker);
    }

    public LiveData<String> getMapStyle() {
        return mMapStyle;
    }

    public LiveData<Integer> getMapType() {
        return mMapType;
    }

    public int getMapTypeValue() {
        return getValueOrDefault(mMapType.getValue(), MAP_TYPE_NORMAL);
    }

    public NonNullMutableLiveData<Boolean> isTrafficEnabled() {
        return mTrafficEnabled;
    }

    public NonNullMutableLiveData<Boolean> isIndoorEnabled() {
        return mIndoorEnabled;
    }

    public NonNullMutableLiveData<Boolean> isBuildingsEnabled() {
        return mBuildingsEnabled;
    }

    public NonNullMutableLiveData<Boolean> isMapFullscreen() {
        return mFullscreenMap;
    }

    public NonNullMutableLiveData<Boolean> isShowHazards() {
        return mShowHazards;
    }

    public NonNullMutableLiveData<Boolean> isShowDistanceCalculatorLiveData() {
        return mShowDistanceCalculator;
    }

    public Boolean isShowDistanceCalculator() {
        return mShowDistanceCalculator.getValue();
    }

    public LiveData<List<Tracking>> getTrackingLayers() {
        return mTrackingLayers;
    }

    public LiveData<List<CollabroomDataLayer>> getCollabroomLayers() {
        return mCollabroomLayers;
    }

    public LiveData<List<OverlappingRoomLayer>> getOverlappingRoomLayers() {
        return mOverlappingRoomLayers;
    }

    public NonNullMutableLiveData<Boolean> isCompassMode() {
        return mIsCompassMode;
    }

    public void setCompassMode(boolean compassMode) {
        mIsCompassMode.postValue(compassMode);
    }

    public boolean getCompassMode() {
        return mIsCompassMode.getValue();
    }

    public void toggleCompassMode() {
        mIsCompassMode.postValue(!mIsCompassMode.getValue());
    }

    public NonNullMutableLiveData<Boolean> isEditing() {
        return mIsEditing;
    }

    public void setIsEditing(boolean isEditing) {
        mIsEditing.postValue(isEditing);
    }

    public NonNullMutableLiveData<Boolean> isSelectingLocation() {
        return mIsSelectingLocation;
    }

    public void setIsSelectingLocation(boolean isSelectingLocation) {
        mIsSelectingLocation.postValue(isSelectingLocation);
    }

    public NonNullMutableLiveData<Long> getEditingMarkupId() {
        return mEditingMarkupId;
    }

    public void setEditingMarkupId(long id) {
        mEditingMarkupId.postValue(id);
    }

    public LiveData<Float> getMapHeightPercentage() {
        return mMapHeightPercentage;
    }

    public void setMapHeightPercentage(float percent) {
        mMapHeightPercentage.postValue(percent);
    }

    public LiveData<Location> getUserLocationLiveData() {
        return mUserLocation;
    }

    public void setUserLocation(Location location) {
        if (!areLocationsEqual(location, mUserLocation.getValue())) {
            mUserLocation.setValue(location);
        }
    }

    public Location getUserLocation() {
        return mUserLocation.getValue();
    }

    public LiveData<EnhancedLocation> getDistancePointLiveData() {
        return mDistancePoint;
    }

    public LiveData<Double> getDistance() {
        return mDistance;
    }

    public void setDistancePoint(EnhancedLocation location) {
        mDistancePoint.setValue(location);
    }

    public EnhancedLocation getDistancePoint() {
        return mDistancePoint.getValue();
    }

    public Location getDistanceLocation() {
        if (mDistancePoint.getValue() != null) {
            return mDistancePoint.getValue().getLocation();
        } else {
            return null;
        }
    }

    public LiveData<Double> getElevation() {
        return mElevation;
    }

    public LiveData<Double> getCourse() {
        return mCourse;
    }
}
