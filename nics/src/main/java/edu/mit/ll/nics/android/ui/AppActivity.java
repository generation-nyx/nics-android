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
package edu.mit.ll.nics.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import net.openid.appauth.AuthorizationService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.auth.AuthStateManager;
import edu.mit.ll.nics.android.auth.HostSelectionInterceptor;
import edu.mit.ll.nics.android.database.AppDatabase;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.repository.AlertRepository;
import edu.mit.ll.nics.android.repository.AuthRepository;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.ConfigRepository;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.services.ServiceManager;
import edu.mit.ll.nics.android.utils.NotificationsHandler;

@AndroidEntryPoint
public abstract class AppActivity extends AppCompatActivity {

    protected Context mContext;
    protected Activity mActivity;
    protected Resources mResources;
    protected NavController mNavController;
    protected NavHostFragment mNavHostFragment;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        mContext = getApplicationContext();
        mResources = getResources();
    }

    @Inject
    WorkManager mWorkManager;

    @Inject
    SettingsRepository mSettings;

    @Inject
    PreferencesRepository mPreferences;

    @Inject
    AuthRepository mAuthRepository;

    @Inject
    AlertRepository mAlertRepository;

    @Inject
    TrackingLayerRepository mTrackingLayerRepository;

    @Inject
    GeneralMessageRepository mGeneralMessageRepository;

    @Inject
    EODReportRepository mEODReportRepository;

    @Inject
    ChatRepository mChatRepository;

    @Inject
    NotificationsHandler mNotificationsHandler;

    @Inject
    ConnectivityManager mConnectivityManager;

    @Inject
    NetworkRepository mNetworkRepository;

    @Inject
    ServiceManager mServiceManager;

    @Inject
    ConfigRepository mConfigRepository;

    @Inject
    HostSelectionInterceptor mHostSelectionInterceptor;

    @Inject
    AuthStateManager mAuthStateManager;

    @Inject
    AuthorizationService mAuthService;

    @Inject
    AppDatabase mDatabase;

    protected void subscribeToWorker(OneTimeWorkRequest request, WorkerCallback callback) {
        LiveData<WorkInfo> workInfo = mWorkManager.getWorkInfoByIdLiveData(request.getId());
        workInfo.observe(this, info -> {
            if (info != null) {
                WorkInfo.State state = info.getState();
                if (state.isFinished()) {
                    if (state == WorkInfo.State.SUCCEEDED) {
                        callback.onSuccess(info);
                    } else if (state == WorkInfo.State.CANCELLED || state == WorkInfo.State.FAILED) {
                        callback.onFailure(info);
                    }

                    // Stop listening to this worker when finished.
                    workInfo.removeObservers(this);
                } else {
                    callback.onWorking();
                }
            }
        });
    }
}
