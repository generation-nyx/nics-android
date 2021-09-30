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

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

import edu.mit.ll.nics.android.utils.gson.DateFullTypeAdapter;
import edu.mit.ll.nics.android.utils.gson.DateTimestampTypeAdapter;
import edu.mit.ll.nics.android.utils.gson.DateXmlTypeAdapter;

public class LayerProperties {

    @SerializedName("description")
    private String description;

    @SerializedName("accuracy")
    private double accuracy;

    @SerializedName("speed")
    private double speed;

    @SerializedName("deviceid")
    private String deviceId;

    @SerializedName("mobiledevicetrackid")
    private double mdtId;

    @SerializedName("name")
    private String name;

    @SerializedName("course")
    private double course;

    @SerializedName("id")
    private double layerId;

    @SerializedName("age")
    private String age;

    @SerializedName("username")
    private String userName;

    @SerializedName("created")
    @JsonAdapter(DateFullTypeAdapter.class)
    private Date created;

    @SerializedName("xmltime")
    @JsonAdapter(DateXmlTypeAdapter.class)
    private Date xmltime;

    @SerializedName("timestamp")
    @JsonAdapter(DateTimestampTypeAdapter.class)
    private Date timestamp;

    @SerializedName("workspaceid")
    private double workspaceId;

    @SerializedName("styleIcon")
    private String styleIcon;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LayerProperties)) return false;

        LayerProperties properties = (LayerProperties) o;

        return new EqualsBuilder()
                .append(getDescription(), properties.getDescription())
                .append(getAccuracy(), properties.getAccuracy())
                .append(getSpeed(), properties.getSpeed())
                .append(getDeviceId(), properties.getDeviceId())
                .append(getMdtId(), properties.getMdtId())
                .append(getName(), properties.getName())
                .append(getCourse(), properties.getCourse())
                .append(getLayerId(), properties.getLayerId())
                .append(getAge(), properties.getAge())
                .append(getUserName(), properties.getUserName())
                .append(getCreated(), properties.getCreated())
                .append(getXmltime(), properties.getXmltime())
                .append(getTimestamp(), properties.getTimestamp())
                .append(getWorkspaceId(), properties.getWorkspaceId())
                .append(getStyleIcon(), properties.getStyleIcon())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getDescription())
                .append(getAccuracy())
                .append(getSpeed())
                .append(getDeviceId())
                .append(getMdtId())
                .append(getName())
                .append(getCourse())
                .append(getLayerId())
                .append(getAge())
                .append(getUserName())
                .append(getCreated())
                .append(getXmltime())
                .append(getTimestamp())
                .append(getWorkspaceId())
                .append(getStyleIcon())
                .toHashCode();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getMdtId() {
        return mdtId;
    }

    public void setMdtId(double mdtId) {
        this.mdtId = mdtId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCourse() {
        return course;
    }

    public void setCourse(double course) {
        this.course = course;
    }

    public double getLayerId() {
        return layerId;
    }

    public void setLayerId(double layerId) {
        this.layerId = layerId;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getXmltime() {
        return xmltime;
    }

    public void setXmltime(Date xmltime) {
        this.xmltime = xmltime;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public double getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(double workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getStyleIcon() {
        return styleIcon;
    }

    public void setStyleIcon(String styleIcon) {
        this.styleIcon = styleIcon;
    }
}
