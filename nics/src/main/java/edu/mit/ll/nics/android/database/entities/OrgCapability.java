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

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OrgCapability {

    @SerializedName("orgId")
    private long orgId;

    @SerializedName("orgCapId")
    private long orgCapId;

    @SerializedName("updated")
    private String updated;

    @SerializedName("activeWeb")
    private boolean activeWeb;

    @SerializedName("activeMobile")
    private boolean activeMobile;

    @SerializedName("cap")
    private OrgCapabilityData cap;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof OrgCapability)) return false;

        OrgCapability orgCapability = (OrgCapability) o;

        return new EqualsBuilder()
                .append(getOrgId(), orgCapability.getOrgId())
                .append(getOrgCapId(), orgCapability.getOrgCapId())
                .append(getUpdated(), orgCapability.getUpdated())
                .append(isActiveWeb(), orgCapability.isActiveWeb())
                .append(isActiveMobile(), orgCapability.isActiveMobile())
                .append(getCap(), orgCapability.getCap())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getOrgId())
                .append(getOrgCapId())
                .append(getUpdated())
                .append(isActiveWeb())
                .append(isActiveMobile())
                .append(getCap())
                .toHashCode();
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public long getOrgCapId() {
        return orgCapId;
    }

    public void setOrgCapId(long orgCapId) {
        this.orgCapId = orgCapId;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public boolean isActiveWeb() {
        return activeWeb;
    }

    public void setActiveWeb(boolean activeWeb) {
        this.activeWeb = activeWeb;
    }

    public boolean isActiveMobile() {
        return activeMobile;
    }

    public void setActiveMobile(boolean activeMobile) {
        this.activeMobile = activeMobile;
    }

    public OrgCapabilityData getCap() {
        return cap;
    }

    public void setCap(OrgCapabilityData cap) {
        this.cap = cap;
    }
}