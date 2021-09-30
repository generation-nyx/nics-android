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
package edu.mit.ll.nics.android;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.utils.timber.DebugLogTree;
import edu.mit.ll.nics.android.utils.timber.ReleaseLogTree;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.FileUtils.clearTempFolder;
import static edu.mit.ll.nics.android.utils.Utils.clearWorkers;

/**
 * NICS Android Application class.
 */
@HiltAndroidApp
public class App extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory mWorkerFactory;

    @Inject
    WorkManager mWorkManager;

    @DiskExecutor
    @Inject
    ExecutorService mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugLogTree());

            strictMode();
        } else {
            Timber.plant(new ReleaseLogTree());
        }

        // Clear out the the application temp folder at app startup.
        mExecutor.execute(() -> clearTempFolder(getApplicationContext()));

        // TODO temp
        NetworkRepository.setDeviceId(getApplicationContext());

        clearWorkers(mWorkManager);
    }

    private void strictMode() {
        // TODO turn this on to fix any issues that appear during strict mode.
//        // Example found at https://stackoverflow.com/a/18100465
//        Timber.tag(DEBUG).w("======================================================");
//        Timber.tag(DEBUG).w("======= APPLICATION IN STRICT MODE - DEBUGGING =======");
//        Timber.tag(DEBUG).w("======================================================");
//
//        // Doesn't allow anything on the main thread that's related to resource access.
//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                .detectAll()
//                .penaltyLog()
//                .penaltyFlashScreen()
//                .penaltyDeath()
//                .build());
//
//        // Doesn't allow any leakage of the application's components.
//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectLeakedRegistrationObjects()
//                .detectFileUriExposure()
//                .detectLeakedSqlLiteObjects()
//                .penaltyLog()
//                .penaltyDeath()
//                .build());
    }

    /**
     * @return The {@link Configuration} used to initialize WorkManager
     */
    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        if (BuildConfig.DEBUG) {
            return new Configuration.Builder()
                    .setMinimumLoggingLevel(Log.VERBOSE)
                    .setExecutor(Executors.newFixedThreadPool(10))
                    .setWorkerFactory(mWorkerFactory)
                    .build();
        } else {
            return new Configuration.Builder()
                    .setExecutor(Executors.newFixedThreadPool(10))
                    .setWorkerFactory(mWorkerFactory)
                    .build();
        }
    }
}
