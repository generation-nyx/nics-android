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

import org.jetbrains.annotations.NotNull;

import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.NetworkUtils.isSuccessStatusCode;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.constants.Events.REFRESH_ACCESS_TOKEN;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class AuthCallback<T> extends RetryCallback<T> {

    public AuthCallback(Callback<T> callback)  {
        super(callback);
    }

    public AuthCallback(Callback<T> callback, int maxRetries)  {
        super(callback, maxRetries);
    }

    @Override
    public void onResponse(@NotNull Call<T> call, @NotNull Response<T> response) {
        boolean sessionTimeout = false;

        Headers headers = response.headers();
        String authError = headers.get("X-AuthErrorCode");
        if (!emptyCheck(authError)) {
            sessionTimeout = true;
        }

        if (sessionTimeout) {
            Timber.tag(DEBUG).w("Authentication Token expired.");

            LiveDataBus.publish(REFRESH_ACCESS_TOKEN);
            mCallback.onFailure(call, new Throwable("Authentication Token expired."));
        } else if (!isSuccessStatusCode(response.code())) {
            super.onFailure(call, new Throwable("Failure status code " + response.code()));
        } else {
            mCallback.onResponse(call, response);
        }
    }
}

