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
package edu.mit.ll.nics.android.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.enums.Polling;
import edu.mit.ll.nics.android.enums.PresenceStatus;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.NetworkUtils.isNetworkConnected;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_ALERTS;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_COLLABROOMS;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_COLLABROOM_LAYERS;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_INCIDENTS;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_MARKUP_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_ORGANIZATIONS;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_TASK_CHAT_MESSAGES;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_TASK_CHAT_PRESENCE;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_TASK_EOD_REPORT;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_TASK_GENERAL_MESSAGE;
import static edu.mit.ll.nics.android.utils.constants.Intents.NICS_POLLING_TRACKING_LAYERS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_NO_RESULTS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_WAKE_LOCK;
import static edu.mit.ll.nics.android.utils.constants.NICS.TEN_MINUTES;
import static edu.mit.ll.nics.android.utils.constants.Notifications.ALERTS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.CHATS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.CHAT_PRESENCE_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.COLLABROOMS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.COLLABROOM_LAYERS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EOD_REPORTS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GENERAL_MESSAGES_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.INCIDENTS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.MARKUP_FEATURES_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.ORGANIZATIONS_REQUEST_CODE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.TRACKING_LAYERS_REQUEST_CODE;

@AndroidEntryPoint
public class PollingService extends AppService {

    private static final String TYPE = "type";

    private final IBinder mBinder = new LocalBinder();

    // TODO poll incident and organizations.

    private boolean mIsPollingIncident = false;
    private boolean mIsPollingCollabroom = false;
    private PendingIntent mPendingAlertIntent;
    private PendingIntent mPendingCollabroomIntent;
    private PendingIntent mPendingIncidentIntent;
    private PendingIntent mPendingOrganizationIntent;
    private PendingIntent mPendingCollabroomLayerIntent;
    private PendingIntent mPendingEODReportIntent;
    private PendingIntent mPendingGeneralMessageIntent;
    private PendingIntent mPendingMarkupRequestIntent;
    private PendingIntent mPendingChatMessagesRequestIntent;
    private PendingIntent mPendingChatPresenceRequestIntent;
    private PendingIntent mPendingTrackingLayerIntent;
    private LiveData<String> mIncidentRateObserver;
    private LiveData<String> mCollabroomRateObserver;

    private long mTimeToDisconnect;
    private Timer mServerCommsTimer;

    @Inject
    AlarmManager mAlarmManager;

    @Nullable
    @Override
    public IBinder onBind(@NotNull Intent intent) {
        super.onBind(intent);
        registerReceivers();
        subscribeToLiveData();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(receiver);

        if (mIncidentRateObserver != null) {
            mIncidentRateObserver.removeObservers(this);
        }

        if (mCollabroomRateObserver != null) {
            mCollabroomRateObserver.removeObservers(this);
        }
        return super.onUnbind(intent);
    }

    private void subscribeToLiveData() {
        mIncidentRateObserver = mSettings.getIncidentDataRateLiveData();
        mIncidentRateObserver.observe(this, rate -> refreshPollingIncident());

        mCollabroomRateObserver = mSettings.getCollabroomDataRateLiveData();
        mCollabroomRateObserver.observe(this, rate -> refreshPollingCollabroom());
    }

