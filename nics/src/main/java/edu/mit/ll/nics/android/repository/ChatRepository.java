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
package edu.mit.ll.nics.android.repository;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingData;
import androidx.paging.PagingSource;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.database.AppDatabase;
import edu.mit.ll.nics.android.database.dao.ChatDao;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.workers.SimpleThreadCallback;
import edu.mit.ll.nics.android.workers.SimpleThreadResult;

import static edu.mit.ll.nics.android.utils.constants.Database.CHAT_TABLE;

/**
 * Repository class that utilizes the {@link ChatDao} to connect to the {@link AppDatabase} and
 * make queries against the chat table.
 */
@Singleton
public class ChatRepository {

    private final ChatDao mDao;
    private final ExecutorService mExecutor;
    private final PreferencesRepository mPreferences;

    @Inject
    public ChatRepository(ChatDao dao,
                          @DiskExecutor ExecutorService executor,
                          PreferencesRepository preferences) {
        mDao = dao;
        mExecutor = executor;
        mPreferences = preferences;
    }

    /**
     * Delete all of the chat entries from the chat database table.
     */
    public void deleteAllChat() {
        mExecutor.execute(mDao::deleteAllChat);
    }

    /**
     * Get the timestamp from last {@link Chat} that was inserted into the database.
     *
     * @param collabroomId The current selected collabroom id.
     * @return long The latest chat timestamp.
     */
    public long getLastChatTimestamp(long collabroomId) {
        return mDao.getLastChatTimestamp(collabroomId, SendStatus.RECEIVED.getId());
    }

    public long getOldestChatTimestamp(long collabroomId) {
        return mDao.getOldestChatTimestamp(collabroomId);
    }

    /**
     * Adds a {@link Chat} to the database. It will replace the entry in the table if a conflict
     * occurs.
     *
     * @param chat The {@link Chat} to add to the database.
     */
    public void addChatToDatabase(Chat chat) {
        mExecutor.execute(() -> mDao.replace(chat));
    }

    public void addChatToDatabase(Chat chat, SimpleThreadCallback callback) {
        mExecutor.execute(() -> {
            mDao.replace(chat);
            callback.onComplete(new SimpleThreadResult.Success());
        });
    }

    /**
     * Returns a {@link List<Chat>} that hasn't been sent to the NICS server yet.
     *
     * @return {@link List<Chat>} The list of chats to send.
     */
    public List<Chat> getChatToSend() {
        return mDao.getAllChats("lastUpdated ASC", SendStatus.WAITING_TO_SEND.getId());
    }

    public Chat getChatById(long id) {
        return mDao.getChatById(id);
    }

    public LiveData<List<Chat>> getNewChats(long incidentId, long collabroomId) {
        return mDao.getNewChats(incidentId, collabroomId, mPreferences.getUserName());
    }

    public LiveData<List<Chat>> getUnreadChats(long incidentId, long collabroomId) {
        return mDao.getUnreadChats(incidentId, collabroomId, mPreferences.getUserName());
    }

    public void markAllRead(long incidentId, long collabroomId) {
        mExecutor.execute(() -> mDao.markAllRead(incidentId, collabroomId));
    }

    /**
     * Returns a {@link LiveData} {@link PagingData<Chat>} for use with a {@link RecyclerView}.
     *
     * @param incidentId The current selected incident id.
     * @param collabroomId The current selected collabroom id.
     * @return {@link LiveData} {@link PagingData<Chat>} Chat messages in a paged list for viewing
     * in the recycler view.
     */
    public PagingSource<Integer, Chat> getChats(long incidentId, long collabroomId) {
        String queryString = "SELECT * FROM " + CHAT_TABLE + " WHERE incidentId = ? AND collabroomId = ? ORDER BY created ASC";
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, new Object[]{incidentId, collabroomId});
        return mDao.getChats(query);
    }
}
