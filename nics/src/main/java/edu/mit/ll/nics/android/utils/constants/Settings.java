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
package edu.mit.ll.nics.android.utils.constants;

import edu.mit.ll.nics.android.di.SettingsModule;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.ui.fragments.SettingsFragment;

/**
 * Constants for user preferences that are dynamically changed during the apps runtime and are
 * stored for persistence in the "settings.pref" file. These are all of the user preferences that
 * are changed via the {@link SettingsFragment}.
 *
 * @see SettingsModule
 * @see SettingsRepository
 */
public class Settings {

    public static final int LOW_DATA_SYNC_RATE = 120;
    public static final String SETTINGS_FILE = "settings.pref";
    public static final String LRF_CHECKBOX = "lrf_checkbox";
    public static final String DISABLE_NOTIFICATIONS_CHECKBOX = "disable_notifications_checkbox";
    public static final String TRACKING_CHECKBOX = "tracking_checkbox";
    public static final String GEOFENCING_CHECKBOX = "geofencing_checkbox";
    public static final String LANGUAGE_SELECT_LIST = "language_select_list";
    public static final String DEVICE_DEFAULT = "Device Default";
    public static final String SYSTEM_OF_MEASUREMENT_SELECT_LIST = "system_of_measurement_select_list";
    public static final String TRACKING_SYNC_OVER_WIFI_ONLY_CHECKBOX = "tracking_sync_over_wifi_only_checkbox";
    public static final String COORDINATE_REPRESENTATION = "coordinate_representation";
    public static final String INCIDENT_SYNC_FREQUENCY = "incident_sync_frequency";
    public static final String COLLABROOM_SYNC_FREQUENCY = "collabroom_sync_frequency";
    public static final String MDT_SYNC_FREQUENCY = "mdt_sync_frequency";
    public static final String WFS_SYNC_FREQUENCY = "wfs_sync_frequency";
    public static final String DEBUG_CHECKBOX = "debug_checkbox";
    public static final String CLEAR_LOCAL_CHAT_DATA = "clear_local_chat_data";
    public static final String CLEAR_LOCAL_MAP_DATA = "clear_local_map_data";
    public static final String CLEAR_LOCAL_REPORTS_DATA = "clear_local_reports_data";
}