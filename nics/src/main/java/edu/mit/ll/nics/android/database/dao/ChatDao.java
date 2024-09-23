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
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import edu.mit.ll.nics.android.database.entities.Chat;

@Dao
public interface ChatDao extends BaseDao<Chat> {

    @Query("SELECT * FROM chatTable WHERE sendStatus=:status ORDER BY :orderBy")
    List<Chat> getAllChats(String orderBy, int status);

    @Query("SELECT * FROM chatTable WHERE id=:id")
    Chat getChatById(long id);

    @Query("SELECT * FROM chatTable WHERE chatId = :chatId")
    Chat getChatByChatId(long chatId);

    @Query("DELETE FROM chatTable WHERE chatId = :chatId")
    void deleteChatByChatId(long chatId);

    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId ORDER BY :orderBy")
    PagingSource<Integer, Chat> getChats(long collabroomId, String orderBy);

    @RawQuery(observedEntities = {Chat.class})
    PagingSource<Integer, Chat> getChats(SupportSQLiteQuery query);

    @Query("SELECT * FROM chatTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND userOrg_user_userName!=:userName AND isNew = 1")
    LiveData<List<Chat>> getNewChats(long incidentId, long collabroomId, String userName);

    @Query("SELECT * FROM chatTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND userOrg_user_userName!=:userName AND hasRead = 0")
    LiveData<List<Chat>> getUnreadChats(long incidentId, long collabroomId, String userName);

    @Query("UPDATE chatTable SET hasRead=1 WHERE incidentId=:incidentId AND collabroomId=:collabroomId AND hasRead=0")
    void markAllRead(long incidentId, long collabroomId);

    @Query("UPDATE chatTable SET sendStatus=0 WHERE sendStatus=1")
    void resetSendStatus();

    @Query("DELETE FROM chatTable")
    int deleteAllChat();

    @Query("SELECT lastUpdated FROM chatTable WHERE collabroomId=:collabroomId AND sendStatus=:status ORDER BY lastUpdated DESC LIMIT 1")
    long getLastChatTimestamp(long collabroomId, int status);

    @Query("SELECT lastUpdated FROM chatTable WHERE collabroomId=:collabroomId ORDER BY lastUpdated ASC LIMIT 1")
    long getOldestChatTimestamp(long collabroomId);

    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND sendStatus=:status ORDER BY lastUpdated ASC")
    List<Chat> getDataForCollaborationRoom(long collabroomId, int status);

    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated<:timestamp AND sendStatus IN (:status) ORDER BY lastUpdated DESC LIMIT :limit")
    List<Chat> getDataForCollaborationRoomStartingFromAndGoingBack(long collabroomId, long timestamp, int limit, int[] status);

//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated<:startTimestamp AND lastUpdated>=:endTimestamp AND nickName IN (:usersToFilter) AND sendStatus IN (:status) ORDER BY lastUpdated DESC LIMIT :limit")
//    List<Chat> getDataForFilteredCollaborationRoomStartingFromAndGoingBack(long collabroomId, long startTimestamp, long endTimestamp, ArrayList<String> usersToFilter, int limit, int[] status);
//
//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated<:startTimestamp AND lastUpdated>=:endTimestamp AND sendStatus IN (:status) ORDER BY lastUpdated DESC LIMIT :limit")
//    List<Chat> getDataForTimeFilteredCollaborationRoomStartingFromAndGoingBack(long collabroomId, long startTimestamp, long endTimestamp, int limit, int[] status);
//
//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated<:timestamp AND nickName IN (:usersToFilter) AND sendStatus IN (:status) ORDER BY lastUpdated DESC LIMIT :limit")
//    List<Chat> getDataForUserFilteredCollaborationRoomStartingFromAndGoingBack(long collabroomId, long timestamp, ArrayList<String> usersToFilter, int limit, int[] status);
//
//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated>:timestamp AND sendStatus IN (:status) ORDER BY lastUpdated DESC")
//    List<Chat> getNewChatMessagesFromDate(long collabroomId, long timestamp, int[] status);
//
//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated>:newestTimestamp AND lastUpdated<=:startTimestamp AND lastUpdated>:endTimestamp AND nickName IN (:usersToFilter) AND sendStatus IN (:status) ORDER BY lastUpdated DESC")
//    List<Chat> getFilteredNewChatMessagesFromDate(long collabroomId, long newestTimestamp, long startTimestamp, long endTimestamp, ArrayList<String> usersToFilter, int[] status);
//
//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated>:newestTimestamp AND lastUpdated<=:startTimestamp AND lastUpdated>:endTimestamp AND sendStatus IN (:status) ORDER BY lastUpdated DESC")
//    List<Chat> getTimeFilteredNewChatMessagesFromDate(long collabroomId, long newestTimestamp, long startTimestamp, long endTimestamp, int[] status);
//
//    @Query("SELECT * FROM chatTable WHERE collabroomId=:collabroomId AND lastUpdated>:timestamp AND nickName IN (:usersToFilter) AND sendStatus IN (:status) ORDER BY lastUpdated DESC")
//    List<Chat> getUserFilteredNewChatMessagesFromDate(long collabroomId, long timestamp, ArrayList<String> usersToFilter, int[] status);
}
