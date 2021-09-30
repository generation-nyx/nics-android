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

import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.enums.SendStatus;

@Dao
public interface GeneralMessageDao extends BaseDao<GeneralMessage> {

    @Query("DELETE FROM generalMessageTable")
    int deleteAllData();

    @Query("DELETE FROM generalMessageTable WHERE id=:id AND sendStatus=:status")
    int deleteById(long id, int status);

    @Query("DELETE FROM generalMessageTable WHERE id=:id")
    int deleteById(long id);

    @Query("SELECT * FROM generalMessageTable WHERE id=:id")
    GeneralMessage getGeneralMessageById(long id);

    @Query("SELECT * FROM generalMessageTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND user!=:userName AND isNew = 1")
    LiveData<List<GeneralMessage>> getNewGeneralMessages(long incidentId, long collabroomId, String userName);

    @Query("SELECT * FROM generalMessageTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND user!=:userName AND hasRead = 0 AND isDraft = 0")
    LiveData<List<GeneralMessage>> getUnreadGeneralMessages(long incidentId, long collabroomId, String userName);

    @Query("SELECT * FROM generalMessageTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId ORDER BY seqTime ")
    PagingSource<Integer, GeneralMessage> getReports(long incidentId, long collabroomId);

    @RawQuery(observedEntities = {GeneralMessage.class})
    PagingSource<Integer, GeneralMessage> getReports(SupportSQLiteQuery query);

    @Query("SELECT * FROM generalMessageTable WHERE sendStatus=:status AND user=:user ORDER BY seqTime DESC")
    List<GeneralMessage> getAllDataForUserByStatus(String user, int status);

    @Query("SELECT seqTime FROM generalMessageTable WHERE incidentid=:incidentId AND collabroomId=:collabroomId AND sendStatus=:status ORDER BY seqTime DESC LIMIT 1")
    long getLastGeneralMessageTimestamp(long incidentId, long collabroomId, int status);

    @Query("SELECT * FROM generalMessageTable WHERE incidentId=:incidentId AND sendStatus=:status ORDER BY seqTime DESC LIMIT 1")
    GeneralMessage getLastDataForIncidentId(long incidentId, int status);

    @Query("SELECT * FROM generalMessageTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND sendStatus IN (:status) ORDER BY seqTime DESC")
    List<GeneralMessage> getGeneralMessages(long incidentId, long collabroomId, int[] status);

    @Query("SELECT * FROM generalMessageTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId ORDER BY seqTime DESC")
    LiveData<List<GeneralMessage>> getGeneralMessagesLiveData(long incidentId, long collabroomId);

    @Query("UPDATE generalMessageTable SET hasRead=1 WHERE incidentId=:incidentId AND collabroomId=:collabroomId")
    void markAllRead(long incidentId, long collabroomId);

    @Query("UPDATE generalMessageTable SET hasRead=1 WHERE id=:id")
    void markAsRead(long id);

    @Query("UPDATE generalMessageTable SET sendStatus=:status WHERE id=:id")
    void updateSendStatus(long id, SendStatus status);

    @Query("UPDATE generalMessageTable SET sendStatus=0 WHERE sendStatus=1")
    void resetSendStatus();

    @Transaction
    @RawQuery(observedEntities = {GeneralMessage.class})
    PagingSource<Integer, GeneralMessage> searchGeneralMessages(SupportSQLiteQuery query);

    String searchQuery = "SELECT generalMessageTable.*, generalMessageTable.user, generalMessageTable.image, generalMessageTable.assignee, " +
            " generalMessageTable.userFull, generalMessageTable.description, generalMessageTable.category, generalMessageTable.fullPath, generalMessageTable.latitude, generalMessageTable.longitude " +
            " FROM generalMessageTable JOIN generalMessageFts ON (generalMessageTable.id == generalMessageFts.rowid) " +
            " WHERE generalMessageTable.incidentId=? AND generalMessageTable.collabroomId=? AND generalMessageFts MATCH ? ";
}