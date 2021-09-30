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

import edu.mit.ll.nics.android.database.entities.Feature;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;

@Dao
public interface MapMarkupDao extends BaseDao<MarkupFeature> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertHazards(List<Hazard> hazards);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> replaceHazards(List<Hazard> hazards);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateHazards(List<Hazard> obj);

    @Transaction
    default void upsertHazards(List<Hazard> hazards) {
        List<Long> ids = insertHazards(hazards);
        List<Hazard> updateList = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) == -1L) updateList.add(hazards.get(i));
        }

        if (!updateList.isEmpty()) updateHazards(updateList);
    }

    @Transaction
    default void insertMarkupFeature(MarkupFeature feature) {
        replace(feature);

        if (feature.getHazards() != null) {
            replaceHazards(feature.getHazards());
        }
    }

    @Transaction
    @Query("DELETE FROM mapMarkupTable WHERE id=:id AND sendStatus=:status")
    int deleteById(long id, int status);

    @Transaction
    @Query("DELETE FROM mapMarkupTable WHERE id=:id")
    int deleteById(long id);

    @Transaction
    @Query("DELETE FROM mapMarkupTable")
    int deleteAllData();

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE id=:id")
    Feature getMarkupFeatureById(long id);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE featureId IN (:featureIds) AND collabRoomId=:collabroomId AND sendStatus=:status ORDER BY lastUpdate DESC")
    List<Feature> getMarkupFeaturesByIds(ArrayList<String> featureIds, long collabroomId, int status);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE sendStatus=:status ORDER BY :orderBy")
    List<Feature> getAllData(String orderBy, int status);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE userName=:username AND sendStatus=:status ORDER BY lastUpdate DESC")
    List<Feature> getAllDataForUser(String username, int status);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE userName=:username AND sendStatus=:status ORDER BY :orderBy")
    List<Feature> getAllDataForUser(String username, String orderBy, int status);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE collabRoomId=:collabroomId AND sendStatus IN (:status) ORDER BY lastUpdate DESC")
    LiveData<List<Feature>> getMarkupFeaturesLiveData(long collabroomId, int[] status);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE collabRoomId=:collabroomId AND sendStatus IN (:status) ORDER BY lastUpdate DESC")
    List<Feature> getMarkupFeatures(long collabroomId, int[] status);

    @Transaction
    @Query("SELECT lastUpdate FROM mapMarkupTable WHERE collabRoomId=:collabroomId AND sendStatus=:status ORDER BY lastUpdate DESC LIMIT 1")
    long getLastMarkupTimestamp(long collabroomId, int status);

    @Transaction
    @Query("DELETE FROM mapMarkupTable WHERE collabRoomId=:collabroomId AND featureId=:featureId AND sendStatus IN (:status)")
    int deleteDataByCollabroomByFeatureId(long collabroomId, String featureId, int[] status);

    @Transaction
    @Query("SELECT * FROM mapMarkupTable WHERE collabRoomId=:collabroomId AND featureId=:featureId AND sendStatus=:status")
    Feature getMarkupFeatureByFeatureIdAndStatus(long collabroomId, String featureId, int status);

    @Transaction
    @Query("UPDATE mapMarkupTable SET sendStatus=4 WHERE sendStatus=5")
    void resetDeleteStatus();

    @Transaction
    @Query("UPDATE mapMarkupTable SET sendStatus=3 WHERE sendStatus=6")
    void resetUpdateStatus();

    @Transaction
    @Query("UPDATE mapMarkupTable SET sendStatus=0 WHERE sendStatus=1")
    void resetSendStatus();

    @Transaction
    default void resetStatus() {
        resetSendStatus();
        resetUpdateStatus();
        resetDeleteStatus();
    }
}
