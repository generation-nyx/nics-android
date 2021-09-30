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

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.GeneralMessageApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.ReportProgress;
import edu.mit.ll.nics.android.data.messages.GeneralMessageMessage;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.ExifUtils;
import edu.mit.ll.nics.android.utils.ProgressRequestBody;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.NetworkUtils.createPartFromString;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.StringUtils.valueOrEmpty;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_GENERAL_MESSAGE_PROGRESS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class GeneralMessagesWorkers {

    @HiltWorker
    public static class Get extends AppWorker {

        private final GeneralMessageRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final GeneralMessageApiService mApiService;

        @AssistedInject
        public Get(@Assisted @NonNull Context context,
                   @Assisted @NonNull WorkerParameters workerParams,
                   GeneralMessageRepository repository,
                   PreferencesRepository preferences,
                   PersonalHistoryRepository personalHistory,
                   GeneralMessageApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting General Message Get Worker.");

            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            return CallbackToFutureAdapter.getFuture(completer -> {
                long incidentId = mPreferences.getSelectedIncidentId();
                long collabroomId = mPreferences.getSelectedCollabroomId();
                long lastTimestamp = mRepository.getLastGeneralMessageTimestamp() + 1;

                Call<GeneralMessageMessage> call = mApiService.getGeneralMessages(incidentId, lastTimestamp, collabroomId);
                call.enqueue(new AuthCallback<>(new Callback<GeneralMessageMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<GeneralMessageMessage> call, @NotNull Response<GeneralMessageMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        GeneralMessageMessage message = response.body();
                        if (message != null && message.getReports() != null && message.getReports().size() > 0) {
                            parseGeneralMessages(message);
                            Timber.tag(DEBUG).i("Successfully received general message information.");
                        } else {
                            Timber.tag(DEBUG).i("Received empty general message information. Status Code: %s", response.code());
                        }

                        Timber.tag(DEBUG).d("Finished General Message Get Request.");
                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<GeneralMessageMessage> call, @NotNull Throwable e) {
                        Timber.tag(DEBUG).e(e, "Failed to receive general message reports from incident: %s", mPreferences.getSelectedIncidentId());

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }

        private void parseGeneralMessages(GeneralMessageMessage message) {
            int numParsed = 0;

            for (GeneralMessage report : message.getReports()) {
                if (report.getIncidentId() == mPreferences.getSelectedIncidentId()) {
                    report.setSendStatus(SendStatus.RECEIVED);
                    report.setNew(true);
                    report.setRead(false);

                    // TODO might not need to do this since formID is unique and will cause conflict?
                    List<GeneralMessage> generalMessages = mRepository.getGeneralMessages();

                    // If the report already exists locally, we need to set the report id so that it can replace it with the correct report id.
                    for (GeneralMessage generalMessage : generalMessages) {
                        if (generalMessage.getFormId() == report.getFormId()) {
                            report.setId(generalMessage.getId());
                            break;
                        }
                    }

                    mRepository.addGeneralMessageToDatabase(report);
                    numParsed++;
                }
            }

            if (numParsed > 0) {
                mPersonalHistory.addPersonalHistory("Successfully received " + numParsed + " general messages from " + mPreferences.getSelectedIncidentName(), mPreferences.getUserId(), mPreferences.getUserNickName());
            }

            Timber.tag(DEBUG).i("Parsed %s general messages successfully.", numParsed);
        }
    }

    @HiltWorker
    public static class Post extends AppWorker {

        private final GeneralMessageRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final GeneralMessageApiService mApiService;

        @AssistedInject
        public Post(@Assisted @NonNull Context context,
                    @Assisted @NonNull WorkerParameters workerParams,
                    GeneralMessageRepository repository,
                    PreferencesRepository preferences,
                    PersonalHistoryRepository personalHistory,
                    GeneralMessageApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @SuppressLint("CheckResult")
        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting General Message Post Worker.");

            return CallbackToFutureAdapter.getFuture(completer -> {
                long reportId = getInputData().getLong("reportId", -1L);
                GeneralMessage report = mRepository.getGeneralMessageById(reportId);
                long id = report.getId();

                report.setUserSessionId(mPreferences.getUserSessionId());
                report.setSendStatus(SendStatus.SENT);
                mRepository.upsertGeneralMessage(report);

                Timber.tag(DEBUG).i("Adding general message " + id + " to send queue.");

                AuthCallback<GeneralMessageMessage> callback = new AuthCallback<>(new Callback<GeneralMessageMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<GeneralMessageMessage> call, @NotNull Response<GeneralMessageMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        GeneralMessageMessage message = response.body();
                        if (message != null && message.getReports() != null && message.getReports().size() > 0) {
                            GeneralMessage r = message.getReports().get(0);
                            r.setSendStatus(SendStatus.SAVED);
                            r.setProgress(100);
                            r.setId(id);
                            r.setSeqTime(report.getSeqTime());
                            r.setIncidentName(report.getIncidentName());
                            mRepository.upsertGeneralMessage(r);
                        } else if (response.code() != 200){
                            report.setFailedToSend(true);
                            report.setSendStatus(SendStatus.WAITING_TO_SEND);
                            mRepository.upsertGeneralMessage(report);
                            LiveDataBus.publish(NICS_GENERAL_MESSAGE_PROGRESS, new ReportProgress(id, 0, true));
                            completer.set(Result.failure());
                            Timber.tag(DEBUG).w("Failed to post general message %s. Response code: %s", report.getId(), response.code());

                            if (response.errorBody() != null) {
                                try {
                                    Timber.tag(DEBUG).w("Response Error: %s", response.errorBody().string());
                                } catch (IOException e) {
                                    Timber.tag(DEBUG).e(e, "Failed to print error body.");
                                }
                            }
                        }

                        Timber.tag(DEBUG).i("Success to post General Message information.");
                        LiveDataBus.publish(NICS_GENERAL_MESSAGE_PROGRESS, new ReportProgress(id, 100, false));
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<GeneralMessageMessage> call, @NotNull Throwable e) {
                        report.setFailedToSend(true);
                        report.setSendStatus(SendStatus.WAITING_TO_SEND);
                        mRepository.upsertGeneralMessage(report);
                        LiveDataBus.publish(NICS_GENERAL_MESSAGE_PROGRESS, new ReportProgress(id, 0, true));
                        Timber.tag(DEBUG).e(e,"Failed to post general message %s.", report.getId());
                        completer.set(Result.failure());
                    }
                });

                try {
                    if (!emptyCheck(report.getFullPath())) {
                        try {
                            File image = new File(report.getFullPath());

                            Location location = new Location(EMPTY);
                            location.setLatitude(report.getLatitude());
                            location.setLongitude(report.getLongitude());
                            ExifUtils.saveGpsExif(image, location);

                            ProgressRequestBody fileBody = new ProgressRequestBody(image, 1);
                            fileBody.getProgressSubject()
                                    .subscribe(progress -> {
                                                Timber.tag(DEBUG).d("Posting General Message Progress: %s", progress);
                                                LiveDataBus.publish(NICS_GENERAL_MESSAGE_PROGRESS, new ReportProgress(id, Math.round(progress), false));
                                            },
                                            throwable -> Timber.tag(DEBUG).e("Error listening to general message upload progress."));

                            MultipartBody.Part body = MultipartBody.Part.createFormData("image", image.getName(), fileBody);
                            Map<String, RequestBody> map = parsePartMap(report);

                            Call<GeneralMessageMessage> call = mApiService.postGeneralMessage(mPreferences.getSelectedIncidentId(), map, body);
                            call.enqueue(callback);
                        } catch (Exception e) {
                            Timber.tag(DEBUG).e("Deleting: %s " + mRepository.deleteGeneralMessageStoreAndForward(report.getId()) + " due to invalid file.", report.getId());
                            mPersonalHistory.addPersonalHistory("Deleting general message: " + report.getId() + " due to invalid/missing image file.", mPreferences.getUserId(), mPreferences.getUserNickName());
                            //TODO if the file fails, should probably let the user know that it failed.
                        }
                    } else {
                          RequestBody body = createPartFromString(report.toJsonStringToSend());
                          Call<GeneralMessageMessage> call = mApiService.postGeneralMessage(mPreferences.getSelectedIncidentId(), body);
                          call.enqueue(callback);
                    }
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed to post general message.");
                    completer.set(Result.failure());
                }

                return Result.success();
            });
        }

        private Map<String, RequestBody> parsePartMap(GeneralMessage report) {
            Map<String, RequestBody> map = new HashMap<>();
            map.put("deviceId", createPartFromString(valueOrEmpty(NetworkRepository.mDeviceId)));
            map.put("incidentId", createPartFromString(String.valueOf(report.getIncidentId())));
            map.put("collabroomId", createPartFromString(String.valueOf(mPreferences.getSelectedCollabroomId())));
            map.put("userId", createPartFromString(String.valueOf(mPreferences.getUserId())));
            map.put("usersessionid", createPartFromString(String.valueOf(mPreferences.getUserSessionId())));
            map.put("latitude", createPartFromString(String.valueOf(report.getLatitude())));
            map.put("longitude", createPartFromString(String.valueOf(report.getLongitude())));
            map.put("description", createPartFromString(valueOrEmpty(report.getDescription())));
            map.put("category", createPartFromString(valueOrEmpty(report.getCategory())));
            map.put("seqtime", createPartFromString(String.valueOf(report.getSeqTime())));
            return map;
        }
    }
}
