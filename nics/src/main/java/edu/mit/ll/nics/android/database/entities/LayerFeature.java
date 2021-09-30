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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.mit.ll.nics.android.utils.ColorUtils;

import static edu.mit.ll.nics.android.utils.constants.Database.LAYER_FEATURE_TABLE;

// TODO will refactor and utilize the layer feature class for all layers. For now getting it working as separate classes.

@Entity(tableName = LAYER_FEATURE_TABLE,
        foreignKeys = {
                @ForeignKey(
                        entity = CollabroomDataLayer.class,
                        parentColumns = "datalayerId",
                        childColumns = "datalayerid",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )},
        indices = {@Index(value = {"datalayerid"}), @Index(value = {"layerFeatureId"}, unique = true)})
public class LayerFeature {

    @PrimaryKey(autoGenerate = true)
    private long id;
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

    @Ignore
    private Hazard hazard;

    public LayerFeature() {
    }

    @Ignore
    public LayerFeature(String layerFeatureId, ArrayList<LatLng> coordinates, Map<String, Object> properties, String type) {
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

    // TODO add a generic check for a full valid url that is also an image and that will be an image that is outside of the nics platform.
    private void setStyle() {
        setOpacity(MathUtils.clamp(MapUtils.getFloatValue(properties, "opacity", 0.4f), 0.6f, 1.0f));
        setRotation((float) Math.toDegrees(MapUtils.getDouble(properties, "rotation", 0d)));
        setFillColor(ColorUtils.parseRGBAColor(MapUtils.getString(properties, "fillcolor", "#FFFFFF"), getOpacity()));
        setStrokeColor(ColorUtils.parseRGBAColor(MapUtils.getString(properties, "strokecolor", "#FFFFFF"), getOpacity()));
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

    public Hazard getHazard() {
        return hazard;
    }

    public void setHazard(Hazard hazard) {
        this.hazard = hazard;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LayerFeature)) return false;

        LayerFeature feature = (LayerFeature) o;

        return Objects.equals(datalayerid, feature.getDatalayerid()) &&
                Objects.equals(type, feature.getType()) &&
                Objects.equals(opacity, feature.getOpacity()) &&
                Objects.equals(fillColor, feature.getFillColor()) &&
                Objects.equals(strokeColor, feature.getStrokeColor()) &&
                Objects.equals(strokeWidth, feature.getStrokeWidth()) &&
                Objects.equals(rotation, feature.getRotation()) &&
                Objects.equals(labelSize, feature.getLabelSize()) &&
                Objects.equals(dashStyle, feature.getDashStyle()) &&
                Objects.equals(labelText, feature.getLabelText()) &&
                Objects.equals(graphic, feature.getGraphic()) &&
                Objects.equals(filename, feature.getFilename()) &&
                Objects.equals(coordinates, feature.getCoordinates()) &&
                Objects.equals(properties, feature.getProperties()) &&
                Objects.equals(layerFeatureId, feature.getLayerFeatureId()) &&
                Objects.equals(hazard, feature.getHazard());
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerFeatureId, datalayerid, type, opacity, fillColor, strokeColor, strokeWidth,
                rotation, labelSize, dashStyle, labelText, graphic, filename, coordinates, properties);
    }

    public static String hash(ArrayList<LatLng> coordinates, HashMap<String, Object> properties) {
        return String.valueOf(Objects.hash(coordinates, properties));
    }
}
