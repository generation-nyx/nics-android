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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.TouchUtils;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.BitmapUtils.generateText;
import static edu.mit.ll.nics.android.utils.ColorUtils.BLACK;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorToIntArray;
import static edu.mit.ll.nics.android.utils.ColorUtils.parseRGBAColor;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngPoint;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class MarkupText extends MarkupBaseShape {

    private Marker mMarker;
    private Bitmap mMarkerBitmap;
    private final MarkerOptions mMarkerOptions = new MarkerOptions();

    public MarkupText(GoogleMap map, PreferencesRepository preferences, Activity activity) {
        super(map, preferences, activity);

        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setFlat(true);
        setDraggable(true);
        setVisibility(true);
        setFeatureId(String.valueOf(getTime()));
        setType(MarkupType.label);
        setStrokeColor(BLACK);
        setTitle(MarkupType.label.toString());
    }

    public MarkupText(GoogleMap map,
                      PreferencesRepository preferences,
                      Activity activity,
                      MarkupFeature feature,
                      boolean draggable) {
        super(map, preferences, activity);

        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setFlat(true);
        setVisibility(true);
        setDraggable(draggable);

        LatLng coordinate = getLatLngPoint(feature.getGeometryVector2());
        if (coordinate != null) {
            setPoint(coordinate);
        }

        JsonObject attr = new JsonObject();
        Resources resources = mActivity.getResources();
        try {
            attr.addProperty("icon", R.drawable.t);

            String nickname = mPreferences.getUserNickName(feature.getUserName());

            if (!nickname.equals("Unknown User")) {
                attr.addProperty(resources.getString(R.string.markup_user), nickname);
            } else {
                attr.addProperty(resources.getString(R.string.markup_user), feature.getUserName());
            }

            attr.addProperty(resources.getString(R.string.markup_message), feature.getLabelText());
            attr.addProperty(resources.getString(R.string.markup_comment), feature.getAttributes().getComments());
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to parse markup text symbol title.");
        }

        setTitle(attr.toString());
        setId(feature.getId());
        setLabelText(feature.getLabelText());
        setLabelSize(feature.getLabelSize());
        setTitle(feature.getLabelText());
        setType(MarkupType.label);
        setTime(feature.getSeqTime());
        setLastUpdate(feature.getLastUpdate());
        setCreator(feature.getUserName());
        setAttributes(feature.getAttributes());
        setFeatureId(feature.getFeatureId());
        setHazards(feature.getHazards());
        setFeature(feature);

        int fillColor = parseRGBAColor(feature.getFillColor(), 1);
        int[] fillColors = colorToIntArray(fillColor);
        Bitmap textBitmap = generateText(feature.getLabelText(), feature.getLabelSize().intValue() + 12, fillColor, Typeface.DEFAULT);
        setIcon(textBitmap, fillColors);
        setTag(new FeatureTag(feature));
    }

    public void setDraggable(boolean draggable) {
        mMarkerOptions.draggable(draggable);

        if (mMarker != null) {
            mMarker.setDraggable(draggable);
        }
    }

    public void setFlat(boolean flat) {
        mMarkerOptions.flat(flat);

        if (mMarker != null) {
            mMarker.setFlat(flat);
        }
    }

    public void setAnchor(float u, float v) {
        mMarkerOptions.anchor(u, v);

        if (mMarker != null) {
            mMarker.setAnchor(u, v);
        }
    }

    public LatLng getPoint() {
        List<LatLng> points = getPoints();
        return points.size() > 0 ? points.get(0) : null;
    }

    public void setPoint(LatLng coordinate) {
        setPoints(new ArrayList<>(Collections.singletonList(coordinate)));

        mMarkerOptions.position(coordinate);

        if (mMarker != null) {
            mMarker.setPosition(coordinate);
        }
    }

    @Override
    public String getDescription() {
        return getTitle();
    }

    public MarkerOptions getOptions() {
        return mMarkerOptions;
    }

    private void setMarker(Marker marker) {
        mMarker = marker;
    }

    public void addToMap(LatLng latLng) {
        setPoint(latLng);
        addToMap();
    }

    @Override
    public void addToMap() {
        if (mMarker == null) {
            mActivity.runOnUiThread(() -> {
                setMarker(mMap.addMarker(mMarkerOptions));
                setTitle(mMarkerOptions.getTitle());
                setPoint(mMarkerOptions.getPosition());

                if (mMarkerBitmap != null) {
                    setSymbolBitmap(mMarkerBitmap);
                    setIcon(mMarkerBitmap, getStrokeColor());
                }
            });
        }
    }

    public void showInfoWindow() {
        if (mMarker != null) {
            mMarker.showInfoWindow();
        }
    }

    public Marker getMarker() {
        return mMarker;
    }

    public GoogleMap getMap() {
        return mMap;
    }

    public Bitmap getSymbolBitmap() {
        return mMarkerBitmap;
    }

    private void setSymbolBitmap(Bitmap symbolBitmap) {
        mMarkerBitmap = symbolBitmap;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        mMarkerOptions.title(title);

        if(mMarker != null) {
            mMarker.setTitle(title);
        }
    }

    @Override
    public void removeFromMap() {
        if (mMarker != null) {
            mMarker.remove();
            mMarker = null;
            mPoints.clear();
        }
    }

    @Override
    public Bitmap getIcon() {
        return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.t_white);
    }

    @Override
    public void setIcon(Bitmap bitmap, int[] color) {
        setStrokeColor(color);
        setFillColor(color);
        setSymbolBitmap(bitmap);
        mMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));

        if (mMarker != null) {
            mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }
    }

    @Override
    public boolean isTouchedBy(LatLng point) {
        return TouchUtils.isLocationInBox(point, mPoints.get(0),
                getSymbolBitmap().getWidth(),
                getSymbolBitmap().getHeight(),
                mMap.getProjection());
    }

    @Override
    public void setClickable(boolean clickable) {
    }

    @Override
    public void setVisibility(boolean visibility) {

    }

    @Override
    public boolean isValid() {
        return getPoint() != null && getPoints().size() > 0
                && getLabelText() != null && getLabelText().length() > 0;
    }

    @Override
    public String getInvalidString() {
        if (getPoint() == null || getPoints().size() == 0) {
            return "Invalid Text Symbol. Symbols must have one point.";
        }
        if (getLabelText() == null || getLabelText().length() == 0) {
            return "Invalid Text Label. A label is required.";
        }
        return "Invalid Text";
    }
}
