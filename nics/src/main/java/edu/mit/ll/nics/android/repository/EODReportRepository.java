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

import edu.mit.ll.nics.android.database.dao.EODReportDao;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.enums.SortBy;
import edu.mit.ll.nics.android.enums.SortOrder;
import edu.mit.ll.nics.android.workers.SimpleThreadCallback;
import edu.mit.ll.nics.android.workers.SimpleThreadResult;

import static edu.mit.ll.nics.android.utils.constants.Database.EOD_REPORT_TABLE;

@Singleton
public class EODReportRepository {

    private final EODReportDao mDao;
    private final ExecutorService mExecutor;
    private final PreferencesRepository mPreferences;

    @Inject
    public EODReportRepository(EODReportDao dao,
                               @DiskExecutor ExecutorService executor,
                               PreferencesRepository preferences) {
        mDao = dao;
        mExecutor = executor;
        mPreferences = preferences;
    }

    public EODReport getEODReportById(long id) {
        return mDao.getEODReportById(id);
    }

    public long getLastEODReportTimestamp(long incidentId, long collabRoomId) {
        return mDao.getLastEODReportTimestamp(incidentId, collabRoomId, SendStatus.RECEIVED.getId());
    }

    public List<EODReport> getEODReports() {
        return mDao.getEODReports(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId(),
                new int[]{SendStatus.RECEIVED.getId(), SendStatus.SAVED.getId()});
    }

    public LiveData<List<EODReport>> getEODReportsLiveData() {
        return mDao.getEODReportsLiveData(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId());
    }

    public void addEODReportToDatabase(EODReport report) {
        mExecutor.execute(() -> mDao.replace(report));
    }

    public void addEODReportToDatabase(EODReport report, SimpleThreadCallback callback) {
        mExecutor.execute(() -> {
            mDao.replace(report);
            callback.onComplete(new SimpleThreadResult.Success());
        });
    }

    public List<EODReport> getEODReportsReadyToSend(String user) {
        return mDao.getAllDataForUserByStatus(user, SendStatus.WAITING_TO_SEND.getId());
    }

    public void upsertEODReport(EODReport report) {
        mExecutor.execute(() -> mDao.upsert(report));
    }

    public void deleteEODReportStoreAndForward(long id) {
        mExecutor.execute(() -> mDao.deleteById(id, SendStatus.WAITING_TO_SEND.getId()));
    }

    public void deleteEODReportById(long id) {
        mExecutor.execute(() -> mDao.deleteById(id));
    }

    public void deleteAllEODReports() {
        mExecutor.execute(mDao::deleteAllData);
    }

    public LiveData<List<EODReport>> getNewEODReports(long incidentId, long collabroomId) {
        return mDao.getNewEODReports(incidentId, collabroomId, mPreferences.getUserName());
    }

    public LiveData<List<EODReport>> getUnreadEODReports(long incidentId, long collabroomId) {
        return mDao.getUnreadEODReports(incidentId, collabroomId, mPreferences.getUserName());
    }

    public PagingSource<Integer, EODReport> getEODReports(SortOrder order, SortBy by) {
        String queryString = "SELECT * FROM " + EOD_REPORT_TABLE + " WHERE incidentId = ? AND collabroomId = ? ORDER BY " + by.getSort() + " " + order.getOrder();
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, new Object[]{mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId()});
        return mDao.getReports(query);
    }

    public PagingSource<Integer, EODReport> searchEODReports(String searchQuery, SortOrder order, SortBy by) {
        String queryString = EODReportDao.searchQuery + "ORDER BY " + by.getSort() + " " + order.getOrder();
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, new Object[]{mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId(), searchQuery});
        return mDao.searchEODReports(query);
    }

    public void resetSendStatus() {
        mExecutor.execute(mDao::resetSendStatus);
    }

    public void markAsRead(long id) {
        mExecutor.execute(() -> mDao.markAsRead(id));
    }

    public void markAllEODReportsRead() {
        mExecutor.execute(() -> mDao.markAllRead(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId()));
    }
}
