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
package edu.mit.ll.nics.android.repository;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;

import java.util.Locale;
import java.util.MissingResourceException;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.di.Qualifiers.AppSettings;
import edu.mit.ll.nics.android.utils.livedata.SharedPreferenceBooleanLiveData;
import edu.mit.ll.nics.android.utils.livedata.SharedPreferenceStringLiveData;

import static edu.mit.ll.nics.android.utils.UnitConverter.METRIC;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SUPPORTED_LANGUAGES;
import static edu.mit.ll.nics.android.utils.constants.Settings.COLLABROOM_SYNC_FREQUENCY;
import static edu.mit.ll.nics.android.utils.constants.Settings.COORDINATE_REPRESENTATION;
import static edu.mit.ll.nics.android.utils.constants.Settings.DEBUG_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.DEVICE_DEFAULT;
import static edu.mit.ll.nics.android.utils.constants.Settings.DISABLE_NOTIFICATIONS_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.GEOFENCING_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.INCIDENT_SYNC_FREQUENCY;
import static edu.mit.ll.nics.android.utils.constants.Settings.LANGUAGE_SELECT_LIST;
import static edu.mit.ll.nics.android.utils.constants.Settings.LOW_DATA_SYNC_RATE;
import static edu.mit.ll.nics.android.utils.constants.Settings.LRF_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.MDT_SYNC_FREQUENCY;
import static edu.mit.ll.nics.android.utils.constants.Settings.SYSTEM_OF_MEASUREMENT_SELECT_LIST;
import static edu.mit.ll.nics.android.utils.constants.Settings.TRACKING_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.TRACKING_SYNC_OVER_WIFI_ONLY_CHECKBOX;
import static edu.mit.ll.nics.android.utils.constants.Settings.WFS_SYNC_FREQUENCY;

@Singleton
public class SettingsRepository {

    private boolean lowDataMode = false;
    private final SharedPreferences mSettings;

    @Inject
    public SettingsRepository(@AppSettings SharedPreferences sharedPreferences) {
        mSettings = sharedPreferences;
    }

    public SharedPreferences getSettings() {
        return mSettings;
    }

    public boolean isLRFEnabled() {
        return mSettings.getBoolean(LRF_CHECKBOX, false);
    }

    public void setLRFEnabled(boolean enabled) {
        mSettings.edit().putBoolean(LRF_CHECKBOX, enabled).apply();
    }

    public boolean isPushNotificationsDisabled() {
        return mSettings.getBoolean(DISABLE_NOTIFICATIONS_CHECKBOX, false);
    }

    public void setPushNotificationsDisabled(boolean enabled) {
        mSettings.edit().putBoolean(DISABLE_NOTIFICATIONS_CHECKBOX, enabled).apply();
    }

    public boolean isMDTEnabled() {
        return mSettings.getBoolean(TRACKING_CHECKBOX, true);
    }

    public void setMDTEnabled(boolean enabled) {
        mSettings.edit().putBoolean(TRACKING_CHECKBOX, enabled).apply();
    }

    public LiveData<Boolean> isGeofencingEnabledLiveData() {
        return new SharedPreferenceBooleanLiveData(mSettings, GEOFENCING_CHECKBOX, true);
    }

    public boolean isGeofencingEnabled() {
        return mSettings.getBoolean(GEOFENCING_CHECKBOX, true);
    }

    public void setGeofencingEnabled(boolean enabled) {
        mSettings.edit().putBoolean(GEOFENCING_CHECKBOX, enabled).apply();
    }

    public String getSelectedLanguage() {
        String code = mSettings.getString(LANGUAGE_SELECT_LIST, DEVICE_DEFAULT);
        if (code.equals(DEVICE_DEFAULT)) {
            try {
                code = Locale.getDefault().getISO3Language().substring(0, 2);
            } catch (MissingResourceException e) {
                // not all locales have 3 letter ISO codes
                code = Locale.getDefault().getLanguage().substring(0, 2);
            }
        }

        return code;
    }

    public void setSupportedLanguages(String[] languages) {
        String json = new Gson().toJson(languages);
        mSettings.edit().putString(SUPPORTED_LANGUAGES, json).apply();
    }

    public String[] getSupportedLanguages() {
        String[] languages = null;

        String json = mSettings.getString(SUPPORTED_LANGUAGES, null);

        if (json != null) {
            languages = new Gson().fromJson(json, String[].class);
        }

        return languages;
    }

    public String getSelectedSystemOfMeasurement() {
        return mSettings.getString(SYSTEM_OF_MEASUREMENT_SELECT_LIST, METRIC);
    }

    public boolean isSyncWifiOnlyEnabled() {
        return mSettings.getBoolean(TRACKING_SYNC_OVER_WIFI_ONLY_CHECKBOX, false);
    }

    public void setSyncWifiOnlyEnabled(boolean enabled) {
        mSettings.edit().putBoolean(TRACKING_SYNC_OVER_WIFI_ONLY_CHECKBOX, enabled).apply();
    }

    public int getCoordinateRepresentation() {
        return Integer.parseInt(mSettings.getString(COORDINATE_REPRESENTATION, "0"));
    }

    public LiveData<String> getCoordinateRepresentationLiveData() {
        return new SharedPreferenceStringLiveData(mSettings, COORDINATE_REPRESENTATION, "0");
    }

    public void setCoordinateRepresentation(String representation) {
        mSettings.edit().putString(COORDINATE_REPRESENTATION, representation).apply();
    }

    public LiveData<String> getIncidentDataRateLiveData() {
        return new SharedPreferenceStringLiveData(mSettings, INCIDENT_SYNC_FREQUENCY, "30");
    }

    public int getIncidentDataRate() {
        if (lowDataMode) {
            return LOW_DATA_SYNC_RATE;
        } else {
            return Integer.parseInt(mSettings.getString(INCIDENT_SYNC_FREQUENCY, "30"));
        }
    }

    public LiveData<String> getCollabroomDataRateLiveData() {
        return new SharedPreferenceStringLiveData(mSettings, COLLABROOM_SYNC_FREQUENCY, "30");
    }

    public int getCollabroomDataRate() {
        if (lowDataMode) {
            return LOW_DATA_SYNC_RATE;
        } else {
            return Integer.parseInt(mSettings.getString(COLLABROOM_SYNC_FREQUENCY, "30"));
        }
    }

    public int getMDTDataRate() {
        if (lowDataMode) {
            return LOW_DATA_SYNC_RATE;
        } else {
            return Integer.parseInt(mSettings.getString(MDT_SYNC_FREQUENCY, "30"));
        }
    }

    public LiveData<String> getMDTDataRateLiveData() {
        return new SharedPreferenceStringLiveData(mSettings, MDT_SYNC_FREQUENCY, "30");
    }

    public int getWFSDataRate() {
        if (lowDataMode) {
            return LOW_DATA_SYNC_RATE;
        } else {
            return Integer.parseInt(mSettings.getString(WFS_SYNC_FREQUENCY, "30"));
        }
    }

    public LiveData<String> getWFSDataRateLiveData() {
        return new SharedPreferenceStringLiveData(mSettings, WFS_SYNC_FREQUENCY, "30");
    }

    public boolean isDebugEnabled() {
        return mSettings.getBoolean(DEBUG_CHECKBOX, false);
    }

    public void setDebugEnabled(boolean enabled) {
        mSettings.edit().putBoolean(DEBUG_CHECKBOX, enabled).apply();
    }
}
