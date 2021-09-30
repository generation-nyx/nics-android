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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.WorkInfo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.EndSessionRequest;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.NavigationGraphDirections;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Alert;
import edu.mit.ll.nics.android.databinding.ActivityMainBinding;
import edu.mit.ll.nics.android.databinding.DialogSplashScreenBinding;
import edu.mit.ll.nics.android.interfaces.ServiceBoundListener;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.ui.dialogs.SplashScreenDialog;
import edu.mit.ll.nics.android.ui.viewmodel.MainViewModel;
import edu.mit.ll.nics.android.utils.constants.Intents;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static edu.mit.ll.nics.android.utils.Utils.clearWorkers;
import static edu.mit.ll.nics.android.utils.Utils.getNicsVersion;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.constants.Events.LOGOUT;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_RECEIVED_ALERT;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_SHOW_PERMISSIONS_DIALOG;
import static edu.mit.ll.nics.android.utils.constants.Events.REFRESH_ACCESS_TOKEN;
import static edu.mit.ll.nics.android.utils.constants.Intents.HAZARD_BOUNDS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_HELP;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_NO_RESULTS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_OID_CLIENT_ID;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_OID_REDIRECT_URI;
import static edu.mit.ll.nics.android.utils.constants.NICS.PRIVACY_POLICY;

@AndroidEntryPoint
public class MainActivity extends AppActivity {

    private MainViewModel mViewModel;
    private SplashScreenDialog mSplashScreenDialog;
    private ActivityResultLauncher<Intent> mLogoutRequest;

    private boolean mEventsRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        binding.setLifecycleOwner(this);
        binding.setViewModel(mViewModel);
        setSupportActionBar(binding.toolbar);

        mNavHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        mNavController = mNavHostFragment.getNavController();

        initialization();

