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
package edu.mit.ll.nics.android.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.repository.HazardRepository;
import edu.mit.ll.nics.android.utils.NotificationsHandler;
import edu.mit.ll.nics.android.utils.UnitConverter;
import edu.mit.ll.nics.android.utils.constants.Intents;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import timber.log.Timber;

import static com.google.maps.android.SphericalUtil.computeDistanceBetween;
import static edu.mit.ll.nics.android.database.entities.Hazard.getHazardBounds;
import static edu.mit.ll.nics.android.utils.GeoUtils.coordinateToGeometryString;
import static edu.mit.ll.nics.android.utils.GeoUtils.findNearestPoint;
import static edu.mit.ll.nics.android.utils.GeoUtils.getCardinalDirection;
import static edu.mit.ll.nics.android.utils.StringUtils.SPACE;
import static edu.mit.ll.nics.android.utils.UnitConverter.IMPERIAL;
import static edu.mit.ll.nics.android.utils.UnitConverter.METRIC;
import static edu.mit.ll.nics.android.utils.UnitConverter.NAUTICAL;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_LOCAL_MAP_FEATURES_CLEARED;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_LOCATION_CHANGED;
import static edu.mit.ll.nics.android.utils.constants.Intents.HAZARD_BOUNDS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.frequencyValues;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EXTRA_NEW_TRACKING_STATE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EXTRA_STARTED_FROM_NOTIFICATION;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_ID_SERVICE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_NAME;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GEOFENCE_SERVICE_NOTIFICATION_ID;

@AndroidEntryPoint
public class GeofenceService extends AppService {

    private ExecutorService mExecutorService;
    private final IBinder mBinder = new LocalBinder();
    private boolean mReceiversRegistered = false;
    private boolean mIsEnabled = true;
    private LatLng mCurrentUserLocation;
    private Ordering<Hazard> mOrdering;
    private List<Hazard> mHazards = new ArrayList<>();
    private ArrayList<Hazard> mActiveHazards = new ArrayList<>();
    private LiveData<Boolean> mGeofencingEnabledObserver;
    private LiveData<List<Hazard>> mHazardsObserver;
    private LiveData<Collabroom> mActiveCollabroom;
    private LiveData<String> mMDTRateObserver;

    @Inject
    HazardRepository mRepository;

    @Inject
    NotificationsHandler mNotificationsHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutorService = Executors.newSingleThreadExecutor();

        mOrdering = Ordering.natural().nullsFirst().onResultOf(
                (Function<Hazard, Comparable<Double>>) hazard -> hazard != null ? hazard.getDistanceFromUser() : null);

        if (!mReceiversRegistered) {
            LiveDataBus.subscribe(NICS_LOCATION_CHANGED, this, data -> onLocationChanged((Location) data));
            LiveDataBus.subscribe(NICS_LOCAL_MAP_FEATURES_CLEARED, this, data -> refresh());
            mReceiversRegistered = true;
        }

