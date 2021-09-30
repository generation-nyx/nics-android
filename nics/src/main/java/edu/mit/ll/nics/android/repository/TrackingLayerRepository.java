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

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.dao.TrackingLayerDao;
import edu.mit.ll.nics.android.database.dao.TrackingLayerFeatureDao;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.database.entities.TrackingLayerFeature;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;

@Singleton
public class TrackingLayerRepository {

    private final TrackingLayerDao mDao;
    private final TrackingLayerFeatureDao mFeatureDao;
    private final ExecutorService mExecutor;

    @Inject
    public TrackingLayerRepository(TrackingLayerDao dao,
                                   TrackingLayerFeatureDao featureDao,
                                   @DiskExecutor ExecutorService executor) {
        mDao = dao;
        mFeatureDao = featureDao;
        mExecutor = executor;
    }

    public void initializeTrackingLayers(Context context, String url) {
        mExecutor.execute(() -> {
            mDao.deleteAllData();
            mDao.insert(getDefaultTrackingLayers(context, url));
        });
    }

    private ArrayList<Tracking> getDefaultTrackingLayers(Context context, String url) {
        ArrayList<Tracking> defaultTrackingLayers = new ArrayList<>();
        Tracking general = new Tracking();
        general.setDisplayName(context.getString(R.string.wfslayer_nics_general_message_title));
        general.setLayerName(Tracking.GENERAL_MESSAGE);
        general.setInternalUrl(url);
        general.setTypeName(Tracking.GENERAL_MESSAGE);

        Tracking eod = new Tracking();
        eod.setDisplayName(context.getString(R.string.wfslayer_nics_eod_report_title));
        eod.setLayerName(Tracking.EOD);
        eod.setInternalUrl(url);
        eod.setTypeName(Tracking.EOD);

        defaultTrackingLayers.add(general);
        defaultTrackingLayers.add(eod);

        return defaultTrackingLayers;
    }

    public Tracking getTrackingLayerByName(String name) {
        return mDao.getTrackingLayerByName(name);
    }

    public LiveData<List<Tracking>> getTrackingLayers() {
        return mDao.getAllTracking();
    }

    public void updateTrackingLayer(Tracking tracking) {
        mExecutor.execute(() -> mDao.update(tracking));
    }

    public void setTrackingLayers(ArrayList<Tracking> layers) {
        mExecutor.execute(() -> mDao.setTrackingLayers(layers));
    }

    public void setLayerActive(String layerName) {
        mExecutor.execute(() -> mDao.setLayerActive(layerName));
    }

    public void deleteAllTrackingLayers() {
        mExecutor.execute(mDao::deleteAllData);
    }

    public void addTrackingLayerFeatureToDatabase(TrackingLayerFeature feature) {
        mExecutor.execute(() -> mFeatureDao.replace(feature));
    }

    public LiveData<List<TrackingLayerFeature>> getTrackingFeaturesByName(String name) {
        return mFeatureDao.getTrackingFeaturesByName(name);
    }
}
