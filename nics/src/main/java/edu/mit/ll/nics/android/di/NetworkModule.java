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

import com.google.gson.GsonBuilder;

import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import edu.mit.ll.nics.android.api.AlertApiService;
import edu.mit.ll.nics.android.api.ApiService;
import edu.mit.ll.nics.android.api.ChatApiService;
import edu.mit.ll.nics.android.api.CollabroomApiService;
import edu.mit.ll.nics.android.api.CollabroomLayerApiService;
import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.api.EODReportApiService;
import edu.mit.ll.nics.android.api.GeneralMessageApiService;
import edu.mit.ll.nics.android.api.IncidentApiService;
import edu.mit.ll.nics.android.api.LoginApiService;
import edu.mit.ll.nics.android.api.MDTApiService;
import edu.mit.ll.nics.android.api.MapApiService;
import edu.mit.ll.nics.android.api.OpenElevationApiService;
import edu.mit.ll.nics.android.api.OrgCapabilitiesApiService;
import edu.mit.ll.nics.android.api.OverlappingRoomLayerApiService;
import edu.mit.ll.nics.android.api.SymbologyApiService;
import edu.mit.ll.nics.android.api.TrackingLayersApiService;
import edu.mit.ll.nics.android.api.UserApiService;
import edu.mit.ll.nics.android.di.Qualifiers.NetworkExecutor;
import edu.mit.ll.nics.android.di.Qualifiers.AuthApiHttpClient;
import edu.mit.ll.nics.android.di.Qualifiers.AuthHttpClient;
import edu.mit.ll.nics.android.di.Qualifiers.HttpClient;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEFAULT_BASE_URL;
import static edu.mit.ll.nics.android.utils.constants.NICS.OPEN_ELEVATION_BASE_URL;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public static ApiService provideApiService(@HttpClient OkHttpClient client,
                                               @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(ApiService.class);
    }

    @Provides
    @Singleton
    public static AlertApiService provideAlertApiService(@AuthApiHttpClient OkHttpClient client,
                                                         @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(AlertApiService.class);
    }

    @Provides
    @Singleton
    public static ChatApiService provideChatApiService(@AuthApiHttpClient OkHttpClient client,
                                                       @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(ChatApiService.class);
    }

    @Provides
    @Singleton
    public static CollabroomApiService provideCollabroomApiService(@AuthApiHttpClient OkHttpClient client,
                                                                   @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(CollabroomApiService.class);
    }

    @Provides
    @Singleton
    public static CollabroomLayerApiService provideCollabroomLayerApiService(@AuthApiHttpClient OkHttpClient client,
                                                                             @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(CollabroomLayerApiService.class);
    }

    @Provides
    @Singleton
    public static DownloaderApiService provideDownloaderApiService(@AuthHttpClient OkHttpClient client,
                                                                   @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(DownloaderApiService.class);
    }

    @Provides
    @Singleton
    public static EODReportApiService provideEODReportApiService(@AuthApiHttpClient OkHttpClient client,
                                                                 @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(EODReportApiService.class);
    }

    @Provides
    @Singleton
    public static GeneralMessageApiService provideGeneralMessageApiService(@AuthApiHttpClient OkHttpClient client,
                                                                           @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(GeneralMessageApiService.class);
    }

    @Provides
    @Singleton
    public static IncidentApiService provideIncidentApiService(@AuthApiHttpClient OkHttpClient client,
                                                               @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(IncidentApiService.class);
    }

    @Provides
    @Singleton
    public static LoginApiService provideLoginApiService(@AuthApiHttpClient OkHttpClient client,
                                                         @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(LoginApiService.class);
    }

    @Provides
    @Singleton
    public static MapApiService provideMapApiService(@AuthApiHttpClient OkHttpClient client,
                                                     @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(MapApiService.class);
    }

    @Provides
    @Singleton
    public static MDTApiService provideMDTApiService(@AuthApiHttpClient OkHttpClient client,
                                                     @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(MDTApiService.class);
    }

    @Provides
    @Singleton
    public static OrgCapabilitiesApiService provideOrgCapabilitiesApiService(@AuthApiHttpClient OkHttpClient client,
                                                                             @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(OrgCapabilitiesApiService.class);
    }

    @Provides
    @Singleton
    public static SymbologyApiService provideSymbologyApiService(@AuthApiHttpClient OkHttpClient client,
                                                                             @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(SymbologyApiService.class);
    }

    @Provides
    @Singleton
    public static OverlappingRoomLayerApiService provideOverlappingRoomLayerApiService(@AuthApiHttpClient OkHttpClient client,
                                                                                       @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(OverlappingRoomLayerApiService.class);
    }

    @Provides
    @Singleton
    public static TrackingLayersApiService provideTrackingLayersApiService(@AuthHttpClient OkHttpClient client,
                                                                           @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(TrackingLayersApiService.class);
    }

    @Provides
    @Singleton
    public static UserApiService provideUserApiService(@AuthApiHttpClient OkHttpClient client,
                                                       @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(UserApiService.class);
    }

    @Provides
    @Singleton
    public static OpenElevationApiService provideOpenElevationApiService(@HttpClient OkHttpClient client,
                                                                         @NetworkExecutor ExecutorService executor) {
        return new Retrofit.Builder()
                .baseUrl(OPEN_ELEVATION_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()))
                .client(client)
                .callbackExecutor(executor)
                .build()
                .create(OpenElevationApiService.class);
    }
}
