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
import android.graphics.Color;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.databinding.FragmentMarkupCircleEditBinding;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.maps.EnhancedCircle;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.MarkupCircle;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditCircleViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditCircleViewModel.MarkupEditCircleViewModelFactory;
import edu.mit.ll.nics.android.utils.Utils;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.ColorUtils.strokeToFillColors;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngBounds;
import static edu.mit.ll.nics.android.utils.GeoUtils.getPolygonForCircle;
import static edu.mit.ll.nics.android.utils.MapUtils.computeArea;
import static edu.mit.ll.nics.android.utils.MapUtils.createInfoMarker;
import static edu.mit.ll.nics.android.utils.MapUtils.createMarker;
import static edu.mit.ll.nics.android.utils.MapUtils.getRadius;
import static edu.mit.ll.nics.android.utils.MapUtils.getRadiusPoint;
import static edu.mit.ll.nics.android.utils.MapUtils.zoomToFeature;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_COLOR_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MarkupEditCircleFragment extends MarkupEditFeatureFragment {

    private Marker mInfoMarker;
    private Marker mMarkerCenter;
    private Marker mMarkerRadius;
    private MarkupCircle mMarkup;
    private MarkupEditCircleViewModel mViewModel;
    private FragmentMarkupCircleEditBinding mBinding;

    @Inject
    MarkupEditCircleViewModelFactory mViewModelFactory;

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_markup_circle_edit.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_markup_circle_edit, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MarkupEditCircleViewModel} for this fragment.
     * Get a reference to the shared {@link MapViewModel}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the view model with the markup feature.
        mMarkup = initMarkup();
        mMarkup.setClickable(false);
        MarkupEditCircleViewModel.Factory factory = new MarkupEditCircleViewModel.Factory(mViewModelFactory, mMarkup);
        mViewModel = new ViewModelProvider(this, factory).get(MarkupEditCircleViewModel.class);

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

    /**
     * When the fragment is destroyed, remove any state handle fields.
     */
    @Override
    public void onDestroy() {
        removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_COLOR_REQUEST);
        super.onDestroy();
    }

    private void subscribeToModel() {
        // Listen for responses from the color picker dialog and update the view model's color.
        subscribeToDestinationResponse(R.id.markupEditCircleFragment, PICK_COLOR_REQUEST, (DestinationResponse<Integer>) color -> {
            if (color != null) {
                mViewModel.setColor(color);
            }
        });

        // When the view model's color changes, update the polyline's color on the map.
        mViewModel.getColor().observe(mLifecycleOwner, this::setColor);

        // When the view model's stroke width changes, update the polyline's stroke width on the map.
        mViewModel.getStrokeWidth().observe(mLifecycleOwner, this::setStrokeWidth);

        // When the view model's comment changes, update the polyline's comment.
        mViewModel.getComment().observe(mLifecycleOwner, this::setComment);

        mViewModel.getCircle().observe(mLifecycleOwner, circle -> {
            if (circle != null) {
                // If the circle is new, zoom to it after creating it.
                if (mMarkerCenter == null || mMarkerRadius == null) {
                    List<LatLng> points = Arrays.asList(circle.getCenterPoint().getLatLng(), circle.getRadiusPoint().getLatLng());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(points), 200));
                }

                // If the center marker is not on the map yet, add it. Otherwise, move it's location.
                if (mMarkerCenter == null) {
                    mMarkerCenter = mMap.addMarker(createMarker(circle.getCenterPoint().getLatLng(), markerBlueDescriptor));
                    if (mMarkerCenter != null) {
                        mMarkerCenter.setTag(circle.getCenterPoint().getUUID());
                    }
                } else {
                    mMarkerCenter.setPosition(circle.getCenterPoint().getLatLng());
                }

                // If the radius marker is not on the map yet, add it. Otherwise, move it's location.
                if (mMarkerRadius == null) {
                    mMarkerRadius = mMap.addMarker(createMarker(circle.getRadiusPoint().getLatLng(), markerBlueDescriptor));
                    if (mMarkerRadius != null) {
                        mMarkerRadius.setTag(circle.getRadiusPoint().getUUID());
                    }
                } else {
                    mMarkerRadius.setPosition(circle.getRadiusPoint().getLatLng());
                }

                // Refresh the markup on the map after setting the markers.
                refreshMarkup();
            } else {
                clearMap();
            }
        });

        // Observe when the map marker has changed and update the text fields accordingly.
        mViewModel.getSelectedPoint().observe(mLifecycleOwner, point -> {
            if (point == null) {
                clearMap();
            } else {
                // Only update the text fields and markers if the point was selected from the map.
                // The other case is when it is selected because of invalid text fields.
                if (point.getTrigger().equals(MAP)) {
                    mViewModel.setTextFields(point.getLatLng());

                    // Update the selected marker to have the correct icon.
                    try {
                        List<Marker> markers = new ArrayList<>();
                        markers.add(mMarkerCenter);
                        markers.add(mMarkerRadius);

                        // Set the selected marker to yellow and the unselected to blue.
                        for (Marker m : markers) {
                            UUID uuid = (UUID) m.getTag();
                            if (Objects.requireNonNull(uuid).equals(point.getUUID())) {
                                m.setIcon(markerYellowDescriptor);
                            } else {
                                m.setIcon(markerBlueDescriptor);
                            }
                        }
                    } catch (Exception e) {
                        clearMap();
                    }
                }
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

        // Toggle the measurement info marker.
        mViewModel.isMeasuring().observe(mLifecycleOwner, measuring -> {
            if (measuring) {
                createMarkerInfoWindow();
            } else {
                removeInfoWindow();
            }
        });
    }

    public void setSelectedMarker(Marker marker) {
        try {
            if (marker.equals(mMarkerCenter)) {
                mViewModel.setSelectedPoint(Objects.requireNonNull(mViewModel.getCircle().getValue()).getCenterPoint());
            } else {
                mViewModel.setSelectedPoint(Objects.requireNonNull(mViewModel.getCircle().getValue()).getRadiusPoint());
            }
        } catch (Exception ignored) {
        }
    }

    private void setComment(String comment) {
        if (mMarkup != null) {
            mMarkup.setComments(comment);
        }
    }

    private void setColor(Integer color) {
        if (mMarkup != null) {
            int[] argb = new int[]{255, Color.red(color), Color.green(color), Color.blue(color)};
            mMarkup.setStrokeColor(argb);
            mMarkup.setFillColor(strokeToFillColors(argb));
        }
    }

    public void setStrokeWidth(Float strokeWidth) {
        if (mMarkup != null) {
            mMarkup.setStrokeWidth(strokeWidth);
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
            mViewModel.updatePoint(latLng, INPUT, mMarkup.getRadius());
        } catch (Exception e) {
            clearMarkup();
            mViewModel.setSelectedPoint(new EnhancedLatLng(UUID.randomUUID(), null, INPUT));
        }
    }

    public void showColorPicker() {
        navigateSafe(mNavController, MarkupEditCircleFragmentDirections.openColorPicker());
    }

    public void zoom() {
        try {
            List<LatLng> points = new ArrayList<>(mMarkup.getPoints());

            if (points.size() == 1) {
                points.add(getRadiusPoint(points.get(0), mMarkup.getRadius()));
                points.add(getRadiusPoint(points.get(0), mMarkup.getRadius(), 180));
            }

            LatLngBounds bounds = getLatLngBounds(points);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
        } catch (Exception e) {
            Snackbar.make(requireView(), "No feature to zoom to.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void refreshMarkup() {
        try {
            EnhancedCircle circle = mViewModel.getCircle().getValue();
            LatLng centerPoint = Objects.requireNonNull(circle).getCenterPoint().getLatLng();
            LatLng radiusPoint = circle.getRadiusPoint().getLatLng();
            mMarkup.setPoint(centerPoint);
            mMarkup.setRadius(getRadius(centerPoint, radiusPoint));
            mMarkup.addToMap();

            if (mViewModel.isMeasuring().getValue()) {
                updateInfoWindow();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected MarkupCircle initMarkup() {
        long id = MarkupEditCircleFragmentArgs.fromBundle(getArguments()).getId();
        if (id != -1L) {
            MarkupFeature feature = mRepository.getMarkupFeatureById(id);
            mMapViewModel.setEditingMarkupId(id);
            zoomToFeature(mMap, feature);
            return new MarkupCircle(mMap, mPreferences, mActivity, feature);
        } else {
            return new MarkupCircle(mMap, mPreferences, mActivity);
        }
    }

    @Override
    public void myLocation() {
        if (Double.isNaN(mPreferences.getMDTLatitude()) || Double.isNaN(mPreferences.getMDTLongitude())) {
            Snackbar.make(requireView(), getString(R.string.no_gps_position), Snackbar.LENGTH_SHORT).show();
        } else {
            LatLng point = new LatLng(mPreferences.getMDTLatitude(), mPreferences.getMDTLongitude());
            mViewModel.updatePoint(point, MAP, mMarkup.getRadius());
            zoom();
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
        try {
            EnhancedCircle circle = mViewModel.getCircle().getValue();
            if (circle != null) {
                List<LatLng> points = getPolygonForCircle(mMarkup.getCenter(), mMarkup.getRadius());
                LatLng midPoint = getLatLngBounds(points).getCenter();

                double measurement;
                String property;

                measurement = computeArea(points, mSettings.getSelectedSystemOfMeasurement());
                property = getString(R.string.markup_area);

                JsonObject attr = new JsonObject();
                try {
                    attr.addProperty("icon", R.drawable.circle_black);
                    attr.addProperty(property, measurement);
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed to add attributes to info window.");
                }

                mInfoMarker = mMap.addMarker(createInfoMarker(midPoint, attr));
                if (mInfoMarker != null) {
                    mInfoMarker.showInfoWindow();
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d(e, "Failed to create info window for circle.");
        }
    }

    private void clearMarkup() {
        removeCircle();
        removeMarkerRadius();
        removeMarkerCenter();
    }

    private void removeCircle() {
        if (mMarkup != null) {
            mMarkup.removeFromMap();
        }
    }

    private void removeMarkerCenter() {
        if (mMarkerCenter != null) {
            mMarkerCenter.remove();
            mMarkerCenter = null;
        }
    }

    private void removeMarkerRadius() {
        if (mMarkerRadius != null) {
            mMarkerRadius.remove();
            mMarkerRadius = null;
        }
    }

    @Override
    protected void clearMap() {
        mViewModel.clearMap();
        clearMarkup();
    }

    @Override
    public void submit() {
        submit(mMarkup);
    }

    @Override
    public void onMapClick(@NotNull LatLng latLng) {
        mViewModel.updatePoint(latLng, MAP, mMarkup.getRadius());
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        mViewModel.updatePoint(marker.getPosition(), MAP, mMarkup.getRadius());
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        mViewModel.updatePoint(marker.getPosition(), MAP, mMarkup.getRadius());
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        mViewModel.updatePoint(marker.getPosition(), MAP, mMarkup.getRadius());
    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        setSelectedMarker(marker);
        return false;
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {
        if (mViewModel.getCircle() != null) {
            showClearDialog();
        }
    }

    @Override
    public String getType() {
        return String.valueOf(MarkupType.circle);
    }
}
