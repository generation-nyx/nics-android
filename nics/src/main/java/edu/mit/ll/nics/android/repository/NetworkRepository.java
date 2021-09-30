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
package edu.mit.ll.nics.android.repository;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.data.OrgCapabilities;
import edu.mit.ll.nics.android.data.Presence;
import edu.mit.ll.nics.android.data.messages.CollaborationRoomMessage;
import edu.mit.ll.nics.android.database.entities.Alert;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.MobileDeviceTracking;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.enums.PresenceStatus;
import edu.mit.ll.nics.android.workers.AlertsWorker;
import edu.mit.ll.nics.android.workers.ChatWorkers;
import edu.mit.ll.nics.android.workers.CollabroomLayersWorker;
import edu.mit.ll.nics.android.workers.CollabroomWorker;
import edu.mit.ll.nics.android.workers.DownloadImageWorker;
import edu.mit.ll.nics.android.workers.EODReportsWorkers;
import edu.mit.ll.nics.android.workers.GeneralMessagesWorkers;
import edu.mit.ll.nics.android.workers.GeocodingWorkers;
import edu.mit.ll.nics.android.workers.IncidentWorker;
import edu.mit.ll.nics.android.workers.LoginWorkers;
import edu.mit.ll.nics.android.workers.MDTWorkers;
import edu.mit.ll.nics.android.workers.MapWorkers;
import edu.mit.ll.nics.android.workers.OpenElevationWorker;
import edu.mit.ll.nics.android.workers.OrgWorkers;
import edu.mit.ll.nics.android.workers.OverlappingRoomWorker;
import edu.mit.ll.nics.android.workers.TrackingLayersWorkers.TrackingLayerWFSDataWorker;
import edu.mit.ll.nics.android.workers.TrackingLayersWorkers.TrackingLayersWorker;
import edu.mit.ll.nics.android.workers.UserWorkers;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.Utils.isCollabroomSelected;
import static edu.mit.ll.nics.android.utils.Utils.isIncidentSelected;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.workers.Workers.DELETE_MARKUP_FEATURES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.DELETE_MOBILE_DEVICE_TRACKS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.DOWNLOAD_IMAGE_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GEOCODE_ADDRESS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GEOCODE_COORDINATE_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_ALERTS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_ALL_INCIDENTS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_ALL_USER_DATA_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_CHAT_MESSAGES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_COLLABROOMS_AND_ROOM_LAYERS_WORKER_CHAIN;
import static edu.mit.ll.nics.android.workers.Workers.GET_COLLABROOMS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_COLLABROOM_LAYERS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_EOD_REPORTS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_GENERAL_MESSAGES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_MARKUP_FEATURES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_ORG_CAPABILITIES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_OVERLAPPING_COLLABROOMS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_SERVER_HOST_CONFIG_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_SYMBOLOGY_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_TRACKING_LAYERS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_TRACKING_LAYER_WFS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_USER_DATA_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_USER_ORGS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_USER_WORKSPACES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.LOGIN_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.LOGOUT_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.OPEN_ELEVATION_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.POST_CHAT_MESSAGES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.POST_CHAT_PRESENCE_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.POST_EOD_REPORTS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.POST_GENERAL_MESSAGES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.POST_MARKUP_FEATURES_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.POST_MOBILE_DEVICE_TRACKS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.UPDATE_MARKUP_FEATURES_WORKER;

@Singleton
public class NetworkRepository {

    public static String mDeviceId;
    public static int loginRetryCount = 0;
    public static boolean mIsAttemptingLogin = false;

    private final WorkManager mWorkManager;
    private final PreferencesRepository mPreferences;
    private final AuthRepository mAuthRepository;
    private final MapRepository mMapRepository;
    private final ChatRepository mChatRepository;
    private final GeneralMessageRepository mGeneralMessageRepository;
    private final EODReportRepository mEODReportRepository;

