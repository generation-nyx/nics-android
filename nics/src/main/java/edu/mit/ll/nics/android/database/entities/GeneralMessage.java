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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.enums.FormType;
import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

import static edu.mit.ll.nics.android.utils.constants.Database.GENERAL_MESSAGE_TABLE;

@Entity(tableName = GENERAL_MESSAGE_TABLE, indices = {@Index(value = {"formId"}, unique = true)})
public class GeneralMessage extends Report {

    @Embedded
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private GeneralMessageData message;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GeneralMessage)) return false;

        GeneralMessage report = (GeneralMessage) o;

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

    public GeneralMessageData getMessage() {
        if (message == null) {
            message = new GeneralMessageData();
        }
        return message;
    }

    public void setMessage(GeneralMessageData message) {
        this.message = message;
    }

    public GeneralMessage create(PreferencesRepository preferences) {
        setDraft(true);
        setFormTypeId(FormType.SR.getId());
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

    public String getAssignee() {
        return getMessage().getAssignee();
    }

    public void setAssignee(String assignee) {
        getMessage().setAssignee(assignee);
    }

    public String getUser() {
        return getMessage().getUser();
    }

    public void setUser(String user) {
        getMessage().setUser(user);
    }

    public String getUserFull() {
        return getMessage().getUserFull();
    }

    public void setUserFull(String userFull) {
        getMessage().setUserFull(userFull);
    }

    public String getDescription() {
        return getMessage().getDescription();
    }

    public void setDescription(String description) {
        getMessage().setDescription(description);
    }

    public String getCategory() {
        return getMessage().getCategory();
    }

    public void setCategory(String category) {
        getMessage().setCategory(category);
    }

    public String getImage() {
        return getMessage().getImage();
    }

    public void setImage(String image) {
        getMessage().setImage(image);
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
}

