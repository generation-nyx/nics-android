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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

public class Organization {

    @SerializedName("orgId")
    private long orgId;

    @SerializedName("name")
    private String name;

    @SerializedName("county")
    private String county;

    @SerializedName("state")
    private String state;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("distribution")
    private String distribution;

    @SerializedName("defaultlatitude")
    private double latitude;

    @SerializedName("defaultlongitude")
    private double longitude;

    @SerializedName("parentorgid")
    private Integer parentOrgId;

    @SerializedName("country")
    private String country;

    @SerializedName("created")
    private long created;

    @SerializedName("userorgs")
    private ArrayList<UserOrg> userOrgs;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Organization)) return false;

        Organization organization = (Organization) o;

        return new EqualsBuilder()
                .append(getOrgId(), organization.getOrgId())
                .append(getName(), organization.getName())
                .append(getCounty(), organization.getCounty())
                .append(getState(), organization.getState())
                .append(getTimezone(), organization.getTimezone())
                .append(getPrefix(), organization.getPrefix())
                .append(getDistribution(), organization.getDistribution())
                .append(getLatitude(), organization.getLatitude())
                .append(getLongitude(), organization.getLongitude())
                .append(getParentOrgId(), organization.getParentOrgId())
                .append(getCountry(), organization.getCountry())
                .append(getCreated(), organization.getCreated())
                .append(getUserOrgs(), organization.getUserOrgs())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getOrgId())
                .append(getName())
                .append(getCounty())
                .append(getState())
                .append(getTimezone())
                .append(getPrefix())
                .append(getDistribution())
                .append(getLatitude())
                .append(getLongitude())
                .append(getParentOrgId())
                .append(getCountry())
                .append(getCreated())
                .append(getUserOrgs())
                .toHashCode();
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Integer getParentOrgId() {
        return parentOrgId;
    }

    public void setParentOrgId(Integer parentOrgId) {
        this.parentOrgId = parentOrgId;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public ArrayList<UserOrg> getUserOrgs() {
        return userOrgs;
    }

    public void setUserOrgs(ArrayList<UserOrg> userOrgs) {
        this.userOrgs = userOrgs;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
