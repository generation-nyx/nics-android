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

import com.loopj.android.http.AsyncHttpClient;

import java.util.concurrent.CountDownLatch;

import edu.mit.ll.nics.android.repository.AuthRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.services.ServiceManager;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public abstract class AuthProvider {

    protected final AsyncHttpClient mClient;
    protected final PreferencesRepository mPreferences;
    protected final AuthRepository mAuthRepository;
    protected final ServiceManager mServiceManager;
    protected CountDownLatch mLatch;
    public boolean mIsAuthenticating = false;

    protected AuthProvider(PreferencesRepository preferences,
                           AuthRepository authRepository,
                           ServiceManager serviceManager) {
        mClient = new AsyncHttpClient(); //true,80,443);
        mClient.setTimeout(60 * 1000);
        mPreferences = preferences;
        mAuthRepository = authRepository;
        mServiceManager = serviceManager;
    }

    public AsyncHttpClient getClient() {
        return mClient;
    }

    public abstract String getType();

    public abstract void clearAuthData();

    public abstract void setupAuth(String username, String password);

    protected String getAbsoluteUrl(String relativeUrl) {
        if (relativeUrl.contains("http://") || relativeUrl.contains("https://")) {
            return relativeUrl;
        }
        String serverUrl = mPreferences.getAPIServer() + relativeUrl.replace(" ", "%20");
        Timber.tag(DEBUG).w("URL: %s", serverUrl);
        return serverUrl;
    }

    void stopPendingRequests() {
        mClient.cancelAllRequests(true);
    }

    public CountDownLatch getLatch() {
        return mLatch;
    }

    public abstract String getOidAccessToken();
}