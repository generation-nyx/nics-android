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

import com.google.gson.JsonObject;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.SphericalUtil;

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
import edu.mit.ll.nics.android.databinding.FragmentMarkupRectangleEditBinding;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.MarkupPolygon;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditRectangleViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditRectangleViewModel.MarkupEditRectangleViewModelFactory;
import edu.mit.ll.nics.android.utils.Utils;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.ColorUtils.strokeToFillColors;
import static edu.mit.ll.nics.android.utils.GeoUtils.calculateGeometryMidpoint;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngBounds;
import static edu.mit.ll.nics.android.utils.MapUtils.computeArea;
import static edu.mit.ll.nics.android.utils.MapUtils.createInfoMarker;
import static edu.mit.ll.nics.android.utils.MapUtils.createMarker;
import static edu.mit.ll.nics.android.utils.MapUtils.zoomToFeature;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_COLOR_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MarkupEditRectangleFragment extends MarkupEditFeatureFragment {

    private Marker mInfoMarker;
    private Marker mMarkerUpperLeft;
    private Marker mMarkerLowerRight;
    private MarkupPolygon mMarkup;
    private MarkupEditRectangleViewModel mViewModel;
    private FragmentMarkupRectangleEditBinding mBinding;

    @Inject
    MarkupEditRectangleViewModelFactory mViewModelFactory;

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_markup_rectangle_edit.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_markup_rectangle_edit, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MarkupEditRectangleViewModel} for this fragment.
     * Get a reference to the shared {@link MapViewModel}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the view model with the markup feature.
        mMarkup = initMarkup();
        mMarkup.setClickable(false);
        MarkupEditRectangleViewModel.Factory factory = new MarkupEditRectangleViewModel.Factory(mViewModelFactory, mMarkup);
        mViewModel = new ViewModelProvider(this, factory).get(MarkupEditRectangleViewModel.class);

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
        subscribeToDestinationResponse(R.id.markupEditRectangleFragment, PICK_COLOR_REQUEST, (DestinationResponse<Integer>) color -> {
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

        mViewModel.getUpperLeft().observe(mLifecycleOwner, ul -> {
            if (ul != null) {
                if (mMarkerUpperLeft == null) {
                    mMarkerUpperLeft = mMap.addMarker(createMarker(ul.getLatLng(), markerBlueDescriptor));
                    if (mMarkerUpperLeft != null) {
                        mMarkerUpperLeft.setTag(ul.getUUID());
                    }
                } else {
                    mMarkerUpperLeft.setPosition(ul.getLatLng());
                }
                // Refresh the markup on the map after setting the marker
                refreshMarkup();
            }
        });

        mViewModel.getLowerRight().observe(mLifecycleOwner, lr -> {
            if (lr != null) {
                if (mMarkerLowerRight == null) {
                    mMarkerLowerRight = mMap.addMarker(createMarker(lr.getLatLng(), markerBlueDescriptor));
                    if (mMarkerLowerRight != null) {
                        mMarkerLowerRight.setTag(lr.getUUID());
                    }
                } else {
                    mMarkerLowerRight.setPosition(lr.getLatLng());
                }
                // Refresh the markup on the map after setting the marker
                refreshMarkup();
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
                        if (mMarkerUpperLeft != null) {
                            markers.add(mMarkerUpperLeft);
                        }
                        if (mMarkerLowerRight != null) {
                            markers.add(mMarkerLowerRight);
                        }

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
            if (marker.equals(mMarkerUpperLeft)) {
                mViewModel.setSelectedPoint(mViewModel.getUpperLeft().getValue());
            } else {
                mViewModel.setSelectedPoint(mViewModel.getLowerRight().getValue());
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
            mViewModel.updatePoint(latLng, INPUT);
        } catch (Exception e) {
            clearMarkup();
            mViewModel.setSelectedPoint(new EnhancedLatLng(UUID.randomUUID(), null, INPUT));
        }
    }

    public void showColorPicker() {
        navigateSafe(mNavController, MarkupEditRectangleFragmentDirections.openColorPicker());
    }

    public void zoom() {
        try {
            if (mMarkup != null && mMarkup.getPoints().size() > 1) {
                LatLngBounds bounds = getLatLngBounds(mMarkup.getPoints());
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
            }
        } catch (Exception e) {
            Snackbar.make(requireView(), "No feature to zoom to.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void refreshMarkup() {
        if (mMarkerUpperLeft != null && mMarkerLowerRight != null) {
            LatLng ul = mMarkerUpperLeft.getPosition();
            LatLng lr = mMarkerLowerRight.getPosition();

            LatLng center = calculateGeometryMidpoint(Arrays.asList(ul, lr));
            double heading = SphericalUtil.computeHeading(ul, center);
            double distance = SphericalUtil.computeDistanceBetween(ul, center);

            LatLng ur = SphericalUtil.computeOffset(center, distance, heading + 90);
            LatLng ll = SphericalUtil.computeOffset(center, distance, heading - 90);

            mMarkup.setPoints(new ArrayList<>(Arrays.asList(ul, ur, lr, ll, ul)));
            mMarkup.addToMap();

            if (mViewModel.isMeasuring().getValue()) {
                updateInfoWindow();
            }
        }
    }

    @Override
    protected MarkupPolygon initMarkup() {
        long id = MarkupEditRectangleFragmentArgs.fromBundle(getArguments()).getId();
        if (id != -1L) {
            MarkupFeature feature = mRepository.getMarkupFeatureById(id);
            mMapViewModel.setEditingMarkupId(id);
            zoomToFeature(mMap, feature);
            return new MarkupPolygon(mMap, mPreferences, mActivity, feature);
        } else {
            return new MarkupPolygon(mMap, mPreferences, mActivity, MarkupType.square);
        }
    }

    @Override
    public void myLocation() {
        if (Double.isNaN(mPreferences.getMDTLatitude()) || Double.isNaN(mPreferences.getMDTLongitude())) {
            Snackbar.make(requireView(), getString(R.string.no_gps_position), Snackbar.LENGTH_SHORT).show();
        } else {
            LatLng point = new LatLng(mPreferences.getMDTLatitude(), mPreferences.getMDTLongitude());
            mViewModel.updatePoint(point, MAP);
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
            if (mMarkup != null) {
                List<LatLng> points = mMarkup.getPoints();
                LatLng midPoint = getLatLngBounds(points).getCenter();

                double measurement;
                String property;

                measurement = computeArea(points, mSettings.getSelectedSystemOfMeasurement());
                property = getString(R.string.markup_area);

                JsonObject attr = new JsonObject();
                try {
                    attr.addProperty("icon", R.drawable.rectangle_black);
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
            Timber.tag(DEBUG).d(e, "Failed to create info window for rectangle.");
        }
    }

    private void clearMarkup() {
        removePolygon();
        removeUpperLeft();
        removeLowerRight();
    }

    private void removePolygon() {
        if (mMarkup != null) {
            mMarkup.removeFromMap();
        }
    }

    private void removeUpperLeft() {
        if (mMarkerUpperLeft != null) {
            mMarkerUpperLeft.remove();
            mMarkerUpperLeft = null;
        }
    }

    private void removeLowerRight() {
        if (mMarkerLowerRight != null) {
            mMarkerLowerRight.remove();
            mMarkerLowerRight = null;
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
        mViewModel.updatePoint(latLng, MAP);
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        setSelectedMarker(marker);
        mViewModel.updatePoint(marker.getPosition(), MAP);
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        mViewModel.updatePoint(marker.getPosition(), MAP);
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        mViewModel.updatePoint(marker.getPosition(), MAP);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        setSelectedMarker(marker);
        return false;
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {
        if (mViewModel.getUpperLeft() != null || mViewModel.getLowerRight() != null) {
            showClearDialog();
        }
    }

    @Override
    public String getType() {
        return String.valueOf(MarkupType.square);
    }
}
