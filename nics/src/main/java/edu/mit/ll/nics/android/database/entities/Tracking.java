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
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static edu.mit.ll.nics.android.utils.constants.Database.TRACKING_LAYER_TABLE;

@Entity(tableName = TRACKING_LAYER_TABLE, indices = {
        @Index(value = {"layerName"}, unique = true)
})
public class Tracking {

    public static final String GENERAL_MESSAGE = "nics_sr";
    public static final String EOD = "nics_eod";

    @PrimaryKey(autoGenerate = true)
    private int id;

    @SerializedName("displayname")
    private String displayName;

    @SerializedName("typename")
    private String typeName;

    @SerializedName("layername")
    private String layerName;

    @SerializedName("styleicon")
    private String styleIcon;

    @SerializedName("internalurl")
    private String internalUrl;

    @SerializedName("datasourceid")
    private String dataSourceId;    //used to request token

    @Embedded
    private TrackingToken authToken;        //must be pulled with datasourceid
    private boolean active = false;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Tracking)) return false;

        Tracking tracking = (Tracking) o;

        return new EqualsBuilder()
                .append(getDisplayName(), tracking.getDisplayName())
                .append(getTypeName(), tracking.getTypeName())
                .append(getLayerName(), tracking.getLayerName())
                .append(getInternalUrl(), tracking.getInternalUrl())
                .append(isActive(), tracking.isActive())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getDisplayName())
                .append(getTypeName())
                .append(getLayerName())
                .append(getInternalUrl())
                .append(isActive())
                .toHashCode();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStyleIcon() {
        return styleIcon;
    }

    public void setStyleIcon(String styleIcon) {
        this.styleIcon = styleIcon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getInternalUrl() {
        return internalUrl;
    }

    public void setInternalUrl(String internalUrl) {
        this.internalUrl = internalUrl;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void toggle() {
        this.active = !active;
    }

    public TrackingToken getAuthToken() {
        return authToken;
    }

    public void setAuthToken(TrackingToken authToken) {
        this.authToken = authToken;
    }

    public boolean shouldExpectJson() {
        return internalUrl.contains("geoserver");
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public boolean isGeneralMessageLayer() {
        return layerName.equals(GENERAL_MESSAGE) && typeName.equals(GENERAL_MESSAGE);
    }

    public boolean isEODReportLayer() {
        return layerName.equals(EOD) && typeName.equals(EOD);
    }
}
