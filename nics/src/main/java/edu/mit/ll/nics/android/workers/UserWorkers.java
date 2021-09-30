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

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.UserApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.messages.OrganizationMessage;
import edu.mit.ll.nics.android.data.messages.UserMessage;
import edu.mit.ll.nics.android.data.messages.WorkspaceMessage;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.valueOrEmpty;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class UserWorkers {

    @HiltWorker
    public static class GetUserData extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final UserApiService mApiService;

        @AssistedInject
        public GetUserData(@Assisted @NonNull Context context,
                           @Assisted @NonNull WorkerParameters workerParams,
                           PreferencesRepository preferences,
                           PersonalHistoryRepository personalHistory,
                           UserApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                Data.Builder output = new Data.Builder();

                Call<UserMessage> call = mApiService.getUserData(mPreferences.getSelectedWorkspaceId(), mPreferences.getUserId());
                call.enqueue(new AuthCallback<>(new Callback<UserMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<UserMessage> call, @NotNull Response<UserMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        UserMessage message = response.body();
                        if (message != null && message.getCount() > 0) {
                            mPreferences.setUserData(message.getUsers().get(0));

                            Timber.tag(DEBUG).i("Successfully received user information.");
                            mPersonalHistory.addPersonalHistory("Successfully received user information.", mPreferences.getUserId(), mPreferences.getUserNickName());

                            completer.set(Result.success());
                        } else {
                            String error = "Received empty user information.";
                            Timber.tag(DEBUG).w(error);
                            completer.set(Result.failure(output.putString("error", error).build()));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<UserMessage> call, @NotNull Throwable t) {
                        String error = "Failed to receive user information. " + valueOrEmpty(t.getLocalizedMessage());
                        Timber.tag(DEBUG).w(error);
                        mPersonalHistory.addPersonalHistory(error, mPreferences.getUserId(), mPreferences.getUserNickName());
                        completer.set(Result.failure(output.putString("error", error).build()));
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class GetAllUserData extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final UserApiService mApiService;

        @AssistedInject
        public GetAllUserData(@Assisted @NonNull Context context,
                              @Assisted @NonNull WorkerParameters workerParams,
                              PreferencesRepository preferences,
                              PersonalHistoryRepository personalHistory,
                              UserApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                Data.Builder output = new Data.Builder();

                Call<UserMessage> call = mApiService.getAllUserData(mPreferences.getSelectedWorkspaceId());
                call.enqueue(new AuthCallback<>(new Callback<UserMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<UserMessage> call, @NotNull Response<UserMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        UserMessage message = response.body();
                        if (message != null && message.getCount() > 0) {
                            mPreferences.setAllUserData(message);
                            Timber.tag(DEBUG).i("Successfully received user information.");
                            completer.set(Result.success());
                        } else {
                            String error = "Received empty user information.";
                            Timber.tag(DEBUG).w(error);
                            completer.set(Result.failure(output.putString("error", error).build()));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<UserMessage> call, @NotNull Throwable t) {
                        String error = "Failed to receive user information. " + valueOrEmpty(t.getLocalizedMessage());
                        Timber.tag(DEBUG).w(error);
                        mPersonalHistory.addPersonalHistory(error, mPreferences.getUserId(), mPreferences.getUserNickName());
                        completer.set(Result.failure(output.putString("error", error).build()));
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class GetUserWorkspaces extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final UserApiService mApiService;

        @AssistedInject
        public GetUserWorkspaces(@Assisted @NonNull Context context,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 PreferencesRepository preferences,
                                 PersonalHistoryRepository personalHistory,
                                 UserApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                Data.Builder output = new Data.Builder();

                Call<WorkspaceMessage> call = mApiService.getUserWorkspaces();
                call.enqueue(new AuthCallback<>(new Callback<WorkspaceMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<WorkspaceMessage> call, @NotNull Response<WorkspaceMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        WorkspaceMessage message = response.body();
                        if (message != null && message.getWorkspaces().size() > 0) {
                            mPreferences.setWorkspaces(message);

                            Timber.tag(DEBUG).i("Successfully received user workspace information.");
                            mPersonalHistory.addPersonalHistory("Successfully received user workspace information.", mPreferences.getUserId(), mPreferences.getUserNickName());
                            completer.set(Result.success());
                        } else {
                            String error = "Received empty user workspace information. Status Code: " + response.code();
                            Timber.tag(DEBUG).w(error);
                            completer.set(Result.failure(output.putString("error", error).build()));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<WorkspaceMessage> call, @NotNull Throwable t) {
                        String error = "Failed to receive workspace information. " + valueOrEmpty(t.getLocalizedMessage());
                        Timber.tag(DEBUG).w(error);
                        completer.set(Result.failure(output.putString("error", error).build()));
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class GetUserOrgs extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final UserApiService mApiService;

        @AssistedInject
        public GetUserOrgs(@Assisted @NonNull Context context,
                           @Assisted @NonNull WorkerParameters workerParams,
                           PreferencesRepository preferences,
                           PersonalHistoryRepository personalHistory,
                           UserApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                Data.Builder output = new Data.Builder();

                Call<OrganizationMessage> call = mApiService.getUserOrgs(mPreferences.getSelectedWorkspaceId(), mPreferences.getUserId());
                call.enqueue(new AuthCallback<>(new Callback<OrganizationMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<OrganizationMessage> call, @NotNull Response<OrganizationMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        OrganizationMessage message = response.body();
                        if (message != null && message.getCount() > 0) {
                            mPreferences.setOrganizations(message);

                            Timber.tag(DEBUG).i("Successfully received user organization information.");
                            mPersonalHistory.addPersonalHistory("Successfully received user organization information.", mPreferences.getUserId(), mPreferences.getUserNickName());
                            completer.set(Result.success());
                        } else {
                            completer.set(Result.failure(output.putString("error", "Received empty user organization information.").build()));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<OrganizationMessage> call, @NotNull Throwable t) {
                        String error = "Failed to receive user organization information. " + valueOrEmpty(t.getLocalizedMessage());
                        Timber.tag(DEBUG).w(error);
                        mPersonalHistory.addPersonalHistory(error, mPreferences.getUserId(), mPreferences.getUserNickName());
                        completer.set(Result.failure(output.putString("error", error).build()));
                    }
                }));

                return Result.success();
            });
        }
    }
}
