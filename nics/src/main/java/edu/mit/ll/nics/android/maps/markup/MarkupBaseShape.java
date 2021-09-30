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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.MarkupFeature.Attributes;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.maps.tags.MarkupTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

import static edu.mit.ll.nics.android.utils.ColorUtils.colorToHexString;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertCoordinatesToGeometryString;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertLatLngToPoints;
import static edu.mit.ll.nics.android.utils.GeoUtils.getPolygonForCircle;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

/**
 * This class is the base class for shapes that represent NICS markup features {@link MarkupFeature}
 * that can be integrated with a {@link GoogleMap} Google map instance.
 */
public abstract class MarkupBaseShape implements Deletable {

    public static final float ANCHOR_CENTER = 0.5f;

    protected final GoogleMap mMap;
    protected final Activity mActivity;
    protected final PreferencesRepository mPreferences;

    private Long mId;
    private Long mTime;
    private Long mLastUpdate;
    private String mTitle;
    private String mCreator;
    private String mFeatureId;
    private String mImagePath;
    private MarkupType mType;
    private MarkupTag mTag;
    private MarkupFeature mFeature;
    private int[] mStrokeColor;
    private double mStrokeWidth;
    private int[] mFillColor;
    private String mDashStyle;
    private double mLabelSize;
    private String mLabelText;
    private double mRadius;

    ArrayList<LatLng> mPoints = new ArrayList<>();
    private Attributes mAttributes = new Attributes();
    private ArrayList<Hazard> mHazards = new ArrayList<>();

    MarkupBaseShape(GoogleMap map, PreferencesRepository preferences, Activity activity) {
        mMap = map;
        mActivity = activity;
        mPreferences = preferences;
        mCreator = mPreferences.getUserNickName();
        mTime = new Date().getTime() / 1000;
        mLastUpdate = new Date().getTime();
    }

    public abstract void addToMap();

    public abstract void removeFromMap();

    public abstract Object getOptions();

    public abstract void showInfoWindow();

    public abstract boolean isTouchedBy(LatLng point);

    public abstract void setClickable(boolean clickable);

    public abstract void setVisibility(boolean visibility);

    public abstract boolean isValid();

    public abstract String getInvalidString();

