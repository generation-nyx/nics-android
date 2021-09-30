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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.ApiService;
import edu.mit.ll.nics.android.api.LoginApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.auth.RetryCallback;
import edu.mit.ll.nics.android.data.HostServerConfig;
import edu.mit.ll.nics.android.data.Login;
import edu.mit.ll.nics.android.data.messages.LoginMessage;
import edu.mit.ll.nics.android.repository.AuthRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.services.ServiceManager;
import edu.mit.ll.nics.android.utils.Utils;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.data.HostServerConfig.API;
import static edu.mit.ll.nics.android.data.HostServerConfig.IDENTITY;
import static edu.mit.ll.nics.android.data.HostServerConfig.MAP;
import static edu.mit.ll.nics.android.data.HostServerConfig.WEB;
import static edu.mit.ll.nics.android.utils.constants.Events.LOGOUT;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.Preferences.IMAGE_UPLOAD_PATH;

public class LoginWorkers {

    @HiltWorker
    public static class GetHostConfig extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final ApiService mApiService;

        @AssistedInject
        public GetHostConfig(@Assisted @NonNull Context context,
                             @Assisted @NonNull WorkerParameters workerParams,
                             PreferencesRepository preferences,
                             ApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                // The url where the host server configuration file is located.
                String url = getInputData().getString("url");
                Data.Builder output = new Data.Builder();

                Call<HostServerConfig> call = mApiService.getHostServerConfig(url);
                call.enqueue(new RetryCallback<>(new Callback<HostServerConfig>() {
                    @Override
                    public void onResponse(@NotNull Call<HostServerConfig> call, @NotNull Response<HostServerConfig> response) {
                        try {
                            // Parse the configuration from the downloaded response.
                            HostServerConfig config = response.body();

                            if (config != null) {
                                // Verify that the required host configurations are received.
                                List<String> required = Arrays.asList(API, MAP, IDENTITY);
                                ArrayList<String> missingRelations = Utils.findMissingItems(required, CollectionUtils.collect(config.getLinks(), HostServerConfig.Link::getRel));

                                // If there are any missing configurations, return with an error message.
                                if (!missingRelations.isEmpty()) {
                                    String error = "Host configuration is missing " + StringUtils.join(missingRelations, ", ") + " configuration.";
                                    completer.set(Result.failure(output.putString("error", error).build()));
                                } else {
                                    for (HostServerConfig.Link link : config.getLinks()) {
                                        switch (link.getRel()) {
                                            case API:
                                                mPreferences.setAPIServer(link.getHref());
                                                break;
                                            case WEB:
                                                mPreferences.setWebServerURL(link.getHref());
                                                break;
                                            case MAP:
                                                mPreferences.setGeoServerURL(link.getHref());
                                                break;
                                            case IDENTITY:
                                                mPreferences.setAuthServerURL(link.getHref());
                                                break;
                                        }
                                    }

                                    try {
                                        URL url = new URL(mPreferences.getWebServerURL());
                                        String baseUrl = url.getProtocol() + "://" + url.getHost();
                                        mPreferences.setBaseServer(baseUrl);
                                        String baseServer = mPreferences.getBaseServer();
                                        if (baseServer != null) {
                                            mPreferences.setImageUploadURL(baseServer + IMAGE_UPLOAD_PATH);
                                        }

                                        // Return the successfully retrieved host configuration.
                                        completer.set(Result.success());
                                    } catch (MalformedURLException | NullPointerException e) {
                                        String error = "Error with base url formatting.";
                                        Timber.tag(DEBUG).e(e, error);
                                        completer.set(Result.failure(output.putString("error", error).build()));
                                    }
                                }
                            } else {
                                completer.set(Result.failure(output.putString("error", "Missing host config.").build()));
                            }
                        } catch (Exception e) {
                            // Failed to parse the host config, return an error message.
                            String error = String.format("Error parsing host configuration: %s", e.getMessage());
                            Timber.tag(DEBUG).e(e, "Failed to parse host config.");
                            completer.set(Result.failure(output.putString("error", error).build()));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<HostServerConfig> call, @NotNull Throwable t) {
                        // Failed to parse the host config, return an error message.
                        String error = String.format("Error fetching host configuration: %s", t.getLocalizedMessage());
                        completer.set(Result.failure(output.putString("error", error).build()));
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class NicsLogin extends AppWorker {

        private final AuthRepository mAuthRepository;
        private final PreferencesRepository mPreferences;
        private final NetworkRepository mNetworkRepository;
        private final ServiceManager mServiceManager;
        private final PersonalHistoryRepository mPersonalHistory;
        private final LoginApiService mApiService;

        @AssistedInject
        public NicsLogin(@Assisted @NonNull Context context,
                         @Assisted @NonNull WorkerParameters workerParams,
                         AuthRepository authRepository,
                         PreferencesRepository preferences,
                         NetworkRepository networkRepository,
                         ServiceManager serviceManager,
                         PersonalHistoryRepository personalHistory,
                         LoginApiService apiService) {
            super(context, workerParams);

            mAuthRepository = authRepository;
            mPreferences = preferences;
            mServiceManager = serviceManager;
            mNetworkRepository = networkRepository;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                String userName = mPreferences.getUserName();
                Data.Builder output = new Data.Builder();

                NetworkRepository.setIsAttemptingLogin(true);
                NetworkRepository.loginRetryCount++;

                Login login = new Login(userName);
                login.setWorkspaceId(mPreferences.getSelectedWorkspaceId());

                Call<LoginMessage> call = mApiService.login(mPreferences.getSelectedWorkspaceId(), login);
                call.enqueue(new AuthCallback<>(new Callback<LoginMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<LoginMessage> call, @NotNull Response<LoginMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                        //TODO need to fix up this stuff.
//                            String content = (responseBody != null) ? new String(responseBody) : "error";
//
//                            Log.i("loginPayload", content);
//
//                            //if you don't properly logout then the api doesn't handle the user session properly and sends back "error" next time you try to log in.
//                            //Until the APi sends back the response that the user is already logged in. I am currently catching the error and logging out, then logging back in.
//                            if (content.equals("error")) {
//                                mNetworkRepository.logout(userName, NetworkRepository.loginRetryCount < 10);
//                                return;
//                            }

                        LoginMessage message = response.body();

                        if (message != null && message.getLogins().size() > 0) {
                            mAuthRepository.setLoggedIn(true);
                            Timber.tag(DEBUG).i("Successfully logged in as: %s status code: %s", userName, response.code());

                            Login login = message.getLogins().get(0);
                            mPreferences.setUserId(login.getUserId());
                            mPreferences.setUserName(login.getUserName());
                            mPreferences.setUserSessionId(login.getUserSessionId());
                            mAuthRepository.setLoginData(login);

                            if (mPreferences.getSelectedIncidentId() != -1) {
                                mNetworkRepository.getCollabrooms(mPreferences.getSelectedIncidentId());
                            }

                            mPreferences.switchToOnlineMode();
                            mNetworkRepository.sendAllLocalContent();
                            NetworkRepository.loginRetryCount = 0;
                            NetworkRepository.setIsAttemptingLogin(false);

                            mPersonalHistory.addPersonalHistory("User " + userName + " logged in successfully. ", mPreferences.getUserId(), mPreferences.getUserNickName());
                            completer.set(Result.success());
                        } else {
                            LiveDataBus.publish(LOGOUT);
                            Timber.tag(DEBUG).e("Failed during login success handler.");
                            completer.set(Result.failure(output.putString("error", "Received empty login payload.").build()));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<LoginMessage> call, @NotNull Throwable error) {
                        Timber.tag(DEBUG).e(error);

                        mAuthRepository.setLoggedIn(false);
                        mServiceManager.stopPolling();
                        NetworkRepository.setIsAttemptingLogin(false);

                        String errorMessage = "";

                        String name = error.getClass().getName();

                        if (error instanceof HttpException) {
                            HttpException exception = (HttpException) error;
                            int statusCode = exception.code();

                            if (statusCode == 401) {
                                Timber.tag(DEBUG).w( "%s - %s", statusCode, exception.getMessage());
                                errorMessage = "Invalid username or password";
                            } else if (exception.getMessage() != null) {
                                errorMessage = exception.getMessage();
                            }
                        } else if (error instanceof IOException) {
                            errorMessage = "Failed to connect to server. Please check your network connection.";
                        }


                        if (error.getClass() == HttpException.class) {

                        } else {
                            if (error.getClass() == UnknownHostException.class) {

                            } else {
                                if (error.getMessage() != null) {
                                    errorMessage = error.getMessage();
                                } else {
                                    Timber.tag(DEBUG).e(error, "null error on failed login attempt");
                                }
                            }
                        }

                        completer.set(Result.failure(output.putString("error", errorMessage).build()));
                        mPersonalHistory.addPersonalHistory("User " + userName +
                                " login failed." + errorMessage, mPreferences.getUserId(),
                                mPreferences.getUserNickName());
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class NicsLogout extends AppWorker {

        private final AuthRepository mAuthRepository;
        private final PreferencesRepository mPreferences;
        private final PersonalHistoryRepository mPersonalHistory;
        private final LoginApiService mApiService;

        @AssistedInject
        public NicsLogout(@Assisted @NonNull Context context,
                         @Assisted @NonNull WorkerParameters workerParams,
                         AuthRepository authRepository,
                         PreferencesRepository preferences,
                         PersonalHistoryRepository personalHistory,
                         LoginApiService apiService) {
            super(context, workerParams);

            mAuthRepository = authRepository;
            mPreferences = preferences;
            mPersonalHistory = personalHistory;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                String userName = mPreferences.getUserName();
                long workspaceId = mPreferences.getSelectedWorkspaceId();
                long userSessionId = mPreferences.getUserSessionId();

                Call<ResponseBody> call = mApiService.removeUserSession(workspaceId, userSessionId);
                call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                        mPreferences.clearLoginState();
                        mAuthRepository.clearLogoutEndpoint();
                        mPersonalHistory.addPersonalHistory("User " + userName + " logged in successfully. ", mPreferences.getUserId(), mPreferences.getUserNickName());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable error) {
                        mPreferences.clearLoginState();
                        mAuthRepository.clearLogoutEndpoint();
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }
    }
}
