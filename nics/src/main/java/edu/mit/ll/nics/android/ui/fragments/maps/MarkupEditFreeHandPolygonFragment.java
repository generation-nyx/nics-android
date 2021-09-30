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
import android.graphics.Point;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentMarkupFreeHandPolygonEditBinding;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.maps.markup.MarkupPolygon;
import edu.mit.ll.nics.android.maps.markup.MarkupSegment;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditFreeHandPolygonViewModel;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.ColorUtils.strokeToFillColors;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngBounds;
import static edu.mit.ll.nics.android.utils.MapUtils.computeArea;
import static edu.mit.ll.nics.android.utils.MapUtils.computeDistance;
import static edu.mit.ll.nics.android.utils.MapUtils.createInfoMarker;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_COLOR_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MarkupEditFreeHandPolygonFragment extends MarkupEditFeatureFragment {

    private View mMapView;
    private Marker mInfoMarker;
    private MarkupPolygon mMarkup;
    private MarkupSegment mPolyline;
    private MarkupEditFreeHandPolygonViewModel mViewModel;
    private FragmentMarkupFreeHandPolygonEditBinding mBinding;

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_markup_free_hand_polygon_edit.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_markup_free_hand_polygon_edit, container, false);
        mMapView = container.getRootView().findViewById(R.id.map_container);
        resetListener();
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MarkupEditFreeHandPolygonViewModel} for this fragment.
     * Get a reference to the shared {@link MapViewModel}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMarkup = initMarkup();
        mMarkup.setClickable(false);
        mViewModel = new ViewModelProvider(this).get(MarkupEditFreeHandPolygonViewModel.class);

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

    @Override
    public void onDestroy() {
        removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_COLOR_REQUEST);
        super.onDestroy();
    }

    private void subscribeToModel() {
        // Listen for responses from the color picker dialog and update the view model's color.
        subscribeToDestinationResponse(R.id.markupEditFreeHandPolygonFragment, PICK_COLOR_REQUEST, (DestinationResponse<Integer>) color -> {
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

        // When drawing mode is on, the user can draw on the map, if not they can use normal map gestures.
        mViewModel.isDrawingMode().observe(mLifecycleOwner, isDrawingMode -> {
            if (isDrawingMode) {
                turnOnDrawingMode();
            } else {
                turnOffDrawingMode();
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

    public void showColorPicker() {
        navigateSafe(mNavController, MarkupEditFreeHandPolygonFragmentDirections.openColorPicker());
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
        List<LatLng> points = mMarkup.getPoints();
        if (points.size() > 1) {
            LatLng midPoint = getLatLngBounds(points).getCenter();

            double measurement;
            String property;

            if (points.size() > 2) {
                measurement = computeArea(points, mSettings.getSelectedSystemOfMeasurement());
                property = getString(R.string.markup_area);
            } else {
                measurement = computeDistance(points, mSettings.getSelectedSystemOfMeasurement());
                property = getString(R.string.markup_distance);
            }

            JsonObject attr = new JsonObject();
            try {
                attr.addProperty("icon", R.drawable.trapezoid_black);
                attr.addProperty(property, measurement);
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to add attributes to info window.");
            }

            mInfoMarker = mMap.addMarker(createInfoMarker(midPoint, attr));
            if (mInfoMarker != null) {
                mInfoMarker.showInfoWindow();
            }
        }
    }

    public void clear() {
        if (mMarkup.getPoints().size() > 0) {
            showClearDialog();
        }
    }

    public void setComment(String comment) {
        if (mMarkup != null) {
            mMarkup.setComments(comment);
        }
    }

    public void setStrokeWidth(Float strokeWidth) {
        if (mMarkup != null) {
            mMarkup.setStrokeWidth(strokeWidth);
        }

        if (mPolyline != null) {
            mPolyline.setStrokeWidth(strokeWidth);
        }
    }

    public void setColor(Integer color) {
        if (mMarkup != null) {
            int[] argb = new int[]{255, Color.red(color), Color.green(color), Color.blue(color)};
            mMarkup.setStrokeColor(argb);
            mMarkup.setFillColor(strokeToFillColors(argb));
        }

        if (mPolyline != null) {
            int[] argb = new int[]{255, Color.red(color), Color.green(color), Color.blue(color)};
            mPolyline.setStrokeColor(argb);
            mPolyline.setFillColor(strokeToFillColors(argb));
        }
    }

    public void zoom() {
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(mMarkup.getPoints()), 200));
        } catch (Exception ignored) {
            Snackbar.make(requireView(), "No feature to zoom to.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void turnOffDrawingMode() {
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        resetListener();
        Snackbar.make(requireView(), "Drawing mode disabled.", Snackbar.LENGTH_SHORT).show();
    }

    private void turnOnDrawingMode() {
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
        setDrawListener();
        Snackbar.make(requireView(),"Drawing mode enabled.", Snackbar.LENGTH_SHORT).show();
    }

    private void resetListener() {
        if (mMapView != null) {
            mMapView.setOnTouchListener((v, event) -> {
                v.performClick();
                return false;
            });
        }
    }

    private void setDrawListener() {
        if (mMapView != null) {
            mMapView.setOnTouchListener((v, event) -> {
                v.performClick();
                return drawOnMap(v, event);
            });
        }
    }

    private boolean drawOnMap(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Point p = new Point();
                p.x = (int) event.getX();
                p.y = (int) event.getY();
                LatLng coordinate = mMap.getProjection().fromScreenLocation(p);
                mMarkup.removeFromMap();

                mMarkup = initMarkup();

                mPolyline.addPoint(coordinate);
                mPolyline.addToMap();

                //apply preset values
                setColor(mViewModel.getColor().getValue());
                setStrokeWidth(mViewModel.getStrokeWidth().getValue());
                break;
            case MotionEvent.ACTION_MOVE:
                Point point = new Point();
                point.x = (int) event.getX();
                point.y = (int) event.getY();
                LatLng pointOnMap = mMap.getProjection().fromScreenLocation(point);
                mPolyline.addPoint(pointOnMap);
                Timber.tag(DEBUG).i("Point drawn: %s", pointOnMap);
                break;
            case MotionEvent.ACTION_UP:
                mViewModel.setIsDrawingMode(false);
                v.performClick();

                mMarkup.setPoints(new ArrayList<>(mPolyline.getPoints()));
                mMarkup.closeOutPolygon();

                mPolyline.removeFromMap();
                mMarkup.addToMap();
                break;
            case MotionEvent.ACTION_CANCEL:
                mViewModel.setIsDrawingMode(false);
                break;
            default:
                break;
        }

        return mGestureDetector.onTouchEvent(event);
    }

    private final GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    });

    @Override
    protected MarkupPolygon initMarkup() {
        mPolyline = new MarkupSegment(mMap, mPreferences, mActivity);
        return new MarkupPolygon(mMap, mPreferences, mActivity, MarkupType.polygon);
    }

    @Override
    protected void clearMap() {
        mPolyline.removeFromMap();
        mMarkup.removeFromMap();
        removeInfoWindow();
    }

    @Override
    public void submit() {
        if (mMarkup != null) {
            mMarkup.closeOutPolygon();
        }

        submit(mMarkup);
    }

    @Override
    public String getType() {
        return String.valueOf(MarkupType.polygon);
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {
        Timber.tag(DEBUG).d("Overriding onMapLongClick. No Implementation Needed.");
    }

    @Override
    public void myLocation() {
        Timber.tag(DEBUG).d("Overriding myLocation. No Implementation Needed.");
    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        Timber.tag(DEBUG).d("Overriding onMarkerClick. No Implementation Needed.");
        return false;
    }

    @Override
    public void onMapClick(@NotNull LatLng latLng) {
        Timber.tag(DEBUG).d("Overriding onMapClick. No Implementation Needed.");
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        Timber.tag(DEBUG).d("Overriding onMarkerDragStart. No Implementation Needed.");
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        Timber.tag(DEBUG).d("Overriding onMarkerDrag. No Implementation Needed.");
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        Timber.tag(DEBUG).d("Overriding onMarkerDragEnd. No Implementation Needed.");
    }
}