        mIsEnabled = mSettings.isGeofencingEnabled();
    }

    /**
     * When onDestroy is called, unregister all receivers and shutdown the executor service.
     */
    @Override
    public void onDestroy() {
        if (mReceiversRegistered) {
            LiveDataBus.unregister(NICS_LOCATION_CHANGED);
            LiveDataBus.unregister(NICS_LOCAL_MAP_FEATURES_CLEARED);
            mReceiversRegistered = false;
        }

        shutdownExecutorService();
        mNotificationsHandler.cancelNotification(GEOFENCE_SERVICE_NOTIFICATION_ID);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

        if (startedFromNotification) {
            boolean next = intent.getBooleanExtra(EXTRA_NEW_TRACKING_STATE, true);
            mSettings.setGeofencingEnabled(next);

            if (next) {
                startGeofenceService();
            } else {
                stopGeofenceService();
            }
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(@NotNull Intent intent) {
        super.onBind(intent);
        Timber.tag(DEBUG).i("bind");

        if (mSettings.isGeofencingEnabled()) {
            startGeofenceService();
        }

        subscribeToLiveData();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mHazardsObserver != null) {
            mHazardsObserver.removeObservers(this);
        }

        if (mActiveCollabroom != null) {
            mActiveCollabroom.removeObservers(this);
        }

        if (mGeofencingEnabledObserver != null) {
            mGeofencingEnabledObserver.removeObservers(this);
        }

        if (mMDTRateObserver != null) {
            mMDTRateObserver.removeObservers(this);
        }

        return super.onUnbind(intent);
    }

    private void subscribeToLiveData() {
        mMDTRateObserver = mSettings.getMDTDataRateLiveData();
        mMDTRateObserver.observe(this, rate -> refreshNotification());

        mActiveCollabroom = mPreferences.getSelectedCollabroomLiveData();
        mHazardsObserver = Transformations.switchMap(mActiveCollabroom, collabroom -> {
            long collabroomId = -1L;
            if (collabroom != null) {
                collabroomId = collabroom.getCollabRoomId();
            }
            return mRepository.getHazardsLiveData(collabroomId);
        });

        mHazardsObserver.observe(this, hazards -> {
            mHazards = hazards;
            refreshNotification();
        });

        mGeofencingEnabledObserver = mSettings.isGeofencingEnabledLiveData();
        mGeofencingEnabledObserver.observe(this, enabled -> {
            if (enabled != mIsEnabled) {
                mIsEnabled = enabled;
                if (mIsEnabled) {
                    startGeofenceService();
                } else {
                    stopGeofenceService();
                }
            }
        });
    }

    private void refreshNotification() {
        try {
            mExecutorService.submit(() -> mNotificationsHandler.notification(GEOFENCE_SERVICE_NOTIFICATION_ID, getNotification(getNotificationText())));
        } catch (Exception e) {
            Timber.tag(DEBUG).d(e, "Failed to refresh hazard notifications.");
        }
    }

    /**
     * When the location service calls onLocationChanged, update the geofences to reflect the user's current location.
     */
    private void onLocationChanged(Location location) {
        try {
            if (location != null && mSettings.isGeofencingEnabled() && mHazards.size() > 0) {
                mCurrentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                refreshNotification();
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to update GeofenceManager with new user location.");
        }
    }

    /**
     * Starts the Geofencing Service in the foreground.
     */
    private void startGeofenceService() {
        mExecutorService = Executors.newSingleThreadExecutor();
        startForeground(GEOFENCE_SERVICE_NOTIFICATION_ID, getNotification(getNotificationText()));
    }

    /**
     * Shutdown the GeofenceService by shutting down the Executor Service and then the GeofenceService itself.
     */
    private void stopGeofenceService() {
        shutdownExecutorService();
        stopSelf();
        stopForeground(true);
    }

    /**
     * Shutdown the Executor Service gracefully.
     */
    private void shutdownExecutorService() {
        if (mExecutorService != null) {
            try {
                mExecutorService.shutdown();
                mExecutorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Timber.tag(DEBUG).e(e, "Failed to shutdown geofence executor service.");
            } finally {
                if (!mExecutorService.isTerminated()) {
                    mExecutorService.shutdownNow();
                }
            }
        }
    }

    private Notification getNotification(SpannableStringBuilder text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_NAME);

        Intent intent = new Intent(this, GeofenceService.class);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_ID_SERVICE);
        }

        int actionIcon;
        String status, action;
        if (mSettings.isGeofencingEnabled()) {
            status = getString(R.string.enabled);
            action = getString(R.string.disable);
            intent.putExtra(EXTRA_NEW_TRACKING_STATE, false);
            actionIcon = R.drawable.ic_location_off;
        } else {
            status = getString(R.string.disabled);
            action = getString(R.string.enable);
            intent.putExtra(EXTRA_NEW_TRACKING_STATE, true);
            actionIcon = R.drawable.ic_location_on;
        }

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent clickIntent = new Intent(Intents.NICS_VIEW_HAZARDS);
        clickIntent.setClassName("edu.mit.ll.nics.android", "edu.mit.ll.nics.android.ui.MainActivity");
        clickIntent.putExtra(HAZARD_BOUNDS, getHazardBounds(mActiveHazards));

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return builder
                .addAction(actionIcon, action, servicePendingIntent)
                .setContentTitle(getString(R.string.hazard_detection_title).concat(SPACE).concat(status))
                .setSubText("Refresh Rate: " + frequencyValues.get(mSettings.getMDTDataRate()))
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_person_pin)
                .setColor(ContextCompat.getColor(mContext, R.color.notification))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setLights(Color.RED, 1000, 1000)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private SpannableStringBuilder getNotificationText() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String systemOfMeasurement = mSettings.getSelectedSystemOfMeasurement();

        if (mCurrentUserLocation != null) {
            // Get the nearest hazards to the user's current location.
            for (Hazard hazard : mHazards) {
                LatLng nearestPoint = findNearestPoint(mCurrentUserLocation, hazard.getCoordinates());
                hazard.setDirectionToHazard(getCardinalDirection(mCurrentUserLocation, nearestPoint));
                hazard.setDistanceFromUser(computeDistanceBetween(mCurrentUserLocation, nearestPoint));
            }

            handleHazardNotifications();

            sortHazards(mHazards);
            List<Hazard> closestHazards = FluentIterable.from(mHazards).limit(10).toList();
            closestHazards = (List<Hazard>) CollectionUtils.subtract(closestHazards, mActiveHazards);

            if (mActiveHazards.size() > 0) {
                String active = "You are currently inside of " + mActiveHazards.size();
                active = active.concat(mActiveHazards.size() == 1 ? " hazard." : " hazards.");
                SpannableString s = new SpannableString(active);
                s.setSpan(new ForegroundColorSpan(Color.RED), 0, active.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(s);
                sb.append("\n");
            }

            for (Hazard hazard : closestHazards) {
                sb.append(hazard.getHazardType());
                DecimalFormat formatter = new DecimalFormat("#.##");
                sb.append(" is ");

                double distance = hazard.getDistanceFromUser();
                String metric = " meters ";
                if (systemOfMeasurement.equals(METRIC) && distance > 1000) {
                    distance = UnitConverter.metersToKilometers(distance);
                    metric = " kilometers ";
                } else if (systemOfMeasurement.equals(IMPERIAL)) {
                    distance = UnitConverter.kilometersToMiles(distance);
                    metric = " miles ";
                } else if (systemOfMeasurement.equals(NAUTICAL)) {
                    distance = UnitConverter.kilometersToNauticalMiles(distance);
                    metric = " nautical miles ";
                }

                sb.append(formatter.format(distance));
                sb.append(metric);
                sb.append(hazard.getDirectionToHazard());
                sb.append(".\n");
            }
        }

        if (sb.length() > 0) {
            sb.insert(0, "There are hazards nearby: \n");
        } else {
            sb.insert(0, "There are no hazards nearby. \n");
        }

        return sb;
    }

    private void handleHazardNotifications() {
        String userLocation = coordinateToGeometryString(mCurrentUserLocation);

        // Get a list of the hazard zones that the user is inside of.
        ArrayList<Hazard> intersectingHazards = mRepository.getIntersectingHazards(mPreferences.getSelectedCollabroomId(), userLocation);

        // Get the difference between the current active ones.
        ArrayList<Hazard> difference = new ArrayList<>(CollectionUtils.subtract(intersectingHazards, mActiveHazards));
        if (difference.size() > 0) {
            ArrayList<String> details = new ArrayList<>();
            for (Hazard hazard : intersectingHazards) {
                StringBuilder stringBuilder = new StringBuilder();

                String label = hazard.getHazardLabel();
                if (label != null && !label.isEmpty()) {
                    stringBuilder.append(label);
                } else {
                    stringBuilder.append("Hazard");
                }
                stringBuilder.append("\n");

                String type = hazard.getHazardType();
                if (type != null && !type.isEmpty()) {
                    stringBuilder.append("Type: ");
                    stringBuilder.append(type);
                    stringBuilder.append("\n");
                }

                stringBuilder.append("Radius: ");
                stringBuilder.append(hazard.getRadius());
                stringBuilder.append(" ");
                stringBuilder.append(UnitConverter.getAbbreviation(hazard.getMetric()));

                details.add(stringBuilder.toString());
            }

            sendHazardNotification(details, intersectingHazards);
        }

        mActiveHazards = intersectingHazards;

        // Turn on the red map border if the user is inside a hazard area.
        Intent intent = new Intent();
        intent.setAction(Intents.NICS_GEOFENCING_CHANGED);
        mContext.sendBroadcast(intent);

        if (mActiveHazards.isEmpty()) {
            mNotificationsHandler.cancelHazardNotifications();
        }
    }

    public void refresh() {
        mServiceManager.forceLocationUpdate();
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendHazardNotification(ArrayList<String> details, ArrayList<Hazard> hazards) {
        mNotificationsHandler.createHazardsNotification(details, hazards, mContext);
    }

    /**
     * Sorts the Hazard list based upon the distance away from the user's current location.
     * Uses the defined Ordering from the constructor.
     */
    private void sortHazards(List<Hazard> hazards) {
        Collections.sort(hazards, mOrdering);
    }

    public ArrayList<Hazard> getActiveHazards() {
        return mActiveHazards;
    }

    /**
     * Class used for the client Binder.  Since this service runs
     * in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public GeofenceService getService() {
            return GeofenceService.this;
        }
    }
}
