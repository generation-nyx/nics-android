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

import com.google.android.gms.maps.model.LatLng;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.api.MapApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.messages.MarkupMessage;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.HazardInfo;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.repository.MapRepository;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.services.ServiceManager;
import edu.mit.ll.nics.android.utils.UnitConverter;
import edu.mit.ll.nics.android.utils.constants.Events;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.GeoUtils.bufferGeometry;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertCoordinatesToGeometryString;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertPointsToLatLng;
import static edu.mit.ll.nics.android.utils.GeoUtils.getSimplifiedPolygonForCircle;
import static edu.mit.ll.nics.android.utils.NetworkUtils.createPartFromString;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class MapWorkers {

    @HiltWorker
    public static class Get extends AppWorker {

        private final MapRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final ServiceManager mServiceManager;
        private final PersonalHistoryRepository mPersonalHistory;
        private final MapApiService mApiService;

        @AssistedInject
        public Get(@Assisted @NonNull Context context,
                   @Assisted @NonNull WorkerParameters workerParams,
                   MapRepository repository,
                   PreferencesRepository preferences,
                   ServiceManager serviceManager,
                   PersonalHistoryRepository personalHistory,
                   MapApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mServiceManager = serviceManager;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting Markup Feature Get Worker.");

            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            return CallbackToFutureAdapter.getFuture(completer -> {
                long userId = mPreferences.getUserId();
                long collabroomId = mPreferences.getSelectedCollabroomId();
                long lastTimestamp = mRepository.getLastMarkupTimestamp() + 1;

                Call<MarkupMessage> call = mApiService.getMarkupFeatures(collabroomId, userId, lastTimestamp);
                call.enqueue(new AuthCallback<>(new Callback<MarkupMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<MarkupMessage> call, @NotNull Response<MarkupMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        MarkupMessage message = response.body();
                        if (message != null && message.getFeatures() != null) {
                            for (MarkupFeature feature : message.getFeatures()) {
                                feature.buildVector2Point(true);
                            }

                            parseMarkupFeatures(message, collabroomId);
                            Timber.tag(DEBUG).i("Successfully received markup information.");
                        } else {
                            Timber.tag(DEBUG).w("Received empty markup information. Status Code: %s", response.code());
                        }

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<MarkupMessage> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to receive markup history: %s", t.getMessage());

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }

        private void parseMarkupFeatures(MarkupMessage message, long collabroomId) {
            int numParsed = 0;
            ArrayList<String> deletedFeatures = message.getDeletedFeatures();

            // Remove markup from local db if it has been removed from server database.
            for (String featureId : deletedFeatures) {
                mRepository.deleteMarkupHistoryForCollabroomByFeatureId(collabroomId, featureId);
            }

            ArrayList<MarkupFeature> features = message.getFeatures();
            if (features != null && features.size() > 0) {
                for (MarkupFeature feature : features) {
                    feature.setCollabRoomId(collabroomId);
                    feature.setSendStatus(SendStatus.RECEIVED);

                    // Parse the hazards that are associated with this feature.
                    try {
                        ArrayList<HazardInfo> hazards = feature.getAttributes().getHazards();

                        if (hazards != null) {
                            for (HazardInfo info : hazards) {
                                // TODO need to have a max and min radius value with MathUtils.clamp
                                double radius = info.getRadius();
                                if (info.getMetric().equalsIgnoreCase("kilometer")) {
                                    radius = UnitConverter.kilometersToMeters(radius);
                                }

                                ArrayList<LatLng> points = convertPointsToLatLng(feature.getGeometryVector2(), true);

                                ArrayList<LatLng> coordinates = new ArrayList<>();
                                // If there is only one point, assume it's a marker, otherwise, assume it's a polygon.
                                if (points.size() == 1) {
                                    coordinates = getSimplifiedPolygonForCircle(points.get(0), radius);
                                } else if (points.size() > 1) {
                                    try {
                                        // Buffer the geometry of the markup feature to use as a geofence.
                                        coordinates = bufferGeometry(feature.getGeometry(), feature.getType(), radius);
                                    } catch (Exception e) {
                                        Timber.tag(DEBUG).e(e, "Failed to buffer geometry for geofence boundary.");
                                    }
                                }

                                Hazard hazard = new Hazard(info, feature.getFeatureId(), collabroomId);
                                hazard.setCoordinates(coordinates);
                                hazard.setGeometry(convertCoordinatesToGeometryString(coordinates, "polygon"));
                                feature.addHazard(hazard);
                            }
                        }
                    } catch (Exception e) {
                        Timber.tag(DEBUG).e(e, "Failed to add hazards to markup feature.");
                    }

                    List<MarkupFeature> storedFeatures = mRepository.getMarkupFeatures(collabroomId);

                    // Check to see if the feature already exists. If it does, set the id so that we can just replace it in the db.
                    if (storedFeatures != null) {
                        for (MarkupFeature f : storedFeatures) {
                            if (f.getFeatureId().equals(feature.getFeatureId())) {
                                feature.setId(f.getId());
                                break;
                            }
                        }
                    }

                    mRepository.addMarkupToDatabase(feature);
                }

                numParsed = features.size();
            }

            if (numParsed > 0) {
                mPersonalHistory.addPersonalHistory("Successfully received " + numParsed + " markup features from " + mPreferences.getSelectedCollabroom().getName(),
                        mPreferences.getUserId(), mPreferences.getUserNickName());
            }

            Timber.tag(DEBUG).i("Successfully parsed %s markup features.", numParsed);
            mServiceManager.forceLocationUpdate();
        }
    }

    @HiltWorker
    public static class Post extends AppWorker {

        private final MapRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final MapApiService mApiService;

        @AssistedInject
        public Post(@Assisted @NonNull Context context,
                    @Assisted @NonNull WorkerParameters workerParams,
                    MapRepository repository,
                    PreferencesRepository preferences,
                    PersonalHistoryRepository personalHistory,
                    MapApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting Map Markup Post Worker.");

            return CallbackToFutureAdapter.getFuture(completer -> {
                long id = getInputData().getLong("id", -1L);
                MarkupFeature feature = mRepository.getMarkupFeatureById(id);

                feature.setUserSessionId(mPreferences.getUserSessionId());
                feature.setSendStatus(SendStatus.SENT);
                mRepository.addMarkupToDatabase(feature);

                Timber.tag(DEBUG).i("Adding markup feature " + id + " to send queue.");

                RequestBody body = createPartFromString(feature.toJson());
                Call<MarkupMessage> call = mApiService.postMarkupFeature(feature.getCollabRoomId(), body);
                call.enqueue(new AuthCallback<>(new Callback<MarkupMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<MarkupMessage> call, @NotNull Response<MarkupMessage> response) {
                        mPersonalHistory.addPersonalHistory("Map Markup successfully sent: " + feature.getId() + "\n", mPreferences.getUserId(), mPreferences.getUserNickName());
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        MarkupMessage message = response.body();
                        if (message != null && message.getFeatures() != null && message.getFeatures().size() > 0) {
                            // Set the feature id to the id provided by the server.
                            feature.setFeatureId(message.getFeatures().get(0).getFeatureId());
                            feature.setSendStatus(SendStatus.SAVED);

                            // Update all of the hazards to have the same id as the feature id.
                            if (feature.getHazards() != null) {
                                for (Hazard hazard : feature.getHazards()) {
                                    hazard.setHazardId(feature.getFeatureId());
                                }
                            }

                            // Replace the old feature with the new feature.
                            mRepository.addMarkupToDatabase(feature);
                        }

                        Timber.tag(DEBUG).i("Successfully posted Map Markup Feature %s", feature.getId());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<MarkupMessage> call, @NotNull Throwable t) {
                        // TODO add retry button in the list view.
                        feature.setFailedToSend(true);
                        feature.setSendStatus(SendStatus.WAITING_TO_SEND);
                        mRepository.addMarkupToDatabase(feature);
                        Timber.tag(DEBUG).e("Failed to post Markup Feature information: %s", t.getMessage());
                        completer.set(Result.failure());
                    }
                }));
                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class Delete extends AppWorker {

        private final MapRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final MapApiService mApiService;

        @AssistedInject
        public Delete(@Assisted @NonNull Context context,
                      @Assisted @NonNull WorkerParameters workerParams,
                      MapRepository repository,
                      PreferencesRepository preferences,
                      PersonalHistoryRepository personalHistory,
                      MapApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting Map Markup Delete Worker.");

            return CallbackToFutureAdapter.getFuture(completer -> {
                long id = getInputData().getLong("id", -1L);
                long collabroomId = mPreferences.getSelectedCollabroomId();
                MarkupFeature feature = mRepository.getMarkupFeatureById(id);
                String featureId = feature.getFeatureId();

                feature.setUserSessionId(mPreferences.getUserSessionId());
                feature.setSendStatus(SendStatus.DELETING);
                mRepository.addMarkupToDatabase(feature);

                Timber.tag(DEBUG).i("Adding markup feature " + id + " to delete queue.");

                Call<ResponseBody> call = mApiService.deleteMarkupFeature(featureId, collabroomId);
                call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                        // Delete the feature from the local database now that it has been deleted from the server.
                        mRepository.deleteMarkupFeatureById(id);
                        Timber.tag(DEBUG).i("Successfully deleted feature: %s", featureId);
                        mPersonalHistory.addPersonalHistory("Successfully deleted feature: " + featureId, mPreferences.getUserId(), mPreferences.getUserNickName());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                        try {
                            // TODO add an alert dialog to tell the user that the feature was added back due to delete failure.
                            // Get the feature that was supposed to be deleted and add the original feature back to the database.
                            MarkupFeature feature = mRepository.getMarkupFeatureToDeleteByFeatureId(collabroomId, featureId);

                            String original = feature.getOriginalFeature();
                            if (original != null) {
                                MarkupFeature originalFeature = new Gson().fromJson(original, MarkupFeature.class);
                                mRepository.addMarkupToDatabase(originalFeature);
                            }
                        } catch (Exception e) {
                            Timber.tag(DEBUG).e(e, "Failed to get original feature to restore after delete failure. Deleting feature from database.");
                        }

                        mRepository.deleteMarkupFeatureToDelete(collabroomId, featureId);

                        Timber.tag(DEBUG).e("Failed to delete out: %s", featureId);
                        mPersonalHistory.addPersonalHistory("Failed to delete out: " + featureId, mPreferences.getUserId(), mPreferences.getUserNickName());

                        completer.set(Result.failure());
                    }
                }));
                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class Update extends AppWorker {

        private final MapRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final MapApiService mApiService;

        @AssistedInject
        public Update(@Assisted @NonNull Context context,
                      @Assisted @NonNull WorkerParameters workerParams,
                      MapRepository repository,
                      PreferencesRepository preferences,
                      PersonalHistoryRepository personalHistory,
                      MapApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting Map Markup Update Worker.");

            return CallbackToFutureAdapter.getFuture(completer -> {
                long id = getInputData().getLong("id", -1L);
                MarkupFeature feature = mRepository.getMarkupFeatureById(id);

                feature.setUserSessionId(mPreferences.getUserSessionId());
                feature.setSendStatus(SendStatus.UPDATING);
                mRepository.addMarkupToDatabase(feature);

                Timber.tag(DEBUG).i("Adding markup feature " + id + " to update queue.");

                RequestBody body = createPartFromString(feature.toJsonStringWithFeatureId());
                Call<ResponseBody> call = mApiService.updateMarkupFeature(feature.getCollabRoomId(), body);
                call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        mPersonalHistory.addPersonalHistory("Map Markup successfully sent: " + feature.getId() + "\n", mPreferences.getUserId(), mPreferences.getUserNickName());
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                        Timber.tag(DEBUG).i("Successfully updated Map Markup Features");
                        feature.setSendStatus(SendStatus.SAVED);
                        mRepository.addMarkupToDatabase(feature);
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to update Markup Feature information: %s", t.getMessage());
                        String content = EMPTY;

                        try {
                            // TODO add an alert dialog to tell the user that the original feature was added back due to update failure.
                            // Get the feature that was supposed to be updated and add the original feature back to the database.
                            MarkupFeature markupFeature = mRepository.getMarkupFeatureToUpdateByFeatureId(feature.getCollabRoomId(), feature.getFeatureId());

                            String original = markupFeature.getOriginalFeature();
                            if (original != null) {
                                MarkupFeature originalFeature = new Gson().fromJson(original, MarkupFeature.class);
                                mRepository.addMarkupToDatabase(originalFeature);
                            }
                        } catch (Exception e) {
                            Timber.tag(DEBUG).e(e, "Failed to get original feature to restore after update failure. Deleting feature from database.");
                        }

                        mRepository.deleteMarkupToUpdateStoreAndForward(feature.getId());

                        HashMap<String, Object> extras = new HashMap<>();

                        // TODO better handling.
                        if (content.contains("{")) {
                            extras.put("message", mContext.getString(R.string.invalid_markup));
                        } else {
                            extras.put("message", content);
                        }

                        extras.put("oldFeatureId", feature.getFeatureId());
                        extras.put("collabroomId", feature.getCollabRoomId());
                        LiveDataBus.publish(Events.NICS_FAILED_TO_UPDATE_MARKUP, extras);
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }
    }
}