    private void registerReceivers() {
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_ALERTS));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_COLLABROOMS));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_MARKUP_REQUEST));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_TASK_EOD_REPORT));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_TASK_GENERAL_MESSAGE));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_TASK_CHAT_MESSAGES));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_TASK_CHAT_PRESENCE));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_COLLABROOM_LAYERS));
        registerReceiver(receiver, new IntentFilter(NICS_POLLING_TRACKING_LAYERS));
    }

    public void requestAlertsRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_ALERTS);
        intent.putExtra(TYPE, Polling.ALERTS.toString());

        mPendingAlertIntent = PendingIntent.getBroadcast(mContext, ALERTS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getAlerts();
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingAlertIntent);

        Timber.tag(DEBUG).i("Set alert report repeating fetch interval: %s seconds.", seconds);
    }

    public void requestCollabroomLayersRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_COLLABROOM_LAYERS);
        intent.putExtra(TYPE, Polling.COLLABROOM_LAYERS.toString());

        mPendingCollabroomLayerIntent = PendingIntent.getBroadcast(mContext, COLLABROOM_LAYERS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getCollabroomLayers();
        }

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingCollabroomLayerIntent);

        Timber.tag(DEBUG).i("Set collabroom layer repeating fetch interval: %s seconds.", seconds);
    }

    public void requestCollabroomsRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_COLLABROOMS);
        intent.putExtra(TYPE, Polling.COLLABROOM.toString());

        mPendingCollabroomIntent = PendingIntent.getBroadcast(mContext, COLLABROOMS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getCollabrooms(mPreferences.getSelectedIncidentId());
        }

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingCollabroomIntent);

        Timber.tag(DEBUG).i("Set collabroom repeating fetch interval: %s seconds.", seconds);
    }

    public void requestIncidentsRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_INCIDENTS);
        intent.putExtra(TYPE, Polling.INCIDENT.toString());

        mPendingIncidentIntent = PendingIntent.getBroadcast(mContext, INCIDENTS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getIncidents();
        }

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingIncidentIntent);

        Timber.tag(DEBUG).i("Set incident repeating fetch interval: %s seconds.", seconds);
    }

    public void requestOrganizationsRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_ORGANIZATIONS);
        intent.putExtra(TYPE, Polling.ORGANIZATION.toString());

        mPendingOrganizationIntent = PendingIntent.getBroadcast(mContext, ORGANIZATIONS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getUserOrgs();
        }

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingOrganizationIntent);

        Timber.tag(DEBUG).i("Set collabroom repeating fetch interval: %s seconds.", seconds);
    }

    public void requestGeneralMessageRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_TASK_GENERAL_MESSAGE);
        intent.putExtra(TYPE, Polling.GENERAL_MESSAGE.toString());

        mPendingGeneralMessageIntent = PendingIntent.getBroadcast(mContext, GENERAL_MESSAGES_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getGeneralMessages();
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingGeneralMessageIntent);

        Timber.tag(DEBUG).i("Set general message repeating fetch interval: %s seconds.", seconds);
    }

    public void requestEODReportRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_TASK_EOD_REPORT);
        intent.putExtra(TYPE, Polling.EOD_REPORT.toString());

        mPendingEODReportIntent = PendingIntent.getBroadcast(mContext, EOD_REPORTS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getEODReports();
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingEODReportIntent);

        Timber.tag(DEBUG).i("Set EOD report repeating fetch interval: %s seconds.", seconds);
    }

    public void requestChatMessagesRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_TASK_CHAT_MESSAGES);
        intent.putExtra(TYPE, Polling.CHAT_MESSAGES.toString());

        mPendingChatMessagesRequestIntent = PendingIntent.getBroadcast(mContext, CHATS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getChatMessages();
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingChatMessagesRequestIntent);

        Timber.tag(DEBUG).i("Set chat message request repeating fetch interval: %s seconds.", seconds);
    }

    public void requestChatPresenceRepeating(boolean immediately) {
        int seconds = 4 * 60;
        Intent intent = new Intent(NICS_POLLING_TASK_CHAT_PRESENCE);
        intent.putExtra(TYPE, Polling.CHAT_PRESENCE.toString());

        mPendingChatPresenceRequestIntent = PendingIntent.getBroadcast(mContext, CHAT_PRESENCE_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.postChatPresence(PresenceStatus.ACTIVE);
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingChatPresenceRequestIntent);

        Timber.tag(DEBUG).i("Set chat presence request repeating fetch interval: %s seconds.", seconds);
    }

    public void requestMarkupRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_MARKUP_REQUEST);
        intent.putExtra(TYPE, Polling.MAP_MARKUP.toString());

        mPendingMarkupRequestIntent = PendingIntent.getBroadcast(mContext, MARKUP_FEATURES_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getMarkupFeatures();
        }

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingMarkupRequestIntent);

        Timber.tag(DEBUG).i("Set map markup repeating fetch interval: %s seconds.", seconds);
    }

    public void requestTrackingLayersRepeating(int seconds, boolean immediately) {
        Intent intent = new Intent(NICS_POLLING_TRACKING_LAYERS);
        intent.putExtra(TYPE, Polling.TRACKING_LAYERS.toString());

        mPendingTrackingLayerIntent = PendingIntent.getBroadcast(mContext, TRACKING_LAYERS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long secondsFromNow = SystemClock.elapsedRealtime() + (seconds * 1000);

        if (immediately) {
            mNetworkRepository.getTrackingLayers();
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, secondsFromNow, (seconds * 1000), mPendingTrackingLayerIntent);

        Timber.tag(DEBUG).i("Set tracking layer repeating fetch interval: %s seconds.", seconds);
    }

    public void startPolling() {
        requestMarkupRepeating(mSettings.getCollabroomDataRate(), true);
        requestChatMessagesRepeating(mSettings.getCollabroomDataRate(), true);
//        requestChatPresenceRepeating(true);
        requestGeneralMessageRepeating(mSettings.getIncidentDataRate(), true);
        requestEODReportRepeating(mSettings.getIncidentDataRate(), true);
        requestAlertsRepeating(mSettings.getIncidentDataRate(), true);
        requestCollabroomLayersRepeating(mSettings.getWFSDataRate(), true);
        requestCollabroomsRepeating(mSettings.getCollabroomDataRate(), true);
        requestTrackingLayersRepeating(mSettings.getWFSDataRate(), true);
    }

    public void stopPolling() {
        if (mPendingGeneralMessageIntent != null) mAlarmManager.cancel(mPendingGeneralMessageIntent);
        if (mPendingEODReportIntent != null) mAlarmManager.cancel(mPendingEODReportIntent);
        if (mPendingChatMessagesRequestIntent != null) mAlarmManager.cancel(mPendingChatMessagesRequestIntent);
        if (mPendingChatPresenceRequestIntent != null) mAlarmManager.cancel(mPendingChatPresenceRequestIntent);
        if (mPendingMarkupRequestIntent != null) mAlarmManager.cancel(mPendingMarkupRequestIntent);
        if (mPendingAlertIntent != null) mAlarmManager.cancel(mPendingAlertIntent);
        if (mPendingCollabroomLayerIntent != null) mAlarmManager.cancel(mPendingCollabroomLayerIntent);
        if (mPendingCollabroomIntent != null) mAlarmManager.cancel(mPendingCollabroomIntent);
        if (mPendingTrackingLayerIntent != null) mAlarmManager.cancel(mPendingTrackingLayerIntent);
        if (mPendingIncidentIntent != null) mAlarmManager.cancel(mPendingIncidentIntent);
        if (mPendingOrganizationIntent != null) mAlarmManager.cancel(mPendingOrganizationIntent);
        mIsPollingIncident = false;
        mIsPollingCollabroom = false;
    }

    public void startPollingServer() {
        requestIncidentsRepeating(mSettings.getIncidentDataRate(), true);
        requestOrganizationsRepeating(mSettings.getIncidentDataRate(), true);
    }

    public void stopPollingServer() {
        if (mPendingIncidentIntent != null) mAlarmManager.cancel(mPendingIncidentIntent);
        if (mPendingOrganizationIntent != null) mAlarmManager.cancel(mPendingOrganizationIntent);
    }

    public void startPollingIncident() {
        requestAlertsRepeating(mSettings.getIncidentDataRate(), true);
        requestCollabroomsRepeating(mSettings.getCollabroomDataRate(), true);
        requestTrackingLayersRepeating(mSettings.getWFSDataRate(), true);
        mIsPollingIncident = true;
    }

    public void stopPollingIncident() {
        if (mPendingAlertIntent != null) mAlarmManager.cancel(mPendingAlertIntent);
        if (mPendingCollabroomLayerIntent != null) mAlarmManager.cancel(mPendingCollabroomLayerIntent);
        if (mPendingCollabroomIntent != null) mAlarmManager.cancel(mPendingCollabroomIntent);
        if (mPendingTrackingLayerIntent != null) mAlarmManager.cancel(mPendingTrackingLayerIntent);
        mIsPollingIncident = false;
    }

    public void startPollingCollabroom() {
        requestMarkupRepeating(mSettings.getCollabroomDataRate(), true);
        requestCollabroomLayersRepeating(mSettings.getWFSDataRate(), true);
        requestChatMessagesRepeating(mSettings.getCollabroomDataRate(), true);
        requestChatPresenceRepeating(true);
        requestGeneralMessageRepeating(mSettings.getCollabroomDataRate(), true);
        requestEODReportRepeating(mSettings.getCollabroomDataRate(), true);
        mIsPollingCollabroom = true;
    }

    public void stopPollingCollabroom() {
        if (mPendingGeneralMessageIntent != null) mAlarmManager.cancel(mPendingGeneralMessageIntent);
        if (mPendingEODReportIntent != null) mAlarmManager.cancel(mPendingEODReportIntent);
        if (mPendingChatMessagesRequestIntent != null) mAlarmManager.cancel(mPendingChatMessagesRequestIntent);
        if (mPendingChatPresenceRequestIntent != null) mAlarmManager.cancel(mPendingChatPresenceRequestIntent);
        if (mPendingMarkupRequestIntent != null) mAlarmManager.cancel(mPendingMarkupRequestIntent);
        mIsPollingCollabroom = false;
    }

    public void refreshPollingIncident() {
        if (mIsPollingIncident) {
            stopPollingIncident();
            startPollingIncident();
        }
    }

    public void refreshPollingCollabroom() {
        if (mIsPollingCollabroom) {
            stopPollingCollabroom();
            startPollingCollabroom();
        }
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkConnected(mConnectivityManager) && mAuthRepository.isLoggedIn()) {
                try {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    if (pm != null) {
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NICS_WAKE_LOCK);
                    }

                    //Acquire the lock
                    if (wakeLock != null) {
                        wakeLock.acquire(TEN_MINUTES);
                    }

                    Polling type = Polling.valueOf(intent.getStringExtra(TYPE));
                    Timber.tag(DEBUG).i("Requesting Data Update: %s", type);
                    switch (type) {
                        case GENERAL_MESSAGE:
                            mNetworkRepository.getGeneralMessages();
                            break;
                        case EOD_REPORT:
                            mNetworkRepository.getEODReports();
                            break;
                        case CHAT_MESSAGES:
                            mNetworkRepository.getChatMessages();
                            break;
                        case CHAT_PRESENCE:
                            mNetworkRepository.postChatPresence(PresenceStatus.ACTIVE);
                            break;
                        case MAP_MARKUP:
                            mNetworkRepository.getMarkupFeatures();
                            break;
                        case ALERTS:
                            mNetworkRepository.getAlerts();
                            break;
                        case COLLABROOM_LAYERS:
                            mNetworkRepository.getCollabroomLayers();
                            break;
                        case TRACKING_LAYERS:
                            mNetworkRepository.getTrackingLayers();
                            break;
                        case COLLABROOM:
                            mNetworkRepository.getCollabrooms(mPreferences.getSelectedIncidentId());
                            break;
                        case INCIDENT:
                            mNetworkRepository.getIncidents();
                            break;
                        case ORGANIZATION:
                            mNetworkRepository.getUserOrgs();
                            break;
                    }

                    //Release the lock
                    if (wakeLock != null) {
                        wakeLock.release();
                    }
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed inside of the polling receiver.");
                }
            }
        }
    };

    // TODO need to implement this back.
    public void startServerPingedTimer() {
        long rate = (long) mSettings.getIncidentDataRate() * 1000L;

        mTimeToDisconnect = rate * 2L;
        mServerCommsTimer = new Timer();
        mServerCommsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkLastTimeServerWasPinged();
            }
        }, rate, rate);
    }

    public void stopServerPingedTimer() {
        if (mServerCommsTimer != null) {
            mServerCommsTimer.cancel();
            mServerCommsTimer = null;
        }
    }

    public void checkLastTimeServerWasPinged() {
        if (isNetworkConnected(mConnectivityManager)) {
            if ((System.currentTimeMillis() - mPreferences.getLastSuccessfulServerCommsTimestamp()) >= mTimeToDisconnect) {
                mPreferences.switchToOfflineMode();
            }
        } else {
            String userName = mPreferences.getUserName();
            if (!userName.equals(NICS_NO_RESULTS)) {
//                mNetworkRepository.setupAuth(activity, mNetworkRepository, mPreferences, mAuthRepository, userName, null);
//                mNetworkRepository.nicsLogin();
            }
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs
     * in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public PollingService getService() {
            return PollingService.this;
        }
    }
}
