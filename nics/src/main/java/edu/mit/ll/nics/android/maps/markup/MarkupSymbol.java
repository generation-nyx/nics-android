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
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
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
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.maps.tags.MarkupTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.BitmapUtils;
import edu.mit.ll.nics.android.utils.TouchUtils;
import timber.log.Timber;


import static edu.mit.ll.nics.android.utils.BitmapUtils.generateBitmap;
import static edu.mit.ll.nics.android.utils.ColorUtils.BLACK;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngPoint;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

/**
 * NICS markup feature {@link MarkupFeature} that represents a {@link Marker} for integrating with
 * a {@link GoogleMap} Google map instance.
 */
public class MarkupSymbol extends MarkupBaseShape {

    private Marker mMarker;
    private final MarkerOptions mMarkerOptions = new MarkerOptions();

    public MarkupSymbol(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity,
                        boolean defaultIcon,
                        boolean draggable) {
        super(map, preferences, activity);

        if (!defaultIcon) {
            setIcon(generateBitmap(R.drawable.x, mActivity));
        }

        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setFlat(true);
        setVisibility(true);
        setDraggable(draggable);
        setFeatureId(String.valueOf(getTime()));
        setType(MarkupType.marker);
        setStrokeColor(BLACK);
        setTitle(MarkupType.marker.toString());
    }

    public MarkupSymbol(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity,
                        LayerFeature feature) {
        super(map, preferences, activity);

        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setFlat(true);
        setVisibility(true);
        setAlpha(feature.getOpacity());
        setRotation(feature.getRotation());
        setDraggable(true);
        setPoint(feature.getCoordinates().get(0));
    }

    public MarkupSymbol(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity,
                        OverlappingLayerFeature feature) {
        super(map, preferences, activity);

        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setFlat(true);
        setVisibility(true);
        setAlpha(feature.getOpacity());
        setRotation(feature.getRotation());
        setDraggable(true);
        setPoint(feature.getCoordinates().get(0));
    }

    public MarkupSymbol(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity,
                        String title,
                        LatLng coordinate,
                        Bitmap symbolBitmap,
                        String symbolPath,
                        boolean isDraggable,
                        int[] strokeColor) {
        super(map, preferences, activity);

        setTitle(title);
        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setPoint(coordinate);
        setFlat(true);
        setIcon(symbolBitmap);
        setDraggable(isDraggable);
        setImagePathAndLoad(symbolPath);
        setFeatureId(String.valueOf(getTime()));
        setType(MarkupType.marker);
        setStrokeColor(strokeColor);
    }

    public MarkupSymbol(GoogleMap map,
                        PreferencesRepository preferences,
                        Activity activity,
                        MarkupFeature feature,
                        boolean draggable) {
        super(map, preferences, activity);

        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER);
        setFlat(true);

        LatLng coordinate = getLatLngPoint(feature.getGeometryVector2());
        if (coordinate != null) {
            setPoint(coordinate);
        }

        if (feature.getLabelText() == null) {
            feature.setLabelText(EMPTY);
        }

        JsonObject attr = new JsonObject();
        Resources resources = mActivity.getResources();
        try {
            attr.addProperty(resources.getString(R.string.description), feature.getAttributes().getDescription());

            String nickname = mPreferences.getUserNickName(feature.getUserName());
            if (!nickname.equals("Unknown User")) {
                attr.addProperty(resources.getString(R.string.markup_user), nickname);
            } else {
                attr.addProperty(resources.getString(R.string.markup_user), feature.getUserName());
            }

            attr.addProperty(resources.getString(R.string.markup_message), feature.getLabelText());
            attr.addProperty(resources.getString(R.string.markup_comment), feature.getAttributes().getComments());
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to parse markup symbol title.");
        }

