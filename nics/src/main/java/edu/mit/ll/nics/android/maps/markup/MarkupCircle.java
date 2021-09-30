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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.maps.tags.MarkupTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.TouchUtils;

import static edu.mit.ll.nics.android.utils.ColorUtils.BLACK;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorArrayToInt;
import static edu.mit.ll.nics.android.utils.ColorUtils.parseRGBAColorArray;
import static edu.mit.ll.nics.android.utils.ColorUtils.strokeToFillColors;
import static edu.mit.ll.nics.android.utils.GeoUtils.getCenterPoint;

public class MarkupCircle extends MarkupBaseShape {

    public static final double DEFAULT_RADIUS = 100d;

    private Circle mCircle;
    private LatLng mCenter;
    private final CircleOptions mCircleOptions = new CircleOptions();

    public MarkupCircle(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity) {
        super(map, preferences, activity);

        setVisibility(true);
        setClickable(true);
        setFeatureId(String.valueOf(getTime()));
        setType(MarkupType.circle);
        setStrokeColor(BLACK);
        setFillColor(BLACK);
        setStrokeWidth(6d);
        setTitle(MarkupType.circle.toString());
        setRadius(DEFAULT_RADIUS);
    }

    public MarkupCircle(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity,
                        MarkupFeature feature) {
        super(map, preferences, activity);

        setId(feature.getId());
        setPoint(getCenterPoint(feature.getGeometryVector2()));
        setRadius(feature.getRadius());
        setType(MarkupType.circle);
        setTime(feature.getSeqTime());
        setLastUpdate(feature.getLastUpdate());
        setCreator(feature.getUserName());
        setStrokeColor(parseRGBAColorArray(feature.getStrokeColor(), 1));
        setStrokeWidth(feature.getStrokeWidth());
        setFillColor(parseRGBAColorArray(feature.getFillColor(), feature.getOpacity()));
        setTitle(MarkupType.circle.toString());
        setFeatureId(feature.getFeatureId());
        setAttributes(feature.getAttributes());
        setHazards(feature.getHazards());
        setFeature(feature);
        setClickable(true);
        setVisibility(true);
        setTag(new FeatureTag(feature));
    }

    public CircleOptions getOptions() {
        return mCircleOptions;
    }

    @Override
    public void setTag(MarkupTag tag) {
        super.setTag(tag);
        if (mCircle != null) {
            mCircle.setTag(tag);
        }
    }

    public Circle getCircle() {
        return mCircle;
    }

    public void setCircle(Circle circle) {
        mCircle = circle;
    }

    public void setPoint(LatLng coordinate) {
        if (mPoints == null || mPoints.size() == 0) {
            addPoint(coordinate);
        } else {
            mPoints.set(0, coordinate);
        }

        setCenter(coordinate);
    }

    public boolean isOnMap() {
        return mCircle != null;
    }

    public void addToMap(LatLng point) {
        setPoint(point);
        if (mCircle == null) {
            addToMap();
        }
    }

    public LatLng getCenter() {
        return mCenter;
    }

    public void setCenter(LatLng center) {
        mCenter = center;
        mCircleOptions.center(center);

        if (mCircle != null) {
            mCircle.setCenter(center);
        }
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);
        mCircleOptions.radius(radius);

        if (mCircle != null) {
            mCircle.setRadius(radius);
        }
    }

    @Override
    public void setStrokeWidth(double width) {
        super.setStrokeWidth(width);
        mCircleOptions.strokeWidth((float) width * 2);

        if (mCircle != null) {
            mCircle.setStrokeWidth((float) width * 2);
        }
    }

    @Override
    public void setStrokeColor(int[] color) {
        super.setStrokeColor(color);
        mCircleOptions.strokeColor(colorArrayToInt(color));

        if (mCircle != null) {
            mCircle.setStrokeColor(colorArrayToInt(color));
        }
    }

    @Override
    public void setFillColor(int[] color) {
        int[] fillColor = strokeToFillColors(color);
        super.setFillColor(fillColor);
        mCircleOptions.fillColor(colorArrayToInt(fillColor));

        if (mCircle != null) {
            mCircle.setFillColor(colorArrayToInt(color));
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        mCircleOptions.clickable(clickable);

        if (mCircle != null) {
            mCircle.setClickable(clickable);
        }
    }

    public void setVisibility(boolean visibility) {
        mCircleOptions.visible(visibility);

        if (mCircle != null) {
            mCircle.setVisible(visibility);
        }
    }

    @Override
    public boolean isValid() {
        return getPoints() != null && getPoints().size() == 1 && getRadius() != 0;
    }

    @Override
    public String getInvalidString() {
        return "Invalid Circle. Circle must have two points";
    }

    @Override
    public Bitmap getIcon() {
        return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.circle);
    }

    @Override
    public void setIcon(Bitmap symbolBitmap, int[] color) {
        setStrokeColor(color);
    }

    @Override
    public void addToMap() {
        if (mCircle == null) {
            mActivity.runOnUiThread(() -> {
                setCircle(mMap.addCircle(mCircleOptions));
                setPoint(mCircleOptions.getCenter());
            });
        }
    }

    @Override
    public void removeFromMap() {
        if (mCircle != null) {
            mCircle.remove();
            mCircle = null;
            mPoints.clear();
            setRadius(DEFAULT_RADIUS);
        }
    }

    @Override
    public void showInfoWindow() {
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
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("radius", getRadius());
        return json;
    }
}
