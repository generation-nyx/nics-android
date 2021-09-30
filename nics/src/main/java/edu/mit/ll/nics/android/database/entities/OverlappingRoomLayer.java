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
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

import edu.mit.ll.nics.android.maps.layers.LayerType;

import static edu.mit.ll.nics.android.utils.constants.Database.OVERLAPPING_ROOM_LAYERS_TABLE;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_LAYER_TYPE;

@Entity(tableName = OVERLAPPING_ROOM_LAYERS_TABLE, indices = {@Index(value = {"collabroomId"}, unique = true)})
public class OverlappingRoomLayer {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String collabroomName;
    private String incidentName;
    private long collabroomId;
    private long incidentId;
    private String created;
    private boolean isActive = false;

    @Ignore
    private List<OverlappingLayerFeature> features;

    public OverlappingRoomLayer(String collabroomName, long collabroomId, String created, String incidentName, long incidentId) {
        this.collabroomName = collabroomName;
        this.collabroomId = collabroomId;
        this.created = created;
        this.incidentName = incidentName;
        this.incidentId = incidentId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof OverlappingRoomLayer)) return false;

        OverlappingRoomLayer layer = (OverlappingRoomLayer) o;

        return new EqualsBuilder()
                .append(getCollabroomName(), layer.getCollabroomName())
                .append(getIncidentName(), layer.getIncidentName())
                .append(getCollabroomId(), layer.getCollabroomId())
                .append(getIncidentId(), layer.getIncidentId())
                .append(getCreated(), layer.getCreated())
                .append(getFeatures(), layer.getFeatures())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCollabroomName())
                .append(getIncidentName())
                .append(getCollabroomId())
                .append(getIncidentId())
                .append(getCreated())
                .append(getFeatures())
                .toHashCode();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getIncidentName() {
        return incidentName;
    }

    public void setIncidentName(String incidentName) {
        this.incidentName = incidentName;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public String getCollabroomName() {
        return collabroomName;
    }

    public void setCollabroomName(String collabroomName) {
        this.collabroomName = collabroomName;
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    public List<OverlappingLayerFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<OverlappingLayerFeature> features) {
        this.features = features;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void toggle() {
        this.isActive = !isActive;
    }

    public String getTypeName() {
        return NICS_LAYER_TYPE + getCollabroomId();
    }

    public boolean hasFeatures() {
        LayerType type = LayerType.lookUp(getTypeName());
        if (type == null) {
            return false;
        }
        return type.equals(LayerType.WFS) || type.equals(LayerType.GEOJSON);
    }

    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }
}
