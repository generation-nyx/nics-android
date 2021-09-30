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
package edu.mit.ll.nics.android.database.entities;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.utils.UnitConverter;
import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.GeoUtils.bufferGeometry;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertCoordinatesToGeometryString;
import static edu.mit.ll.nics.android.utils.constants.Database.MAP_MARKUP_TABLE;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@Entity(tableName = MAP_MARKUP_TABLE, indices = {@Index(value = {"featureId"}, unique = true)})
public class MarkupFeature {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @SerializedName("featureId")
    private String featureId;

    @Expose
    @SerializedName("usersessionId")
    private long userSessionId;

    @Expose
    @SerializedName("strokeColor")
    private String strokeColor;

    @Expose
    @SerializedName("strokeWidth")
    private double strokeWidth;

    @Expose
    @SerializedName("fillColor")
    private String fillColor;

    @Expose
    @SerializedName("dashStyle")
    private String dashStyle;

    @Expose
    @SerializedName("opacity")
    private Double opacity;

    @Expose
    @SerializedName("rotation")
    private double rotation;

    @Expose
    @SerializedName("graphic")
    private String graphic;

    @Expose
    @SerializedName("labelSize")
    private Double labelSize;

    @Expose
    @SerializedName("labelText")
    private String labelText;

    @Expose
    @SerializedName("username")
    private String userName;

    @SerializedName("topic")
    private String topic;

    @SerializedName("ip")
    private String ip;

    @Expose
    @SerializedName("seqtime")
    private long seqTime;

    @Expose
    @SerializedName("lastupdate")
    private long lastUpdate;

    @Expose
    @SerializedName("type")
    private String type;

    @Expose
    @SerializedName("geometry")
    private String geometry;

    @Expose
    @SerializedName("pointRadius")
    private Double pointRadius;

    @SerializedName("collabRoomId")
    private long collabRoomId;

    private SendStatus sendStatus;

    private ArrayList<Vector2> geometryVector2;

    @Embedded
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private Attributes attributes;

    @Ignore
    private ArrayList<Hazard> hazards;

    private String originalFeature;

