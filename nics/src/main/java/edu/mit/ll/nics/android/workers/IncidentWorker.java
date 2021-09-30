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
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.api.IncidentApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.messages.IncidentMessage;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.valueOrEmpty;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@HiltWorker
public class IncidentWorker extends AppWorker {

    private final PreferencesRepository mPreferences;
    private final PersonalHistoryRepository mPersonalHistory;
    private final IncidentApiService mApiService;

    @AssistedInject
    public IncidentWorker(@Assisted @NonNull Context context,
                          @Assisted @NonNull WorkerParameters workerParams,
                          PreferencesRepository preferences,
                          PersonalHistoryRepository personalHistory,
                          IncidentApiService apiService) {
        super(context, workerParams);

        mPreferences = preferences;
        mPersonalHistory = personalHistory;
        mApiService = apiService;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Timber.tag(DEBUG).d("Starting Get Incidents Worker.");

        // Initialize the progress to 0, so that any observers can be updated that the request has started.
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

        return CallbackToFutureAdapter.getFuture(completer -> {
            Data.Builder output = new Data.Builder();

            Call<IncidentMessage> call = mApiService.getIncidents(mPreferences.getSelectedWorkspaceId(), mPreferences.getUserId());
            call.enqueue(new AuthCallback<>(new Callback<IncidentMessage>() {
                @Override
                public void onResponse(@NotNull Call<IncidentMessage> call, @NotNull Response<IncidentMessage> response) {
                    mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                    IncidentMessage message = response.body();
                    if (message != null) {
                        Timber.tag(DEBUG).i("Successfully received incident information.");
                        mPreferences.setIncidents(message);
                        mPersonalHistory.addPersonalHistory("Successfully received incident information.", mPreferences.getUserId(), mPreferences.getUserNickName());

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.success());
                    } else {
                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure(output.putString("error", "Received empty incidents payload.").build()));
                    }
                }

                @Override
                public void onFailure(@NotNull Call<IncidentMessage> call, @NotNull Throwable t) {
                    String error = mContext.getString(R.string.failed_to_receive_incident_information).concat(valueOrEmpty(t.getLocalizedMessage()));
                    Timber.tag(DEBUG).w(error);
                    mPersonalHistory.addPersonalHistory(error, mPreferences.getUserId(), mPreferences.getUserNickName());

                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    completer.set(Result.failure(output.putString("error", error).build()));
                }
            }));

            return Result.success();
        });
    }
}
