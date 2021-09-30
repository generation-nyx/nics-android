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

import edu.mit.ll.nics.android.database.AppDatabase;
import edu.mit.ll.nics.android.database.dao.OverlappingRoomLayerDao;
import edu.mit.ll.nics.android.database.entities.OverlappingDatalayer;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;

/**
 * Repository class that utilizes the {@link OverlappingRoomLayerDao} to connect to the
 * {@link AppDatabase} and make queries against the overlapping room layer table.
 */
@Singleton
public class OverlappingRoomLayerRepository {

    private final OverlappingRoomLayerDao mDao;
    private final ExecutorService mExecutor;

    @Inject
    public OverlappingRoomLayerRepository(OverlappingRoomLayerDao dao,
                                          @DiskExecutor ExecutorService executor) {
        mDao = dao;
        mExecutor = executor;
    }

    public LiveData<List<OverlappingRoomLayer>> getOverlappingLayersLiveData(long incidentId, long collabroomId) {
        LiveData<List<OverlappingDatalayer>> layers = mDao.getOverlappingLayersLiveData(incidentId, collabroomId);

        return Transformations.switchMap(layers, layersList -> {
            MutableLiveData<List<OverlappingRoomLayer>> layersLiveData = new MutableLiveData<>();

            // Datalayers have embedded features and embedded layer objects. Need to combine them all.
            ArrayList<OverlappingRoomLayer> overlappingLayers = new ArrayList<>();
            for (OverlappingDatalayer datalayer : layersList) {
                OverlappingRoomLayer roomLayer = datalayer.getOverlappingRoomLayer();

                if (datalayer.getLayerFeatures() != null) {
                    roomLayer.setFeatures(datalayer.getLayerFeatures());
                }
                overlappingLayers.add(roomLayer);
            }
            layersLiveData.postValue(overlappingLayers);
            return layersLiveData;
        });
    }

    public List<OverlappingRoomLayer> getOverlappingLayers(long incidentId) {
        List<OverlappingDatalayer> layers = mDao.getOverlappingLayers(incidentId);

        // Datalayers have embedded features and embedded layer objects. Need to combine them all.
        ArrayList<OverlappingRoomLayer> overlappingLayers = new ArrayList<>();
        for (OverlappingDatalayer datalayer : layers) {
            OverlappingRoomLayer roomLayer = datalayer.getOverlappingRoomLayer();

            if (datalayer.getLayerFeatures() != null) {
                roomLayer.setFeatures(datalayer.getLayerFeatures());
            }
            overlappingLayers.add(roomLayer);
        }

        return overlappingLayers;
    }

    public void addOverlappingLayerToDatabase(OverlappingRoomLayer layer) {
        mExecutor.execute(() -> mDao.insertOverlappingLayer(layer));
    }

    public void deleteOverlappingLayer(long incidentId, long collabroomId) {
        mExecutor.execute(() -> mDao.deleteOverlappingLayer(incidentId, collabroomId));
    }

    public void deleteOverlappingLayers() {
        mExecutor.execute(mDao::deleteAllData);
    }

    public void updateOverlappingLayer(OverlappingRoomLayer layer) {
        mExecutor.execute(() -> mDao.update(layer));
    }
}
