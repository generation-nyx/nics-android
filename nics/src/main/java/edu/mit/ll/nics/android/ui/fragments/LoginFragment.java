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
package edu.mit.ll.nics.android.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import com.auth0.android.jwt.Claim;
import com.auth0.android.jwt.JWT;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.auth.AppConnectionBuilder;
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.data.Workspace;
import edu.mit.ll.nics.android.databinding.DialogSplashScreenBinding;
import edu.mit.ll.nics.android.databinding.FragmentLoginBinding;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.repository.AuthRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.ui.dialogs.SplashScreenDialog;
import edu.mit.ll.nics.android.ui.viewmodel.LoginViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import timber.log.Timber;

import static edu.mit.ll.nics.android.data.HostServerConfig.DEFAULT_SERVERS;
import static edu.mit.ll.nics.android.utils.AuthUtils.refreshAccessToken;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.StringUtils.cleanUrlString;
import static edu.mit.ll.nics.android.utils.StringUtils.isValidUrl;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.forceHideKeyboard;
import static edu.mit.ll.nics.android.utils.Utils.getNicsVersion;
import static edu.mit.ll.nics.android.utils.Utils.isOrganizationSelected;
import static edu.mit.ll.nics.android.utils.Utils.isWorkspaceSelected;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.Utils.showSimpleDialog;
import static edu.mit.ll.nics.android.utils.constants.Events.LOGOUT;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_ORGANIZATION_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_WORKSPACE_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_HTTP_RESPONSE_ERROR;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_LOGIN_ERROR;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_OID_CLIENT_ID;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_OID_REDIRECT_URI;
import static edu.mit.ll.nics.android.utils.constants.Preferences.END_SESSION_ENDPOINT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@AndroidEntryPoint
public class LoginFragment extends AppFragment {

    private LoginViewModel mViewModel;

    private boolean mIncidentsReceived = false;
    private boolean mOrganizationsReceived = false;
    private SplashScreenDialog mSplashScreenDialog;
    private ActivityResultLauncher<Intent> mLoginRequest;

    private FragmentLoginBinding mBinding;

    @DiskExecutor
    @Inject
    ExecutorService mExecutor;

    @Inject
    AuthRepository mAuthRepository;

    @Inject
    AuthorizationService mAuthService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the nav controller for this fragment.
        mNavController = mNavHostFragment.getNavController();

