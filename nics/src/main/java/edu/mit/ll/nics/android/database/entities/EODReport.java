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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.enums.FormType;
import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

import static edu.mit.ll.nics.android.utils.StringUtils.safeTrim;
import static edu.mit.ll.nics.android.utils.constants.Database.EOD_REPORT_TABLE;

@Entity(tableName = EOD_REPORT_TABLE)
public class EODReport extends Report {

    @Embedded
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private EODReportData message;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EODReport)) return false;

        EODReport report = (EODReport) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(getMessage(), report.getMessage())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(getMessage())
                .toHashCode();
    }

    public EODReportData getMessage() {
        if (message == null) {
            message = new EODReportData();
        }
        return message;
    }

    public void setMessage(EODReportData message) {
        this.message = message;
    }

    public EODReport create(PreferencesRepository preferences) {
        setDraft(true);
        setFormTypeId(FormType.EOD.getId());
        setUserSessionId(preferences.getUserSessionId());
        setIncidentId(preferences.getSelectedIncidentId());
        setIncidentName(preferences.getSelectedIncidentName());
        setCollabroomId(preferences.getSelectedCollabroomId());
        setUser(preferences.getUserName());
        setUserFull(preferences.getUserNickName());
        return this;
    }

    public String toJson() {
        String messageString = getMessage().toJson();
        JsonElement jsonElement = new GsonBuilder().create().toJsonTree(this);
        jsonElement.getAsJsonObject().addProperty("message", messageString);
        return new Gson().toJson(this);
    }

    public String toJsonStringToSend() {
        String messageString = getMessage().toJson();
        JsonElement jsonElement = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJsonTree(this);
        jsonElement.getAsJsonObject().addProperty("message", messageString);
        return new Gson().toJson(jsonElement);
    }

    public void trimAll() {
        setUser(safeTrim(getUser()));
    }

    public String getUser() {
        return getMessage().getUser();
    }

    public void setUser(String user) {
        getMessage().setUser(user);
    }

    public String getImage() {
        return getMessage().getImage();
    }

    public void setImage(String image) {
        getMessage().setImage(image);
    }

    public String getAssignee() {
        return getMessage().getAssignee();
    }

    public void setAssignee(String assignee) {
        getMessage().setAssignee(assignee);
    }

    public String getUserFull() {
        return getMessage().getUserFull();
    }

    public void setUserFull(String userFull) {
        getMessage().setUserFull(userFull);
    }

    public String getStatus() {
        return getMessage().getStatus();
    }

    public void setStatus(String status) {
        getMessage().setStatus(status);
    }

    public String getDescription() {
        return getMessage().getDescription();
    }

    public void setDescription(String description) {
        getMessage().setDescription(description);
    }

    public String getTeam() {
        return getMessage().getTeam();
    }

    public void setTeam(String team) {
        getMessage().setTeam(team);
    }

    public String getCanton() {
        return getMessage().getCanton();
    }

    public void setCanton(String canton) {
        getMessage().setCanton(canton);
    }

    public String getTown() {
        return getMessage().getTown();
    }

    public void setTown(String town) {
        getMessage().setTown(town);
    }

    public String getTaskType() {
        return getMessage().getTaskType();
    }

    public void setTaskType(String taskType) {
        getMessage().setTaskType(taskType);
    }

    public String getContactPerson() {
        return getMessage().getContactPerson();
    }

    public void setContactPerson(String contactPerson) {
        getMessage().setContactPerson(contactPerson);
    }

    public String getContactPhone() {
        return getMessage().getContactPhone();
    }

    public void setContactPhone(String contactPhone) {
        getMessage().setContactPhone(contactPhone);
    }

    public String getContactAddress() {
        return getMessage().getContactAddress();
    }

    public void setContactAddress(String contactAddress) {
        getMessage().setContactAddress(contactAddress);
    }

    public String getMacID() {
        return getMessage().getMacID();
    }

    public void setMacID(String macID) {
        getMessage().setMacID(macID);
    }

    public String getMedevacPointTimeDistance() {
        return getMessage().getMedevacPointTimeDistance();
    }

    public void setMedevacPointTimeDistance(String medevacPointTimeDistance) {
        getMessage().setMedevacPointTimeDistance(medevacPointTimeDistance);
    }

    public String getRemarks() {
        return getMessage().getRemarks();
    }

    public void setRemarks(String remarks) {
        getMessage().setRemarks(remarks);
    }

    public String getExpendedResources() {
        return getMessage().getExpendedResources();
    }

    public void setExpendedResources(String expendedResources) {
        getMessage().setExpendedResources(expendedResources);
    }

    public String getDirectlyInvolved() {
        return getMessage().getDirectlyInvolved();
    }

    public void setDirectlyInvolved(String directlyInvolved) {
        getMessage().setDirectlyInvolved(directlyInvolved);
    }

    public String getFullPath() {
        return getMessage().getFullPath();
    }

    public void setFullPath(String fullPath) {
        getMessage().setFullPath(fullPath);
    }

    public double getLatitude() {
        return getMessage().getLatitude();
    }

    public void setLatitude(double latitude) {
        getMessage().setLatitude(latitude);
    }

    public double getLongitude() {
        return getMessage().getLongitude();
    }

    public void setLongitude(double longitude) {
        getMessage().setLongitude(longitude);
    }

    public ArrayList<Uxo> getUxo() {
        return getMessage().getUxo();
    }

    public void setUxo(ArrayList<Uxo> uxo) {
        getMessage().setUxo(uxo);
    }

    public String getUxoString() {
        return getMessage().getUxoString();
    }
}
