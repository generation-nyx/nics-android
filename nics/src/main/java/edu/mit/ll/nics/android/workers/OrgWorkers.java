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

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.OrgCapabilitiesApiService;
import edu.mit.ll.nics.android.api.SymbologyApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.OrgCapabilities;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.database.entities.SymbologyResponse;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SymbologyRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class OrgWorkers {

    @HiltWorker
    public static class OrgCapabilitiesWorker extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final OrgCapabilitiesApiService mApiService;

        @AssistedInject
        public OrgCapabilitiesWorker(@Assisted @NonNull Context context,
                                     @Assisted @NonNull WorkerParameters workerParams,
                                     PreferencesRepository preferences,
                                     OrgCapabilitiesApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                long orgId = mPreferences.getUserOrgId();

                Call<OrgCapabilities> call = mApiService.getOrgCapabilities(mPreferences.getSelectedWorkspaceId(), orgId);
                call.enqueue(new AuthCallback<>(new Callback<OrgCapabilities>() {
                    @Override
                    public void onResponse(@NotNull Call<OrgCapabilities> call, @NotNull Response<OrgCapabilities> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        OrgCapabilities caps = response.body();
                        if (caps != null && caps.getOrgCaps().length > 0) {
                            mPreferences.setOrgCapabilites(caps);
                        }

                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<OrgCapabilities> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to get Org Capabilities for orgID: %s", orgId);
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class OrgSymbologyWorker extends AppWorker {

        private final SymbologyRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final SymbologyApiService mApiService;

        @AssistedInject
        public OrgSymbologyWorker(@Assisted @NonNull Context context,
                                     @Assisted @NonNull WorkerParameters workerParams,
                                     SymbologyRepository repository,
                                     PreferencesRepository preferences,
                                     SymbologyApiService apiService) {
            super(context, workerParams);

            mRepository = repository;
            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                long orgId = mPreferences.getUserOrgId();

                mRepository.deleteAll();

                Call<SymbologyResponse> call = mApiService.getOrgSymbology(orgId);
                call.enqueue(new AuthCallback<>(new Callback<SymbologyResponse>() {
                    @Override
                    public void onResponse(@NotNull Call<SymbologyResponse> call, @NotNull Response<SymbologyResponse> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        SymbologyResponse symbResp = response.body();
                        if (symbResp != null && symbResp.getOrgSymbologies() != null) {
                            mRepository.addSymbologyToDatabase(symbResp.getOrgSymbologies());
                        }
                        if (symbResp != null && symbResp.getSymbologies() != null) {
                            mRepository.addSymbologyToDatabase(symbResp.getSymbologies());
                        }

                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<SymbologyResponse> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to get Org Symbology for orgID: %s", orgId);
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }
    }
}