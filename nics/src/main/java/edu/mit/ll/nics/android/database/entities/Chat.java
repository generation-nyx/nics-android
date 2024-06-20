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
import androidx.room.PrimaryKey;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import edu.mit.ll.nics.android.data.UserOrg;
import edu.mit.ll.nics.android.enums.SendStatus;

import static edu.mit.ll.nics.android.utils.constants.Database.CHAT_TABLE;

@Entity(tableName = CHAT_TABLE, indices = {@Index(value = {"chatId"}, unique = true)})
public class Chat {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private boolean isNew;
    private boolean hasRead;
    private long incidentId;
    private SendStatus sendStatus = SendStatus.WAITING_TO_SEND;

    @Expose
    @SerializedName("created")
    private long created;

    @Expose
    @SerializedName("message")
    private String message;

    @Expose
    @SerializedName("seqnum")
    private long seqNum;

    @Expose
    @SerializedName("chatid")
    private long chatId;

    @Expose
    @SerializedName("collabroomid")
    private long collabroomId;

    @Expose
    @SerializedName("userorgid")
    private long userOrgId;

    @SerializedName("lastupdated")
    private long lastUpdated;

    @Embedded(prefix = "userOrg_")
    @SerializedName("userorg")
    private UserOrg userOrganization;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Chat)) return false;

        Chat chat = (Chat) o;

        return new EqualsBuilder()
                .append(getCreated(), chat.getCreated())
                .append(getMessage(), chat.getMessage())
                .append(getChatId(), chat.getChatId())
                .append(getCollabroomId(), chat.getCollabroomId())
                .append(getUserOrgId(), chat.getUserOrgId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCreated())
                .append(getMessage())
                .append(getChatId())
                .append(getCollabroomId())
                .append(getUserOrgId())
                .toHashCode();
    }

    public SendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public String getNickName() {
        return getUserOrganization().getUser().getFirstName() + " " + getUserOrganization().getUser().getLastName();
    }

    public long getUserOrgId() {
        return userOrgId;
    }

    public void setUserOrgId(long userOrgId) {
        this.userOrgId = userOrgId;
    }

    public UserOrg getUserOrganization() {
        return userOrganization;
    }

    public void setUserOrganization(UserOrg userOrganization) {
        this.userOrganization = userOrganization;
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

    public String toJson() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(this);
    }
}
