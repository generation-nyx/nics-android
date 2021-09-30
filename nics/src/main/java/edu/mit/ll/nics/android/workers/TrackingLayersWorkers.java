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

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.MapApiService;
import edu.mit.ll.nics.android.api.TrackingLayersApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.geo.wfs.Feature;
import edu.mit.ll.nics.android.data.geo.wfs.FeatureCollection;
import edu.mit.ll.nics.android.data.geo.wfs.XmlParser;
import edu.mit.ll.nics.android.data.messages.TrackingMessage;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.database.entities.TrackingLayerFeature;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.utils.WfsUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class TrackingLayersWorkers {

    @HiltWorker
    public static class TrackingLayersWorker extends AppWorker {

        private final TrackingLayerRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final MapApiService mApiService;

        @AssistedInject
        public TrackingLayersWorker(@Assisted @NonNull Context context,
                                    @Assisted @NonNull WorkerParameters workerParams,
                                    TrackingLayerRepository repository,
                                    PreferencesRepository preferences,
                                    MapApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            return CallbackToFutureAdapter.getFuture(completer -> {
                Call<TrackingMessage> call = mApiService.getTrackingLayers(mPreferences.getSelectedWorkspaceId());
                call.enqueue(new AuthCallback<>(new Callback<TrackingMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<TrackingMessage> call, @NotNull Response<TrackingMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        TrackingMessage message = response.body();
                        if (message != null && message.getLayers() != null) {
                            mRepository.setTrackingLayers(message.getLayers());
                            Timber.tag(DEBUG).i("Successfully received Tracking Layers: %s", message.getCount());
                        } else {
                            Timber.tag(DEBUG).w("Received empty Tracking Layers. Status Code: %s", response.code());
                        }

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<TrackingMessage> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to receive Tracking Layers: %s", t.getLocalizedMessage());

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class TrackingLayerWFSDataWorker extends AppWorker {

        public static final int MAX_FEATURES = 500;

        private final TrackingLayerRepository mRepository;
        private final TrackingLayersApiService mApiService;

        @AssistedInject
        public TrackingLayerWFSDataWorker(@Assisted @NonNull Context context,
                                          @Assisted @NonNull WorkerParameters workerParams,
                                          TrackingLayerRepository repository,
                                          TrackingLayersApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                Tracking tracking = mRepository.getTrackingLayerByName(getInputData().getString("name"));

                if (tracking.getInternalUrl() != null && tracking.getLayerName() != null) {
                    if (tracking.shouldExpectJson()) {
                        String url = new WfsUrl.Builder(tracking.getInternalUrl(), tracking.getLayerName()).withMaxFeatures(String.valueOf(MAX_FEATURES)).build().getUrl();

                        Call<FeatureCollection> call = mApiService.getTrackingLayer(url);
                        call.enqueue(new AuthCallback<>(new Callback<FeatureCollection>() {
                            @Override
                            public void onResponse(@NotNull Call<FeatureCollection> call, @NotNull Response<FeatureCollection> response) {
                                Timber.tag(DEBUG).i("Successfully pulled tracking layer %s", tracking.getDisplayName());

                                FeatureCollection collection = response.body();
                                try {
                                    ArrayList<Feature> features = Objects.requireNonNull(collection).getFeatures();

                                    for (Feature feature : features) {
                                        feature.getProperties().setStyleIcon(tracking.getStyleIcon());
                                    }

                                    parseFeatures(features, tracking, mRepository);
                                } catch (Exception e) {
                                    Timber.tag(DEBUG).e(e, "Failed to parse feature collection from tracking layer. ");
                                }
                                completer.set(Result.success());
                            }

                            @Override
                            public void onFailure(@NotNull Call<FeatureCollection> call, @NotNull Throwable t) {
                                Timber.tag(DEBUG).i("Failed to pull tracking layer %s", tracking.getDisplayName());
                                completer.set(Result.failure());
                            }
                        }));
                    } else {
                        String url = new WfsUrl.Builder(tracking.getInternalUrl(), tracking.getLayerName())
                                .withMaxFeatures(String.valueOf(MAX_FEATURES))
                                .withOutputFormat("xml")
                                .build().getUrl();

                        Call<ResponseBody> call = mApiService.getTrackingLayerXml(url);
                        call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                                Timber.tag(DEBUG).i("Successfully pulled tracking layer %s", tracking.getDisplayName());

                                XmlParser parser = new XmlParser();

                                try (ResponseBody body = response.body()) {
                                    InputStream stream = Objects.requireNonNull(body).byteStream();

                                    FeatureCollection collection = new FeatureCollection();
                                    collection.setFeatures(parser.parse(stream));
                                    collection.setType("TRACKING TYPE");

                                    ArrayList<Feature> features = collection.getFeatures();

                                    parseFeatures(features, tracking, mRepository);
                                } catch (XmlPullParserException | IOException e) {
                                    Timber.tag(DEBUG).e(e, "Failed to parse feature collection from tracking layer. ");
                                }
                                completer.set(Result.success());
                            }

                            @Override
                            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                                Timber.tag(DEBUG).i("Failed to pull tracking layer %s", tracking.getDisplayName());
                                completer.set(Result.failure());
                            }
                        }));
                    }
                } else {
                    Timber.tag(DEBUG).w("Tracking layer doesn't have an internal url or a layer name.");
                    completer.set(Result.failure());
                }

                return Result.success();
            });
        }

        private static void parseFeatures(ArrayList<Feature> features, Tracking tracking, TrackingLayerRepository repository) {
            for (Feature feature : features) {
                String uniqueId = feature.getUniqueId();

                if (uniqueId != null) {
                    repository.addTrackingLayerFeatureToDatabase(new TrackingLayerFeature(feature, tracking.getLayerName()));
                } else {
                    Timber.tag(DEBUG).i("Failed to parse MDT id from feature properties. Can't add to database.");
                }
            }
        }
    }
}
