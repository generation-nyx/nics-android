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
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

import edu.mit.ll.nics.android.database.entities.OverlappingDatalayer;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;

@Dao
public interface OverlappingRoomLayerDao extends BaseDao<OverlappingRoomLayer> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replaceLayerFeatures(List<OverlappingLayerFeature> layerFeatures);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertLayerFeatures(List<OverlappingLayerFeature> layerFeatures);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateLayerFeatures(List<OverlappingLayerFeature> layerFeatures);

    @Transaction
    default void upsertLayerFeatures(List<OverlappingLayerFeature> features) {
        List<Long> ids = insertLayerFeatures(features);
        List<OverlappingLayerFeature> updateList = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) == -1L) updateList.add(features.get(i));
        }

        if (!updateList.isEmpty()) updateLayerFeatures(updateList);
    }

    @Transaction
    default int insertOverlappingLayer(OverlappingRoomLayer layer) {
        int retValue = (int) insert(layer);

        // If there was a conflict, check to see if the datalayer has changed at all before updating.
        // We want to only update them when they change. Otherwise it will cause the map to flash.
        if (retValue == -1) {
            for (OverlappingDatalayer datalayer : getOverlappingLayers(layer.getIncidentId(), layer.getCollabroomId())) {
                OverlappingRoomLayer roomLayer = datalayer.getOverlappingRoomLayer();
                roomLayer.setFeatures(datalayer.getLayerFeatures());

                // If the new layer is different, then update the layer. It should CASCADE the update.
                if (!layer.equals(roomLayer)) {
                    if (roomLayer.isActive()) {
                        layer.setActive(true);
                    }
                    retValue = (int) replace(layer);

                    if (layer.getFeatures() != null) {
                        upsertLayerFeatures(layer.getFeatures());
                    }
                }
            }
        } else {
            if (layer.getFeatures() != null) {
                insertLayerFeatures(layer.getFeatures());
            }
        }

        return retValue;
    }

    @Transaction
    @Query("SELECT * FROM overlappingRoomLayersTable WHERE incidentId=:incidentId AND collabroomId!=:collabroomId ORDER BY incidentId DESC")
    LiveData<List<OverlappingDatalayer>> getOverlappingLayersLiveData(long incidentId, long collabroomId);

    @Transaction
    @Query("SELECT * FROM overlappingRoomLayersTable WHERE incidentId=:incidentId AND collabroomId=:collabroomId ORDER BY incidentId DESC")
    List<OverlappingDatalayer> getOverlappingLayers(long incidentId, long collabroomId);

    @Transaction
    @Query("SELECT * FROM overlappingRoomLayersTable WHERE incidentId=:incidentId ORDER BY incidentId DESC")
    List<OverlappingDatalayer> getOverlappingLayers(long incidentId);

    @Transaction
    @Query("UPDATE overlappingRoomLayersTable SET isActive=0")
    int updateAllOverlappingRoomLayersToInactive();

    @Transaction
    @Query("DELETE FROM overlappingRoomLayersTable")
    int deleteAllData();

    @Transaction
    @Query("DELETE FROM overlappingRoomLayersTable WHERE collabroomId=:collabroomId AND incidentId=:incidentId")
    int deleteOverlappingLayer(long incidentId, long collabroomId);

    @Transaction
    @Query("DELETE FROM overlappingRoomLayersTable WHERE incidentId=:incidentId")
    int deleteOverlappingLayer(long incidentId);
}
