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
package edu.mit.ll.nics.android.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationService;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import edu.mit.ll.nics.android.auth.AppConnectionBuilder;
import edu.mit.ll.nics.android.auth.AuthInterceptor;
import edu.mit.ll.nics.android.auth.AuthStateManager;
import edu.mit.ll.nics.android.auth.HostSelectionInterceptor;
import edu.mit.ll.nics.android.di.Qualifiers.AuthApiHttpClient;
import edu.mit.ll.nics.android.di.Qualifiers.AuthHttpClient;
import edu.mit.ll.nics.android.di.Qualifiers.HttpClient;
import edu.mit.ll.nics.android.di.Qualifiers.NetworkExecutor;
import edu.mit.ll.nics.android.di.Qualifiers.SharedPrefs;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import static edu.mit.ll.nics.android.utils.NetworkUtils.getLoggingInterceptor;
import static edu.mit.ll.nics.android.utils.NetworkUtils.isPicassoLogging;

@Module
@InstallIn(SingletonComponent.class)
public class AuthModule {

    @Provides
    @Singleton
    public static AuthStateManager provideAuthStateManager(@SharedPrefs SharedPreferences preferences) {
        return AuthStateManager.getInstance(preferences);
    }

    @Provides
    @Singleton
    public static AppAuthConfiguration provideAppAuthConfiguration() {
        return new AppAuthConfiguration.Builder()
                .setConnectionBuilder(AppConnectionBuilder.INSTANCE)
                .build();
    }

    @Provides
    @Singleton
    public static AuthorizationService provideAuthorizationService(@ApplicationContext Context context,
                                                                   AppAuthConfiguration configuration) {
        return new AuthorizationService(context, configuration);
    }

    @Provides
    @Singleton
    public static HostSelectionInterceptor provideHostSelectorInterceptor() {
        return new HostSelectionInterceptor();
    }

    @Provides
    @Singleton
    public static AuthInterceptor provideAuthInterceptor(AuthStateManager authStateManager,
                                                         AuthorizationService authorizationService,
                                                         PreferencesRepository preferencesRepository) {
        return new AuthInterceptor(authStateManager, authorizationService, preferencesRepository);
    }

    @AuthHttpClient
    @Provides
    @Singleton
    public static OkHttpClient provideAuthOkHttpClient(AuthInterceptor authInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addNetworkInterceptor(getLoggingInterceptor())
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    @AuthApiHttpClient
    @Provides
    @Singleton
    public static OkHttpClient provideAuthApiOkHttpClient(AuthInterceptor authInterceptor,
                                                          HostSelectionInterceptor hostSelectionInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(hostSelectionInterceptor)
                .addInterceptor(authInterceptor)
                .addNetworkInterceptor(getLoggingInterceptor())
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    @HttpClient
    @Provides
    @Singleton
    public static OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .addNetworkInterceptor(getLoggingInterceptor())
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public static Picasso providePicasso(@ApplicationContext Context context,
                                         @NetworkExecutor ExecutorService executor,
                                         @AuthHttpClient OkHttpClient client) {
        return new Picasso.Builder(context)
                .loggingEnabled(isPicassoLogging())
                .downloader(new OkHttp3Downloader(client))
                .executor(executor)
                .build();
    }
}