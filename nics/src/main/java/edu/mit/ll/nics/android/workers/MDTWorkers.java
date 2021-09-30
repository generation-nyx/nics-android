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
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.MDTApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.database.entities.MobileDeviceTracking;
import edu.mit.ll.nics.android.repository.MDTRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.Utils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class MDTWorkers {

    @HiltWorker
    public static class Post extends AppWorker {

        private final MDTRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final MDTApiService mApiService;

        @AssistedInject
        public Post(@Assisted @NonNull Context context,
                    @Assisted @NonNull WorkerParameters workerParams,
                    MDTRepository repository,
                    PreferencesRepository preferences,
                    MDTApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                List<MobileDeviceTracking> mdts = mRepository.getMDTs();

                if (mdts.size() > 0) {
                    MobileDeviceTracking mdt = mdts.get(mdts.size() - 1);

                    if (!Utils.emptyCheck(mdt.getDeviceId())) {
                        Call<ResponseBody> call = mApiService.postMDT(mPreferences.getSelectedWorkspaceId(), mdt);
                        call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                                mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                                Timber.tag(DEBUG).i("Successfully posted MDT messages");

                                for (MobileDeviceTracking feature : mdts) {
                                    mRepository.deleteMDT(feature.getId());
                                }

                                completer.set(Result.success());
                            }

                            @Override
                            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                                Timber.tag(DEBUG).e("Failed to post MDT information: %s", t.getMessage());
                                completer.set(Result.failure());
                            }
                        }));
                    } else {
                        mRepository.deleteMDT(mdt.getId());
                        completer.set(Result.failure());
                        Timber.tag(DEBUG).w("Failed to post mobile device track because it lacks a deviceId. Deleting from the queue.");
                    }
                } else {
                    Timber.tag(DEBUG).i("No mdts to send.");
                    completer.set(Result.failure());
                }

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class Delete extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final MDTApiService mApiService;

        @AssistedInject
        public Delete(@Assisted @NonNull Context context,
                      @Assisted @NonNull WorkerParameters workerParams,
                      PreferencesRepository preferences,
                      MDTApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                String deviceId = NetworkRepository.mDeviceId;
                if (!Utils.emptyCheck(deviceId)) {
                    Call<ResponseBody> call = mApiService.deleteMDT(mPreferences.getSelectedWorkspaceId(), mPreferences.getUserName(), deviceId);
                    call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                            mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                            Timber.tag(DEBUG).i("Successfully posted MDT messages");
                            completer.set(Result.success());
                        }

                        @Override
                        public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                            Timber.tag(DEBUG).e("Failed to post MDT information: %s", t.getMessage());
                            completer.set(Result.failure());
                        }
                    }));
                } else {
                    completer.set(Result.failure());
                    Timber.tag(DEBUG).w("MDT can't be deleted because there is no device id specified.");
                }

                return Result.success();
            });
        }
    }
}