    @Inject
    public NetworkRepository(WorkManager workManager,
                             AuthRepository authRepository,
                             PreferencesRepository preferences,
                             MapRepository mapRepository,
                             ChatRepository chatRepository,
                             GeneralMessageRepository generalMessageRepository,
                             EODReportRepository eodReportRepository) {
        mWorkManager = workManager;
        mAuthRepository = authRepository;
        mMapRepository = mapRepository;
        mChatRepository = chatRepository;
        mGeneralMessageRepository = generalMessageRepository;
        mEODReportRepository = eodReportRepository;
        mPreferences = preferences;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to fetch the host configuration file for the server that
     * we will use for the API endpoints.
     *
     * @param config_host The host server that should have the config file.
     *
     * @see LoginWorkers.GetHostConfig
     */
    public OneTimeWorkRequest fetchHostConfig(String config_host) {
        String url = "https://" + config_host + "/.well-known/host-meta.json";

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LoginWorkers.GetHostConfig.class)
                .addTag(GET_SERVER_HOST_CONFIG_WORKER)
                .setInputData(new Data.Builder()
                        .putString("url", url)
                        .build())
                .build();
        mWorkManager.enqueueUniqueWork(GET_SERVER_HOST_CONFIG_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to have the user login to the server.
     *
     * @see LoginWorkers.NicsLogin
     */
    public OneTimeWorkRequest nicsLogin() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LoginWorkers.NicsLogin.class)
                .addTag(LOGIN_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(LOGIN_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to have the user login to the server.
     *
     * @see LoginWorkers.NicsLogout
     */
    public OneTimeWorkRequest nicsLogout() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LoginWorkers.NicsLogout.class)
                .addTag(LOGOUT_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(LOGOUT_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the user's org capabilities using the REST API
     * endpoint and set them in the user's preferences.
     *
     * @see OrgWorkers.OrgCapabilitiesWorker
     * @see OrgCapabilities
     */
    public void getOrgCapabilities() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OrgWorkers.OrgCapabilitiesWorker.class)
                .addTag(GET_ORG_CAPABILITIES_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_ORG_CAPABILITIES_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the symbology using the REST API
     * endpoint and store them in the database.
     *
     * @see OrgWorkers.OrgSymbologyWorker
     * @see edu.mit.ll.nics.android.database.entities.SymbologyGroup
     */
    public void getOrgSymbology() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OrgWorkers.OrgSymbologyWorker.class)
                .addTag(GET_SYMBOLOGY_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_SYMBOLOGY_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the organizations that are available to the
     * current user using the REST API endpoint and set them in the user's preferences.
     *
     * @see UserWorkers.GetUserOrgs
     */
    public OneTimeWorkRequest getUserOrgs() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UserWorkers.GetUserOrgs.class)
                .addTag(GET_USER_ORGS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_USER_ORGS_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request user data given the userId,
     * using the REST API endpoint and set them in the user's preferences.
     *
     * @see UserWorkers.GetUserData
     */
    public OneTimeWorkRequest getUserData() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UserWorkers.GetUserData.class)
                .addTag(GET_USER_DATA_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_USER_DATA_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request all user data using the REST API endpoint
     * and set them in the user's preferences.
     *
     * @see UserWorkers.GetAllUserData
     */
    public OneTimeWorkRequest getAllUserData() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UserWorkers.GetAllUserData.class)
                .addTag(GET_ALL_USER_DATA_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_ALL_USER_DATA_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request all user workspaces using the REST API endpoint
     * and set them in the user's preferences.
     *
     * @see UserWorkers.GetUserWorkspaces
     */
    public OneTimeWorkRequest getUserWorkspaces() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UserWorkers.GetUserWorkspaces.class)
                .addTag(GET_USER_WORKSPACES_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_USER_WORKSPACES_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request all of the incidents available to the user
     * using the REST API endpoint and set them in the user's preferences.
     *
     * @see IncidentWorker
     */
    public OneTimeWorkRequest getIncidents() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(IncidentWorker.class)
                .addTag(GET_ALL_INCIDENTS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_ALL_INCIDENTS_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request all of the collaboration rooms that are a part
     * of the provided incident using the REST API endpoint and add them to the local database.
     *
     * @param incidentId The incidentId of the incident to request for.
     *
     * @see CollabroomWorker
     * @see CollaborationRoomMessage
     * @see OverlappingRoomLayer
     */
    public void getCollabrooms(long incidentId) {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest collabroomWorker = new OneTimeWorkRequest.Builder(CollabroomWorker.class)
                .addTag(GET_COLLABROOMS_WORKER)
                .build();

        OneTimeWorkRequest overlappingRoomWorker = new OneTimeWorkRequest.Builder(OverlappingRoomWorker.class)
                .addTag(GET_OVERLAPPING_COLLABROOMS_WORKER)
                .setInputData(new Data.Builder()
                        .putLong("incidentId", incidentId)
                        .build())
                .build();

        mWorkManager.beginUniqueWork(GET_COLLABROOMS_AND_ROOM_LAYERS_WORKER_CHAIN, ExistingWorkPolicy.KEEP, collabroomWorker)
                .then(overlappingRoomWorker).enqueue();
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the General Message reports using the REST API
     * endpoint and add them to the local database.
     *
     * @see GeneralMessagesWorkers.Get
     * @see GeneralMessage
     */
    public void getGeneralMessages() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GeneralMessagesWorkers.Get.class)
                .addTag(GET_GENERAL_MESSAGES_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_GENERAL_MESSAGES_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to post the locally stored General Message reports to the
     * server.
     *
     * @see GeneralMessagesWorkers.Post
     * @see GeneralMessage
     */
    public void postGeneralMessages() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        List<GeneralMessage> generalMessages = mGeneralMessageRepository.getGeneralMessagesReadyToSend(mPreferences.getUserName());

        if (generalMessages.size() > 0) {
            Timber.tag(DEBUG).d("Preparing to send %s general messages.", generalMessages.size());
        }

        for (GeneralMessage generalMessage : generalMessages) {
            if (!generalMessage.isDraft()) {
                postGeneralMessage(generalMessage.getId());
            }
        }
    }

    public void postGeneralMessage(long id) {
        String worker = POST_GENERAL_MESSAGES_WORKER + id;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GeneralMessagesWorkers.Post.class)
                .addTag(POST_GENERAL_MESSAGES_WORKER)
                .addTag(String.valueOf(id))
                .setInputData(new Data.Builder().putLong("reportId", id).build())
                .build();
        mWorkManager.enqueueUniqueWork(worker, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the EOD reports using the REST API
     * endpoint and add them to the local database.
     *
     * @see EODReportsWorkers.Get
     * @see EODReport
     */
    public void getEODReports() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EODReportsWorkers.Get.class)
                .addTag(GET_EOD_REPORTS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_EOD_REPORTS_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to post the locally stored EOD reports to the
     * server.
     *
     * @see EODReportsWorkers.Post
     * @see EODReport
     */
    public void postEODReports() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        List<EODReport> eodReports = mEODReportRepository.getEODReportsReadyToSend(mPreferences.getUserName());

        if (eodReports.size() > 0) {
            Timber.tag(DEBUG).d("Preparing to send %s eod reports.", eodReports.size());
        }

        for (EODReport report : eodReports) {
            if (!report.isDraft()) {
                postEODReport(report.getId());
            }
        }
    }

    public void postEODReport(long id) {
        String worker = POST_EOD_REPORTS_WORKER + id;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EODReportsWorkers.Post.class)
                .addTag(POST_EOD_REPORTS_WORKER)
                .addTag(String.valueOf(id))
                .setInputData(new Data.Builder().putLong("reportId", id).build())
                .build();
        mWorkManager.enqueueUniqueWork(worker, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the chat messages using the REST API
     * endpoint and add them to the local database.
     *
     * @see ChatWorkers.Get
     * @see Chat
     */
    public void getChatMessages() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ChatWorkers.Get.class)
                .addTag(GET_CHAT_MESSAGES_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_CHAT_MESSAGES_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to post the locally stored chat messages to the server.
     *
     * @see ChatWorkers.Post
     * @see Chat
     */
    public void postChatMessages() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        List<Chat> chats = mChatRepository.getChatToSend();

        if (chats.size() > 0) {
            Timber.tag(DEBUG).d("Preparing to send %s chats.", chats.size());
        }

        for (Chat chat : chats) {
            postChatMessage(chat.getId());
        }
    }

    public void postChatMessage(long id) {
        String worker = POST_CHAT_MESSAGES_WORKER + id;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ChatWorkers.Post.class)
                .addTag(POST_CHAT_MESSAGES_WORKER)
                .addTag(String.valueOf(id))
                .setInputData(new Data.Builder().putLong("id", id).build())
                .build();
        mWorkManager.enqueueUniqueWork(worker, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to post the chat presence to the server.
     *
     * @param status The chat presence status.
     *
     * @see ChatWorkers.ChatPresence
     * @see Presence
     * @see PresenceStatus
     */
    public void postChatPresence(PresenceStatus status) {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ChatWorkers.ChatPresence.class)
                .addTag(POST_CHAT_PRESENCE_WORKER)
                .setInputData(new Data.Builder()
                        .putString("sendStatus", status.getText())
                        .build())
                .build();
        mWorkManager.enqueueUniqueWork(POST_CHAT_PRESENCE_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to request the markup features using the REST API
     * endpoint and add them to the local database.
     *
     * @see MapWorkers.Get
     * @see MarkupFeature
     */
    public void getMarkupFeatures() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MapWorkers.Get.class)
                .addTag(GET_MARKUP_FEATURES_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_MARKUP_FEATURES_WORKER, ExistingWorkPolicy.KEEP, request);
    }


    /**
     * Start a {@link OneTimeWorkRequest} to post the local markup features using the REST API
     * endpoint and add them to the server's database.
     *
     * @see MapWorkers.Post
     * @see MarkupFeature
     */
    public void postMarkupFeatures() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        List<MarkupFeature> features = mMapRepository.getAllMarkupReadyToSendForUser(mPreferences.getUserName());

        if (features.size() > 0) {
            Timber.tag(DEBUG).d("Preparing to send %s markup features.", features.size());
        }

        for (MarkupFeature feature : features) {
            postMarkupFeature(feature.getId());
        }
    }

    public void postMarkupFeature(long id) {
        String worker = POST_MARKUP_FEATURES_WORKER + id;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MapWorkers.Post.class)
                .addTag(worker)
                .addTag(String.valueOf(id))
                .setInputData(new Data.Builder().putLong("id", id).build())
                .build();
        mWorkManager.enqueueUniqueWork(POST_MARKUP_FEATURES_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to update the server's markup features with the local
     * updates using the REST API endpoint.
     *
     * @see MapWorkers.Update
     * @see MarkupFeature
     */
    public void updateMarkupFeatures() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        List<MarkupFeature> features = mMapRepository.getAllMarkupReadyToUpdateForUser(mPreferences.getUserName());

        if (features.size() > 0) {
            Timber.tag(DEBUG).d("Preparing to update %s markup features.", features.size());
        }

        for (MarkupFeature feature : features) {
            updateMarkupFeature(feature.getId());
        }
    }

    public void updateMarkupFeature(long id) {
        String worker = UPDATE_MARKUP_FEATURES_WORKER + id;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MapWorkers.Update.class)
                .addTag(UPDATE_MARKUP_FEATURES_WORKER)
                .addTag(String.valueOf(id))
                .setInputData(new Data.Builder().putLong("id", id).build())
                .build();
        mWorkManager.enqueueUniqueWork(worker, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to delete the server's markup features with the local
     * updates using the REST API endpoint.
     *
     * @see MapWorkers.Delete
     * @see MarkupFeature
     */
    public void deleteMarkupFeatures() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        List<MarkupFeature> features = mMapRepository.getAllMarkupReadyToDelete(mPreferences.getUserName());

        if (features.size() > 0) {
            Timber.tag(DEBUG).d("Preparing to delete %s markup features.", features.size());
        }

        for (MarkupFeature feature : features) {
            deleteMarkupFeature(feature.getId());
        }
    }

    public void deleteMarkupFeature(long id) {
        String worker = DELETE_MARKUP_FEATURES_WORKER + id;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MapWorkers.Delete.class)
                .addTag(DELETE_MARKUP_FEATURES_WORKER)
                .addTag(String.valueOf(id))
                .setInputData(new Data.Builder().putLong("id", id).build())
                .build();
        mWorkManager.enqueueUniqueWork(worker, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get the collabroom layers using the REST API endpoint.
     *
     * @see CollabroomLayersWorker
     * @see CollabroomDataLayer
     */
    public OneTimeWorkRequest getCollabroomLayers() {
        if (!mAuthRepository.isLoggedIn()) {
            return null;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CollabroomLayersWorker.class)
                .addTag(GET_COLLABROOM_LAYERS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_COLLABROOM_LAYERS_WORKER, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get the tracking layers using the REST API endpoint.
     *
     * @see TrackingLayersWorker
     * @see Tracking
     */
    public void getTrackingLayers() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(TrackingLayersWorker.class)
                .addTag(GET_TRACKING_LAYERS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_TRACKING_LAYERS_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get the tracking layer's WFS data using the REST API endpoint.
     *
     * @param name The layer name to use to query the database for the tracking payload.
     *
     * @see TrackingLayerWFSDataWorker
     * @see Tracking
     */
    public void getTrackingLayerWfsData(String name) {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(TrackingLayerWFSDataWorker.class)
                .addTag(GET_TRACKING_LAYER_WFS_WORKER)
                .setInputData(new Data.Builder()
                        .putString("name", name)
                        .build())
                .build();
        mWorkManager.enqueueUniqueWork(GET_TRACKING_LAYER_WFS_WORKER + name,
                ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get the incident alerts using the REST API endpoint.
     *
     * @see AlertsWorker
     * @see Alert
     */
    public void getAlerts() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AlertsWorker.class)
                .addTag(GET_ALERTS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(GET_ALERTS_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get the post mobile device tracks to the server using
     * the REST API endpoint.
     *
     * @see MDTWorkers.Post
     * @see MobileDeviceTracking
     */
    public void postMDTs() {
        if (!mAuthRepository.isLoggedIn()) {
            return;
        }

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MDTWorkers.Post.class)
                .addTag(POST_MOBILE_DEVICE_TRACKS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(POST_MOBILE_DEVICE_TRACKS_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get the delete mobile device tracks to the server using
     * the REST API endpoint.
     *
     * @see MDTWorkers.Delete
     * @see MobileDeviceTracking
     */
    public void deleteMDT() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MDTWorkers.Delete.class)
                .addTag(DELETE_MOBILE_DEVICE_TRACKS_WORKER)
                .build();
        mWorkManager.enqueueUniqueWork(DELETE_MOBILE_DEVICE_TRACKS_WORKER, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * Start a {@link OneTimeWorkRequest} to download an image from a url.
     *
     * @see DownloadImageWorker
     */
    public OneTimeWorkRequest downloadImage(String url) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DownloadImageWorker.class)
                .addTag(DOWNLOAD_IMAGE_WORKER)
                .setInputData(new Data.Builder()
                        .putString("url", url)
                        .build())
                .build();
        mWorkManager.enqueueUniqueWork(DOWNLOAD_IMAGE_WORKER + url, ExistingWorkPolicy.KEEP, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to geocode a LatLng coordinate.
     */
    public OneTimeWorkRequest geocodeCoordinate(@NonNull LatLng coordinate) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GeocodingWorkers.GeocodeCoordinate.class)
                .addTag(GEOCODE_COORDINATE_WORKER)
                .setInputData(new Data.Builder()
                        .putDouble("latitude", coordinate.latitude)
                        .putDouble("longitude", coordinate.longitude)
                        .build())
                .build();
        mWorkManager.enqueueUniqueWork(GEOCODE_COORDINATE_WORKER, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to geocode an address.
     */
    public OneTimeWorkRequest geocodeAddress(String address) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GeocodingWorkers.GeocodeAddress.class)
                .addTag(GEOCODE_ADDRESS_WORKER)
                .setInputData(new Data.Builder()
                        .putString("address", address)
                        .build())
                .build();
        mWorkManager.enqueueUniqueWork(GEOCODE_ADDRESS_WORKER, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
        return request;
    }

    /**
     * Start a {@link OneTimeWorkRequest} to get an elevation for the provided coordinate using
     * Open Elevation.
     */
    public OneTimeWorkRequest getElevation(LatLng coordinate) {
        if (coordinate != null) {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OpenElevationWorker.class)
                    .addTag(OPEN_ELEVATION_WORKER)
                    .setInputData(new Data.Builder()
                            .putDouble("latitude", coordinate.latitude)
                            .putDouble("longitude", coordinate.longitude)
                            .build())
                    .build();
            mWorkManager.enqueueUniqueWork(OPEN_ELEVATION_WORKER, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
            return request;
        } else {
            return null;
        }
    }

    /**
     * Send all pending local content to the server.
     */
    public void sendAllLocalContent() {
        postChatMessages();
        postChatPresence(PresenceStatus.ACTIVE);
        postGeneralMessages();
        postEODReports();
        deleteMarkupFeatures();
        postMarkupFeatures();
        updateMarkupFeatures();
    }

    public void refreshMapContent() {
        postMarkupFeatures();
        deleteMarkupFeatures();
        updateMarkupFeatures();
        getMarkupFeatures();
        getCollabroomLayers();
        getTrackingLayers();
        getCollabrooms(mPreferences.getSelectedIncidentId());
    }

    public void refreshCollabroom() {
        if (isCollabroomSelected(mPreferences.getSelectedCollabroom())) {
            refreshMapContent();
            postChatMessages();
            postChatPresence(PresenceStatus.ACTIVE);
            postGeneralMessages();
            postEODReports();
            getChatMessages();
            getGeneralMessages();
            getEODReports();
        }
    }

    public void refreshIncident() {
        if (isIncidentSelected(mPreferences.getSelectedIncident())) {
            getAlerts();
            getCollabrooms(mPreferences.getSelectedIncidentId());
            getTrackingLayers();
        }
    }

    public void refreshWorkspace() {
        getAllUserData();
        getUserWorkspaces();
        getIncidents();
        getUserOrgs();
        getUserData();
        getOrgCapabilities();
        getOrgSymbology();
    }

    public void refreshAllContent() {
        refreshIncident();
        refreshCollabroom();
    }

    /**
     * Request the legal information for Google Maps from Google.
     *
     * @param responseHandler The {@link AsyncHttpResponseHandler} for the network request.
     */
    public void getGoogleMapsLegalInfo(AsyncHttpResponseHandler responseHandler) {
        AsyncHttpClient mClient = new AsyncHttpClient();
        mClient.setTimeout(60 * 1000);
        mClient.setURLEncodingEnabled(false);
        mClient.setMaxRetriesAndTimeout(2, 1000);
        mClient.get("https://www.google.com/mobile/legalnotices/", responseHandler);
    }

    // TODO don't think these need to be static.
    /**
     * Set the device id for this device to use for sending to the server.
     *
     * @param context The application context to use to get the device id.
     */
    public static void setDeviceId(Context context) {
        // TODO not sure how to handle this yet : https://developer.android.com/training/articles/user-data-ids
        if (mDeviceId == null || mDeviceId.isEmpty() || mDeviceId.equalsIgnoreCase("unknown")) {
            mDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
    }

    /**
     * Set a flag for whether or not the user is currently attempting to login.
     *
     * @param isAttemptingLogin The boolean value for whether or not they are logging in.
     */
    public static void setIsAttemptingLogin(boolean isAttemptingLogin) {
        mIsAttemptingLogin = isAttemptingLogin;
    }
}
