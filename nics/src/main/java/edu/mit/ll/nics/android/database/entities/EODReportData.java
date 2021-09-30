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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;

public class EODReportData {

    @SerializedName("user")
    private String user;

    @SerializedName("image")
    private String image;

    @SerializedName("assign")
    private String assignee;

    @SerializedName("userFull")
    private String userFull;

    @SerializedName("status")
    private String status;

    @SerializedName("desc")
    private String description;

    @SerializedName("team")
    private String team;

    @SerializedName("canton")
    private String canton;

    @SerializedName("town")
    private String town;

    @SerializedName("tasktype")
    private String taskType;

    @SerializedName("contactPerson")
    private String contactPerson;

    @SerializedName("contactPhone")
    private String contactPhone;

    @SerializedName("contactAddress")
    private String contactAddress;

    @SerializedName("macID")
    private String macID;

    @SerializedName("medevacPointTimeDistance")
    private String medevacPointTimeDistance;

    @SerializedName("remarks")
    private String remarks;

    @SerializedName("expendedResources")
    private String expendedResources;

    @SerializedName("directlyInvolved")
    private String directlyInvolved;

    @SerializedName("fullPath")
    private String fullPath;

    @SerializedName("lat")
    private double latitude;

    @SerializedName("lon")
    private double longitude;

    @SerializedName("uxo")
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private ArrayList<Uxo> uxo;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EODReportData)) return false;

        EODReportData data = (EODReportData) o;

        return new EqualsBuilder()
                .append(getAssignee(), data.getAssignee())
                .append(getUser(), data.getUser())
                .append(getUserFull(), data.getUserFull())
                .append(getDescription(), data.getDescription())
                .append(getTeam(), data.getTeam())
                .append(getCanton(), data.getCanton())
                .append(getTown(), data.getTown())
                .append(getContactPerson(), data.getContactPerson())
                .append(getContactPhone(), data.getContactPhone())
                .append(getContactAddress(), data.getContactAddress())
                .append(getMacID(), data.getMacID())
                .append(getMedevacPointTimeDistance(), data.getMedevacPointTimeDistance())
                .append(getRemarks(), data.getRemarks())
                .append(getExpendedResources(), data.getExpendedResources())
                .append(getDirectlyInvolved(), data.getDirectlyInvolved())
                .append(getImage(), data.getImage())
                .append(getFullPath(), data.getFullPath())
                .append(getLatitude(), data.getLatitude())
                .append(getLongitude(), data.getLongitude())
                .append(getUxo(), data.getUxo())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getAssignee())
                .append(getUser())
                .append(getUserFull())
                .append(getDescription())
                .append(getTeam())
                .append(getCanton())
                .append(getTown())
                .append(getContactPerson())
                .append(getContactPhone())
                .append(getContactAddress())
                .append(getMacID())
                .append(getMedevacPointTimeDistance())
                .append(getRemarks())
                .append(getExpendedResources())
                .append(getDirectlyInvolved())
                .append(getImage())
                .append(getFullPath())
                .append(getLatitude())
                .append(getLongitude())
                .append(getUxo())
                .toHashCode();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getUserFull() {
        return userFull;
    }

    public void setUserFull(String userFull) {
        this.userFull = userFull;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getCanton() {
        return canton;
    }

    public void setCanton(String canton) {
        this.canton = canton;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public String getMacID() {
        return macID;
    }

    public void setMacID(String macID) {
        this.macID = macID;
    }

    public String getMedevacPointTimeDistance() {
        return medevacPointTimeDistance;
    }

    public void setMedevacPointTimeDistance(String medevacPointTimeDistance) {
        this.medevacPointTimeDistance = medevacPointTimeDistance;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getExpendedResources() {
        return expendedResources;
    }

    public void setExpendedResources(String expendedResources) {
        this.expendedResources = expendedResources;
    }

    public String getDirectlyInvolved() {
        return directlyInvolved;
    }

    public void setDirectlyInvolved(String directlyInvolved) {
        this.directlyInvolved = directlyInvolved;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
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

    public ArrayList<Uxo> getUxo() {
        return uxo;
    }

    public void setUxo(ArrayList<Uxo> uxo) {
        this.uxo = uxo;
    }

    public String getUxoString() {
        try {
            JSONObject obj = new JSONObject(new Gson().toJson(this));
            return obj.getJSONArray("uxo").toString();
        } catch (JSONException e) {
            return EMPTY;
        }
    }

    public String toJson() {
        return new GsonBuilder().serializeNulls().create().toJson(this);
    }
}