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
import edu.mit.ll.nics.android.api.CollabroomApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.messages.CollaborationRoomMessage;
import edu.mit.ll.nics.android.repository.CollabroomRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@HiltWorker
public class CollabroomWorker extends AppWorker {

    private final CollabroomRepository mRepository;
    private final PreferencesRepository mPreferences;
    private final CollabroomApiService mApiService;

    @AssistedInject
    public CollabroomWorker(@Assisted @NonNull Context context,
                            @Assisted @NonNull WorkerParameters workerParams,
                            CollabroomRepository repository,
                            PreferencesRepository preferences,
                            CollabroomApiService apiService) {
        super(context, workerParams);

        mRepository = repository;
        mPreferences = preferences;
        mApiService = apiService;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Timber.tag(DEBUG).d("Starting Get Collabrooms Worker.");

        // Initialize the progress to 0, so that any observers can be updated that the request has started.
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

        return CallbackToFutureAdapter.getFuture(completer -> {
            long incidentId = mPreferences.getSelectedIncidentId();

            Call<CollaborationRoomMessage> call = mApiService.getCollabrooms(incidentId, mPreferences.getUserId());
            call.enqueue(new AuthCallback<>(new Callback<CollaborationRoomMessage>() {
                @Override
                public void onResponse(@NotNull Call<CollaborationRoomMessage> call, @NotNull Response<CollaborationRoomMessage> response) {
                    mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                    CollaborationRoomMessage message = response.body();

                    if (message != null && message.getResults().size() > 0) {
                        mRepository.replaceAllByIncident(message.getResults(), incidentId);
                    }

                    // Log the success and finish the process.
                    Timber.tag(DEBUG).i("Successfully pulled rooms for %s with code %s.", incidentId, response.code());

                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    completer.set(Result.success());
                }

                @Override
                public void onFailure(@NotNull Call<CollaborationRoomMessage> call, @NotNull Throwable t) {
                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());

                    Timber.tag(DEBUG).e("Failed to pull collabrooms from %s", incidentId);
                    completer.set(Result.failure());
                }
            }));

            return Result.success();
        });
    }
}
