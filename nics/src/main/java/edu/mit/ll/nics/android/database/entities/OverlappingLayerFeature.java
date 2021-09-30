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

import androidx.core.math.MathUtils;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static edu.mit.ll.nics.android.utils.ColorUtils.parseRGBAColor;
import static edu.mit.ll.nics.android.utils.constants.Database.OVERLAPPING_LAYER_FEATURE_TABLE;

// TODO will refactor and utilize the layer feature class for all layers. For now getting it working as separate classes.

@Entity(tableName = OVERLAPPING_LAYER_FEATURE_TABLE,
        foreignKeys = {
                @ForeignKey(
                        entity = OverlappingRoomLayer.class,
                        parentColumns = "collabroomId",
                        childColumns = "collabroomId",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )},
        indices = {@Index(value = {"layerFeatureId"}, unique = true), @Index(value = {"collabroomId"})})
public class OverlappingLayerFeature {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long collabroomId;
    private String layerFeatureId;
    private String datalayerid;
    private String type;
    private float opacity;
    private int fillColor;
    private int strokeColor;
    private int strokeWidth;
    private float rotation;
    private int labelSize;
    private String dashStyle;
    private String labelText;
    private String graphic;
    private String filename;
    private ArrayList<LatLng> coordinates;
    private Map<String, Object> properties;

    public OverlappingLayerFeature() {
    }

    @Ignore
    public OverlappingLayerFeature(long collabroomId, String layerFeatureId, ArrayList<LatLng> coordinates, Map<String, Object> properties, String type) {
        this.collabroomId = collabroomId;
        this.layerFeatureId = layerFeatureId;
        this.type = type;
        this.coordinates = coordinates;
        this.properties = properties;
        setStyle();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLayerFeatureId() {
        return layerFeatureId;
    }

    public void setLayerFeatureId(String layerFeatureId) {
        this.layerFeatureId = layerFeatureId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<LatLng> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ArrayList<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public int getLabelSize() {
        return labelSize;
    }

    public void setLabelSize(int labelSize) {
        this.labelSize = labelSize;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public String getGraphic() {
        return graphic;
    }

    public void setGraphic(String graphic) {
        this.graphic = graphic;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDashStyle() {
        return dashStyle;
    }

    public void setDashStyle(String dashStyle) {
        this.dashStyle = dashStyle;
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    // TODO add a generic check for a full valid url that is also an image and that will be an image that is outside of the nics platform.
    private void setStyle() {
        setOpacity(MathUtils.clamp(MapUtils.getFloatValue(properties, "opacity", 0.4f), 0.6f, 1.0f));
        setRotation((float) Math.toDegrees(MapUtils.getDouble(properties, "rotation", 0d)));
        setFillColor(parseRGBAColor(MapUtils.getString(properties, "fillcolor", "#FFFFFF"), getOpacity()));
        setStrokeColor(parseRGBAColor(MapUtils.getString(properties, "strokecolor", "#FFFFFF"), getOpacity()));
        setStrokeWidth(MapUtils.getIntValue(properties, "strokewidth", 3));
        setLabelSize(MapUtils.getIntValue(properties, "labelsize", 30) + 12);
        setLabelText(MapUtils.getString(properties, "labeltext", ""));
        setDashStyle(MapUtils.getString(properties, "dashstyle", ""));
        setGraphic(MapUtils.getString(properties, "graphic", ""));
        setFilename(MapUtils.getString(properties, "filename", ""));
    }

    public String getDatalayerid() {
        return datalayerid;
    }

    public void setDatalayerid(String datalayerid) {
        this.datalayerid = datalayerid;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof OverlappingLayerFeature)) return false;

        OverlappingLayerFeature feature = (OverlappingLayerFeature) o;

        return new EqualsBuilder()
                .append(getDatalayerid(), feature.getDatalayerid())
                .append(getCollabroomId(), feature.getCollabroomId())
                .append(getType(), feature.getType())
                .append(getOpacity(), feature.getOpacity())
                .append(getFillColor(), feature.getFillColor())
                .append(getStrokeColor(), feature.getStrokeColor())
                .append(getRotation(), feature.getRotation())
                .append(getLabelSize(), feature.getLabelSize())
                .append(getDashStyle(), feature.getDashStyle())
                .append(getLabelText(), feature.getLabelText())
                .append(getGraphic(), feature.getGraphic())
                .append(getFilename(), feature.getFilename())
                .append(getCoordinates(), feature.getCoordinates())
                .append(getProperties(), feature.getProperties())
                .append(getLayerFeatureId(), feature.getLayerFeatureId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCollabroomId())
                .append(getLayerFeatureId())
                .append(getDatalayerid())
                .append(getType())
                .append(getOpacity())
                .append(getFillColor())
                .append(getStrokeColor())
                .append(getStrokeWidth())
                .append(getRotation())
                .append(getLabelSize())
                .append(getDashStyle())
                .append(getLabelText())
                .append(getGraphic())
                .append(getFilename())
                .append(getCoordinates())
                .append(getProperties())
                .toHashCode();
    }

    public static String hash(long collabroomId, ArrayList<LatLng> coordinates, HashMap<String, Object> properties) {
        return String.valueOf(Objects.hash(collabroomId, coordinates, properties));
    }
}