        // Register for activity result callback for when the user is done authenticating through the browser.
        mLoginRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                openIdTokenRequest(result.getData());
            }
        });

        tryLogin();
    }

    /**
     * Attempt to automatically login if the user has already been authorized. Will need to login
     * to NICS again for a fresh user session.
     */
    @Override
    public void onStart() {
        super.onStart();

    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_login.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link LoginViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setFragment(this);
        mBinding.setViewModel(mViewModel);
        mBinding.setNicsVersion(getNicsVersion(mContext));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, R.layout.dropdown_item);
        mBinding.setAdapter(adapter);
        mBinding.executePendingBindings();

        subscribeToModel(adapter);
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_WORKSPACE_REQUEST, PICK_ORGANIZATION_REQUEST);
        closeSplashScreen();
        super.onDestroy();
    }

    /**
     * When preparing the options menu, hide all of the options that would normally show up except
     * for the menu options that pertain to the login page.
     *
     * @param menu The {@link Menu} to work with.
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull @NotNull Menu menu) {
        List<Integer> options = Arrays.asList(R.id.settings, R.id.privacyPolicy, R.id.help, R.id.about);

        // Hide all options except the valid options for the login page.
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (!options.contains(item.getItemId())) {
                item.setVisible(false);
            }
        }
    }

    /**
     * Subscribe this fragment to the {@link LoginViewModel} to observe changes and dynamically
     * update the UI components.
     *
     * @param adapter The {@link ArrayAdapter<String>} for the server host dropdown list.
     */
    private void subscribeToModel(ArrayAdapter<String> adapter) {
        mViewModel.getServers().observe(mLifecycleOwner, servers -> {
            adapter.clear();
            adapter.addAll(servers);
            adapter.notifyDataSetChanged();

            if (adapter.getCount() > 0) {
                mViewModel.setSelectedServer(adapter.getItem(0));
            }
            mBinding.executePendingBindings();
        });


        subscribeToDestinationResponse(R.id.loginFragment, PICK_WORKSPACE_REQUEST, (DestinationResponse<String>) result -> {
            if (result != null) {
                Workspace workspace = new Gson().fromJson(result, Workspace.class);
                mPreferences.setSelectedWorkspace(workspace);
                login();
            }
            removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_WORKSPACE_REQUEST);
        });

        subscribeToDestinationResponse(R.id.loginFragment, PICK_ORGANIZATION_REQUEST, (DestinationResponse<String>) result -> {
            if (result != null) {
                Organization organization = new Gson().fromJson(result, Organization.class);
                mPreferences.setSelectedOrganization(organization);
                mNetworkRepository.getOrgSymbology();
                openOverviewFragment();
            }
            removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_ORGANIZATION_REQUEST);
        });
    }

    /**
     * Tries to login on a new app startup if the user has already been authorized.
     */
    private void tryLogin() {
        // Show the splash screen until the user logins in or for five seconds if they aren't already logged in.
        mSplashScreenDialog = new SplashScreenDialog(mActivity, R.style.SplashScreen);

        // Bind the NICS version to the splash screen.
        DialogSplashScreenBinding binding = DataBindingUtil.inflate(LayoutInflater.from(mSplashScreenDialog.getContext()), R.layout.dialog_splash_screen, null, false);
        binding.setNicsVersion(getNicsVersion(mContext));
        binding.setSigningIn(false);
        mSplashScreenDialog.setContentView(binding.getRoot());
        mSplashScreenDialog.show();

        // Check to see if the user is completely logged in.
        if (isCompletelyLoggedIn()) {
            binding.setSigningIn(true);
            // If the user is already fully logged in, refresh the access token, close the splash screen, and open the overview fragment.
            // Refresh the access token in the background. Once it's finished, close the splash screen.
            mExecutor.execute(() -> {
                boolean success = refreshAccessToken(mAuthStateManager, mAuthService);
                if (mAuthStateManager.getCurrent().isAuthorized() && success) {
                    mMainHandler.post(() -> subscribeToWorker(mNetworkRepository.nicsLogin(), new WorkerCallback() {
                        @Override
                        public void onSuccess(@NotNull WorkInfo workInfo) {
                            mNetworkRepository.getUserWorkspaces();
                            mNetworkRepository.getIncidents();
                            mNetworkRepository.getUserOrgs();
                            mNetworkRepository.getUserData();
                            mNetworkRepository.getAllUserData();
                            // Navigate to the overview fragment.
                            navigateSafe(mNavController, LoginFragmentDirections.openOverview());
                        }

                        @Override
                        public void onFailure(@NotNull WorkInfo workInfo) {
                            binding.setSigningIn(false);
                            closeSplashScreen();
                            cancelLogin("Failed to sign in automatically.");
                        }

                        @Override
                        public void onWorking() {
                            mViewModel.setLoading(true);
                        }
                    }));
                } else {
                    closeSplashScreen();
                    cancelLogin("Failed to sign in automatically.");
                }
            });
        } else {
            // If the user hasn't logged in yet, show the splash screen for 5 seconds and then show the login page.
            mMainHandler.postDelayed(this::closeSplashScreen, 5000);
        }
    }

    private void closeSplashScreen() {
        try {
            if (mSplashScreenDialog != null && mSplashScreenDialog.isShowing()) {
                mSplashScreenDialog.dismiss();
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Error closing the splash screen.");
        }
    }

    /**
     * Open a simple dialog to enter a URL string to add to the server list.
     * It will validate that the string is an actual url and it will trim out the front to make it a
     * protocol-relative URL string.
     */
    public void addServerHost() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mActivity);
        builder.setTitle(String.format("%s %s", getString(R.string.add), getString(R.string.server_, EMPTY)));

        View addHostView = View.inflate(mContext, R.layout.dialog_add_host, null);
        builder.setView(addHostView);

        EditText input = addHostView.findViewById(R.id.host_input);

        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String host = input.getText().toString();
                Set<String> customHosts = mPreferences.getCustomServerSet();

                if (Patterns.WEB_URL.matcher(host).matches()) {
                    host = cleanUrlString(host);

                    if (!customHosts.contains(host) && !DEFAULT_SERVERS.contains(host) && mPreferences.addCustomServer(host)) {
                        Timber.tag(DEBUG).i("Added server host: %s", host);
                        dialog.dismiss();
                    } else {
                        input.setError("Server has already been added.");
                    }
                } else {
                    input.setError("Invalid Url");
                }
            });
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> dialog.cancel());
        });

        dialog.show();
    }

    /**
     * Removes the currently selected server host url from the custom servers list.
     *
     * Note: Users can only remove custom server entries.
     */
    public void removeServerHost() {
        String host = mViewModel.getSelectedServer().getValue();

        // If the selected server is a default server, tell the user that they can only remove custom servers.
        if (DEFAULT_SERVERS.contains(host)) {
            Snackbar.make(requireView(), "You can only remove custom server entries.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Show a dialog confirming that they want to remove the selected server.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mActivity);
        builder.setTitle(String.format("%s %s", getString(R.string.remove), getString(R.string.server_, EMPTY)));

        // Set the dialog view.
        View removeHostView = View.inflate(mContext, R.layout.dialog_remove_host, null);
        builder.setView(removeHostView);

        // Set the text view to be the selected server to be removed.
        TextView input = removeHostView.findViewById(R.id.host_input);
        input.setText(host);

        // When they click the ok button, remove the selected server from the custom servers list.
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            if (mPreferences.removeCustomServer(host)) {
                Timber.tag(DEBUG).i("Removing server host: %s", host);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Attempts a login specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        // Get the server url that will be used to login the user with.
        String server = mViewModel.getSelectedServer().getValue();

        // Check for a valid server address.
        if (!isValidUrl(server)) {
            mBinding.configHost.setError(getString(R.string.error_field_required));
            mBinding.configHost.requestFocus();
            return;
        }

        // Set the server as currently selected in preferences.
        mPreferences.setConfigHost(server);

        // Hide the keyboard if it's open for some reason.
        forceHideKeyboard(mActivity);

        // The first step to logging in is to fetch the host config using the selected server url.
        fetchHostConfig(server);
    }

    /**
     * Downloads and parses the host configuration from the server to use for the authorization
     * request.
     *
     * @param server The selected server to download the host configuration from.
     */
    private void fetchHostConfig(String server) {
        // Update the status message to let the user know that we are fetching the host config.
        setLoginStatus(R.string.fetching_host_configuration);

        // Waits for a response from the fetch host config worker.
        subscribeToWorker(mNetworkRepository.fetchHostConfig(server), new WorkerCallback() {
            @Override
            public void onSuccess(@NotNull WorkInfo workInfo) {
                if (NetworkRepository.mIsAttemptingLogin) {
                    return;
                }

                requestAuthorization();
            }

            @Override
            public void onFailure(@NotNull WorkInfo workInfo) {
                // If fetching the host config fails, show a dialog with the error message.
                String error = workInfo.getOutputData().getString("error");
                cancelLogin(error);
            }

            @Override
            public void onWorking() {
                mViewModel.setLoading(true);
            }
        });
    }

    /**
     * If user authorization is complete, move onto the next step. If the user hasn't selected a
     * workspace yet, show them the workspace dialog. Otherwise, login to NICS using their
     * selected workspace.
     */
    public void authSuccessful() {
        if (!isWorkspaceSelected(mPreferences.getSelectedWorkspace())) {
            setLoginStatus(R.string.loading_workspaces);
            subscribeToWorker(mNetworkRepository.getUserWorkspaces(), new WorkerCallback() {
                @Override
                public void onSuccess(@NotNull WorkInfo workInfo) {
                    if (!isWorkspaceSelected(mPreferences.getSelectedWorkspace())) {
                        showWorkspaceDialog();
                    } else {
                        login();
                    }
                }

                @Override
                public void onFailure(@NotNull WorkInfo workInfo) {
                    // If fetching the host config fails, show a dialog with the error message.
                    String error = workInfo.getOutputData().getString("error");
                    cancelLogin(error);
                }

                @Override
                public void onWorking() {
                    mViewModel.setLoading(true);
                }
            });
        } else {
            login();
        }
    }

    /**
     * Open the workspaces dialog to have the user select the workspace to login to.
     */
    private void showWorkspaceDialog() {
        navigateSafe(mNavController, LoginFragmentDirections.openWorkspacesDialog());
    }

    /**
     * Open the organizations dialog to have the user select the organization to login as.
     */
    private void showOrganizationDialog() {
        navigateSafe(mNavController, LoginFragmentDirections.openOrganizationsDialog());
    }

    /**
     * Login to NICS using the currently selected workspace.
     */
    private void login() {
        setLoginStatus(R.string.login_progress_signing_in);
        subscribeToWorker(mNetworkRepository.nicsLogin(), new WorkerCallback() {
            @Override
            public void onSuccess(@NotNull WorkInfo workInfo) {
                setLoginStatus(R.string.loading_incidents_and_collabrooms);
                getIncidents();
                getUserOrgs();
                mNetworkRepository.getUserData();
                mNetworkRepository.getAllUserData();
            }

            @Override
            public void onFailure(@NotNull WorkInfo workInfo) {
                String error = workInfo.getOutputData().getString("error");
                cancelLogin(error);
            }

            @Override
            public void onWorking() {
                mViewModel.setLoading(true);
            }
        });
    }

    private void getIncidents() {
        subscribeToWorker(mNetworkRepository.getIncidents(), new WorkerCallback() {
            @Override
            public void onSuccess(@NotNull WorkInfo workInfo) {
                mIncidentsReceived = true;
                if (mOrganizationsReceived) {
                    openOverviewFragment();
                }
            }

            @Override
            public void onFailure(@NotNull WorkInfo workInfo) {
                String error = workInfo.getOutputData().getString("error");
                cancelLogin(error);
            }

            @Override
            public void onWorking() {
                mViewModel.setLoading(true);
            }
        });
    }

    private void getUserOrgs() {
        setLoginStatus(R.string.loading_organizations);
        subscribeToWorker(mNetworkRepository.getUserOrgs(), new WorkerCallback() {
            @Override
            public void onSuccess(@NotNull WorkInfo workInfo) {
                mOrganizationsReceived = true;
                if (mIncidentsReceived) {
                    openOverviewFragment();
                }
                mNetworkRepository.getOrgCapabilities();
            }

            @Override
            public void onFailure(@NotNull WorkInfo workInfo) {
                String error = workInfo.getOutputData().getString("error");
                cancelLogin(error);
            }

            @Override
            public void onWorking() {
                mViewModel.setLoading(true);
            }
        });
    }

    /**
     * Cancel the login attempt and tell the user why the login attempt was canceled based upon the
     * provided message.
     *
     * @param message The message to display to the user.
     */
    public void cancelLogin(String message) {
        mMainHandler.post(() -> {
            mViewModel.setLoading(false);
            LiveDataBus.publish(LOGOUT);

            mIncidentsReceived = false;
            mOrganizationsReceived = false;

            mPreferences.clearLoginState();

            if (message != null) {
                Timber.tag(DEBUG).d(message);
                // Show an alert dialog with the error message.
                showSimpleDialog(mActivity, NICS_LOGIN_ERROR, emptyCheck(message) ? NICS_HTTP_RESPONSE_ERROR : message);
            }

            setLoginStatus(R.string.login_progress_signing_in);
        });
    }

    private void openOverviewFragment() {
        if (!isOrganizationSelected(mPreferences.getSelectedOrganization())) {
            showOrganizationDialog();
        } else {
            mPreferences.setTabletLayoutOn(mBinding.tabletLayoutToggle.isChecked());

            // Navigate to the overview fragment.
            navigateSafe(mNavController, LoginFragmentDirections.openOverview());
        }
    }

    private boolean isCompletelyLoggedIn() {
        return mAuthStateManager.getCurrent().isAuthorized()
                && isWorkspaceSelected(mPreferences.getSelectedWorkspace())
                && isOrganizationSelected(mPreferences.getSelectedOrganization());
    }

    private void setLoginStatus(int id) {
        mBinding.loginStatusMessage.setText(id);
    }

    /**
     * Request authorization from the specific auth server. Will open a browser for the user to
     * login using their credentials and will return with the results.
     */
    private void requestAuthorization() {
        Uri issuerUri = Uri.parse(mPreferences.getAuthServerURL());
        Timber.tag(DEBUG).d("Attempting to fetch configuration from issuer uri: %s", issuerUri.toString());

        AuthorizationServiceConfiguration.fetchFromIssuer(issuerUri, (serviceConfiguration, ex) -> {
            if (ex != null || serviceConfiguration == null) {
                String error = "Failed to retrieve configuration for " + issuerUri;
                Timber.tag(DEBUG).e(ex, error);
                cancelLogin(error);
            } else {
                Uri redirectUri = Uri.parse(NICS_OID_REDIRECT_URI);

                AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                        serviceConfiguration,
                        NICS_OID_CLIENT_ID,
                        ResponseTypeValues.CODE,
                        redirectUri
                );

                builder.setScopes(AuthorizationRequest.Scope.OPENID);

                // Force a login (prevents a cached cookie from auto logging in).
                builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT);

                mAuthStateManager.replace(new AuthState(serviceConfiguration));

                Intent authIntent = mAuthService.getAuthorizationRequestIntent(builder.build());
                mLoginRequest.launch(authIntent);

                try {
                    // END_SESSION_ENDPOINT is currently non-standard so may not be present
                    if (serviceConfiguration.discoveryDoc != null && serviceConfiguration.discoveryDoc.docJson.has(END_SESSION_ENDPOINT)) {
                        mAuthRepository.saveLogoutEndpoint(serviceConfiguration.discoveryDoc.docJson.getString(END_SESSION_ENDPOINT));
                    }
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed to save logout endpoint.");
                }
            }
        }, AppConnectionBuilder.INSTANCE);
    }

    /**
     * After requesting authorization, perform a token request and save it into the auth state.
     *
     * @param data The intent data result after requesting authorization.
     */
    public void openIdTokenRequest(Intent data) {
        try {
            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException error = AuthorizationException.fromIntent(data);
            AuthState authState = new AuthState(response, error);

            if (response != null) {
                Timber.tag(DEBUG).d("Handled Authorization Response %s ", authState.jsonSerializeString());
                mAuthStateManager.updateAfterAuthorization(response, null);
                mAuthService.performTokenRequest(response.createTokenExchangeRequest(), (tokenResponse, exception) -> {
                    if (exception != null) {
                        Timber.tag(DEBUG).e(exception, "Token Exchange Failed.");
                        cancelLogin(defaultIfBlank(exception.getMessage(), "Token Exchange Failed."));
                    } else {
                        if (tokenResponse != null) {
                            mAuthStateManager.updateAfterTokenResponse(tokenResponse, null);

                            Timber.tag(DEBUG).i("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken);
                            NetworkRepository.setIsAttemptingLogin(false);
                            mAuthRepository.setLoggedIn(true);

                            //get UserInfo
                            JWT jwt;
                            if (tokenResponse.accessToken != null) {
                                jwt = new JWT(tokenResponse.accessToken);

                                Claim email = jwt.getClaim("email");
                                mPreferences.setUserName(email.asString());
                            }

                            authSuccessful();
                        }
                    }
//                mAuthService.dispose();
                });
            } else {
                if (error != null) {
                    cancelLogin(defaultIfBlank(error.getMessage(), "Token Exchange Failed."));
                } else {
                    cancelLogin("Token Exchange Failed.");
                }
            }
        } catch (Exception e) {
            cancelLogin("Login Cancelled.");
        }
    }
}
