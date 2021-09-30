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

import edu.mit.ll.nics.android.database.entities.Collabroom;

public class Incident {

    @SerializedName("created")
    private long created;

    @SerializedName("incidentid")
    private long incidentId;

    @SerializedName("parentincidentid")
    private long parentIncidentId;

    @SerializedName("usersessionid")
    private long userSessionId;

    @SerializedName("incidentname")
    private String incidentName;

    @SerializedName("lat")
    private double latitude;

    @SerializedName("lon")
    private double longitude;

    @SerializedName("active")
    private boolean active;

    @SerializedName("description")
    private String description;

    @SerializedName("workspaceid")
    private long workspaceId;

    @SerializedName("collabrooms")
    private ArrayList<Collabroom> collabrooms;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Incident)) return false;

        Incident incident = (Incident) o;

        return new EqualsBuilder()
                .append(getIncidentId(), incident.getIncidentId())
                .append(getIncidentName(), incident.getIncidentName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getIncidentId())
                .append(getIncidentName())
                .toHashCode();
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public long getParentIncidentId() {
        return parentIncidentId;
    }

    public void setParentIncidentId(long parentIncidentId) {
        this.parentIncidentId = parentIncidentId;
    }

    public long getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(long userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getIncidentName() {
        return incidentName;
    }

    public void setIncidentName(String incidentName) {
        this.incidentName = incidentName;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public ArrayList<Collabroom> getCollabrooms() {
        return collabrooms;
    }

    public void setCollabrooms(ArrayList<Collabroom> collabrooms) {
        this.collabrooms = collabrooms;
    }

    public boolean containsCollabroom(String collabroomName, long collabroomId) {
        for (Collabroom room : collabrooms) {
            if (room.getCollabRoomId() == collabroomId && room.getName().equals(collabroomName)) {
                return true;
            }
        }
        return false;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
