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

import android.content.Context;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;
import java.util.List;

import edu.mit.ll.nics.android.R;

import static edu.mit.ll.nics.android.utils.constants.Database.ALERT_TABLE;

@Entity(tableName = ALERT_TABLE)
public class Alert {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @SerializedName("alertid")
    private long alertId;

    @SerializedName("incidentid")
    private long incidentId;

    @SerializedName("username")
    private String userName;

    @SerializedName("message")
    private String message;

    @SerializedName("created")
    private long created;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Alert)) return false;

        Alert alert = (Alert) o;

        return new EqualsBuilder()
                .append(getId(), alert.getId())
                .append(getAlertId(), alert.getAlertId())
                .append(getIncidentId(), alert.getIncidentId())
                .append(getUserName(), alert.getUserName())
                .append(getMessage(), alert.getMessage())
                .append(getCreated(), alert.getCreated())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getId())
                .append(getAlertId())
                .append(getIncidentId())
                .append(getUserName())
                .append(getMessage())
                .append(getCreated())
                .toHashCode();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAlertId() {
        return alertId;
    }

    public void setAlertId(long alertId) {
        this.alertId = alertId;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * Static method to take a list of alerts as input and create a formatted message to display.
     *
     * @param alerts The alerts to use for building the message.
     * @param context {@link Context} to use to get the the resource string.
     * @return The formatted string to return.
     */
    public static String toMessage(List<Alert> alerts, Context context) {
        StringBuilder message = new StringBuilder();

        if (alerts.size() > 0) {
            for (int i = 0; i < alerts.size(); i++) {
                Alert alert = alerts.get(i);
                message.append("From: ").append(alert.getUserName()).append("\n");
                message.append(new Date(alert.getCreated()).toString()).append("\n\n\n");
                message.append(alert.getMessage()).append("\n").append("\n\n\n\n\n");
            }
        } else {
            message = new StringBuilder(context.getString(R.string.no_alerts_have_been_posted));
        }

        return message.toString();
    }
}
