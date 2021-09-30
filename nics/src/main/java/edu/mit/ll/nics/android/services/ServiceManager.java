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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;

import javax.inject.Singleton;

import edu.mit.ll.nics.android.interfaces.ServiceBoundListener;

@Singleton
public final class ServiceManager {

    private final Context mContext;

    private PollingService mPollingService;
    private LocationService mLocationService;
    private GeofenceService mGeofenceService;

    private boolean mPollingServiceBound = false;
    private boolean mGeofenceServiceBound = false;
    private boolean mLocationServiceBound = false;

    private ServiceBoundListener mPollingServiceBoundListener;
    private ServiceBoundListener mLocationServiceBoundListener;
    private ServiceBoundListener mGeofenceServiceBoundListener;

    public ServiceManager(Context context) {
        mContext = context;
    }

    public void setPollingServiceBoundListener(ServiceBoundListener boundListener) {
        mPollingServiceBoundListener = boundListener;
    }

    public void setLocationServiceBoundListener(ServiceBoundListener boundListener) {
        mLocationServiceBoundListener = boundListener;
    }

    public void setGeofenceServiceBoundListener(ServiceBoundListener boundListener) {
        mGeofenceServiceBoundListener = boundListener;
    }

    private final ServiceConnection mPollingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PollingService.LocalBinder binder = (PollingService.LocalBinder) service;
            mPollingService = binder.getService();

            mPollingServiceBound = true;

            if (mPollingServiceBoundListener != null) {
                mPollingServiceBoundListener.onBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPollingService = null;

            mPollingServiceBound = false;

            if (mPollingServiceBoundListener != null) {
                mPollingServiceBoundListener.onUnbound();
            }
        }
    };

    public PollingService getPollingService() {
        if (mPollingService == null || !mPollingServiceBound) {
            mContext.bindService(new Intent(mContext, PollingService.class), mPollingServiceConnection, Context.BIND_AUTO_CREATE);
        }

        return mPollingService;
    }

    public void stopPollingService() {
        if (mPollingServiceBound) {
            mContext.unbindService(mPollingServiceConnection);
            mPollingServiceBound = false;
        }
    }

    private final ServiceConnection mGeofenceServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GeofenceService.LocalBinder binder = (GeofenceService.LocalBinder) service;
            mGeofenceService = binder.getService();
            mGeofenceServiceBound = true;

            if (mGeofenceServiceBoundListener != null) {
                mGeofenceServiceBoundListener.onBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mGeofenceService = null;
            mGeofenceServiceBound = false;

            if (mGeofenceServiceBoundListener != null) {
                mGeofenceServiceBoundListener.onUnbound();
            }
        }
    };

    public GeofenceService getGeofenceService() {
        if (mGeofenceService == null || !mGeofenceServiceBound) {
            mContext.bindService(new Intent(mContext, GeofenceService.class), mGeofenceServiceConnection, Context.BIND_AUTO_CREATE);
        }

        return mGeofenceService;
    }

    public void stopGeofenceService() {
        if (mGeofenceServiceBound) {
            mContext.unbindService(mGeofenceServiceConnection);
            mGeofenceServiceBound = false;
        }
    }

    private final ServiceConnection mLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mLocationService = binder.getService();
            mLocationServiceBound = true;

            if (mLocationServiceBoundListener != null) {
                mLocationServiceBoundListener.onUnbound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationService = null;
            mLocationServiceBound = false;

            if (mLocationServiceBoundListener != null) {
                mLocationServiceBoundListener.onUnbound();
            }
        }
    };

    public LocationService getLocationService() {
        if (mLocationService == null || !mLocationServiceBound) {
            mContext.bindService(new Intent(mContext, LocationService.class), mLocationServiceConnection, Context.BIND_AUTO_CREATE);
        }
        return mLocationService;
    }

    public void stopLocationSource() {
        if (mLocationServiceBound) {
            mContext.unbindService(mLocationServiceConnection);
            mLocationServiceBound = false;
        }
    }

    public Location getLastLocation() {
        Location lastLocation = null;

        if (mLocationService != null) {
            lastLocation = mLocationService.getLastLocation();
        }

        return lastLocation;
    }

    public void forceLocationUpdate() {
        if (mLocationService != null) {
            mLocationService.forceUpdate();
        }
    }

    public void stopAllServices() {
        // Stop Location Service.
        stopLocationSource();

        // Stop Geofencing Service.
        stopGeofenceService();

        // Stop Polling Service.
        stopPollingService();
    }

    public void startPolling() {
        if (mPollingService != null) {
            mPollingService.startPolling();
        }
    }

    public void startPollingIncident() {
        if (mPollingService != null) {
            mPollingService.startPollingIncident();
        }
    }

    public void startPollingCollabroom() {
        if (mPollingService != null) {
            mPollingService.startPollingCollabroom();
        }
    }

    public void stopPolling() {
        if (mPollingService != null) {
            mPollingService.stopPolling();
        }
    }

    public void stopPollingIncident() {
        if (mPollingService != null) {
            mPollingService.stopPollingIncident();
        }
    }

    public void stopPollingCollabroom() {
        if (mPollingService != null) {
            mPollingService.stopPollingCollabroom();
        }
    }
}