        setTitle(attr.toString());
        setRotation((float) Math.toDegrees(feature.getRotation()));
        setDraggable(draggable);
        setId(feature.getId());
        setType(MarkupType.marker);
        setTime(feature.getSeqTime());
        setLastUpdate(feature.getLastUpdate());
        setCreator(feature.getUserName());
        setFeatureId(feature.getFeatureId());
        setHazards(feature.getHazards());
        setFeature(feature);
        setAttributes(feature.getAttributes());
        setImagePathAndLoad(feature.getGraphic());
        setStrokeColor(BLACK);
        setTag(new FeatureTag(feature));

    }


    private void loadBitmap(String url) {
        Glide.with(mActivity)
                .asDrawable()
                .fitCenter()
                .placeholder(R.drawable.x)
                .error(R.drawable.x)
                .load(mPreferences.getSymbologyURL() + url)
                .into(new CustomTarget<Drawable>(75, 75) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        setIcon(BitmapUtils.fromDrawable(resource));
                    }

                    @Override
                    public void onLoadStarted(@Nullable Drawable placeholder) {
                        setIcon(BitmapUtils.fromDrawable(placeholder));
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        setIcon(BitmapUtils.fromDrawable(errorDrawable));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        setIcon(BitmapUtils.fromDrawable(placeholder));
                    }
                });
    }

    public void setImagePathAndLoad(String imagePath) {
        setImagePath(imagePath);
        loadBitmap(imagePath);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        mMarkerOptions.title(title);

        if(mMarker != null) {
            mMarker.setTitle(title);
        }
    }

    public void addToMap(LatLng latLng) {
        setPoint(latLng);
        if (mMarker == null) {
            addToMap();
        }
    }

    public LatLng getPoint() {
        List<LatLng> points = getPoints();
        return points.size() > 0 ? points.get(0) : null;
    }

    public void setPoint(@NonNull LatLng coordinate) {
        setPoints(new ArrayList<>(Collections.singletonList(coordinate)));

        mMarkerOptions.position(coordinate);

        if (mMarker != null) {
            mMarker.setPosition(coordinate);
        }
    }

    public MarkerOptions getOptions() {
        return mMarkerOptions;
    }

    private void setMarker(Marker marker) {
        mMarker = marker;
    }

    public Marker getMarker() {
        return mMarker;
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
    public void addToMap() {
        if (mMarker == null) {
            mActivity.runOnUiThread(() -> {
                setMarker(mMap.addMarker(mMarkerOptions));
                setTitle(mMarkerOptions.getTitle());
                setPoint(mMarkerOptions.getPosition());
                setTag(getTag());
            });
        }
    }

    @Override
    public void setTag(MarkupTag tag) {
        super.setTag(tag);
        if (mMarker != null) {
            mMarker.setTag(tag);
        }
    }

    public void setIcon(Bitmap bitmap) {
        setIcon(bitmap, BLACK);
    }

    @Override
    public void setIcon(Bitmap bitmap, int[] color) {
        setStrokeColor(color);
        setFillColor(color);

        if (bitmap == null) {
            mMarkerOptions.icon(null);

            if (mMarker != null) {
                mMarker.setIcon(null);
            }
        } else {
            mMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));

            if (mMarker != null) {
                mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
            }
        }

    }

    public void showInfoWindow() {
        if (mMarker != null) {
            mMarker.showInfoWindow();
        }
    }

    @Override
    public MarkupFeature toFeature() {
        ArrayList<LatLng> point = new ArrayList<>();
        point.add(mMarker.getPosition());
        setPoints(point);
        return super.toFeature();
    }

    public void setAlpha(float alpha) {
        mMarkerOptions.alpha(alpha);

        if (mMarker != null) {
            mMarker.setAlpha(alpha);
        }
    }

    public void setRotation(float rotation) {
        mMarkerOptions.rotation(rotation);

        if (mMarker != null) {
            mMarker.setRotation(rotation);
        }
    }

    public void setDraggable(boolean draggable) {
        mMarkerOptions.draggable(draggable);

        if (mMarker != null) {
            mMarker.setDraggable(draggable);
        }
    }

    public void setAnchor(float u, float v) {
        mMarkerOptions.anchor(u, v);

        if (mMarker != null) {
            mMarker.setAnchor(u, v);
        }
    }

    public void setFlat(boolean flat) {
        mMarkerOptions.flat(flat);

        if (mMarker != null) {
            mMarker.setFlat(flat);
        }
    }

    @Override
    public boolean isTouchedBy(LatLng point) {
        return TouchUtils.isLocationInBox(point, mPoints.get(0),
                75, 75,
                mMap.getProjection());
    }

    @Override
    public void setClickable(boolean clickable) {
    }

    @Override
    public void setVisibility(boolean visibility) {
        mMarkerOptions.visible(visibility);

        if (mMarker != null) {
            mMarker.setVisible(visibility);
        }
    }

    @Override
    public boolean isValid() {
        return getPoint() != null;
    }

    @Override
    public String getInvalidString() {
        return "Invalid Symbol. Symbols must have one point.";
    }
}