    private boolean failedToSend;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MarkupFeature)) return false;

        MarkupFeature feature = (MarkupFeature) o;

        return new EqualsBuilder()
                .append(getId(), feature.getId())
                .append(getFeatureId(), feature.getFeatureId())
                .append(getUserSessionId(), feature.getUserSessionId())
                .append(getStrokeColor(), feature.getStrokeColor())
                .append(getStrokeWidth(), feature.getStrokeWidth())
                .append(getFillColor(), feature.getFillColor())
                .append(getDashStyle(), feature.getDashStyle())
                .append(getOpacity(), feature.getOpacity())
                .append(getRotation(), feature.getRotation())
                .append(getGraphic(), feature.getGraphic())
                .append(getLabelSize(), feature.getLabelSize())
                .append(getLabelText(), feature.getLabelText())
                .append(getUserName(), feature.getUserName())
                .append(getTopic(), feature.getTopic())
                .append(getIp(), feature.getIp())
                .append(getSeqTime(), feature.getSeqTime())
                .append(getLastUpdate(), feature.getLastUpdate())
                .append(getType(), feature.getType())
                .append(getGeometry(), feature.getGeometry())
                .append(getPointRadius(), feature.getPointRadius())
                .append(getCollabRoomId(), feature.getCollabRoomId())
                .append(getSendStatus(), feature.getSendStatus())
                .append(getAttributes(), feature.getAttributes())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getId())
                .append(getFeatureId())
                .append(getUserSessionId())
                .append(getStrokeColor())
                .append(getStrokeWidth())
                .append(getFillColor())
                .append(getDashStyle())
                .append(getOpacity())
                .append(getRotation())
                .append(getGraphic())
                .append(getLabelSize())
                .append(getLabelText())
                .append(getUserName())
                .append(getTopic())
                .append(getIp())
                .append(getSeqTime())
                .append(getLastUpdate())
                .append(getType())
                .append(getGeometry())
                .append(getPointRadius())
                .append(getCollabRoomId())
                .append(getSendStatus())
                .append(getAttributes())
                .toHashCode();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUserSessionId(long userSessionId) {
        this.userSessionId = userSessionId;
    }

    public long getSeqTime() {
        return seqTime;
    }

    public void setSeqTime(long seqTime) {
        this.seqTime = seqTime;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public Double getPointRadius() {
        return pointRadius;
    }

    public void setPointRadius(Double pointRadius) {
        this.pointRadius = pointRadius;
    }

    public long getCollabRoomId() {
        return collabRoomId;
    }

    public void setCollabRoomId(long collabRoomId) {
        this.collabRoomId = collabRoomId;
    }

    public SendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public long getUserSessionId() {
        return userSessionId;
    }

    public long getId() {
        return id;
    }

    public String getDashStyle() {
        return dashStyle;
    }

    public void setDashStyle(String dashStyle) {
        this.dashStyle = dashStyle;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public String getGraphic() {
        return graphic;
    }

    public void setGraphic(String graphic) {
        this.graphic = graphic;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Double getLabelSize() {
        return labelSize;
    }

    public void setLabelSize(Double labelSize) {
        this.labelSize = labelSize;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(String strokeColor) {
        this.strokeColor = strokeColor;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getOpacity() {
        if (opacity == null) {
            opacity = -1.0;
        }
        return opacity;
    }

    public void setOpacity(Double opacity) {
        this.opacity = opacity;
    }

    public ArrayList<Vector2> getGeometryVector2() {
        buildVector2Point(true);
        return geometryVector2;
    }

    public void setGeometryVector2(ArrayList<Vector2> geometryVector2) {
        this.geometryVector2 = geometryVector2;
    }

    public double getRadius() {
        return pointRadius;
    }

    public void setRadius(double radius) {
        this.pointRadius = radius;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public String toJson() {
        String attributesString = getAttributes().toJson();
        JsonElement jsonElement = new GsonBuilder().serializeSpecialFloatingPointValues().serializeNulls().excludeFieldsWithoutExposeAnnotation().create().toJsonTree(this);
        jsonElement.getAsJsonObject().addProperty("attributes", attributesString);
        return new GsonBuilder().serializeSpecialFloatingPointValues().excludeFieldsWithoutExposeAnnotation().create().toJson(jsonElement);
    }

    public String toJsonStringWithFeatureId() {
        String attributesString = getAttributes().toJson();
        JsonElement jsonElement = new GsonBuilder().serializeSpecialFloatingPointValues().serializeNulls().excludeFieldsWithoutExposeAnnotation().create().toJsonTree(this);
        jsonElement.getAsJsonObject().addProperty("featureId", getFeatureId());
        jsonElement.getAsJsonObject().addProperty("attributes", attributesString);
        return new Gson().toJson(jsonElement);
    }

    public String toFullJsonString() {
        return new GsonBuilder().serializeSpecialFloatingPointValues().create().toJson(this);
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public Attributes getAttributes() {
        if (attributes == null) {
            attributes = new Attributes();
        }
        return attributes;
    }

    public void buildVector2Point(boolean isFromNicsWeb) {
        String geomTemp = geometry;

        geomTemp = geomTemp.replace("(", "");
        geomTemp = geomTemp.replace(")", "");

        geomTemp = geomTemp.replace("POLYGON", "");
        geomTemp = geomTemp.replace("POINT", "");
        geomTemp = geomTemp.replace("LINESTRING", "");

        String[] separatedCommas = geomTemp.split(",");

        ArrayList<Vector2> fullySeperated = new ArrayList<>();
        for (String separatedComma : separatedCommas) {

            String[] seperateSpaces = separatedComma.split(" ");

            if (isFromNicsWeb) {
                fullySeperated.add(new Vector2(
                        Double.parseDouble(seperateSpaces[1]),
                        Double.parseDouble(seperateSpaces[0])
                ));
            } else {
                fullySeperated.add(new Vector2(
                        Double.parseDouble(seperateSpaces[0]),
                        Double.parseDouble(seperateSpaces[1])
                ));
            }

        }

        setGeometryVector2(fullySeperated);
    }

    public boolean isFailedToSend() {
        return failedToSend;
    }

    public void setFailedToSend(boolean failedToSend) {
        this.failedToSend = failedToSend;
    }

    public String getOriginalFeature() {
        return originalFeature;
    }

    public void setOriginalFeature(String originalFeature) {
        this.originalFeature = originalFeature;
    }

    public ArrayList<Hazard> getHazards() {
        return hazards;
    }

    public void setHazards(ArrayList<Hazard> hazards) {
        this.hazards = hazards;
    }

    // TODO can refactor this later.
    public void addHazard(Hazard hazard) {
        if (attributes == null) {
            attributes = new Attributes();
        }

        if (attributes.getHazards() == null) {
            attributes.setHazards(new ArrayList<>());
        }

        if (hazards == null) {
            hazards = new ArrayList<>();
        }

        hazards.add(hazard);
    }

    public void setHazards() {
        if (getHazards() != null) {
            for (Hazard hazard : getHazards()) {
                double radius = hazard.getRadius();
                if (hazard.getMetric().equalsIgnoreCase("kilometer")) {
                    radius = UnitConverter.kilometersToMeters(radius);
                }
                try {
                    // Buffer the geometry of the markup feature to use as a geofence.
                    ArrayList<LatLng> coordinates = bufferGeometry(getGeometry(), getType(), radius);
                    hazard.setCoordinates(coordinates);
                    hazard.setGeometry(convertCoordinatesToGeometryString(coordinates, "polygon"));
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed to buffer geometry for geofence boundary.");
                }
            }
        }
    }

    public static class Attributes {

        @SerializedName("layerid")
        private long layerId;

        @SerializedName("comments")
        private String comments;

        @SerializedName("description")
        private String description;

        @SerializedName("hazards")
        private ArrayList<HazardInfo> hazards;

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Attributes)) return false;

            Attributes attributes = (Attributes) o;

            return new EqualsBuilder()
                    .append(getLayerId(), attributes.getLayerId())
                    .append(getComments(), attributes.getComments())
                    .append(getDescription(), attributes.getDescription())
                    .append(getHazards(), attributes.getHazards())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(getLayerId())
                    .append(getComments())
                    .append(getDescription())
                    .append(getHazards())
                    .toHashCode();
        }

        public ArrayList<HazardInfo> getHazards() {
            return hazards;
        }

        public void setHazards(ArrayList<HazardInfo> hazards) {
            this.hazards = hazards;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getLayerId() {
            return layerId;
        }

        public void setLayerId(long layerId) {
            this.layerId = layerId;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        public String toJson() {
            return new GsonBuilder().serializeNulls().create().toJson(this);
        }
    }
}
