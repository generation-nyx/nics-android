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

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.data.Login;
import edu.mit.ll.nics.android.di.Qualifiers.SharedPrefs;

import static edu.mit.ll.nics.android.utils.constants.Preferences.LOGOUT_ENDPOINT;
import static edu.mit.ll.nics.android.utils.constants.Preferences.USER_ORG_ID;

@Singleton
public class AuthRepository {

    private boolean mIsLoggedIn;
    private final SharedPreferences mPreferences;

    @Inject
    public AuthRepository(@SharedPrefs SharedPreferences sharedPreferences) {
        mPreferences = sharedPreferences;
    }

    public void saveLogoutEndpoint(String logoutEndpoint) {
        mPreferences.edit().putString(LOGOUT_ENDPOINT, logoutEndpoint).apply();
    }

    public String getLogoutEndpoint() {
        return mPreferences.getString(LOGOUT_ENDPOINT, null);
    }

    public void clearLogoutEndpoint() {
        mPreferences.edit().remove(LOGOUT_ENDPOINT).apply();
    }

    public void setLoginData(Login login) {
        mPreferences.edit().putLong(USER_ORG_ID, login.getOrgId()).apply();
    }

    public boolean isLoggedIn() {
        return mIsLoggedIn;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        mIsLoggedIn = isLoggedIn;
    }
}
