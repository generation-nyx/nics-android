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
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

import edu.mit.ll.nics.android.data.geo.wfs.Feature;
import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

import static edu.mit.ll.nics.android.utils.constants.Database.TRACKING_LAYER_FEATURE_TABLE;

@Entity(tableName = TRACKING_LAYER_FEATURE_TABLE,
        foreignKeys = {
            @ForeignKey(
                    entity = Tracking.class,
                    parentColumns = "layerName",
                    childColumns = "layerName",
                    onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.CASCADE
            )
        },
        indices = {
            @Index(value = {"featureId"}, unique = true),
            @Index(value = {"layerName"})
        }
)
public class TrackingLayerFeature {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String layerName;
    private String featureId;
    private LatLng coordinate;

    @Embedded
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private LayerProperties properties;

    public TrackingLayerFeature(int id, String layerName, String featureId, LatLng coordinate, LayerProperties properties) {
        this.id = id;
        this.layerName = layerName;
        this.featureId = featureId;
        this.coordinate = coordinate;
        this.properties = properties;
    }

    @Ignore
    public TrackingLayerFeature(Feature feature, String layerName) {
        this.coordinate = feature.geometryToLatLng();
        this.featureId = feature.getUniqueId();
        this.properties = feature.getProperties();
        this.layerName = layerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TrackingLayerFeature)) return false;

        TrackingLayerFeature feature = (TrackingLayerFeature) o;

        return new EqualsBuilder()
                .append(getLayerName(), feature.getLayerName())
                .append(getFeatureId(), feature.getFeatureId())
                .append(getCoordinate(), feature.getCoordinate())
                .append(getProperties(), feature.getProperties())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getLayerName())
                .append(getFeatureId())
                .append(getCoordinate())
                .append(getProperties())
                .toHashCode();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public LatLng getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(LatLng coordinate) {
        this.coordinate = coordinate;
    }

    public LayerProperties getProperties() {
        return properties;
    }

    public void setProperties(LayerProperties properties) {
        this.properties = properties;
    }

    public String getDescription() {
        return getProperties().getDescription();
    }

    public String getName() {
        return getProperties().getName();
    }

    public String getAge() {
        return getProperties().getAge();
    }

    public Date getCreated() {
        return getProperties().getCreated();
    }

    public Date getTimestamp() {
        return getProperties().getTimestamp();
    }

    public Date getXmltime() {
        return getProperties().getXmltime();
    }

    public float getCourse() {
        return (float) getProperties().getCourse();
    }

    public String getStyleIcon() {
        return getProperties().getStyleIcon();
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
