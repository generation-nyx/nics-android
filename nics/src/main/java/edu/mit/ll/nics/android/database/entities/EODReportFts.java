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

import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.FtsOptions;

import static edu.mit.ll.nics.android.utils.constants.Database.EOD_REPORT_FTS_TABLE;

@Fts4(contentEntity = EODReport.class, order = FtsOptions.Order.DESC)
@Entity(tableName = EOD_REPORT_FTS_TABLE)
public class EODReportFts {

    private final String user;
    private final String userFull;
    private final String description;
    private final String team;
    private final String canton;
    private final String town;
    private final String taskType;
    private final String contactPerson;
    private final String contactPhone;
    private final String contactAddress;
    private final String remarks;

    public EODReportFts(String user, String userFull, String description, String team, String canton,
                        String town, String taskType, String contactPerson, String contactPhone,
                        String contactAddress, String remarks) {
        this.user = user;
        this.userFull = userFull;
        this.description = description;
        this.team = team;
        this.canton = canton;
        this.town = town;
        this.taskType = taskType;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
        this.contactAddress = contactAddress;
        this.remarks = remarks;
    }

    public String getUser() {
        return user;
    }

    public String getUserFull() {
        return userFull;
    }

    public String getDescription() {
        return description;
    }

    public String getTeam() {
        return team;
    }

    public String getCanton() {
        return canton;
    }

    public String getTown() {
        return town;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public String getRemarks() {
        return remarks;
    }
}