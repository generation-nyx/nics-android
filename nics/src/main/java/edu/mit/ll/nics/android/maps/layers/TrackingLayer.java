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
package edu.mit.ll.nics.android.maps.layers;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.work.WorkManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.database.entities.TrackingLayerFeature;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.utils.constants.Intents;
import edu.mit.ll.nics.android.workers.ThreadCallback;
import edu.mit.ll.nics.android.workers.ThreadResult;
import edu.mit.ll.nics.android.workers.Workers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.BitmapUtils.generateBitmapFromBytes;
import static edu.mit.ll.nics.android.utils.BitmapUtils.generateRotatedBitmap;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_WAKE_LOCK;
import static edu.mit.ll.nics.android.utils.constants.NICS.TEN_MINUTES;

public class TrackingLayer extends Layer {

    private final AlarmManager mAlarmManager;
    private final Tracking mTracking;
    private long mLastFeatureTimestamp;
    private boolean mReceiverRegistered = false;
    private final LifecycleOwner mLifecycleOwner;

    private PendingIntent mPendingTrackingLayerIntent;
    private final TrackingLayerRepository mRepository;
    private final PreferencesRepository mPreferences;
    private final WorkManager mWorkManager;
    private final SettingsRepository mSettings;
    private final NetworkRepository mNetworkRepository;
    private final Handler mMainHandler;
    private final ExecutorService mExecutor;
    private final DownloaderApiService mDownloader;
    private LiveData<List<TrackingLayerFeature>> mDatabaseObserver;

    public TrackingLayer(Activity activity,
                         GoogleMap map,
                         Tracking tracking,
                         LifecycleOwner lifecycleOwner,
                         PreferencesRepository preferences,
                         WorkManager workManager,
                         SettingsRepository settings,
                         NetworkRepository networkRepository,
                         TrackingLayerRepository trackingLayerRepository,
                         Handler handler,
                         ExecutorService service,
                         DownloaderApiService downloader) {
        super(activity, map, tracking.getDisplayName());

        mTracking = tracking;
        mLifecycleOwner = lifecycleOwner;
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mWorkManager = workManager;
        mSettings = settings;
        mNetworkRepository = networkRepository;
        mRepository = trackingLayerRepository;
        mPreferences = preferences;
        mMainHandler = handler;
        mExecutor = service;
        mDownloader = downloader;

        startPolling();
        subscribeToUpdates();
    }

    private void subscribeToUpdates() {
        mSettings.getWFSDataRateLiveData().observe(mLifecycleOwner, rate -> refreshPolling());

        mDatabaseObserver = mRepository.getTrackingFeaturesByName(mTracking.getLayerName());

        final ThreadCallback<List<MarkupBaseShape>> callback = result -> mMainHandler.post(() -> {
            if (result instanceof ThreadResult.Success) {
                List<MarkupBaseShape> shapes = ((ThreadResult.Success<List<MarkupBaseShape>>) result).data;
                removeFromMap();
                for (MarkupBaseShape shape : shapes) {
                    addToMap(shape);
                }
            }
        });
        mDatabaseObserver.observe(mLifecycleOwner, trackingLayerFeatures -> {
            if (trackingLayerFeatures != null) {
                // Parse the features in the background and then display them using the main thread.
                mExecutor.execute(() -> {
                    CountDownLatch latch = new CountDownLatch(trackingLayerFeatures.size());

                    List<MarkupBaseShape> shapes = new ArrayList<>(trackingLayerFeatures.size());
                    for (TrackingLayerFeature feature : trackingLayerFeatures) {
                        MarkupBaseShape shape = parseFeature(feature, latch);
                        shapes.add(shape);
                    }

                    try {
                        //await all features complete
                        latch.await();
                    } catch (InterruptedException e) {
                        Timber.tag(DEBUG).e("Refresh access token interrupted.");
                    }

                    ThreadResult<List<MarkupBaseShape>> result = new ThreadResult.Success<>(shapes);
                    callback.onComplete(result);
                });
            }
        });
    }

    private void unsubscribeFromUpdates() {
        if (mDatabaseObserver != null) {
            mDatabaseObserver.removeObservers(mLifecycleOwner);
        }
    }

