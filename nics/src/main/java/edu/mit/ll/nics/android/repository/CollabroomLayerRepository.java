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

import edu.mit.ll.nics.android.database.dao.CollabroomLayerDao;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.Datalayer;
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.LayerFeatureWithHazards;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;

@Singleton
public class CollabroomLayerRepository {

    private final CollabroomLayerDao mDao;
    private final ExecutorService mExecutor;

    @Inject
    public CollabroomLayerRepository(CollabroomLayerDao dao, @DiskExecutor ExecutorService executor) {
        mDao = dao;
        mExecutor = executor;
    }

    public void deleteAllCollabroomLayers() {
        mExecutor.execute(mDao::deleteAllData);
    }

    public void addCollabroomLayerToDatabase(CollabroomDataLayer collabroomDataLayer) {
        mExecutor.execute(() -> mDao.insertCollabroomDatalayer(collabroomDataLayer));
    }

    public void updateCollabroomLayer(CollabroomDataLayer layer) {
        mExecutor.execute(() -> mDao.update(layer));
    }

    public LiveData<List<CollabroomDataLayer>> getCollabroomLayersLiveData(long collabroomId) {
        LiveData<List<Datalayer>> layers = mDao.getCollabroomLayersLiveData(collabroomId);

        return Transformations.switchMap(layers, layersList -> {
            MutableLiveData<List<CollabroomDataLayer>> layersLiveData = new MutableLiveData<>();

            // Datalayers have embedded features and embedded layer objects. Need to combine them all.
            ArrayList<CollabroomDataLayer> collabroomDataLayers = new ArrayList<>();
            for (Datalayer datalayer : layersList) {
                CollabroomDataLayer collabroomDataLayer = datalayer.getCollabroomDataLayer();
                List<LayerFeatureWithHazards> layerFeatureWithHazards = datalayer.getLayerFeatures();
                if (layerFeatureWithHazards != null) {
                    ArrayList<LayerFeature> features = new ArrayList<>();
                    for (LayerFeatureWithHazards l : layerFeatureWithHazards) {
                        LayerFeature feature = l.getLayerFeature();
                        feature.setHazard(l.getHazards());
                        features.add(feature);
                    }

                    collabroomDataLayer.setFeatures(features);
                }

                if (datalayer.getEmbeddedLayers() != null) {
                    collabroomDataLayer.setCollabroomDatalayers(datalayer.getEmbeddedLayers());
                }
                collabroomDataLayers.add(collabroomDataLayer);
            }
            layersLiveData.postValue(collabroomDataLayers);
            return layersLiveData;
        });
    }

    public List<CollabroomDataLayer> getCollabroomLayers(long collabroomId) {
        List<Datalayer> layers = mDao.getCollabroomLayers(collabroomId);

        // Datalayers have embedded features and embedded layer objects. Need to combine them all.
        ArrayList<CollabroomDataLayer> collabroomDataLayers = new ArrayList<>();
        for (Datalayer datalayer : layers) {
            CollabroomDataLayer collabroomDataLayer = datalayer.getCollabroomDataLayer();
            List<LayerFeatureWithHazards> layerFeatureWithHazards = datalayer.getLayerFeatures();
            if (layerFeatureWithHazards != null) {
                ArrayList<LayerFeature> features = new ArrayList<>();
                for (LayerFeatureWithHazards l : layerFeatureWithHazards) {
                    LayerFeature feature = l.getLayerFeature();
                    feature.setHazard(l.getHazards());
                    features.add(feature);
                }

                collabroomDataLayer.setFeatures(features);
            }

            if (datalayer.getEmbeddedLayers() != null) {
                collabroomDataLayer.setCollabroomDatalayers(datalayer.getEmbeddedLayers());
            }
            collabroomDataLayers.add(collabroomDataLayer);
        }

        return collabroomDataLayers;
    }

    public void deleteCollabroomLayer(long collabroomId) {
        mExecutor.execute(() -> mDao.deleteCollabroomLayer(collabroomId));
    }

    public void deleteCollabroomLayer(long collabroomId, long collabroomDatalayerId) {
        mExecutor.execute(() -> mDao.deleteCollabroomLayer(collabroomId, collabroomDatalayerId));
    }
}
