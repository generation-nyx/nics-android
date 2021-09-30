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

import java.util.ArrayList;
import java.util.Date;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.api.AlertApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.messages.AlertMessage;
import edu.mit.ll.nics.android.database.entities.Alert;
import edu.mit.ll.nics.android.repository.AlertRepository;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.utils.NotificationsHandler;
import edu.mit.ll.nics.android.utils.constants.Events;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@HiltWorker
public class AlertsWorker extends AppWorker {

    private final AlertRepository mRepository;
    private final SettingsRepository mSettings;
    private final PreferencesRepository mPreferences;
    private final PersonalHistoryRepository mPersonalHistoryRepository;
    private final NotificationsHandler mNotificationsHandler;
    private final AlertApiService mApiService;

    @AssistedInject
    public AlertsWorker(@Assisted @NonNull Context context,
                        @Assisted @NonNull WorkerParameters workerParams,
                        AlertApiService alertApiService,
                        AlertRepository alertRepository,
                        PersonalHistoryRepository personalHistoryRepository,
                        SettingsRepository settings,
                        PreferencesRepository preferences,
                        NotificationsHandler notificationsHandler) {
        super(context, workerParams);

        mRepository = alertRepository;
        mSettings = settings;
        mPreferences = preferences;
        mNotificationsHandler = notificationsHandler;
        mPersonalHistoryRepository = personalHistoryRepository;
        mApiService = alertApiService;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            long incidentId = mPreferences.getSelectedIncidentId();
            Call<AlertMessage> call = mApiService.getAlerts(incidentId, mPreferences.getUserId());
            call.enqueue(new AuthCallback<>(new Callback<AlertMessage>() {
                @Override
                public void onResponse(@NotNull Call<AlertMessage> call, @NotNull Response<AlertMessage> response) {
                    mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                    AlertMessage message = response.body();
                    if (message != null && message.getResults().size() > 0) {
                        parseAlerts(incidentId, message.getResults());
                        Timber.tag(DEBUG).i("Successfully received alerts.");
                    } else {
                        Timber.tag(DEBUG).w("Received empty alerts. Status Code: %s", response.code());
                    }

                    completer.set(Result.success());
                }

                @Override
                public void onFailure(@NotNull Call<AlertMessage> call, @NotNull Throwable t) {
                    Timber.tag(DEBUG).e(t, "Failed to receive alerts information for: %s - %s", incidentId, mPreferences.getUserId());
                    completer.set(Result.failure());
                }
            }));

            return Result.success();
        });
    }

    private void parseAlerts(long incidentId, ArrayList<Alert> alerts) {
        int numParsed = 0;

        long lastAlertTimestamp = mRepository.getLastAlertTimestamp(incidentId);

        ArrayList<Alert> newAlerts = new ArrayList<>();

        for (Alert alert : alerts) {
            if (alert.getCreated() <= lastAlertTimestamp) {
                break;
            }
            alert.setIncidentId(incidentId);
            newAlerts.add(alert);
            mRepository.addAlertToDatabase(alert);
            numParsed++;
        }

        if (numParsed > 0) {
            // Build a message to display in the alert dialog.
            StringBuilder message = new StringBuilder();

            for (Alert alert : newAlerts) {
                message.append(mContext.getString(R.string.from)).append(alert.getUserName()).append("\n");
                message.append(new Date(alert.getCreated()).toString()).append("\n\n\n");
                message.append(alert.getMessage()).append("\n").append("\n\n\n\n\n");
            }

            // Create a notification with the new alerts.
            if (!mSettings.isPushNotificationsDisabled()) {
                mNotificationsHandler.createAlertsNotification(newAlerts);
            }

            // Publish that new alerts have been received and pass in the message that will be displayed in the dialog.
            LiveDataBus.publish(Events.NICS_RECEIVED_ALERT, message.toString());

            mPersonalHistoryRepository.addPersonalHistory("Successfully received " + numParsed + " alerts from " + mPreferences.getSelectedIncidentName(),
                    mPreferences.getUserId(), mPreferences.getUserNickName());
        }
    }
}