    private void startPolling() {
        Intent intent = new Intent(Intents.NICS_POLLING_TRACKING_LAYER + mTracking.getLayerName());

        mContext.registerReceiver(receiver, new IntentFilter(Intents.NICS_POLLING_TRACKING_LAYER + mTracking.getLayerName()));
        mReceiverRegistered = true;

        mPendingTrackingLayerIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 200, (mSettings.getWFSDataRate() * 1000), mPendingTrackingLayerIntent);
        receiver.onReceive(mContext, intent);
    }

    private void stopPolling() {
        if (mPendingTrackingLayerIntent != null) mAlarmManager.cancel(mPendingTrackingLayerIntent);
    }

    private void refreshPolling() {
        stopPolling();
        startPolling();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NICS_WAKE_LOCK);
                }

                //Acquire the lock
                if (wakeLock != null) {
                    wakeLock.acquire(TEN_MINUTES);
                }

                Timber.tag(DEBUG).i("Attempting to pull tracking layer %s", mTracking.getDisplayName());
                mNetworkRepository.getTrackingLayerWfsData(mTracking.getLayerName());

                //Release the lock
                if (wakeLock != null) {
                    wakeLock.release();
                }
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to receive tracking layer wfs data. ");
            }
        }
    };

    private long getLastFeatureTimestamp() {
        return mLastFeatureTimestamp;
    }

    private void setLastFeatureTimestamp(long timestamp) {
        mLastFeatureTimestamp = timestamp;
    }

    private MarkupBaseShape parseFeature(TrackingLayerFeature feature, CountDownLatch latch) {
        Bitmap symbolBitmap;
        Bitmap tempBitmap = null;
        Resources resources = mContext.getResources();
        String age = feature.getAge();
        float course = feature.getCourse();
        String description = feature.getDescription();

        // Attempt to download the style icon if it exists.
        try {
            String styleIcon = feature.getStyleIcon();
            if (!emptyCheck(styleIcon)) {
                Call<ResponseBody> call = mDownloader.download(mPreferences.getBaseServer() + styleIcon);
                try {
                    Response<ResponseBody> response = call.execute();
                    if (response.body() != null) {
                        try {
                            tempBitmap = generateBitmapFromBytes(response.body().bytes());
                        } catch (IOException e) {
                            Timber.tag(DEBUG).e(e, "Failed to save and parse geojson response.");
                        }
                    }
                } catch (IOException e) {
                    Timber.tag(DEBUG).e(e, "Failed to execute wfs/geojson download call.");
                }
            }
        } catch (NullPointerException e) {
            Timber.tag(DEBUG).e(e, "Failed to general bitmap for tracking layer feature. ");
        }

        if (tempBitmap != null) {
            symbolBitmap = tempBitmap;
        } else {
            symbolBitmap = generateRotatedBitmap(R.drawable.mdt_dot_directional, course, resources);
        }

        Date mdtDate = null;
        // Get the date from one of the date properties if there is any.
        if (feature.getCreated() != null) {
            mdtDate = feature.getCreated();
        } else if (feature.getTimestamp() != null) {
            mdtDate = feature.getTimestamp();
        } else if (feature.getXmltime() != null) {
            mdtDate = feature.getXmltime();
        }

        if (mdtDate != null) {
            long featureTimestamp = mdtDate.getTime();
            if (featureTimestamp > getLastFeatureTimestamp()) {
                setLastFeatureTimestamp(featureTimestamp);
            }
        }

        // TODO update the window adapter to use a pojo instead of json string.
        JsonObject attr = new JsonObject();
        try {
            attr.addProperty("icon", R.drawable.vehicle);
            attr.addProperty(resources.getString(R.string.markup_resource_id), feature.getName());
            if (mdtDate != null) {
                attr.addProperty(resources.getString(R.string.markup_timestamp), mdtDate.getTime());
            }
            attr.addProperty(resources.getString(R.string.markup_course), course + "&#xb0;");

            if (!emptyCheck(description)) {
                attr.addProperty(resources.getString(R.string.markup_description), description);
            }

            if (!emptyCheck(age)) {
                attr.addProperty(resources.getString(R.string.markup_age), age);
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to add properties to tracking layer title.");
        }

        MarkupSymbol symbol = new MarkupSymbol(mMap,
                mPreferences,
                mActivity,
                attr.toString(),
                feature.getCoordinate(),
                symbolBitmap,
                null,
                false,
                new int[] {255, 255, 255, 255});

        symbol.setFeatureId(feature.getFeatureId());

        latch.countDown();

        return symbol;
    }

    @Override
    public Object getLayer() {
        return null;
    }

    public List<MarkupBaseShape> getFeatures() {
        return mFeatures;
    }

    @Override
    public void unregister() {
        mWorkManager.cancelUniqueWork(Workers.GET_TRACKING_LAYER_WFS_WORKER + mTracking.getLayerName());

        mAlarmManager.cancel(mPendingTrackingLayerIntent);
        if (mReceiverRegistered) {
            mContext.unregisterReceiver(receiver);
            mReceiverRegistered = false;
        }

        unsubscribeFromUpdates();
    }

    @Override
    public void removeFromMap() {
        Timber.tag(DEBUG).d("Removing from map. %s", mTracking.getDisplayName());
        for (MarkupBaseShape shape : mFeatures) {
            shape.removeFromMap();
            Timber.tag(DEBUG).d("Removed %s", shape.getTitle());
        }
        mFeatures.clear();
    }

    @Override
    public void addToMap() {
    }

    public void addToMap(MarkupBaseShape symbol) {
        Timber.tag(DEBUG).d("Added to map %s %s", mTracking.getDisplayName(), symbol.getFeatureId());
        mFeatures.add(symbol);
        symbol.addToMap();
    }
}
