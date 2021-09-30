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
package edu.mit.ll.nics.android.maps.layers.reports;

import android.app.Activity;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.google.android.gms.maps.GoogleMap;

import java.util.List;

import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.Report;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.maps.markup.EODReportMarker;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

public class EODReportLayer extends ReportLayer<EODReport> {

    private final EODReportRepository mRepository;
    private LiveData<List<EODReport>> mDatabaseObserver;

    public EODReportLayer(Activity activity,
                          GoogleMap map,
                          Tracking tracking,
                          LifecycleOwner lifecycleOwner,
                          PreferencesRepository preferences,
                          EODReportRepository repository) {
        super(activity, map, tracking.getDisplayName(), lifecycleOwner, preferences);

        mRepository = repository;
        subscribeToUpdates();
    }

    @Override
    protected void subscribeToUpdates() {
        mDatabaseObserver = mRepository.getEODReportsLiveData();
        mDatabaseObserver.observe(mLifecycleOwner, this::updateMap);
    }

    @Override
    protected void unsubscribeFromUpdates() {
        if (mDatabaseObserver != null) {
            mDatabaseObserver.removeObservers(mLifecycleOwner);
        }
    }

    @Override
    protected <R extends Report> void addReport(R report) {
        EODReportMarker marker = new EODReportMarker((EODReport) report, mMap, mPreferences, mActivity);
        mReports.put(report.getId(), marker);
        addToMap(marker);
    }

    @Override
    protected void removeReport(long id) {
        EODReportMarker marker = (EODReportMarker) mReports.get(id);
        if (marker != null) {
            mReports.remove(id);
            marker.removeFromMap();
        }
    }

    @Override
    public Object getLayer() {
        return null;
    }
}

