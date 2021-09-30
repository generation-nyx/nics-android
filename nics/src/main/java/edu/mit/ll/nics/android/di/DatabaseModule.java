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

import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import edu.mit.ll.nics.android.database.AppDatabase;
import edu.mit.ll.nics.android.database.dao.AlertDao;
import edu.mit.ll.nics.android.database.dao.ChatDao;
import edu.mit.ll.nics.android.database.dao.CollabroomDao;
import edu.mit.ll.nics.android.database.dao.CollabroomLayerDao;
import edu.mit.ll.nics.android.database.dao.EODReportDao;
import edu.mit.ll.nics.android.database.dao.GeneralMessageDao;
import edu.mit.ll.nics.android.database.dao.HazardDao;
import edu.mit.ll.nics.android.database.dao.MapMarkupDao;
import edu.mit.ll.nics.android.database.dao.MobileDeviceTrackingDao;
import edu.mit.ll.nics.android.database.dao.OverlappingRoomLayerDao;
import edu.mit.ll.nics.android.database.dao.PersonalHistoryDao;
import edu.mit.ll.nics.android.database.dao.SymbologyDao;
import edu.mit.ll.nics.android.database.dao.TrackingLayerDao;
import edu.mit.ll.nics.android.database.dao.TrackingLayerFeatureDao;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context,
                                                 @DiskExecutor ExecutorService executor) {
        return AppDatabase.getDatabase(context, executor);
    }

    @Provides
    @Singleton
    public static AlertDao provideAlertDao(AppDatabase database) {
        return database.alertDao();
    }

    @Provides
    @Singleton
    public static ChatDao provideChatDao(AppDatabase database) {
        return database.chatDao();
    }

    @Provides
    @Singleton
    public static CollabroomDao provideCollabroomDao(AppDatabase database) {
        return database.collabroomDao();
    }

    @Provides
    @Singleton
    public static CollabroomLayerDao provideCollabroomLayerDao(AppDatabase database) {
        return database.collabroomLayerDao();
    }

    @Provides
    @Singleton
    public static EODReportDao provideEODReportDao(AppDatabase database) {
        return database.eodReportDao();
    }

    @Provides
    @Singleton
    public static GeneralMessageDao provideGeneralMessageDao(AppDatabase database) {
        return database.generalMessageDao();
    }

    @Provides
    @Singleton
    public static HazardDao provideHazardDao(AppDatabase database) {
        return database.hazardDao();
    }

    @Provides
    @Singleton
    public static MapMarkupDao provideMapMarkupDao(AppDatabase database) {
        return database.mapMarkupDao();
    }

    @Provides
    @Singleton
    public static MobileDeviceTrackingDao provideMDTDao(AppDatabase database) {
        return database.mobileDeviceTrackingDao();
    }

    @Provides
    @Singleton
    public static OverlappingRoomLayerDao provideOverlappingRoomLayerDao(AppDatabase database) {
        return database.overlappingRoomLayerDao();
    }

    @Provides
    @Singleton
    public static PersonalHistoryDao providePersonalHistoryDao(AppDatabase database) {
        return database.personalHistoryDao();
    }

    @Provides
    @Singleton
    public static TrackingLayerDao provideTrackingLayerDao(AppDatabase database) {
        return database.trackingLayerDao();
    }

    @Provides
    @Singleton
    public static TrackingLayerFeatureDao provideTrackingLayerFeatureDao(AppDatabase database) {
        return database.trackingLayerFeatureDao();
    }

    @Provides
    @Singleton
    public static SymbologyDao provideSymbologyDao(AppDatabase database) {
        return database.symbologyDao();
    }
}
