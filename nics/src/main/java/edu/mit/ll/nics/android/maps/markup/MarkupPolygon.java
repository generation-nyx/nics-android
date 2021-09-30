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

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.maps.tags.MarkupTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.TouchUtils;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.ColorUtils.BLACK;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorArrayToInt;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorToIntArray;
import static edu.mit.ll.nics.android.utils.ColorUtils.parseRGBAColorArray;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertPointsToLatLng;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class MarkupPolygon extends MarkupBaseShape {

    protected Polygon mPolygon;
    protected final PolygonOptions mPolygonOptions  = new PolygonOptions();

    public MarkupPolygon(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity) {
        this(map, preferences, activity, MarkupType.polygon);
    }

    public MarkupPolygon(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         MarkupType type) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setFeatureId(String.valueOf(getTime()));
        setType(type);
        setTitle(type.toString());
        setStrokeWidth(6d);
        setStrokeColor(BLACK);
        setFillColor(BLACK);
    }

    public MarkupPolygon(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         LayerFeature feature) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setType(MarkupType.polygon);
        setPoints(feature.getCoordinates());
        setStrokeColor(colorToIntArray(feature.getStrokeColor()));
        setStrokeWidth(feature.getStrokeWidth() * 2);
        setFillColor(colorToIntArray(feature.getFillColor()));
    }

    public MarkupPolygon(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         OverlappingLayerFeature feature) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setType(MarkupType.polygon);
        setPoints(feature.getCoordinates());
        setStrokeColor(colorToIntArray(feature.getStrokeColor()));
        setStrokeWidth(feature.getStrokeWidth() * 2);
        setFillColor(colorToIntArray(feature.getFillColor()));
    }

    public MarkupPolygon(GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity,
                         MarkupFeature feature) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setId(feature.getId());
        setPoints(convertPointsToLatLng(feature.getGeometryVector2()));
        setTitle(feature.getLabelText());
        setType(MarkupType.valueOf(feature.getType()));
        setTime(feature.getSeqTime());
        setLastUpdate(feature.getLastUpdate());
        setCreator(feature.getUserName());
        setFeatureId(feature.getFeatureId());
        setAttributes(feature.getAttributes());
        setHazards(feature.getHazards());
        setFeature(feature);
        setStrokeWidth(feature.getStrokeWidth());
        setStrokeColor(parseRGBAColorArray(feature.getStrokeColor()));
        setFillColor(parseRGBAColorArray(feature.getStrokeColor(), feature.getOpacity()));
        setTag(new FeatureTag(feature));
    }

    public void addPoint(LatLng point) {
        if (mPoints == null) {
            mPoints = new ArrayList<>();
        }

        mPoints.add(point);
        mPolygonOptions.add(point);

        if (mPolygon != null) {
            mPolygon.setPoints(mPoints);
        }
    }

    public void closeOutPolygon() {
        if (mPoints != null && mPoints.size() > 0) {
            mPoints.add(mPoints.get(0));

            if (mPolygon != null) {
                mPolygon.setPoints(mPoints);
            }
        }
    }

    public PolygonOptions getOptions() {
        return mPolygonOptions;
    }

    public void setPolygon(Polygon polygon) {
        mPolygon = polygon;
    }

    @Override
    public void removeFromMap() {
        if (mPolygon != null) {
            mPolygon.remove();
            mPolygon = null;
            mPoints.clear();
        }
    }

    @Override
    public void setTag(MarkupTag tag) {
        super.setTag(tag);
        if (mPolygon != null) {
            mPolygon.setTag(tag);
        }
    }

    @Override
    public void addToMap() {
        if (mPolygon == null && mPolygonOptions.getPoints().size() > 0) {
            mActivity.runOnUiThread(() -> {
                mPolygon = mMap.addPolygon(mPolygonOptions);
                mPolygon.setTag(getTag());
            });
        }
    }

    @Override
    public void setIcon(Bitmap symbolBitmap, int[] color) {
        setStrokeColor(color);
    }

    @Override
    public Bitmap getIcon() {
        if (getType().equals(MarkupType.square)) {
            return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.rectangle);
        } else if (getType().equals(MarkupType.polygon)) {
            return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.trapezoid);
        } else if (getType().equals(MarkupType.triangle)) {
            return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.triangle);
        } else if (getType().equals(MarkupType.hexagon)) {
            return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.hexagon);
        } else if (getType().equals(MarkupType.circle)) {
            return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.circle);
        } else {
            Timber.tag(DEBUG).e("Unexpected feature type of %s", getType().toString());
            return null;
        }
    }

    @Override
    public void showInfoWindow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStrokeColor(@NonNull int[] color) {
        super.setStrokeColor(color);

        mPolygonOptions.strokeColor(colorArrayToInt(color));

        if (mPolygon != null) {
            mPolygon.setStrokeColor(colorArrayToInt(color));
        }
    }

    @Override
    public void setFillColor(@NonNull int[] color) {
        super.setFillColor(color);

        mPolygonOptions.fillColor(colorArrayToInt(color));

        if (mPolygon != null) {
            mPolygon.setFillColor(colorArrayToInt(color));
        }
    }

    @Override
    public void setStrokeWidth(double width) {
        super.setStrokeWidth(width);
        mPolygonOptions.strokeWidth((float) width * 2);

        if (mPolygon != null) {
            mPolygon.setStrokeWidth((float) width * 2);
        }
    }

    public void removePoint(int pointIndex) {
        mPoints.remove(pointIndex);
        if (mPoints.size() > 0) {
            mPolygon.setPoints(mPoints);
        }
    }

    public void movePoint(int index, LatLng coordinate) {
        mPoints.set(index, coordinate);
        mPolygon.setPoints(mPoints);
    }

    public boolean isOnMap() {
        return mPolygon != null;
    }

    @Override
    public void setPoints(ArrayList<LatLng> points) {
        super.setPoints(points);

        mPolygonOptions.getPoints().clear();
        mPolygonOptions.addAll(points);

        if (mPolygon != null) {
            mPolygon.getPoints().clear();
            mPolygon.setPoints(points);
        }
    }

    @Override
    public boolean isTouchedBy(LatLng point) {
        if (PolyUtil.containsLocation(point, mPoints, true)) {
            return true;
        } else {
            // add fudge factor around the edges
            return TouchUtils.isLocationOnEdge(point, mPoints, mMap.getProjection());
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        mPolygonOptions.clickable(clickable);

        if (mPolygon != null) {
            mPolygon.setClickable(clickable);
        }
    }

    public void setVisibility(boolean visibility) {
        mPolygonOptions.visible(visibility);

        if (mPolygon != null) {
            mPolygon.setVisible(visibility);
        }
    }

    @Override
    public boolean isValid() {
        return mPoints != null && mPoints.size() > 2 && PolyUtil.isClosedPolygon(mPoints);
    }

    @Override
    public String getInvalidString() {
        return String.format("Invalid %1$s. %1$s must have at least 3 points.", this.getType().toString());
    }
}