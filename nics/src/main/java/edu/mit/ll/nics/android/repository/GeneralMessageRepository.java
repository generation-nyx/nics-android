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
import androidx.paging.PagingSource;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.database.dao.GeneralMessageDao;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.enums.SortBy;
import edu.mit.ll.nics.android.enums.SortOrder;
import edu.mit.ll.nics.android.workers.SimpleThreadCallback;
import edu.mit.ll.nics.android.workers.SimpleThreadResult;

import static edu.mit.ll.nics.android.utils.constants.Database.GENERAL_MESSAGE_TABLE;

@Singleton
public class GeneralMessageRepository {

    private final GeneralMessageDao mDao;
    private final ExecutorService mExecutor;
    private final PreferencesRepository mPreferences;

    @Inject
    public GeneralMessageRepository(GeneralMessageDao dao,
                                    @DiskExecutor ExecutorService executor,
                                    PreferencesRepository preferences) {
        mDao = dao;
        mExecutor = executor;
        mPreferences = preferences;
    }

    public PagingSource<Integer, GeneralMessage> getGeneralMessages(SortOrder order, SortBy by) {
        String queryString = "SELECT * FROM " + GENERAL_MESSAGE_TABLE + " WHERE incidentId = ? AND collabroomId = ? ORDER BY " + by.getSort() + " " + order.getOrder();
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, new Object[]{mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId()});
        return mDao.getReports(query);
    }

    public PagingSource<Integer, GeneralMessage> searchGeneralMessages(String searchQuery, SortOrder order, SortBy by) {
        String queryString = GeneralMessageDao.searchQuery + "ORDER BY " + by.getSort() + " " + order.getOrder();
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, new Object[]{mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId(), searchQuery});
        return mDao.searchGeneralMessages(query);
    }

    public LiveData<List<GeneralMessage>> getNewGeneralMessages(long incidentId, long collabroomId) {
        return mDao.getNewGeneralMessages(incidentId, collabroomId, mPreferences.getUserName());
    }

    public LiveData<List<GeneralMessage>> getUnreadGeneralMessages(long incidentId, long collabroomId) {
        return mDao.getUnreadGeneralMessages(incidentId, collabroomId, mPreferences.getUserName());
    }

    public long getLastGeneralMessageTimestamp() {
        return mDao.getLastGeneralMessageTimestamp(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId(), SendStatus.RECEIVED.getId());
    }

    public void addGeneralMessageToDatabase(GeneralMessage generalMessage) {
        mExecutor.execute(() -> mDao.replace(generalMessage));
    }

    public void addGeneralMessageToDatabase(GeneralMessage generalMessage, SimpleThreadCallback callback) {
        mExecutor.execute(() -> {
            mDao.replace(generalMessage);
            callback.onComplete(new SimpleThreadResult.Success());
        });
    }

    public void upsertGeneralMessage(GeneralMessage generalMessage) {
        mExecutor.execute(() -> mDao.upsert(generalMessage));
    }

    public void markAsRead(long id) {
        mExecutor.execute(() -> mDao.markAsRead(id));
    }

    public GeneralMessage getGeneralMessageById(long id) {
        return mDao.getGeneralMessageById(id);
    }

    public List<GeneralMessage> getGeneralMessages() {
        return mDao.getGeneralMessages(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId(),
                new int[] {SendStatus.RECEIVED.getId(), SendStatus.SAVED.getId()} );
    }

    public LiveData<List<GeneralMessage>> getGeneralMessagesLiveData() {
        return mDao.getGeneralMessagesLiveData(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId());
    }

    public List<GeneralMessage> getGeneralMessagesReadyToSend(String user) {
        return mDao.getAllDataForUserByStatus(user, SendStatus.WAITING_TO_SEND.getId());
    }

    public void deleteGeneralMessageById(long id) {
        mExecutor.execute(() -> mDao.deleteById(id));
    }

    public boolean deleteGeneralMessageStoreAndForward(long id) {
          return mDao.deleteById(id, SendStatus.WAITING_TO_SEND.getId()) > 0;
    }

    public void deleteAllGeneralMessages() {
        mExecutor.execute(mDao::deleteAllData);
    }

    public void markAllGeneralMessagesRead() {
        mExecutor.execute(() -> mDao.markAllRead(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId()));
    }
}
