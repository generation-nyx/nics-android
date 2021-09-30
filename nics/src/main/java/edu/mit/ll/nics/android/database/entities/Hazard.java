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

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

import static edu.mit.ll.nics.android.utils.constants.Database.HAZARD_TABLE;

@Entity(tableName = HAZARD_TABLE,
        foreignKeys = {
                @ForeignKey(
                        entity = MarkupFeature.class,
                        parentColumns = "featureId",
                        childColumns = "hazardFeatureId",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = LayerFeature.class,
                        parentColumns = "layerFeatureId",
                        childColumns = "hazardLayerId",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"hazardId"}, unique = true),
                @Index(value = {"hazardFeatureId"}, unique = true),
                @Index(value = {"hazardLayerId"}, unique = true)
        }
)
public class Hazard {

    //TODO embed hazard info object.

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String hazardId;
    private String hazardLabel;
    private String hazardType;
    private double radius;
    private String metric;
    private String geometry;
    private long collabroomId;

    private ArrayList<LatLng> coordinates;

    private String hazardFeatureId;
    private String hazardLayerId;

    @Ignore
    private Double distanceFromUser;

    @Ignore
    private String directionToHazard;

    @Ignore
    private boolean isInside = false;

    @Ignore
    private boolean isAcknowledged = false;

    public Hazard() {
    }

    @Ignore
    public Hazard(HazardInfo info, String featureId, long collabroomId) {
        this.hazardId = featureId;
        this.hazardFeatureId = featureId;
        this.hazardLabel = info.getHazardLabel();
        this.hazardType = info.getHazardType();
        this.radius = info.getRadius();
        this.metric = info.getMetric();
        this.collabroomId = collabroomId;
    }

    @Ignore
    public Hazard(String hazardId, String hazardLabel, String hazardType, double radius, String metric, String geometry, long collabroomId, ArrayList<LatLng> coordinates) {
        this.hazardId = hazardId;
        this.hazardLabel = hazardLabel;
        this.hazardType = hazardType;
        this.radius = radius;
        this.metric = metric;
        this.geometry = geometry;
        this.collabroomId = collabroomId;
        this.coordinates = coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Hazard)) return false;

        Hazard hazard = (Hazard) o;

        return new EqualsBuilder()
                .append(getCollabroomId(), hazard.getCollabroomId())
                .append(getHazardId(), hazard.getHazardId())
                .append(getHazardType(), hazard.getHazardType())
                .append(getHazardLabel(), hazard.getHazardLabel())
                .append(getRadius(), hazard.getRadius())
                .append(getMetric(), hazard.getMetric())
                .append(getCoordinates(), hazard.getCoordinates())
                .append(getGeometry(), hazard.getGeometry())
                .append(getHazardFeatureId(), hazard.getHazardFeatureId())
                .append(getHazardLayerId(), hazard.getHazardLayerId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCollabroomId())
                .append(getHazardId())
                .append(getHazardType())
                .append(getHazardLabel())
                .append(getRadius())
                .append(getMetric())
                .append(getCoordinates())
                .append(getGeometry())
                .append(getHazardFeatureId())
                .append(getHazardLayerId())
                .toHashCode();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHazardId() {
        return hazardId;
    }

    public void setHazardId(String hazardId) {
        this.hazardId = hazardId;
    }

    public String getHazardFeatureId() {
        return hazardFeatureId;
    }

    public void setHazardFeatureId(String hazardFeatureId) {
        this.hazardFeatureId = hazardFeatureId;
    }

    public String getHazardLayerId() {
        return hazardLayerId;
    }

    public void setHazardLayerId(String hazardLayerId) {
        this.hazardLayerId = hazardLayerId;
    }

    public String getHazardType() {
        return hazardType;
    }

    public void setHazardType(String hazardType) {
        this.hazardType = hazardType;
    }

    public String getHazardLabel() {
        return hazardLabel;
    }

    public void setHazardLabel(String hazardLabel) {
        this.hazardLabel = hazardLabel;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    public ArrayList<LatLng> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ArrayList<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public Double getDistanceFromUser() {
        return distanceFromUser;
    }

    public void setDistanceFromUser(Double distanceFromUser) {
        this.distanceFromUser = distanceFromUser;
    }

    public String getDirectionToHazard() {
        return directionToHazard;
    }

    public void setDirectionToHazard(String directionToHazard) {
        this.directionToHazard = directionToHazard;
    }

    public boolean isInside() {
        return isInside;
    }

    public void setInside(boolean inside) {
        isInside = inside;
    }

    public boolean isAcknowledged() {
        return isAcknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        isAcknowledged = acknowledged;
    }

    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }

    public static LatLngBounds getHazardBounds(ArrayList<Hazard> hazards) {
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Hazard hazard : hazards) {
                for (LatLng coordinate : hazard.getCoordinates()) {
                    builder.include(coordinate);
                }
            }
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
