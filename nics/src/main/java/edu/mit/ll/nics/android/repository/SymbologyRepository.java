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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.database.dao.MapMarkupDao;
import edu.mit.ll.nics.android.database.dao.SymbologyDao;
import edu.mit.ll.nics.android.database.entities.Feature;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.workers.SimpleThreadCallback;
import edu.mit.ll.nics.android.workers.SimpleThreadResult;

@Singleton
public class SymbologyRepository {

    private final SymbologyDao mDao;
    private final ExecutorService mExecutor;
    private final PreferencesRepository mPreferences;

    @Inject
    public SymbologyRepository(SymbologyDao dao,
                               @DiskExecutor ExecutorService executor,
                               PreferencesRepository preferences) {
        mDao = dao;
        mExecutor = executor;
        mPreferences = preferences;
    }

    public void deleteAll() {
        mExecutor.submit(mDao::deleteAllData);
    }

    public void addSymbologyToDatabase(List<SymbologyGroup> symbology) {
        mExecutor.execute(() -> mDao.replace(symbology));
    }

    public List<SymbologyGroup> getSymbology() {
        return mDao.getSymbology();
    }

    public SymbologyGroup getSymbologyByName(String name) {
        return mDao.getSymbologyByName(name);
    }

    public List<String> getSymbologyGroupNames() {
        return mDao.getSymbologyGroupNames();
    }
}
