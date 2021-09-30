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
package edu.mit.ll.nics.android.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.Datalayer;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.LayerFeatureWithHazards;
import edu.mit.ll.nics.android.database.entities.EmbeddedCollabroomDatalayer;
import edu.mit.ll.nics.android.database.entities.LayerFeature;

@Dao
public interface CollabroomLayerDao extends BaseDao<CollabroomDataLayer> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLayerFeatures(List<LayerFeature> layerFeatures);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertHazard(Hazard hazard);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertEmbeddedCollabroomDataLayers(List<EmbeddedCollabroomDatalayer> embeddedCollabroomDatalayers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replaceLayerFeatures(List<LayerFeature> layerFeatures);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replaceHazard(Hazard hazard);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replaceEmbeddedCollabroomDataLayers(List<EmbeddedCollabroomDatalayer> embeddedCollabroomDatalayers);

    @Transaction
    default long insertCollabroomDatalayer(CollabroomDataLayer layer) {
        int retValue = (int) insert(layer);

        // If there was a conflict, check to see if the datalayer has changed at all before updating.
        // We want to only update them when they change. Otherwise it will cause the map to flash and the hazard notifications to trigger.
        if (retValue == -1) {
            for (Datalayer datalayer : getCollabroomLayers(layer.getCollabroomId(), layer.getDatalayerId())) {
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

                // If the new layer is different, then update the layer. It should CASCADE the update.
                if (!layer.equals(collabroomDataLayer)) {
                    if (collabroomDataLayer.isActive()) {
                        layer.setActive(true);
                    }
                    retValue = (int) replace(layer);

                    if (layer.getCollabroomDatalayers() != null && layer.getCollabroomDatalayers().size() > 0) {
                        replaceEmbeddedCollabroomDataLayers(layer.getCollabroomDatalayers());
                    }

                    if (layer.getFeatures() != null) {
                        replaceLayerFeatures(layer.getFeatures());

                        for (LayerFeature feature : layer.getFeatures()) {
                            Hazard hazard = feature.getHazard();
                            if (hazard != null) {
                                replaceHazard(hazard);
                            }
                        }
                    }
                }
            }
        } else {
            if (layer.getCollabroomDatalayers() != null && layer.getCollabroomDatalayers().size() > 0) {
                insertEmbeddedCollabroomDataLayers(layer.getCollabroomDatalayers());
            }

            if (layer.getFeatures() != null) {
                insertLayerFeatures(layer.getFeatures());

                for (LayerFeature feature : layer.getFeatures()) {
                    Hazard hazard = feature.getHazard();
                    if (hazard != null) {
                        insertHazard(hazard);
                    }
                }
            }
        }

        return retValue;
    }

    @Transaction
    @Query("SELECT * FROM collabroomLayersTable WHERE collabroomId=:collabroomId ORDER BY created DESC")
    LiveData<List<Datalayer>> getCollabroomLayersLiveData(long collabroomId);

    @Transaction
    @Query("SELECT * FROM collabroomLayersTable WHERE collabroomId=:collabroomId AND datalayerId=:datalayerId ORDER BY created DESC")
    List<Datalayer> getCollabroomLayers(long collabroomId, String datalayerId);

    @Transaction
    @Query("SELECT * FROM collabroomLayersTable WHERE collabroomId=:collabroomId ORDER BY created DESC")
    List<Datalayer> getCollabroomLayers(long collabroomId);

    @Transaction
    @Query("UPDATE collabroomLayersTable SET active=:isActive WHERE datalayerId=:datalayerId")
    int updateCollabroomLayerIsActive(String datalayerId, boolean isActive);

    @Transaction
    @Query("UPDATE collabroomLayersTable SET active=0")
    int updateAllCollabroomLayersToInactive();

    @Transaction
    @Query("DELETE FROM collabroomLayersTable WHERE collabroomId=:collabroomId AND EXISTS (SELECT * FROM collabroomLayersTable INNER JOIN embeddedLayersTable ON embeddedLayersTable.datalayerId=collabroomLayersTable.datalayerId WHERE collabroomDatalayerId=:collabroomDatalayerId)")
    int deleteCollabroomLayer(long collabroomId, long collabroomDatalayerId);

    @Transaction
    @Query("DELETE FROM collabroomLayersTable WHERE collabroomId=:collabroomId")
    int deleteCollabroomLayer(long collabroomId);

    @Transaction
    @Query("DELETE FROM collabroomLayersTable")
    int deleteAllData();
}