        // Register for activity result callback for when the end session request is finished.
        mLogoutRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Timber.tag(DEBUG).i("End session request successful");
            }
        });

        subscribeToModel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mServiceManager.setPollingServiceBoundListener(new ServiceBoundListener() {
            @Override
            public void onBound() {
                mViewModel.setPollingServiceBound(true);
            }

            @Override
            public void onUnbound() {
                mViewModel.setPollingServiceBound(false);
            }
        });
    }

    private void subscribeToModel() {
        mViewModel.pollIncident().observe(this, poll -> {
            if (poll) {
                mServiceManager.startPollingIncident();
            } else {
                mServiceManager.stopPolling();
            }
        });

        mViewModel.pollCollabroom().observe(this, poll -> {
            if (poll) {
                mServiceManager.startPollingCollabroom();
            } else {
                mServiceManager.stopPollingCollabroom();
            }
        });

        mViewModel.getHostSelection().observe(this, host -> mHostSelectionInterceptor.setHost(host));

        // TODO when checking for notifications, check the active fragment and don't show the notification if it's from the current user.
        // Listen for any new general message reports to create a notification for.
        mViewModel.getNewGeneralMessages().observe(this, reports -> {
            // Only show the notification if the user has push notifications enabled.
            if (!mSettings.isPushNotificationsDisabled()) {
                mNotificationsHandler.createGeneralMessagesNotification(reports, mContext, mGeneralMessageRepository);
            }
        });

        // Listen for any new eod reports to create a notification for.
        mViewModel.getNewEODReports().observe(this, reports -> {
            // Only show the notification if the user has push notifications enabled.
            if (!mSettings.isPushNotificationsDisabled()) {
                mNotificationsHandler.createEODReportNotification(reports, mContext, mEODReportRepository);
            }
        });

        // Listen for any new chats to create a notification for.
        mViewModel.getNewChats().observe(this, chats -> {
            // Only show the notification if the user has push notifications enabled.
            if (!mSettings.isPushNotificationsDisabled()) {
                mNotificationsHandler.createNewChatNotification(chats, mContext, mChatRepository);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.showAlerts) {
            showAlertsDialog(Alert.toMessage(mAlertRepository.getAlerts(mPreferences.getSelectedIncidentId()), this));
        } else if (id == R.id.settings) {
            navigateSafe(mNavController, NavigationGraphDirections.settings());
        } else if (id == R.id.about) {
            navigateSafe(mNavController, NavigationGraphDirections.about());
        } else if (id == R.id.privacyPolicy) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)));
        } else if (id == R.id.help) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(NICS_HELP)));
        } else if (id == R.id.logout) {
            logout();
        }

        // Removed the switch org button as there are issues on the web.
        // Should also update user session via api when this implemented.
        //        else if (id == R.id.actionSwitchOrgs) {
        //            navigateSafe(mNavController, NavigationGraphDirections.openOrganizationsDialog());
        //        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case Intents.NICS_VIEW_HAZARDS:
                case Intents.NICS_VIEW_MAP:
                    try {
                        NavController controller = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
                        NavigationGraphDirections.GlobalMap globalMap = NavigationGraphDirections.globalMap();
                        globalMap.setHazardBounds(intent.getParcelableExtra(HAZARD_BOUNDS));
                        navigateSafe(controller, globalMap);
                    } catch (Exception e) {
                        Timber.tag(DEBUG).e(e, "Failed to open map from hazard notification click.");
                    }
                    break;
                case Intents.NICS_VIEW_CHAT_LIST:
                    try {
                        NavController controller = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
                        navigateSafe(controller, NavigationGraphDirections.globalChat());
                    } catch (Exception e) {
                        Timber.tag(DEBUG).w(e, "Failed to navigate to chat list fragment.");
                    }
                    break;
                case Intents.NICS_VIEW_GENERAL_MESSAGES_LIST:
                    try {
                        NavController controller = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
                        navigateSafe(controller, NavigationGraphDirections.globalGeneralMessageList());
                    } catch (Exception e) {
                        Timber.tag(DEBUG).w(e, "Failed to navigate to general message list fragment.");
                    }
                    break;
                case Intents.NICS_VIEW_EOD_REPORTS_LIST:
                    try {
                        NavController controller = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
                        navigateSafe(controller, NavigationGraphDirections.globalEODReportList());
                    } catch (Exception e) {
                        Timber.tag(DEBUG).w(e, "Failed to navigate to eod report list fragment.");
                    }
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mEventsRegistered) {
            LiveDataBus.subscribe(NICS_SHOW_PERMISSIONS_DIALOG, this, data -> showPermissionDialog());
            LiveDataBus.subscribe(NICS_RECEIVED_ALERT, this, data -> showAlertsDialog((String) data));
            LiveDataBus.subscribe(LOGOUT, this, data -> logout());
            LiveDataBus.subscribe(REFRESH_ACCESS_TOKEN, this, data -> refreshAccessToken());
            registerReceiver(connectivityReceiver, new IntentFilter(CONNECTIVITY_ACTION));
            mEventsRegistered = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mEventsRegistered) {
            unregisterReceiver(connectivityReceiver);
            LiveDataBus.unregister(NICS_SHOW_PERMISSIONS_DIALOG);
            LiveDataBus.unregister(NICS_RECEIVED_ALERT);
            LiveDataBus.unregister(LOGOUT);
            LiveDataBus.unregister(REFRESH_ACCESS_TOKEN);
            mEventsRegistered = false;
        }
        mServiceManager.stopAllServices();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setLocale();
    }

    private void initialization() {
        // Initialize the default tracking layers for the reports.
        mTrackingLayerRepository.initializeTrackingLayers(mContext, mPreferences.getGeoServerURL());

        mSettings.setSupportedLanguages(mResources.getStringArray(R.array.pref_language_list_values));

        setLocale();
    }

    /**
     * Set the configuration's Locale.
     */
    private void setLocale() {
        if (!mConfigRepository.getLocale().getLanguage().equals(mSettings.getSelectedLanguage())) {
            mConfigRepository.setCurrentLocale(mSettings.getSelectedLanguage());
        }

        Locale locale = mConfigRepository.getLocale();
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        DisplayMetrics dm = mResources.getDisplayMetrics();
        config.locale = locale;
        mResources.updateConfiguration(config, dm);
    }

    private void showPermissionDialog() {
        navigateSafe(mNavController, NavigationGraphDirections.openLocationPermissionsDialog());
    }

    private void showAlertsDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.broadcast_alert))
                .setIcon(R.drawable.nics_logo)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), null)
                .setCancelable(true)
                .create()
                .show();
    }

    /**
     * Connectivity receiver that gets called when the connectivity status changes.
     */
    final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();

            if (info != null) {
                if (info.isConnectedOrConnecting()) {
                    if (mPreferences.getUserSessionId() == -1) {
                        String username = mPreferences.getUserName();

                        if (mAuthStateManager.getCurrent().getAccessToken() != null && !username.equals(NICS_NO_RESULTS) && !username.isEmpty()) {
//                            mNetworkRepository.setupAuth(mActivity, mNetworkRepository, mPreferences, mAuthRepository, username, null);
//                            mNetworkRepository.nicsLogin(username);
                        }
                    }
                    mPreferences.switchToOnlineMode();

                    Timber.tag(DEBUG).i("Device reconnected to " + info.getTypeName() + " network.");
                } else {
                    mPreferences.switchToOfflineMode();
                }
            } else {
                mPreferences.switchToOfflineMode();
                Timber.tag(DEBUG).i("Device disconnected from the data network.");
            }
        }
    };

    private void refreshAccessToken() {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthStateManager.getCurrent().getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Timber.tag(DEBUG).d(ex, "Token request cannot be made, client authentication for the token endpoint could not be constructed (%s)", ex.getMessage());
            return;
        }

        mAuthService.performTokenRequest(
                mAuthStateManager.getCurrent().createTokenRefreshRequest(),
                clientAuthentication,
                (response, ex) -> mAuthStateManager.updateAfterTokenResponse(response, ex));
    }

    private void endSession() {
        AuthState currentState = mAuthStateManager.getCurrent();
        AuthorizationServiceConfiguration config = currentState.getAuthorizationServiceConfiguration();
        if (config != null) {
            Map<String, String> params = new HashMap<>();
            params.put("client_id", NICS_OID_CLIENT_ID);

            if (currentState.getRefreshToken() != null) {
                params.put("refresh_token", currentState.getRefreshToken());
            }

            Intent endSessionIntent = mAuthService.getEndSessionRequestIntent(
                    new EndSessionRequest.Builder(config)
                            .setIdTokenHint(currentState.getIdToken())
                            .setPostLogoutRedirectUri(Uri.parse(NICS_OID_REDIRECT_URI))
                            .setAdditionalParameters(params)
                            .build());

            mLogoutRequest.launch(endSessionIntent);
        } else {
            signOut();
        }

        mActivity.runOnUiThread(() -> {
            closeSplashScreen();

            try {
                if (Objects.requireNonNull(mNavController.getCurrentDestination()).getId() != R.id.loginFragment) {
                    navigateSafe(mNavController, NavigationGraphDirections.logout());
                }
            } catch (NullPointerException ignored) {
            }
        });
    }

    private void closeSplashScreen() {
        mActivity.runOnUiThread(() -> {
            try {
                if (mSplashScreenDialog != null && mSplashScreenDialog.isShowing()) {
                    mSplashScreenDialog.dismiss();
                }
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Error closing the splash screen.");
            }
        });
    }

    private void signOut() {
        AuthorizationServiceConfiguration config = mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration();

        if (config != null) {
            AuthState clearedState = new AuthState(config);
            mAuthStateManager.replace(clearedState);
        } else {
            mAuthStateManager.replace(new AuthState());
        }
    }

    private void logout() {
        Completable.fromAction(() -> {
            // Show the splash screen on the UI thread while the working is happening in the background.
            mActivity.runOnUiThread(() -> {
                // Showing the splash screen during the logout background thread.
                mSplashScreenDialog = new SplashScreenDialog(mActivity, R.style.SplashScreen);

                // Bind the NICS version to the splash screen.
                DialogSplashScreenBinding binding = DataBindingUtil.inflate(LayoutInflater.from(mSplashScreenDialog.getContext()), R.layout.dialog_splash_screen, null, false);
                binding.setNicsVersion(getNicsVersion(mContext));
                binding.loggingIn.setText(getString(R.string.login_progress_signing_out));
                binding.setSigningIn(true);
                mSplashScreenDialog.setContentView(binding.getRoot());
                mSplashScreenDialog.show();

                // Since the service has a lifecycle, need to stop the polling on the UI or main thread.
                mServiceManager.stopPolling();
                mServiceManager.stopAllServices();
            });

            // Clear all local database tables since all of the database data is tied to the user's login session.
            mDatabase.clearAllTables();

            // Stop all workers if there are any.
            clearWorkers(mWorkManager);

            // Remove all notifications.
            mNotificationsHandler.cancelAllNotifications();

            // Tell the server to remove the mobile tracking points.
            if (mAuthRepository.isLoggedIn()) {
                mNetworkRepository.deleteMDT();
            }

            mAuthRepository.setLoggedIn(false);
            NetworkRepository.setIsAttemptingLogin(false);
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(() -> {
                    Timber.tag(DEBUG).d("Successfully cleared database tables for user logout.");
                    // Logout from nics on the main thread.
                    Handler mainHandler = new Handler(getMainLooper());
                    mainHandler.post(this::nicsLogout);
                }, throwable -> {
                    Timber.tag(DEBUG).d("Failed to clear database tables during user logout.");
                    Timber.tag(DEBUG).d("throwable.getMessage(): %s", throwable.getMessage());
                    // Logout from nics on the main thread.
                    Handler mainHandler = new Handler(getMainLooper());
                    mainHandler.post(this::nicsLogout);
                });
    }

    private void nicsLogout() {
        // Wait for the logout to complete and end the user's auth session as well.
        subscribeToWorker(mNetworkRepository.nicsLogout(), new WorkerCallback() {
            @Override
            public void onSuccess(@NotNull WorkInfo workInfo) {
                endSession();
            }

            @Override
            public void onFailure(@NotNull WorkInfo workInfo) {
                endSession();
            }

            @Override
            public void onWorking() {
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}