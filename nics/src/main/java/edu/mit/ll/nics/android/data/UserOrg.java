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
package edu.mit.ll.nics.android.data;

import androidx.room.Embedded;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class UserOrg {

    @SerializedName("userorgid")
    private long userOrgId;

    @SerializedName("orgid")
    private long orgId;

    @SerializedName("unit")
    private String unit;

    @SerializedName("rank")
    private String rank;

    @SerializedName("description")
    private String description;

    @SerializedName("jobTitle")
    private String jobTitle;

    @SerializedName("userId")
    private long userId;

    @SerializedName("systemroleid")
    private long systemRoleId;

    @Embedded(prefix = "org_")
    @SerializedName("org")
    private Organization organization;

    @Embedded(prefix = "user_")
    @SerializedName("user")
    private User user;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserOrg)) return false;

        UserOrg userOrg = (UserOrg) o;

        return new EqualsBuilder()
                .append(getUserOrgId(), userOrg.getUserOrgId())
                .append(getOrgId(), userOrg.getOrgId())
                .append(getUnit(), userOrg.getUnit())
                .append(getRank(), userOrg.getRank())
                .append(getDescription(), userOrg.getDescription())
                .append(getJobTitle(), userOrg.getJobTitle())
                .append(getUserId(), userOrg.getUserId())
                .append(getSystemRoleId(), userOrg.getSystemRoleId())
                .append(getOrganization(), userOrg.getOrganization())
                .append(getUser(), userOrg.getUser())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getUserOrgId())
                .append(getOrgId())
                .append(getUnit())
                .append(getRank())
                .append(getDescription())
                .append(getJobTitle())
                .append(getUserId())
                .append(getSystemRoleId())
                .append(getOrganization())
                .append(getUser())
                .toHashCode();
    }

    public long getUserOrgId() {
        return userOrgId;
    }

    public void setUserOrgId(long userOrgId) {
        this.userOrgId = userOrgId;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getSystemRoleId() {
        return systemRoleId;
    }

    public void setSystemRoleId(long systemRoleId) {
        this.systemRoleId = systemRoleId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}