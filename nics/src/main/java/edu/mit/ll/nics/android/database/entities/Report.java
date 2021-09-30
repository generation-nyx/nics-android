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

import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import edu.mit.ll.nics.android.enums.SendStatus;

public class Report {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private boolean isDraft;
    private boolean isNew;
    private boolean hasRead;
    private SendStatus sendStatus = SendStatus.WAITING_TO_SEND;

    @Expose
    @SerializedName("formId")
    private long formId;

    @SerializedName("formtypeid")
    private long formTypeId;

    @Expose
    @SerializedName("incidentid")
    private long incidentId;

    @Expose
    @SerializedName("usersessionid")
    private long userSessionId;

    @Expose
    @SerializedName("seqtime")
    private long seqTime;

    @Expose
    @SerializedName("seqnum")
    private long seqNum;

    @Expose
    @SerializedName("incidentname")
    private String incidentName;

    @Expose
    @SerializedName("collabroomid")
    private long collabroomId;

    @Ignore
    private int progress;

    private boolean failedToSend;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Report)) return false;

        Report report = (Report) o;

        return new EqualsBuilder()
                .append(getId(), report.getId())
                .append(isDraft(), report.isDraft())
                .append(isNew(), report.isNew())
                .append(getSendStatus(), report.getSendStatus())
                .append(getFormId(), report.getFormId())
                .append(getFormTypeId(), report.getFormTypeId())
                .append(getIncidentId(), report.getIncidentId())
                .append(getUserSessionId(), report.getUserSessionId())
                .append(getSeqTime(), report.getSeqTime())
                .append(getSeqNum(), report.getSeqNum())
                .append(getIncidentName(), report.getIncidentName())
                .append(getCollabroomId(), report.getCollabroomId())
                .append(hasRead(), report.hasRead())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getId())
                .append(isDraft())
                .append(isNew())
                .append(getSendStatus())
                .append(getFormId())
                .append(getFormTypeId())
                .append(getIncidentId())
                .append(getUserSessionId())
                .append(getSeqTime())
                .append(getSeqNum())
                .append(getIncidentName())
                .append(getCollabroomId())
                .append(hasRead())
                .toHashCode();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public void setDraft(boolean isDraft) {
        this.isDraft = isDraft;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean hasRead() {
        return hasRead;
    }

    public void setRead(boolean hasRead) {
        this.hasRead = hasRead;
    }

    public long getFormId() {
        return formId;
    }

    public void setFormId(long formId) {
        this.formId = formId;
    }

    public long getFormTypeId() {
        return formTypeId;
    }

    public void setFormTypeId(long formTypeId) {
        this.formTypeId = formTypeId;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    public long getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(long userSessionId) {
        this.userSessionId = userSessionId;
    }

    public long getSeqTime() {
        return seqTime;
    }

    public void setSeqTime(long seqTime) {
        this.seqTime = seqTime;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public SendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getIncidentName() {
        return incidentName;
    }

    public void setIncidentName(String incidentName) {
        this.incidentName = incidentName;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isFailedToSend() {
        return failedToSend;
    }

    public void setFailedToSend(boolean failedToSend) {
        this.failedToSend = failedToSend;
    }
}
