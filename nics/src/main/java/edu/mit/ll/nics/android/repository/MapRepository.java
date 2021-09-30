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
import edu.mit.ll.nics.android.database.entities.Feature;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.workers.SimpleThreadCallback;
import edu.mit.ll.nics.android.workers.SimpleThreadResult;

@Singleton
public class MapRepository {

    private final MapMarkupDao mDao;
    private final ExecutorService mExecutor;
    private final PreferencesRepository mPreferences;

    @Inject
    public MapRepository(MapMarkupDao dao,
                         @DiskExecutor ExecutorService executor,
                         PreferencesRepository preferences) {
        mDao = dao;
        mExecutor = executor;
        mPreferences = preferences;
    }

    public void deleteAllMarkupFeatures() {
        mExecutor.submit(mDao::deleteAllData);
    }

    public long getLastMarkupTimestamp() {
        return mDao.getLastMarkupTimestamp(mPreferences.getSelectedCollabroomId(), SendStatus.RECEIVED.getId());
    }

    public void addMarkupToDatabase(MarkupFeature feature) {
        mExecutor.submit(() -> mDao.insertMarkupFeature(feature));
    }

    public void addMarkupToDatabase(MarkupFeature feature, SimpleThreadCallback callback) {
        mExecutor.execute(() -> {
            mDao.insertMarkupFeature(feature);
            callback.onComplete(new SimpleThreadResult.Success());
        });
    }

    public MarkupFeature getMarkupFeatureById(long id) {
        Feature feature = mDao.getMarkupFeatureById(id);

        MarkupFeature markupFeature = feature.getMarkupFeature();
        markupFeature.setHazards((ArrayList<Hazard>) feature.getHazards());

        return markupFeature;
    }

    public void deleteMarkupFeatureById(long id) {
        mExecutor.submit(() -> mDao.deleteById(id));
    }

    public ArrayList<MarkupFeature> getAllMarkupReadyToUpdateForUser(String username) {
        ArrayList<MarkupFeature> retValue = new ArrayList<>();

        ArrayList<Feature> features = new ArrayList<>(mDao.getAllDataForUser(username, SendStatus.UPDATE.getId()));

        for (Feature feature : features) {
            MarkupFeature markupFeature = feature.getMarkupFeature();
            markupFeature.setHazards((ArrayList<Hazard>) feature.getHazards());
            retValue.add(markupFeature);
        }

        return retValue;
    }

    public ArrayList<MarkupFeature> getAllMarkupReadyToSendForUser(String username) {
        ArrayList<MarkupFeature> retValue = new ArrayList<>();

        ArrayList<Feature> features = new ArrayList<>(mDao.getAllDataForUser(username, SendStatus.WAITING_TO_SEND.getId()));

        for (Feature feature : features) {
            MarkupFeature markupFeature = feature.getMarkupFeature();
            markupFeature.setHazards((ArrayList<Hazard>) feature.getHazards());
            retValue.add(markupFeature);
        }

        return retValue;
    }

    public MarkupFeature getMarkupFeatureToDeleteByFeatureId(long collabroomId, String featureToRemove) {
        Feature feature = mDao.getMarkupFeatureByFeatureIdAndStatus(collabroomId, featureToRemove, SendStatus.DELETE.getId());

        MarkupFeature retValue = feature.getMarkupFeature();
        retValue.setHazards((ArrayList<Hazard>) feature.getHazards());

        return retValue;
    }

    public ArrayList<MarkupFeature> getAllMarkupReadyToDelete(String username) {
        ArrayList<MarkupFeature> retValue = new ArrayList<>();

        ArrayList<Feature> features = new ArrayList<>(mDao.getAllDataForUser(username, "seqtime ASC", SendStatus.DELETE.getId()));
        for (Feature feature : features) {
            MarkupFeature markupFeature = feature.getMarkupFeature();
            markupFeature.setHazards((ArrayList<Hazard>) feature.getHazards());
            retValue.add(markupFeature);
        }

        return retValue;
    }

    public void deleteMarkupFeatureToDelete(long collabroomId, String featureToRemove) {
        mExecutor.submit(() -> mDao.deleteDataByCollabroomByFeatureId(collabroomId, featureToRemove,
                new int[]{ SendStatus.DELETE.getId()} ));
    }

    public void deleteMarkupHistoryForCollabroomByFeatureId(long collabroomId, String featureToRemove) {
        mExecutor.submit(() -> mDao.deleteDataByCollabroomByFeatureId(collabroomId, featureToRemove,
                new int[]{ SendStatus.RECEIVED.getId(), SendStatus.SAVED.getId()} ));
    }

    public MarkupFeature getMarkupFeatureToUpdateByFeatureId(long collabroomId, String featureToUpdate) {
        Feature feature = mDao.getMarkupFeatureByFeatureIdAndStatus(collabroomId, featureToUpdate, SendStatus.UPDATE.getId());

        MarkupFeature retValue  = feature.getMarkupFeature();
        retValue.setHazards((ArrayList<Hazard>) feature.getHazards());

        return retValue;
    }

    public void deleteMarkupToUpdateStoreAndForward(long id) {
        mExecutor.submit(() -> mDao.deleteById(id, SendStatus.UPDATE.getId()));
    }

    public void deleteMarkupStoreAndForward(long id) {
        mExecutor.submit(() -> mDao.deleteById(id, SendStatus.WAITING_TO_SEND.getId()));
    }

    public List<MarkupFeature> getMarkupFeatures(long collabroomId) {
        int[] statuses = new int[] {SendStatus.RECEIVED.getId(), SendStatus.WAITING_TO_SEND.getId(),
                SendStatus.SENT.getId(), SendStatus.UPDATING.getId(), SendStatus.UPDATE.getId(),
                SendStatus.SAVED.getId()};
        List<Feature> features = mDao.getMarkupFeatures(collabroomId, statuses);

        ArrayList<MarkupFeature> temp = new ArrayList<>();
        for (Feature feature : features) {
            MarkupFeature markupFeature = feature.getMarkupFeature();
            markupFeature.setHazards((ArrayList<Hazard>) feature.getHazards());
            temp.add(markupFeature);
        }
        return temp;
    }

    public MutableLiveData<List<MarkupFeature>> getMarkupFeaturesLiveData(long collabroomId) {
        int[] statuses = new int[] {SendStatus.RECEIVED.getId(), SendStatus.WAITING_TO_SEND.getId(),
                SendStatus.SENT.getId(), SendStatus.UPDATING.getId(), SendStatus.UPDATE.getId(),
                SendStatus.SAVED.getId()};
        LiveData<List<Feature>> features = mDao.getMarkupFeaturesLiveData(collabroomId, statuses);

        return (MutableLiveData<List<MarkupFeature>>) Transformations.switchMap(features, featureList -> {
            MutableLiveData<List<MarkupFeature>> featuresLiveData = new MutableLiveData<>();

            ArrayList<MarkupFeature> temp = new ArrayList<>();
            for (Feature feature : featureList) {
                MarkupFeature markupFeature = feature.getMarkupFeature();
                markupFeature.setHazards((ArrayList<Hazard>) feature.getHazards());
                temp.add(markupFeature);
            }

            featuresLiveData.postValue(temp);
            return featuresLiveData;
        });
    }
}
