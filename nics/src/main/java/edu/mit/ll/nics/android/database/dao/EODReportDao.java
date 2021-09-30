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
package edu.mit.ll.nics.android.database.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import edu.mit.ll.nics.android.database.entities.EODReport;

@Dao
public interface EODReportDao extends BaseDao<EODReport> {

    @Query("DELETE FROM eodReportTable WHERE id=:id AND sendStatus=:status")
    int deleteById(long id, int status);

    @Query("DELETE FROM eodReportTable WHERE id=:id")
    int deleteById(long id);

    @Query("DELETE FROM eodReportTable")
    int deleteAllData();

    @Query("SELECT * FROM eodReportTable WHERE id=:id")
    EODReport getEODReportById(long id);

    @Query("SELECT * FROM eodReportTable WHERE incidentid=:incidentId AND collabroomId=:collabroomId ORDER BY seqTime")
    PagingSource<Integer, EODReport> getReports(long incidentId, long collabroomId);

    @RawQuery(observedEntities = {EODReport.class})
    PagingSource<Integer, EODReport> getReports(SupportSQLiteQuery query);

    @Query("SELECT * FROM eodReportTable WHERE sendStatus=:status AND user=:user ORDER BY seqTime DESC")
    List<EODReport> getAllDataForUserByStatus(String user, int status);

    @Query("SELECT seqTime FROM eodReportTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND sendStatus=:status ORDER BY seqTime DESC LIMIT 1")
    long getLastEODReportTimestamp(long incidentId, long collabroomId, int status);

    @Query("SELECT * FROM eodReportTable WHERE incidentId=:incidentId AND sendStatus=:status ORDER BY seqTime DESC LIMIT 1")
    EODReport getLastDataForIncidentId(long incidentId, int status);

    @Query("SELECT * FROM eodReportTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND sendStatus IN (:status) ORDER BY seqTime DESC")
    List<EODReport> getEODReports(long incidentId, long collabroomId, int[] status);

    @Query("SELECT * FROM eodReportTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId ORDER BY seqTime DESC")
    LiveData<List<EODReport>> getEODReportsLiveData(long incidentId, long collabroomId);

    @Query("SELECT * FROM eodReportTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND user!=:userName AND isNew = 1")
    LiveData<List<EODReport>> getNewEODReports(long incidentId, long collabroomId, String userName);

    @Query("SELECT * FROM eodReportTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND user!=:userName AND hasRead = 0 AND isDraft = 0")
    LiveData<List<EODReport>> getUnreadEODReports(long incidentId, long collabroomId, String userName);

    @Query("UPDATE eodReportTable SET hasRead=1 WHERE id=:id")
    void markAsRead(long id);

    @Query("UPDATE eodReportTable SET sendStatus=0 WHERE sendStatus=1")
    void resetSendStatus();

    @Query("UPDATE eodReportTable SET hasRead=1 WHERE incidentId=:incidentId AND collabroomId=:collabroomId")
    void markAllRead(long incidentId, long collabroomId);

    @Transaction
    @RawQuery(observedEntities = {EODReport.class})
    PagingSource<Integer, EODReport> searchEODReports(SupportSQLiteQuery query);

    String searchQuery = "SELECT eodReportTable.*, eodReportTable.user, eodReportTable.image, eodReportTable.assignee, " +
            " eodReportTable.userFull, eodReportTable.description, eodReportTable.fullPath, eodReportTable.latitude, eodReportTable.longitude " +
            " FROM eodReportTable JOIN eodReportFts ON (eodReportTable.id = eodReportFts.rowid) " +
            " WHERE eodReportTable.incidentId=? AND eodReportTable.collabroomId=? AND eodReportFts MATCH ? ";
}
