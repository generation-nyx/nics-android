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
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.repository.MDTRepository;
import edu.mit.ll.nics.android.utils.NotificationsHandler;
import edu.mit.ll.nics.android.utils.constants.Events;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.CheckPermissions.hasLocationPermissions;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.StringUtils.SPACE;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_SHOW_PERMISSIONS_DIALOG;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EXTRA_NEW_TRACKING_STATE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EXTRA_STARTED_FROM_NOTIFICATION;
import static edu.mit.ll.nics.android.utils.constants.Notifications.LOCATION_NOTIFICATION_CHANNEL_ID_SERVICE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.LOCATION_NOTIFICATION_ID;
import static edu.mit.ll.nics.android.utils.constants.Settings.GEOFENCING_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.TRACKING_CHECKBOX;

@AndroidEntryPoint
public class LocationService extends AppService implements LocationSource, SharedPreferences.OnSharedPreferenceChangeListener {

    private FusedLocationProviderClient mLocationClient;
    private final IBinder mBinder = new LocalBinder();

    private OnLocationChangedListener mOnLocationChangedListener;
    private Location mLastLocation;

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            LocationService.this.onLocationChanged(locationResult.getLastLocation());
        }
    };

    private LiveData<String> mMDTRateObserver;

    @Inject
    MDTRepository mMDTRepository;

    @Inject
    NotificationsHandler mNotificationsHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // TODO should unregister when service is stopped. .. use livedata instead.
        mSettings.getSettings().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

        // We got here because the user decided to remove onLocationChanged updates from the notification.
        if (startedFromNotification) {
            boolean next = intent.getBooleanExtra(EXTRA_NEW_TRACKING_STATE, true);
            mSettings.setMDTEnabled(next);
            stopSelf();
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(@NotNull Intent intent) {
        super.onBind(intent);
        Timber.tag(DEBUG).i("bind");

        if (mSettings.isMDTEnabled() || mSettings.isGeofencingEnabled()) {
            startLocationUpdates(mSettings.getMDTDataRate());
        }

        startForeground(LOCATION_NOTIFICATION_ID, getNotification());

        subscribeToLiveData();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        deactivate();
        mNotificationsHandler.cancelNotification(LOCATION_NOTIFICATION_ID);
        super.onDestroy();
    }

    private void subscribeToLiveData() {
        mMDTRateObserver = mSettings.getMDTDataRateLiveData();
        mMDTRateObserver.observe(this, rate -> startLocationUpdates(Integer.parseInt(rate)));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.tag(DEBUG).i("unbind");

        stopLocationUpdates();

        if (mMDTRateObserver != null) {
            mMDTRateObserver.removeObservers(this);
        }

        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service.
        Timber.tag(DEBUG).i("rebind");
        super.onRebind(intent);
    }

    /**
     * Called when activated as LocationSource.
     *
     * @see LocationSource#activate(OnLocationChangedListener)
     */
    @Override
    public void activate(@NotNull OnLocationChangedListener onLocationChangedListener) {
        mOnLocationChangedListener = onLocationChangedListener;

        if (mSettings.isMDTEnabled() || mSettings.isGeofencingEnabled()) {
            startLocationUpdates(mSettings.getMDTDataRate());
        }

        forceUpdate();
    }

    /**
     * Called when deactivated as LocationSource.
     *
     * @see LocationSource#deactivate()
     */
    @Override
    public void deactivate() {
        mOnLocationChangedListener = null;

        if (!mSettings.isMDTEnabled() && !mSettings.isGeofencingEnabled()) {
            stopLocationUpdates();
        }
    }

    public void startLocationUpdates(int rate) {
        stopLocationUpdates();
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(1000L * (long) rate);
        request.setFastestInterval(0L);

        // TODO need to create a dialog telling the user that they denied the permissions or something like that.
        // Call requires location permissions, check if permission are available. If not denied by user than request permission
        if (mSettings.isMDTEnabled() || mSettings.isGeofencingEnabled()) {
            try {
                if (hasLocationPermissions(getApplicationContext())) {
                    mLocationClient.requestLocationUpdates(request, mLocationCallback, Looper.getMainLooper());
                } else {
                    LiveDataBus.publish(NICS_SHOW_PERMISSIONS_DIALOG);
                }
            } catch (SecurityException e) {
                Timber.tag(DEBUG).e(e, "Problem with GPS location permission");
            }
        }
    }

    private void stopLocationUpdates() {
        mLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationService.class);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        int actionIcon;
        String status, action, text, accuracy;
        boolean chronometer;
        if (mSettings.isMDTEnabled()) {
            status = getString(R.string.enabled);
            action = getString(R.string.disable);
            intent.putExtra(EXTRA_NEW_TRACKING_STATE, false);
            actionIcon = R.drawable.ic_location_off;
            text = (mLastLocation != null) ? mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude() : EMPTY;
            accuracy = (mLastLocation != null) ? "Accuracy: " + mLastLocation.getAccuracy() + " meters" : EMPTY;
        } else {
            status = getString(R.string.disabled);
            action = getString(R.string.enable);
            intent.putExtra(EXTRA_NEW_TRACKING_STATE, true);
            actionIcon = R.drawable.ic_location_on;
            text = EMPTY;
            accuracy = EMPTY;
        }

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, LOCATION_NOTIFICATION_CHANNEL_ID_SERVICE);

        return builder
                .addAction(actionIcon, action, servicePendingIntent)
                .setSubText(accuracy)
                .setContentText(text)
                .setContentTitle(getString(R.string.mobile_tracking_title) + SPACE + status)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_person_pin)
                .setColor(ContextCompat.getColor(mContext, R.color.notification))
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher))
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
                .build();
    }

    private void onLocationChanged(Location location) {
        if (location != null) {
            mLastLocation = location;

            long lastTime = mPreferences.getMDTTime();
            long curTime = location.getTime();

            if (mSettings.isMDTEnabled() && curTime >= lastTime + ((mSettings.getMDTDataRate() - 5) * 1000) && mAuthRepository.isLoggedIn()) {
                mMDTRepository.addMDT(mPreferences.setMDT(location));
                mNetworkRepository.postMDTs();
            }

            if (mOnLocationChangedListener != null) {
                mOnLocationChangedListener.onLocationChanged(mLastLocation);
            }

            LiveDataBus.publish(Events.NICS_LOCATION_CHANGED, location);

            mNotificationsHandler.notification(LOCATION_NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Forces the LocationClient to get the last known location.
     */
    public void forceUpdate() {
        try {
            mLocationClient.getLastLocation()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            onLocationChanged(task.getResult());
                        } else {
                            Timber.tag(DEBUG).w("Failed to get location.");
                        }
                    });
        } catch (SecurityException unlikely) {
            Timber.tag(DEBUG).e(unlikely, "Lost location permission.");
        }
    }

    /**
     * Return the last known location retrieved by the LocationClient.
     *
     * @return Location The last known location.
     */
    public Location getLastLocation() {
        return mLastLocation;
    }

    public void setUpdateRate(int rate) {
        startLocationUpdates(rate);
    }

    // TODO notification may be triggered more than we want since it's not checking to see if the value has actually changed and this seems to get called sometimes without even changing that specific key.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TRACKING_CHECKBOX.equals(key)) {
            if (sharedPreferences.getBoolean(TRACKING_CHECKBOX, true)) {
                mLastLocation = null;
                startLocationUpdates(mSettings.getMDTDataRate());
            } else {
                // tracking disabled, disconnect client only if not active LocationSource
                if (mOnLocationChangedListener == null && !mSettings.isGeofencingEnabled()) {
                    stopLocationUpdates();
                    mLastLocation = null;
                }
            }

            mNotificationsHandler.notification(LOCATION_NOTIFICATION_ID, getNotification());
        }

        // If both hazard detection and mobile device tracking were off, and geofencing is turned on, we need to start location updates for the geofencing service.
        else if (key.equals(GEOFENCING_CHECKBOX) && sharedPreferences.getBoolean(GEOFENCING_CHECKBOX, true) && !mSettings.isMDTEnabled()) {
            mLastLocation = null;
            startLocationUpdates(mSettings.getMDTDataRate());
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs
     * in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }
}
