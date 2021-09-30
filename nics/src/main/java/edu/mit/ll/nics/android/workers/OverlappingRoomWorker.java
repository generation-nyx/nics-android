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
package edu.mit.ll.nics.android.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;
import edu.mit.ll.nics.android.di.Qualifiers.NetworkExecutor;
import edu.mit.ll.nics.android.repository.CollabroomRepository;
import edu.mit.ll.nics.android.repository.OverlappingRoomLayerRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.WfsUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.FileUtils.clearDirectory;
import static edu.mit.ll.nics.android.utils.FileUtils.createTempFile;
import static edu.mit.ll.nics.android.utils.FileUtils.deleteFile;
import static edu.mit.ll.nics.android.utils.GeoUtils.parseGeojsonFile;
import static edu.mit.ll.nics.android.utils.GeoUtils.parseGeojsonOverlapping;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_OVERLAPPING_LAYERS_TEMP_FOLDER;

@HiltWorker
public class OverlappingRoomWorker extends AppWorker {

    private final PreferencesRepository mPreferences;
    private final DownloaderApiService mDownloader;
    private final CollabroomRepository mCollabroomRepository;
    private final OverlappingRoomLayerRepository mRepository;
    private final ExecutorService mNetworkExecutor;

    @AssistedInject
    public OverlappingRoomWorker(@Assisted @NonNull Context context,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 @NetworkExecutor ExecutorService executor,
                                 DownloaderApiService downloader,
                                 OverlappingRoomLayerRepository repository,
                                 CollabroomRepository collabroomRepository,
                                 PreferencesRepository preferences) {
        super(context, workerParams);

        mPreferences = preferences;
        mRepository = repository;
        mNetworkExecutor = executor;
        mDownloader = downloader;
        mCollabroomRepository = collabroomRepository;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        // Initialize the progress to 0, so that any observers can be updated that the request has started.
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

        return CallbackToFutureAdapter.getFuture(completer -> {
            long incidentId = mPreferences.getSelectedIncidentId();
            String incidentName = mPreferences.getSelectedIncidentName();
            List<OverlappingRoomLayer> storedLayers = mRepository.getOverlappingLayers(incidentId);
            List<Collabroom> rooms = mCollabroomRepository.getCollabrooms(incidentId);

            mNetworkExecutor.execute(() -> {
                HashMap<String, Collabroom> collabrooms = new HashMap<>();

                for (Collabroom collabroom : rooms) {
                    collabrooms.put(collabroom.getName(), collabroom);
                }

                // Remove any layers from local db that are no longer in the collabroom.
                if (storedLayers != null) {
                    for (OverlappingRoomLayer layer : storedLayers) {
                        if (!collabrooms.containsKey(layer.getCollabroomName())) {
                            mRepository.deleteOverlappingLayer(layer.getIncidentId(), layer.getCollabroomId());
                        }
                    }
                }

                ExecutorService service = Executors.newCachedThreadPool();
                for (Collabroom room : rooms) {
                    service.execute(() -> {
                        try {
                            OverlappingRoomLayer layer = new OverlappingRoomLayer(room.getName(), room.getCollabRoomId(), room.getCreated(), incidentName, incidentId);
                            layer.setFeatures(downloadLayerFile(layer, room.getCollabRoomId()));

                            // Check to see if the layer already exists. If it does, set the id so that we can just replace it in the db.
                            if (storedLayers != null) {
                                for (OverlappingRoomLayer overlappingRoomLayer : storedLayers) {
                                    if (overlappingRoomLayer.getCollabroomId() == layer.getCollabroomId()) {
                                        layer.setId(overlappingRoomLayer.getId());
                                        break;
                                    }
                                }
                            }

                            Timber.tag(DEBUG).i("Downloaded %s", layer.getCollabroomName());
                            mRepository.addOverlappingLayerToDatabase(layer);
                        } catch (AssertionError e) {
                            Timber.tag(DEBUG).e(e, "Error adding overlapping room layer to database.");
                        }
                    });
                }

                service.shutdown();

                try {
                    service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    service.shutdown();
                }

                Timber.tag(DEBUG).i("Fetched %s overlapping layers.", rooms.size());

                // Clean out any remaining temp files if for some reason they weren't removed.
                clearDirectory(mContext.getCacheDir() + NICS_OVERLAPPING_LAYERS_TEMP_FOLDER);

                // Set progress to 100 after you are done doing your work.
                setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                completer.set(Result.success());
            });

            return Result.success();
        });
    }

    private ArrayList<OverlappingLayerFeature> downloadLayerFile(OverlappingRoomLayer layer, long collabroomId) {
        String url = new WfsUrl.Builder(mPreferences.getGeoServerURL().concat("wfs"), layer.getTypeName()).build().getUrl();

        String tempDirectory = mContext.getCacheDir() + NICS_OVERLAPPING_LAYERS_TEMP_FOLDER;
        Call<ResponseBody> call = mDownloader.download(url);
        try {
            Response<ResponseBody> response = call.execute();
            if (response.body() != null) {
                try (InputStream stream = response.body().byteStream()) {
                    File file = createTempFile(tempDirectory);
                    Files.asByteSink(file).writeFrom(stream);
                    ArrayList<String> features = parseGeojsonFile(file);
                    deleteFile(file);
                    return parseGeojsonOverlapping(features, collabroomId);
                } catch (IOException e) {
                    Timber.tag(DEBUG).e(e, "Failed to save and parse geojson response.");
                }
            }
        } catch (IOException e) {
            Timber.tag(DEBUG).e(e, "Failed to execute wfs/geojson download call.");
        }

        return null;
    }
}