    @Override
    public Bitmap getIcon() {
        return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.circle);
    }

    public abstract void setIcon(Bitmap bitmap, int[] color);

    public GoogleMap getMap() {
        return mMap;
    }

    public Long getId() {
        return mId;
    }

    protected void setId(Long id) {
        mId = id;
    }

    public long getTime() {
        return mTime;
    }

    protected void setTime(long time) {
        mTime = time;
    }

    public long getLastUpdate() {
        return mLastUpdate;
    }

    protected void setLastUpdate(long lastUpdate) {
        mLastUpdate = lastUpdate;
    }

    public String getTitle() {
        return mTitle;
    }

    protected void setTitle(String title) {
        mTitle = title;
    }

    public String getCreator() {
        return mCreator;
    }

    protected void setCreator(String creator) {
        mCreator = creator;
    }

    public String getFeatureId() {
        return mFeatureId;
    }

    public void setFeatureId(String featureId) {
        mFeatureId = featureId;
    }

    public MarkupFeature getFeature() {
        return mFeature;
    }

    void setFeature(MarkupFeature feature) {
        mFeature = feature;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String imagePath) {
        mImagePath = imagePath;
    }

    public MarkupType getType() {
        return mType;
    }

    void setType(MarkupType type) {
        mType = type;
    }

    public MarkupTag getTag() {
        return mTag;
    }

    public void setTag(MarkupTag tag) {
        mTag = tag;
    }

    public int[] getStrokeColor() {
        return mStrokeColor;
    }

    public void setStrokeColor(int[] color) {
        mStrokeColor = color;
    }

    public int[] getFillColor() {
        return mFillColor;
    }

    public void setFillColor(int[] color) {
        mFillColor = color;
    }

    public String getDashStyle() {
        return mDashStyle;
    }

    public void setDashStyle(String dashStyle) {
        mDashStyle = dashStyle;
    }

    public double getLabelSize() {
        return mLabelSize;
    }

    public void setLabelSize(double size) {
        mLabelSize = size;
    }

    public String getLabelText() {
        return mLabelText;
    }

    public void setLabelText(String text) {
        mLabelText = text;
    }

    public double getRadius() {
        return mRadius;
    }

    void setRadius(double radius) {
        mRadius = radius;
    }

    public double getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(double width) {
        mStrokeWidth = width;
    }

    public Attributes getAttributes() {
        return mAttributes;
    }

    public void setAttributes(@NonNull Attributes attributes) {
        mAttributes = attributes;
    }

    public String getComments() {
        return getAttributes().getComments();
    }

    public void setComments(String comments) {
        getAttributes().setComments(comments);
    }

    public void setDescription(String description) {
        getAttributes().setDescription(description);
    }

    public ArrayList<Hazard> getHazards() {
        return mHazards;
    }

    public void setHazards(ArrayList<Hazard> hazards) {
        mHazards = hazards;
    }

    public void addHazard(Hazard hazard) {
        mHazards.add(hazard);
    }

    public List<LatLng> getPoints() {
        return mPoints;
    }

    public void setPoints(ArrayList<LatLng> points) {
        mPoints = points;
    }

    public void addPoint(LatLng point) {
        mPoints.add(point);
    }

    public void clearPoints() {
        mPoints.clear();
    }

    public String getDescription() {
        String description = getAttributes().getDescription();
        if (!emptyCheck(description)) {
            return description;
        } else {
            return getType().toString();
        }
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", mType);
        json.put("created", mTime);

        if (mStrokeColor != null) {
            json.put("strokeColor", colorToHexString(mStrokeColor));
        }

        if (mFillColor != null) {
            json.put("fillColor", colorToHexString(mFillColor));
        }

        JSONArray pointsArray = new JSONArray();
        for (LatLng latlng : mPoints) {
            pointsArray.put(new JSONArray(Arrays.asList(String.valueOf(latlng.latitude), String.valueOf(latlng.longitude))));
        }
        json.put("points", pointsArray);

        return json;
    }

    public String toJsonString() throws JSONException {
        return toJson().toString();
    }

    public MarkupFeature toFeature() {
        MarkupFeature feature = new MarkupFeature();

        double opacity = 0.4;
        if (mType.equals(MarkupType.sketch) && mPoints.size() == 2) {
            opacity = 1;
        }

        if (mDashStyle != null) {
            feature.setDashStyle(mDashStyle);
        }

        feature.setCollabRoomId(mPreferences.getSelectedCollabroomId());
        feature.setUserSessionId(-1L);
        feature.setFillColor(colorToHexString(mFillColor));
        feature.setGraphic(mImagePath);
        feature.setUserName(mPreferences.getUserName());
        feature.setStrokeColor(colorToHexString(mStrokeColor));
        feature.setStrokeWidth(mStrokeWidth);
        feature.setIp("127.0.0.1");
        feature.setSeqTime(mTime);
        feature.setLastUpdate(mLastUpdate);
        feature.setFeatureId(mFeatureId);
        feature.setLabelSize(mLabelSize);
        feature.setLabelText(mLabelText);
        feature.setTopic("NICS.incidents." + mPreferences.getSelectedIncidentName() + ".collab." + mPreferences.getSelectedCollabroomName());
        feature.setType(mType.toString());
        feature.setOpacity(opacity);

        feature.setAttributes(getAttributes());

        if (mType.equals(MarkupType.circle) && mPoints.size() == 1) {
            LatLng circleMid = mPoints.get(0);
            mPoints = getPolygonForCircle(circleMid, mRadius);
        }

        feature.setGeometryVector2(convertLatLngToPoints((ArrayList<LatLng>) getPoints()));
        feature.setGeometry(convertCoordinatesToGeometryString((ArrayList<LatLng>) getPoints(), mType.toString()));
        feature.setRadius(mRadius);
        feature.setRotation(0.0d);

        if (feature.getSendStatus() == null) {
            feature.setSendStatus(SendStatus.WAITING_TO_SEND);
        }

        feature.setHazards(getHazards());

        return feature;
    }
}
