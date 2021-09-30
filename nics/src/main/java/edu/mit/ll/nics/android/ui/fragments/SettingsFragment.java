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
package edu.mit.ll.nics.android.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;
import java.util.MissingResourceException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.CollabroomLayerRepository;
import edu.mit.ll.nics.android.repository.ConfigRepository;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.HazardRepository;
import edu.mit.ll.nics.android.repository.MapRepository;
import edu.mit.ll.nics.android.repository.OverlappingRoomLayerRepository;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.utils.EncryptedPreferenceDataStore;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_LOCAL_MAP_FEATURES_CLEARED;
import static edu.mit.ll.nics.android.utils.constants.Settings.CLEAR_LOCAL_CHAT_DATA;
import static edu.mit.ll.nics.android.utils.constants.Settings.CLEAR_LOCAL_MAP_DATA;
import static edu.mit.ll.nics.android.utils.constants.Settings.CLEAR_LOCAL_REPORTS_DATA;
import static edu.mit.ll.nics.android.utils.constants.Settings.COLLABROOM_SYNC_FREQUENCY;
import static edu.mit.ll.nics.android.utils.constants.Settings.DEVICE_DEFAULT;
import static edu.mit.ll.nics.android.utils.constants.Settings.INCIDENT_SYNC_FREQUENCY;
import static edu.mit.ll.nics.android.utils.constants.Settings.LANGUAGE_SELECT_LIST;
import static edu.mit.ll.nics.android.utils.constants.Settings.MDT_SYNC_FREQUENCY;
import static edu.mit.ll.nics.android.utils.constants.Settings.SYSTEM_OF_MEASUREMENT_SELECT_LIST;
import static edu.mit.ll.nics.android.utils.constants.Settings.WFS_SYNC_FREQUENCY;

@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat {

    private NavHostFragment mNavHostFragment;

    @ApplicationContext
    @Inject
    Context mContext;

    @Inject
    SettingsRepository mSettings;

    @Inject
    ConfigRepository mConfigRepository;

    @Inject
    MapRepository mMapRepository;

    @Inject
    HazardRepository mHazardRepository;

    @Inject
    CollabroomLayerRepository mCollabroomLayerRepository;

    @Inject
    TrackingLayerRepository mTrackingLayerRepository;

    @Inject
    GeneralMessageRepository mGeneralMessageRepository;

    @Inject
    EODReportRepository mEODReportRepository;

    @Inject
    OverlappingRoomLayerRepository mOverlappingRoomLayerRepository;

    @Inject
    ChatRepository mChatRepository;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setPreferenceDataStore(new EncryptedPreferenceDataStore(mSettings.getSettings()));

        setPreferencesFromResource(R.xml.preferences, rootKey);

        mNavHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        setupPreferenceScreen();
    }

    private void setupPreferenceScreen() {
        bindPreferenceSummaryToValue(findPreference(INCIDENT_SYNC_FREQUENCY));
        bindPreferenceSummaryToValue(findPreference(COLLABROOM_SYNC_FREQUENCY));
        bindPreferenceSummaryToValue(findPreference(MDT_SYNC_FREQUENCY));
        bindPreferenceSummaryToValue(findPreference(WFS_SYNC_FREQUENCY));
        bindPreferenceSummaryToValue(findPreference(LANGUAGE_SELECT_LIST));
        bindPreferenceSummaryToValue(findPreference(SYSTEM_OF_MEASUREMENT_SELECT_LIST));

        Preference clearLocalMapFeaturesButton = findPreference(CLEAR_LOCAL_MAP_DATA);
        if (clearLocalMapFeaturesButton != null) {
            clearLocalMapFeaturesButton.setOnPreferenceClickListener(preference -> {
                mMapRepository.deleteAllMarkupFeatures();
                mOverlappingRoomLayerRepository.deleteOverlappingLayers();
                mCollabroomLayerRepository.deleteAllCollabroomLayers();
                mHazardRepository.deleteAllHazards();
                mTrackingLayerRepository.deleteAllTrackingLayers();
                LiveDataBus.publish(NICS_LOCAL_MAP_FEATURES_CLEARED);
                return true;
            });
        }

        Preference clearLocalChatFeaturesButton = findPreference(CLEAR_LOCAL_CHAT_DATA);
        if (clearLocalChatFeaturesButton != null) {
            clearLocalChatFeaturesButton.setOnPreferenceClickListener(preference -> {
                mChatRepository.deleteAllChat();
                return true;
            });
        }

        Preference clearLocalReportsFeaturesButton = findPreference(CLEAR_LOCAL_REPORTS_DATA);
        if (clearLocalReportsFeaturesButton != null) {
            clearLocalReportsFeaturesButton.setOnPreferenceClickListener(preference -> {
                mGeneralMessageRepository.deleteAllGeneralMessages();
                mEODReportRepository.deleteAllEODReports();
                return true;
            });
        }
    }

    private final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @SuppressLint("ConstantLocale")
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                if (preference.getKey().equals(LANGUAGE_SELECT_LIST) && index > -1) {
                    String newLanguage = listPreference.getEntryValues()[index].toString();
                    if (newLanguage.equals(DEVICE_DEFAULT)) {
                        try {
                            newLanguage = Locale.getDefault().getISO3Language().substring(0, 2);
                        } catch (MissingResourceException e) {
                            //not all locales have 3 letter ISO codes
                            newLanguage = Locale.getDefault().getLanguage().substring(0, 2);
                        }
                    }

                    String storedLanguage = mSettings.getSelectedLanguage();
                    if (!newLanguage.equals(storedLanguage)) {
                        // TODO feel like we don't really need to logout to change the language. Keeping for now though.
                        mConfigRepository.setCurrentLocale(newLanguage);
                    }
                } else if (preference.getKey().equals(MDT_SYNC_FREQUENCY)) {
                    String selectMDTNote = preference.getContext().getString(R.string.pref_description_mdt_sync_rate);
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] + "\n\n" + selectMDTNote : selectMDTNote);
                } else {
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                }
            } else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };

    private void bindPreferenceSummaryToValue(Preference preference) {
        if (preference != null) {
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, mSettings.getSettings().getString(preference.getKey(), EMPTY));
        }
    }
}
