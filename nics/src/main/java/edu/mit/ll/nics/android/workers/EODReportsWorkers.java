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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.EODReportApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.ReportProgress;
import edu.mit.ll.nics.android.data.messages.EODReportMessage;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.repository.EODReportRepository;
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
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_EOD_REPORT_PROGRESS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class EODReportsWorkers {

    @HiltWorker
    public static class Get extends AppWorker {

        private final EODReportRepository mRepository;
        private final PersonalHistoryRepository mPersonalHistory;
        private final PreferencesRepository mPreferences;
        private final EODReportApiService mApiService;

        @AssistedInject
        public Get(@Assisted @NonNull Context context,
                   @Assisted @NonNull WorkerParameters workerParams,
                   EODReportRepository repository,
                   PersonalHistoryRepository personalHistory,
                   PreferencesRepository preferences,
                   EODReportApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPersonalHistory = personalHistory;
            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting EOD Report Get Worker.");

            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            return CallbackToFutureAdapter.getFuture(completer -> {
                long incidentId = mPreferences.getSelectedIncidentId();
                long collabroomId = mPreferences.getSelectedCollabroomId();
                long lastTimestamp = mRepository.getLastEODReportTimestamp(incidentId, collabroomId) + 1;

                Call<EODReportMessage> call = mApiService.getEODReports(incidentId, lastTimestamp, collabroomId);
                call.enqueue(new AuthCallback<>(new Callback<EODReportMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<EODReportMessage> call, @NotNull Response<EODReportMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        EODReportMessage message = response.body();
                        if (message != null && message.getReports() != null && message.getReports().size() > 0) {
                            parseEODReports(message.getReports());
                            Timber.tag(DEBUG).i("Successfully received EOD report information.");
                        } else {
                            Timber.tag(DEBUG).w("Received empty EOD report information. Status Code: %s", response.code());
                        }

                        Timber.tag(DEBUG).d("Finished EOD Report Get Request.");
                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<EODReportMessage> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e(t, "Failed to receive EOD report information from incident %s", incidentId);

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }

        private void parseEODReports(ArrayList<EODReport> reports) {
            int numParsed = 0;

            for (EODReport report : reports) {
                if (report.getIncidentId() == mPreferences.getSelectedIncidentId()) {
                    report.setSendStatus(SendStatus.RECEIVED);
                    report.setNew(true);
                    report.setRead(false);

                    List<EODReport> eodReports = mRepository.getEODReports();

                    // If the report already exists locally, we need to set the report id so that it can replace it with the correct report id.
                    for (EODReport eodReport : eodReports) {
                        if (eodReport.getFormId() == report.getFormId()) {
                            report.setId(eodReport.getId());
                            break;
                        }
                    }

                    mRepository.addEODReportToDatabase(report);
                    numParsed++;
                }
            }

            if (numParsed > 0) {
                mPersonalHistory.addPersonalHistory("Successfully received " + numParsed + " EOD reports from " + mPreferences.getSelectedIncidentName(), mPreferences.getUserId(), mPreferences.getUserNickName());
            }
        }
    }

    @HiltWorker
    public static class Post extends AppWorker {

        private final EODReportRepository mRepository;
        private final PersonalHistoryRepository mPersonalHistory;
        private final PreferencesRepository mPreferences;
        private final EODReportApiService mApiService;

        @AssistedInject
        public Post(@Assisted @NonNull Context context,
                    @Assisted @NonNull WorkerParameters workerParams,
                    EODReportRepository repository,
                    PersonalHistoryRepository personalHistory,
                    PreferencesRepository preferences,
                    EODReportApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPersonalHistory = personalHistory;
            mPreferences = preferences;
            mApiService = apiService;
        }

        @SuppressLint("CheckResult")
        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting EOD Report Post Worker.");

            return CallbackToFutureAdapter.getFuture(completer -> {
                long reportId = getInputData().getLong("reportId", -1L);
                EODReport report = mRepository.getEODReportById(reportId);
                long id = report.getId();

                report.setUserSessionId(mPreferences.getUserSessionId());
                report.setSendStatus(SendStatus.SENT);
                mRepository.upsertEODReport(report);

                Timber.tag(DEBUG).i("Adding EOD report " + id + " to send queue.");

                AuthCallback<EODReportMessage> callback = new AuthCallback<>(new Callback<EODReportMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<EODReportMessage> call, @NotNull Response<EODReportMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        EODReportMessage message = response.body();
                        if (message != null && message.getReports() != null && message.getReports().size() > 0) {
                            EODReport r = message.getReports().get(0);
                            r.setSendStatus(SendStatus.SAVED);
                            r.setProgress(100);
                            r.setId(id);
                            r.setSeqTime(report.getSeqTime());
                            r.setIncidentName(report.getIncidentName());
                            mRepository.upsertEODReport(r);
                        } else if (response.code() != 200){
                            report.setFailedToSend(true);
                            report.setSendStatus(SendStatus.WAITING_TO_SEND);
                            mRepository.upsertEODReport(report);
                            LiveDataBus.publish(NICS_EOD_REPORT_PROGRESS, new ReportProgress(id, 0, true));
                            completer.set(Result.failure());
                            Timber.tag(DEBUG).w("Failed to post EOD report %s. Response code: %s", report.getId(), response.code());

                            if (response.errorBody() != null && emptyCheck(response.errorBody().toString())) {
                                Timber.tag(DEBUG).w("Response Error: %s", response.errorBody().toString());
                            }

                            return;
                        }

                        Timber.tag(DEBUG).i("Success to post EOD report information.");
                        LiveDataBus.publish(NICS_EOD_REPORT_PROGRESS, new ReportProgress(id, 100, false));
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<EODReportMessage> call, @NotNull Throwable e) {
                        report.setFailedToSend(true);
                        report.setSendStatus(SendStatus.WAITING_TO_SEND);
                        mRepository.upsertEODReport(report);
                        LiveDataBus.publish(NICS_EOD_REPORT_PROGRESS, new ReportProgress(id, 0, true));
                        completer.set(Result.failure());
                        Timber.tag(DEBUG).e(e,"Failed to post EOD report %s.", report.getId());
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
                                                Timber.tag(DEBUG).d("Posting EOD Report Progress: %s", progress);
                                                LiveDataBus.publish(NICS_EOD_REPORT_PROGRESS, new ReportProgress(id, Math.round(progress), false));
                                            },
                                            throwable -> Timber.tag(DEBUG).e("Error listening to EOD report upload progress."));

                            MultipartBody.Part body = MultipartBody.Part.createFormData("image", image.getName(), fileBody);
                            Map<String, RequestBody> map = parsePartMap(report);

                            Call<EODReportMessage> call = mApiService.postEODReport(mPreferences.getSelectedIncidentId(), map, body);
                            call.enqueue(callback);
                        } catch (Exception e) {
                            mRepository.deleteEODReportStoreAndForward(id);
                            Timber.tag(DEBUG).e("Deleting Report: %s due to invalid file.", id);
                            mPersonalHistory.addPersonalHistory("Deleting EOD report: " + id + " due to invalid/missing image file.", mPreferences.getUserId(), mPreferences.getUserNickName());
                            //TODO if the file fails, should probably let the user know that it failed.
                        }
                    } else {
                        RequestBody body = createPartFromString(report.toJsonStringToSend());
                        Call<EODReportMessage> call = mApiService.postEODReport(mPreferences.getSelectedIncidentId(), body);
                        call.enqueue(callback);
                    }
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed to post EOD report.");
                    completer.set(Result.failure());
                }

                return Result.success();
            });
        }

        private Map<String, RequestBody> parsePartMap(EODReport report) {
            Map<String, RequestBody> map = new HashMap<>();
            map.put("deviceId", createPartFromString(valueOrEmpty(NetworkRepository.mDeviceId)));
            map.put("incidentId", createPartFromString(String.valueOf(report.getIncidentId())));
            map.put("collabroomId", createPartFromString(String.valueOf(mPreferences.getSelectedCollabroomId())));
            map.put("userId", createPartFromString(String.valueOf(mPreferences.getUserId())));
            map.put("usersessionid", createPartFromString(String.valueOf(report.getUserSessionId())));
            map.put("latitude", createPartFromString(String.valueOf(report.getLatitude())));
            map.put("longitude", createPartFromString(String.valueOf(report.getLongitude())));
            map.put("description", createPartFromString(valueOrEmpty(report.getDescription())));
            map.put("seqtime", createPartFromString(String.valueOf(report.getSeqTime())));
            map.put("team", createPartFromString(valueOrEmpty(report.getTeam())));
            map.put("canton", createPartFromString(valueOrEmpty(report.getCanton())));
            map.put("town", createPartFromString(valueOrEmpty(report.getTown())));
            map.put("tasktype", createPartFromString(valueOrEmpty(report.getTaskType())));
            map.put("macID", createPartFromString(valueOrEmpty(report.getMacID())));
            map.put("medevacPointTimeDistance", createPartFromString(valueOrEmpty(report.getMedevacPointTimeDistance())));
            map.put("contactPerson", createPartFromString(valueOrEmpty(report.getContactPerson())));
            map.put("contactPhone", createPartFromString(valueOrEmpty(report.getContactPhone())));
            map.put("contactAddress", createPartFromString(valueOrEmpty(report.getContactAddress())));
            map.put("remarks", createPartFromString(valueOrEmpty(report.getRemarks())));
            map.put("expendedResources", createPartFromString(valueOrEmpty(report.getExpendedResources())));
            map.put("directlyInvolved", createPartFromString(valueOrEmpty(report.getDirectlyInvolved())));
            map.put("imagePath", createPartFromString(valueOrEmpty(report.getFullPath())));
            map.put("uxo", createPartFromString(valueOrEmpty(report.getUxoString())));
            return map;
        }
    }
}
