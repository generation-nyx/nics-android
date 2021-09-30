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
package edu.mit.ll.nics.android.database;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SupportFactory;

import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;

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
import edu.mit.ll.nics.android.database.entities.Alert;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.EODReportFts;
import edu.mit.ll.nics.android.database.entities.EmbeddedCollabroomDatalayer;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.database.entities.GeneralMessageFts;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.MobileDeviceTracking;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;
import edu.mit.ll.nics.android.database.entities.PersonalHistory;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.database.entities.TrackingLayerFeature;
import edu.mit.ll.nics.android.utils.AesCbcWithIntegrity;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.Database.DATABASE_NAME;
import static edu.mit.ll.nics.android.utils.constants.Database.DATABASE_VERSION;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.USER_KEY;
import static edu.mit.ll.nics.android.utils.constants.Preferences.PREFS_FILE;

@Database(entities = {Alert.class, Chat.class, Collabroom.class, CollabroomDataLayer.class, EmbeddedCollabroomDatalayer.class,
        EODReport.class, EODReportFts.class, GeneralMessage.class, GeneralMessageFts.class, Hazard.class, LayerFeature.class, MarkupFeature.class,
        MobileDeviceTracking.class, OverlappingLayerFeature.class, OverlappingRoomLayer.class, PersonalHistory.class, Tracking.class,
        TrackingLayerFeature.class, SymbologyGroup.class},
        version = DATABASE_VERSION,
        exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract AlertDao alertDao();

    public abstract ChatDao chatDao();

    public abstract CollabroomDao collabroomDao();

    public abstract CollabroomLayerDao collabroomLayerDao();

    public abstract EODReportDao eodReportDao();

    public abstract GeneralMessageDao generalMessageDao();

    public abstract HazardDao hazardDao();

    public abstract MapMarkupDao mapMarkupDao();

    public abstract MobileDeviceTrackingDao mobileDeviceTrackingDao();

    public abstract OverlappingRoomLayerDao overlappingRoomLayerDao();

    public abstract PersonalHistoryDao personalHistoryDao();

    public abstract TrackingLayerDao trackingLayerDao();

    public abstract TrackingLayerFeatureDao trackingLayerFeatureDao();

    public abstract SymbologyDao symbologyDao();

    private static volatile AppDatabase sInstance;

    public static AppDatabase getDatabase(Context context, ExecutorService executor) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .openHelperFactory(getFactory(context))
                            .addCallback(getCallback(executor))
                            .build();
                }
            }
        }
        return sInstance;
    }

    private static RoomDatabase.Callback getCallback(ExecutorService executor) {
        return new Callback() {
            /**
             * Called when the database is created for the first time. This is called after all the
             * tables are created.
             *
             * @param db The database.
             */
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                executor.execute(() -> Timber.tag(DEBUG).i("Database version %s created.", DATABASE_VERSION));
            }

            /**
             * Called when the database has been opened.
             *
             * @param db The database.
             */
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                executor.execute(() -> sInstance.eodReportDao().resetSendStatus());
                executor.execute(() -> sInstance.generalMessageDao().resetSendStatus());
                executor.execute(() -> sInstance.mapMarkupDao().resetStatus());
                executor.execute(() -> sInstance.chatDao().resetSendStatus());
            }
        };
    }

    private static SupportSQLiteOpenHelper.Factory getFactory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String passphrase = prefs.getString(USER_KEY, EMPTY);

        if (passphrase != null && passphrase.equals(EMPTY)) {
            try {
                AesCbcWithIntegrity.SecretKeys key;
                key = AesCbcWithIntegrity.generateKey();
                passphrase = AesCbcWithIntegrity.keyString(key);

                prefs.edit().putString(USER_KEY, passphrase).apply();
            } catch (GeneralSecurityException e) {
                Timber.tag(DEBUG).e(e, "GeneralSecurityException");
            }
        }

        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteDatabase database) {
            }

            @Override
            public void postKey(SQLiteDatabase database) {
                database.rawExecSQL("PRAGMA cipher_migrate;");
            }
        };

        return passphrase != null ? new SupportFactory(passphrase.getBytes(), hook) : null;
    }

    private static void attach(SupportSQLiteDatabase db, String databaseName, String databasePath) {
        String sql = "ATTACH DATABASE '" + databasePath + "' AS \"" + databaseName + "\";";
        db.execSQL(sql);
    }
}
