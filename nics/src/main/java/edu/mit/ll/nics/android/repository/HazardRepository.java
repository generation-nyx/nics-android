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

import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.database.dao.HazardDao;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.GeoUtils.geometryStringToGeometry;
import static edu.mit.ll.nics.android.utils.GeoUtils.intersects;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@Singleton
public class HazardRepository {

    private final HazardDao mDao;
    private final ExecutorService mExecutor;

    @Inject
    public HazardRepository(HazardDao dao, @DiskExecutor ExecutorService executor) {
        mDao = dao;
        mExecutor = executor;
    }

    public void deleteAllHazards() {
        mExecutor.execute(mDao::deleteAllData);
    }

    public int getHazardCount(long collabroomId) {
        return mDao.getHazardCount(collabroomId);
    }

    public void addHazardToDatabase(Hazard hazard) {
        mExecutor.execute(() -> mDao.replace(hazard));
    }

    public void addHazardsToDatabase(ArrayList<Hazard> hazards) {
        mExecutor.execute(() -> mDao.replace(hazards));
    }

    public List<Hazard> getHazards(long collabroomId) {
        return mDao.getHazards(collabroomId);
    }

    public LiveData<List<Hazard>> getHazardsLiveData(long collabroomId) {
        return mDao.getHazardsLiveData(collabroomId);
    }

    public ArrayList<Hazard> getIntersectingHazards(long collabroomId, String userLocation) {
        ArrayList<Hazard> retValue = new ArrayList<>();

        ArrayList<Hazard> hazards = (ArrayList<Hazard>) mDao.getHazards(collabroomId);

        Geometry userGeometry;
        try {
            userGeometry = geometryStringToGeometry(userLocation);

            for (Hazard hazard : hazards) {
                Geometry hazardGeometry = geometryStringToGeometry(hazard.getGeometry());
                if (intersects(hazardGeometry, userGeometry)) {
                    retValue.add(hazard);
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to create Geometry object out of user's location geometry string.");
        }

        return retValue;
    }
}
