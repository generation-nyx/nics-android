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
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;

import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.maps.tileproviders.FirelineTileProvider;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.TouchUtils;

import static edu.mit.ll.nics.android.utils.ColorUtils.BLACK;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorStringToIntArray;
import static edu.mit.ll.nics.android.utils.ColorUtils.colorToIntArray;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertPointsToLatLng;

public class MarkupFireLine extends MarkupBaseShape {

    private TileOverlay mTileOverlay;
    private FirelineTileProvider mFirelineTileProvider;

    public MarkupFireLine(GoogleMap map,
                          PreferencesRepository preferences,
                          Activity activity) {
        super(map, preferences, activity);

        setFeatureId(String.valueOf(getTime()));
        setType(MarkupType.sketch);
        setTitle(MarkupType.sketch.toString());
        setDashStyle(FirelineType.PRIMARY_FIRELINE.getType());
        setStroke();
        setDescription();
    }

    public MarkupFireLine(GoogleMap map,
                          PreferencesRepository preferences,
                          Activity activity,
                          LayerFeature feature) {
        super(map, preferences, activity);

        setDashStyle(feature.getDashStyle());
        setStrokeColor(colorToIntArray(feature.getStrokeColor()));
        setStrokeWidth(feature.getStrokeWidth());
        setPoints(feature.getCoordinates());
    }

    public MarkupFireLine(GoogleMap map,
                          PreferencesRepository preferences,
                          Activity activity,
                          OverlappingLayerFeature feature) {
        super(map, preferences, activity);

        setDashStyle(feature.getDashStyle());
        setStrokeColor(colorToIntArray(feature.getStrokeColor()));
        setStrokeWidth(feature.getStrokeWidth());
        setPoints(feature.getCoordinates());
    }

    public MarkupFireLine(GoogleMap map,
                          PreferencesRepository preferences,
                          Activity activity,
                          MarkupFeature feature) {
        super(map, preferences, activity);

        setFeature(feature);
        setId(feature.getId());
        setTitle(feature.getLabelText());
        setType(MarkupType.sketch);
        setTime(feature.getSeqTime());
        setLastUpdate(feature.getLastUpdate());
        setStrokeColor(colorStringToIntArray(feature.getStrokeColor()));
        setCreator(feature.getUserName());
        setAttributes(feature.getAttributes());
        setFeatureId(feature.getFeatureId());
        setHazards(feature.getHazards());
        setDashStyle(feature.getDashStyle());
        setPoints(convertPointsToLatLng(feature.getGeometryVector2()));
        setTag(new FeatureTag(feature));
    }

    @Override
    public void setDashStyle(String dashStyle) {
        super.setDashStyle(dashStyle);
        setStroke();
        setDescription();
        refreshTileProvider();
        clearTileCache();
    }

    public void setDescription(String description) {
        getAttributes().setDescription(description);
    }

    private void setStroke() {
        setStrokeWidth(5);

        FirelineType type = FirelineType.lookUp(getDashStyle());
        switch (type) {
            case FIRE_SPREAD_PREDICTION:
            case MANAGEMENT_ACTION_POINT:
                setStrokeColor(new int[]{255, 255, 165, 0});
                break;
            case FIRE_EDGE_LINE:
                setStrokeColor(new int[]{255, 255, 0, 0});
                break;
            default:
                setStrokeColor(BLACK);
                break;
        }
    }

    public void setDescription() {
        if (getAttributes() == null) {
            setAttributes(new MarkupFeature.Attributes());
        }

        FirelineType type = FirelineType.lookUp(getDashStyle());
        setDescription(type.getName());
    }

    private void setPoint(LatLng coordinate) {
        if (mPoints == null || mPoints.size() == 0) {
            addPoint(coordinate);
        } else {
            mPoints.set(0, coordinate);
        }
    }

    @Override
    public void addPoint(LatLng point) {
        mPoints.add(point);
        refreshTileProvider();
        clearTileCache();
    }

    @Override
    public void setPoints(ArrayList<LatLng> points) {
        super.setPoints(points);
        refreshTileProvider();
        clearTileCache();
    }

    public void remove() {
        mPoints.clear();
        refreshTileProvider();
        clearTileCache();
    }

    public void removePoint(int pointIndex) {
        mPoints.remove(pointIndex);
        refreshTileProvider();
        clearTileCache();
    }

    public void movePoint(int index, LatLng coordinate) {
        mPoints.set(index, coordinate);
        refreshTileProvider();
        clearTileCache();
    }

    public GroundOverlayOptions getOptions() {
        return null;
    }

    @Override
    public void removeFromMap() {
        if (mTileOverlay != null) {
            mTileOverlay.remove();
            mTileOverlay = null;
        }
    }

    public boolean isOnMap() {
        return mTileOverlay != null;
    }

    @Override
    public void addToMap() {
        if (mTileOverlay == null) {
            mActivity.runOnUiThread(() -> {
                if (mFirelineTileProvider == null) {
                    mFirelineTileProvider = new FirelineTileProvider(mMap.getMinZoomLevel(), mMap.getMaxZoomLevel(), this);
                }
                mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mFirelineTileProvider));
            });
        }
    }

    public void refreshTileProvider() {
        if (mFirelineTileProvider != null) {
            mFirelineTileProvider.updateFireline(this);
        }
    }

    @Override
    public void setIcon(Bitmap bitmap, int[] color) {
    }

    @Override
    public Bitmap getIcon() {
        return BitmapFactory.decodeResource(mActivity.getResources(), FirelineType.lookUp(getDashStyle()).getLightId());
    }

    @Override
    public void showInfoWindow() {
    }

    public void clearTileCache() {
        if (mTileOverlay != null) {
            mTileOverlay.clearTileCache();
        }
    }

    public LatLngBounds getFirelineBounds() {
        ArrayList<LatLng> coordinates = new ArrayList<>(getPoints());

        double top = coordinates.get(0).latitude;
        double bot = coordinates.get(0).latitude;
        double left = coordinates.get(0).longitude;
        double right = coordinates.get(0).longitude;

        for (int i = 1; i < coordinates.size(); i++) {

            LatLng coord = coordinates.get(i);

            if (coord.latitude < bot) {
                bot = coord.latitude;
            } else if (coord.latitude > top) {
                top = coord.latitude;
            }

            if (coord.longitude > right) {
                right = coord.longitude;
            } else if (coord.longitude < left) {
                left = coord.longitude;
            }
        }

        return new LatLngBounds(new LatLng(bot, left), new LatLng(top, right));
    }

    @Override
    public boolean isTouchedBy(LatLng point) {
        return TouchUtils.isLocationOnPath(point, mPoints, mMap.getProjection());
    }

    @Override
    public void setClickable(boolean clickable) {
    }

    @Override
    public void setVisibility(boolean visibility) {

    }

    @Override
    public boolean isValid() {
        return mPoints != null && mPoints.size() > 1;
    }

    @Override
    public String getInvalidString() {
        return "Invalid Fireline. Fireline must have at least 2 points.";
    }
}
