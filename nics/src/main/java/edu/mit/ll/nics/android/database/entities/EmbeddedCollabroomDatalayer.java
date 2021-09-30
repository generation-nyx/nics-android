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
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static edu.mit.ll.nics.android.utils.constants.Database.EMBEDDED_LAYERS_TABLE;

@Entity(tableName = EMBEDDED_LAYERS_TABLE, foreignKeys = {
        @ForeignKey(
                entity = CollabroomDataLayer.class,
                parentColumns = "datalayerId",
                childColumns = "datalayerId",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
        )},
        indices = {@Index(value = {"datalayerId"}), @Index(value = {"collabroomDatalayerId"}, unique = true)}
)
public class EmbeddedCollabroomDatalayer {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @SerializedName("collabroomid")
    private long collabroomId;

    @SerializedName("datalayerId")
    private String datalayerId;

    @SerializedName("collabroomDatalayerId")
    private Long collabroomDatalayerId;

    @SerializedName("enablemobile")
    private boolean enableMobile;

    @SerializedName("collabroomOpacity")
    private double collabroomOpacity;

    @Embedded
    private HazardInfo hazard;

    public EmbeddedCollabroomDatalayer(long collabroomId, String datalayerId, Long collabroomDatalayerId, boolean enableMobile, double collabroomOpacity, HazardInfo hazard) {
        this.collabroomId = collabroomId;
        this.datalayerId = datalayerId;
        this.collabroomDatalayerId = collabroomDatalayerId;
        this.enableMobile = enableMobile;
        this.collabroomOpacity = collabroomOpacity;
        this.hazard = hazard;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    public String getDatalayerId() {
        return datalayerId;
    }

    public void setDatalayerId(String datalayerId) {
        this.datalayerId = datalayerId;
    }

    public Long getCollabroomDatalayerId() {
        return collabroomDatalayerId;
    }

    public void setCollabroomDatalayerId(Long collabroomDatalayerId) {
        this.collabroomDatalayerId = collabroomDatalayerId;
    }

    public boolean isEnableMobile() {
        return enableMobile;
    }

    public void setEnableMobile(boolean enableMobile) {
        this.enableMobile = enableMobile;
    }

    public double getCollabroomOpacity() {
        return collabroomOpacity;
    }

    public void setCollabroomOpacity(double collabroomOpacity) {
        this.collabroomOpacity = collabroomOpacity;
    }

    public HazardInfo getHazard() {
        return hazard;
    }

    public void setHazard(HazardInfo hazard) {
        this.hazard = hazard;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EmbeddedCollabroomDatalayer)) return false;

        EmbeddedCollabroomDatalayer embeddedLayer = (EmbeddedCollabroomDatalayer) o;

        return new EqualsBuilder()
                .append(getCollabroomId(), embeddedLayer.getCollabroomId())
                .append(getDatalayerId(), embeddedLayer.getDatalayerId())
                .append(getCollabroomDatalayerId(), embeddedLayer.getCollabroomDatalayerId())
                .append(isEnableMobile(), embeddedLayer.isEnableMobile())
                .append(getCollabroomOpacity(), embeddedLayer.getCollabroomOpacity())
                .append(getHazard(), embeddedLayer.getHazard())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCollabroomId())
                .append(getDatalayerId())
                .append(getCollabroomDatalayerId())
                .append(isEnableMobile())
                .append(getCollabroomOpacity())
                .append(getHazard())
                .toHashCode();
    }
}


