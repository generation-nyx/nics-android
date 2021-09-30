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

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.database.AppDatabase;
import edu.mit.ll.nics.android.database.dao.MobileDeviceTrackingDao;
import edu.mit.ll.nics.android.database.entities.MobileDeviceTracking;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;

/**
 * Repository class that utilizes the {@link MobileDeviceTrackingDao} to connect to the {@link AppDatabase} and
 * make queries against the alert table.
 */
@Singleton
public class MDTRepository {

    private final MobileDeviceTrackingDao mDao;
    private final ExecutorService mExecutor;

    /**
     * Injects the dependencies for the repository class.
     *
     * @param dao The {@link MobileDeviceTrackingDao} for querying the database.
     * @param executor The {@link DiskExecutor} to run the database queries on.
     */
    @Inject
    public MDTRepository(MobileDeviceTrackingDao dao,
                        @DiskExecutor ExecutorService executor,
                        PreferencesRepository preferences) {
        mDao = dao;
        mExecutor = executor;
    }

    /**
     * Returns a {@link List<MobileDeviceTracking>} from the database that are ready to send to the
     * NICS server.
     *
     * @return {@link List<MobileDeviceTracking>} Mobile device tracks to send to server.
     */
    public List<MobileDeviceTracking> getMDTs() {
        return mDao.getMDTs("createdUTC ASC");
    }

    /**
     * Deletes the {@link MobileDeviceTracking} entry from the database provided the unique id.
     *
     * @param id The unique id of the {@link MobileDeviceTracking} to delete.
     */
    public void deleteMDT(long id) {
        mExecutor.submit(() -> mDao.deleteById(id));
    }

    /**
     * Add a {@link MobileDeviceTracking} object to the database.
     *
     * @param mdt The {@link MobileDeviceTracking} to add.
     */
    public void addMDT(MobileDeviceTracking mdt) {
        mExecutor.submit(() -> mDao.insert(mdt));
    }
}
