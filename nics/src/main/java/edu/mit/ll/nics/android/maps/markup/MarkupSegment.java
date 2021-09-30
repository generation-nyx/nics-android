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
package edu.mit.ll.nics.android.maps.markup;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.maps.tags.MarkupTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.TouchUtils;

import static edu.mit.ll.nics.android.utils.ColorUtils.BLACK;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorArrayToInt;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorToIntArray;
import static edu.mit.ll.nics.android.utils.ColorUtils.parseRGBAColorArray;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertPointsToLatLng;

public class MarkupSegment extends MarkupBaseShape {

    protected Polyline mPolyline;
    protected final PolylineOptions mPolylineOptions = new PolylineOptions();

    public MarkupSegment(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setFeatureId(String.valueOf(getTime()));
        setType(MarkupType.sketch);
        setTitle(MarkupType.sketch.toString());
        setStrokeWidth(6d);
        setStrokeColor(BLACK);
    }

    public MarkupSegment(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         LayerFeature feature) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setType(MarkupType.sketch);
        setPoints(feature.getCoordinates());
        setStrokeColor(colorToIntArray(feature.getStrokeColor()));
        setStrokeWidth(feature.getStrokeWidth() * 2);
    }

    public MarkupSegment(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         OverlappingLayerFeature feature) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setType(MarkupType.sketch);
        setPoints(feature.getCoordinates());
        setStrokeColor(colorToIntArray(feature.getStrokeColor()));
        setStrokeWidth(feature.getStrokeWidth() * 2);
    }

    public MarkupSegment(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         MarkupFeature feature) {
        super(map, preferences, activity);

        setClickable(true);
        setVisibility(true);
        setId(feature.getId());
        setPoints(convertPointsToLatLng(feature.getGeometryVector2()));
        setTitle(feature.getLabelText());
        setType(MarkupType.sketch);
        setTime(feature.getSeqTime());
        setLastUpdate(feature.getLastUpdate());
        setCreator(feature.getUserName());
        setFeatureId(feature.getFeatureId());
        setAttributes(feature.getAttributes());
        setHazards(feature.getHazards());
        setFeature(feature);
        setStrokeWidth(Math.max(feature.getStrokeWidth(), 1));
        setStrokeColor(parseRGBAColorArray(feature.getStrokeColor()));
        setTag(new FeatureTag(feature));
    }

    public void addPoint(LatLng point) {
        if (mPoints == null) {
            mPoints = new ArrayList<>();
        }

        mPoints.add(point);
        mPolylineOptions.add(point);

        if (mPolyline != null) {
            mPolyline.setPoints(mPoints);
        }
    }

    public boolean isOnMap() {
        return mPolyline != null;
    }

    public PolylineOptions getOptions() {
        return mPolylineOptions;
    }

    public void setPolyline(Polyline polyline) {
        mPolyline = polyline;
    }

    @Override
    public void removeFromMap() {
        if (mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }
        mPoints.clear();
    }

    @Override
    public void addToMap() {
        if (mPolyline == null) {
            mActivity.runOnUiThread(() -> {
                mPolyline = mMap.addPolyline(mPolylineOptions);
                mPolyline.setTag(getTag());
            });
        }
    }

    @Override
    public void setTag(MarkupTag tag) {
        super.setTag(tag);
        if (mPolyline != null) {
            mPolyline.setTag(tag);
        }
    }

    @Override
    public void setIcon(Bitmap symbolBitmap, int[] color) {
        setStrokeColor(color);
    }

    @Override
    public Bitmap getIcon() {
        return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.line);
    }

    @Override
    public void showInfoWindow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStrokeColor(int[] color) {
        super.setStrokeColor(color);
        mPolylineOptions.color(colorArrayToInt(color));

        if (mPolyline != null) {
            mPolyline.setColor(colorArrayToInt(color));
        }
    }

    @Override
    public void setStrokeWidth(double width) {
        super.setStrokeWidth(width);
        mPolylineOptions.width((float) width * 2);

        if (mPolyline != null) {
            mPolyline.setWidth((float) width * 2);
        }
    }

    public void removePoint(int pointIndex) {
        mPoints.remove(pointIndex);
        if (mPoints.size() > 0) {
            mPolyline.setPoints(mPoints);
        }
    }

    public void movePoint(int index, LatLng coordinate) {
        mPoints.set(index, coordinate);
        mPolyline.setPoints(mPoints);
    }

    @Override
    public void setPoints(ArrayList<LatLng> points) {
        super.setPoints(points);

        mPolylineOptions.addAll(points);

        if (mPolyline != null) {
            mPolyline.setPoints(points);
        }
    }

    @Override
    public boolean isTouchedBy(LatLng point) {
        return TouchUtils.isLocationOnPath(point, mPoints, mMap.getProjection());
    }

    @Override
    public void setClickable(boolean clickable) {
        mPolylineOptions.clickable(clickable);

        if (mPolyline != null) {
            mPolyline.setClickable(clickable);
        }
    }

    public void setVisibility(boolean visibility) {
        mPolylineOptions.visible(visibility);

        if (mPolyline != null) {
            mPolyline.setVisible(visibility);
        }
    }

    public void setPattern(List<PatternItem> pattern) {
        mPolylineOptions.pattern(pattern);

        if (mPolyline != null) {
            mPolyline.setPattern(pattern);
        }
    }

    public void setStartCap(Cap cap) {
        mPolylineOptions.startCap(cap);

        if (mPolyline != null) {
            mPolyline.setStartCap(cap);
        }
    }

    public void setEndCap(Cap cap) {
        mPolylineOptions.endCap(cap);

        if (mPolyline != null) {
            mPolyline.setEndCap(cap);
        }
    }

    @Override
    public boolean isValid() {
        return mPoints != null && mPoints.size() > 1;
    }

    @Override
    public String getInvalidString() {
        return "Invalid Polyline. Polyline must have at least 2 points.";
    }
}
