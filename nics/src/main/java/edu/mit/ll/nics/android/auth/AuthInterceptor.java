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
package edu.mit.ll.nics.android.auth;

import net.openid.appauth.AuthorizationService;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.Events.REFRESH_ACCESS_TOKEN;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@Singleton
public class AuthInterceptor implements Interceptor {

    private final PreferencesRepository mPreferences;
    private final AuthStateManager mAuthStateManager;
    private final AuthorizationService mAuthorizationService;

    @Inject
    public AuthInterceptor(AuthStateManager authStateManager,
                           AuthorizationService authorizationService,
                           PreferencesRepository preferences) {
        mAuthStateManager = authStateManager;
        mAuthorizationService = authorizationService;
        mPreferences = preferences;
    }

    private String getAccessToken(String accessToken) {
        return String.format("Bearer %s", accessToken);
    }

    @NotNull
    @Override public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();

        mAuthStateManager.getCurrent().performActionWithFreshTokens(mAuthorizationService, (accessToken, idToken, error) -> {
            if (error != null) {
                // Negotiation for fresh tokens failed, check error for more details.
                Timber.tag(DEBUG).e("OpenIDAuthProvider.get error code=%s message=%s", error.code, error.getMessage());

                if (error.code == 2002) { // refreshModel token expired
                    Timber.tag(DEBUG).e(error);
                    LiveDataBus.publish(REFRESH_ACCESS_TOKEN);
                }

                return;
            }

            requestBuilder.addHeader("Authorization", getAccessToken(accessToken));
            requestBuilder.addHeader("CUSTOM-uid", mPreferences.getUserName());
        });

        return chain.proceed(requestBuilder.build());
    }
}
