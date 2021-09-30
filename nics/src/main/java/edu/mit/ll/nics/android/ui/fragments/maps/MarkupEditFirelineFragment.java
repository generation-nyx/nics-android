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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.TransformerUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.databinding.FragmentMarkupFirelineEditBinding;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.FirelineType;
import edu.mit.ll.nics.android.maps.markup.MarkupFireLine;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.repository.MapRepository;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditFirelineViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditFirelineViewModel.MarkupEditFirelineViewModelFactory;
import edu.mit.ll.nics.android.utils.Utils;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngBounds;
import static edu.mit.ll.nics.android.utils.MapUtils.computeDistance;
import static edu.mit.ll.nics.android.utils.MapUtils.createMarker;
import static edu.mit.ll.nics.android.utils.MapUtils.zoomToFeature;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MarkupEditFirelineFragment extends MarkupEditFeatureFragment {

    private Marker mInfoMarker;
    private MarkupFireLine mMarkup;
    private MarkupEditFirelineViewModel mViewModel;
    private FragmentMarkupFirelineEditBinding mBinding;
    private final HashMap<UUID, Marker> mMarkers = new HashMap<>();

    @Inject
    MarkupEditFirelineViewModelFactory mViewModelFactory;

    @Inject
    MapRepository mRepository;

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_markup_fireline_edit.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_markup_fireline_edit, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MarkupEditFirelineViewModel} for this fragment.
     * Get a reference to the shared {@link MapViewModel}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the view model with the markup feature.
        mMarkup = initMarkup();
        mMarkup.setClickable(false);
        MarkupEditFirelineViewModel.Factory factory = new MarkupEditFirelineViewModel.Factory(mViewModelFactory, mMarkup);
        mViewModel = new ViewModelProvider(this, factory).get(MarkupEditFirelineViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);

        subscribeToModel();
    }

    /**
     * Unbind from all xml layouts and remove the back button pressed callback.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void subscribeToModel() {
        // When the view model's comment changes, update the fireline's comment.
        mViewModel.getComment().observe(mLifecycleOwner, this::setComment);

        // Observe changes in the points and do a diff with what's on the map currently. Then refresh.
        mViewModel.getPoints().observe(mLifecycleOwner, points -> {
            for (EnhancedLatLng point : points) {
                if (!mMarkers.containsKey(point.getUUID())) {
                    addToMap(point);
                }
            }

            ArrayList<UUID> uuids = new ArrayList<>(CollectionUtils.collect(points,
                    TransformerUtils.invokerTransformer("getUUID")));

            ArrayList<UUID> toRemove = new ArrayList<>(CollectionUtils.subtract(mMarkers.keySet(), uuids));

            for (UUID uuid : toRemove) {
                removeFromMap(uuid);
            }

            refreshMarkup();
        });

        // Observe when the map marker has changed and update the text fields accordingly.
        mViewModel.getSelectedPoint().observe(mLifecycleOwner, point -> {
            if (point == null) {
                mViewModel.clearTextFields();
            } else {
                if (point.getTrigger().equals(MAP)) {
                    mViewModel.setTextFields(point.getLatLng());
                }

                for (Marker marker : mMarkers.values()) {
                    UUID uuid = (UUID) marker.getTag();
                    if (uuid != null) {
                        if (uuid.equals(point.getUUID())) {
                            marker.setIcon(markerYellowDescriptor);
                        } else {
                            marker.setIcon(markerBlueDescriptor);
                        }
                    }
                }
            }
        });

        // Observe if the user selected a fireline type and update the fireline to reflect the change.
        mViewModel.getFirelineType().observe(mLifecycleOwner, this::setDashStyle);

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

        // Toggle the measurement info marker.
        mViewModel.isMeasuring().observe(mLifecycleOwner, measuring -> {
            if (measuring) {
                createMarkerInfoWindow();
            } else {
                removeInfoWindow();
            }
        });
    }

    private void refreshMarkup() {
        mMarkup.setPoints(mViewModel.getLatLngs());
        mMarkup.addToMap();

        if (mViewModel.isMeasuring().getValue()) {
            updateInfoWindow();
        }
    }

    public void addToMap(EnhancedLatLng point) {
        try {
            Marker marker = mMap.addMarker(createMarker(point.getLatLng()));
            Objects.requireNonNull(marker).setTag(point.getUUID());

            // Default to blue marker.
            marker.setIcon(markerBlueDescriptor);

            mMarkers.put(point.getUUID(), marker);

            mViewModel.setSelectedPoint(point);
        } catch (Exception ignored) {
        }
    }

    private void removeFromMap(UUID uuid) {
        try {
            Marker marker = mMarkers.get(uuid);
            Objects.requireNonNull(marker).remove();
            mMarkers.remove(uuid);

            if (mViewModel.getPoints().getValue().size() > 0) {
                ArrayList<EnhancedLatLng> points = mViewModel.getPoints().getValue();
                mViewModel.setSelectedPoint(points.get(points.size() - 1));
            } else {
                mViewModel.setSelectedPoint(null);
            }
        } catch (Exception ignored) {
        }
    }

    private void moveMarker(EnhancedLatLng latLng) {
        mViewModel.movePoint(latLng);
        mViewModel.setSelectedPoint(latLng);
        refreshMarkup();
    }

    private void moveMarker(Marker marker) {
        EnhancedLatLng latLng = new EnhancedLatLng((UUID) marker.getTag(), marker.getPosition(), LiveDataTrigger.MAP);
        moveMarker(latLng);
    }

    public void removeSelectedPoint() {
        EnhancedLatLng selectedPoint = mViewModel.getSelectedPoint().getValue();

        if (selectedPoint != null) {
            mViewModel.removePoint(selectedPoint);
        }
    }

    private void removeInfoWindow() {
        if (mInfoMarker != null) {
            mInfoMarker.remove();
            mInfoMarker = null;
        }
    }

    private void updateInfoWindow() {
        removeInfoWindow();
        createMarkerInfoWindow();
    }

    private void createMarkerInfoWindow() {
        ArrayList<LatLng> points = mViewModel.getLatLngs();
        if (points.size() > 1) {
            LatLng midPoint = getLatLngBounds(points).getCenter();

            double measurement;
            String property;

            measurement = computeDistance(points, mSettings.getSelectedSystemOfMeasurement());
            property = getString(R.string.markup_distance);

            JsonObject attr = new JsonObject();
            try {
                attr.addProperty("icon", R.drawable.line_black);
                attr.addProperty(property, measurement);
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to add attributes to info window.");
            }

            MarkerOptions options = new MarkerOptions()
                    .alpha(0.0f)
                    .infoWindowAnchor(0.6f, 1.0f)
                    .position(midPoint)
                    .title(attr.toString());
            mInfoMarker = mMap.addMarker(options);
            if (mInfoMarker != null) {
                mInfoMarker.showInfoWindow();
            }
        }
    }

    public void setComment(String comment) {
        if (mMarkup != null) {
            mMarkup.setComments(comment);
        }
    }

    public void setDashStyle(FirelineType type) {
        if (mMarkup != null) {
            mMarkup.setDashStyle(type.getType());
        }
    }

    private void updateMap() {
        try {
            // Gather the text field values.
            String latitude = Objects.requireNonNull(mViewModel.getLatitude().getValue()).getData();
            String longitude = Objects.requireNonNull(mViewModel.getLongitude().getValue()).getData();

            // If the text is empty, the whole coordinate with be invalid, so throw an exception.
            if (Utils.multiEmptyCheck(latitude, longitude)) {
                throw new IllegalArgumentException();
            }

            // Create a LatLng object from the text fields.
            LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

            // Add/move the marker on the map depending on the new coordinate value.
            EnhancedLatLng point = mViewModel.getSelectedPoint().getValue();
            Objects.requireNonNull(point).setTrigger(INPUT);
            point.setLatLng(latLng);
            moveMarker(point);
            Objects.requireNonNull(mMarkers.get(point.getUUID())).setPosition(latLng);
        } catch (Exception e) {
            Snackbar.make(requireView(), "No feature to zoom to.", Snackbar.LENGTH_SHORT).show();
        }
    }

    public void zoom() {
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(mViewModel.getLatLngs()), 200));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected MarkupFireLine initMarkup() {
        long id = MarkupEditLineFragmentArgs.fromBundle(getArguments()).getId();
        if (id != -1L) {
            MarkupFeature feature = mRepository.getMarkupFeatureById(id);
            mMapViewModel.setEditingMarkupId(id);
            zoomToFeature(mMap, feature);
            return new MarkupFireLine(mMap, mPreferences, mActivity, feature);
        } else {
            return new MarkupFireLine(mMap, mPreferences, mActivity);
        }
    }

    @Override
    public void onMapClick(@NotNull LatLng latLng) {
        mViewModel.addPoint(new EnhancedLatLng(UUID.randomUUID(), latLng, MAP));
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        moveMarker(marker);
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        moveMarker(marker);
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        moveMarker(marker);
    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        Object tag = marker.getTag();
        if (tag != null && tag instanceof UUID) {
            UUID uuid = (UUID) tag;

            ArrayList<EnhancedLatLng> points = mViewModel.getPoints().getValue();
            for (EnhancedLatLng point : points) {
                if (point.getUUID().equals(uuid)) {
                    mViewModel.setSelectedPoint(point);
                }
            }
        }
        return true;
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {
        if (mViewModel.getPoints().getValue().size() > 0) {
            showClearDialog();
        }
    }

    @Override
    public void myLocation() {
        if (Double.isNaN(mPreferences.getMDTLatitude()) || Double.isNaN(mPreferences.getMDTLongitude())) {
            Snackbar.make(requireView(), getString(R.string.no_gps_position), Snackbar.LENGTH_SHORT).show();
        } else {
            LatLng latLng = new LatLng(mPreferences.getMDTLatitude(), mPreferences.getMDTLongitude());
            mViewModel.addPoint(new EnhancedLatLng(UUID.randomUUID(), latLng, MAP));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }
    }

    @Override
    protected void clearMap() {
        mViewModel.clearPoints();
        for (Marker marker : mMarkers.values()) {
            marker.remove();
        }
        mMarkup.removeFromMap();
        removeInfoWindow();
    }

    @Override
    public void submit() {
        submit(mMarkup);
    }

    @Override
    public String getType() {
        return String.valueOf(MarkupType.sketch);
    }
}
