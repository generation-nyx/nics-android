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
package edu.mit.ll.nics.android.utils;

import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientAuthentication;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.mit.ll.nics.android.auth.AuthStateManager;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

/**
 * Utility class to perform an methods pertaining to user authentication.
 */
public class AuthUtils {

    /**
     * Utility method to refresh the access token that is saved in a {@link AuthStateManager}
     * instance. This should be run on a separate thread and the {@link CountDownLatch} latch will
     * signal when the token request is completed.
     *
     * @param authStateManager The {@link AuthStateManager} that holds an access token. The new
     *                         token will be saved here.
     * @param authorizationService The {@link AuthorizationService} to use to perform the token
     *                             request.
     */
    public static boolean refreshAccessToken(AuthStateManager authStateManager,
                                             AuthorizationService authorizationService) {
        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            ClientAuthentication clientAuthentication;
            clientAuthentication = authStateManager.getCurrent().getClientAuthentication();
            authorizationService.performTokenRequest(
                    authStateManager.getCurrent().createTokenRefreshRequest(),
                    clientAuthentication,
                    (response, ex) -> {
                        authStateManager.updateAfterTokenResponse(response, ex);
                        if (ex == null) {
                            success.set(true);
                        }
                        latch.countDown();
                    });
            latch.await();
        } catch (InterruptedException e) {
            Timber.tag(DEBUG).e("Refresh access token interrupted.");
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod e) {
            Timber.tag(DEBUG).d(e, "Token request cannot be made, client authentication for the token endpoint could not be constructed (%s)", e.getMessage());
        }

        return success.get();
    }
}
