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

import edu.mit.ll.nics.android.di.PreferencesModule;
import edu.mit.ll.nics.android.ui.fragments.SettingsFragment;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

/**
 * Constants for user preferences that are dynamically changed during the apps runtime and are
 * stored for persistence in the "nics.pref" file. These are all of the user preferences besides
 * the ones that are changed via the {@link SettingsFragment}. Those are stored in a separate
 * preferences file.
 *
 * @see Settings
 * @see PreferencesModule
 * @see PreferencesRepository
 */
public class Preferences {

    public static final String PREFS_FILE = "nics.pref";
    public static final String BASE_SERVER = "base_server";
    public static final String WEB_SERVER = "web_server";
    public static final String API_SERVER = "api_server";
    public static final String GEO_SERVER = "geo_server";
    public static final String IMAGE_UPLOAD_PATH = "/upload/image/";
    public static final String IMAGE_UPLOAD_URL = "image_upload_url";
    public static final String AUTH_SERVER = "auth_server";
    public static final String CONFIG_HOST = "config_host";
    public static final String SELECTED_COLLABROOM = "selected_collabroom";
    public static final String SELECTED_INCIDENT = "selected_incident";
    public static final String SHOW_HAZARDS = "show_hazards";
    public static final String SHOW_DISTANCE_CALCULATOR = "show_distance_calculator";
    public static final String TABLET_LAYOUT = "tablet_layout";
    public static final String IS_ONLINE = "is_online";
    public static final String USER_DATA = "user_data";
    public static final String ALL_USER_DATA = "all_user_data";
    public static final String USER_ID = "user_id";
    public static final String USER_ORG_ID = "user_org_id";
    public static final String CURRENT_USER_ORG = "current_user_org";
    public static final String USER_SESSION_ID = "user_session_id";
    public static final String USER_NAME = "user_name";
    public static final String PASSWORD = "password";
    public static final String SUPPORTED_LANGUAGES = "supported_languages";
    public static final String SAVED_INCIDENTS = "saved_incidents";
    public static final String SAVED_COLLABROOMS = "saved_collabrooms";
    public static final String SAVED_ORGANIZATIONS = "saved_organizations";
    public static final String SAVED_ORGCAPABILITES = "saved_orgcapabilites";
    public static final String SAVED_WORKSPACES = "saved_workspaces";
    public static final String SELECTED_WORKSPACE = "selected_workspace";
    public static final String SAVED_TRACKING_LAYERS = "saved_tracking_layers";
    public static final String SAVED_OVERLAPPING_ROOM_LAYERS = "saved_overlapping_room_layers";
    public static final String CUSTOM_SERVER_SET = "custom_servers";
    public static final String LAST_LATITUDE = "last_latitude";
    public static final String LAST_LONGITUDE = "last_longitude";
    public static final String LAST_ALTITUDE = "last_altitude";
    public static final String LAST_ACCURACY = "last_accuracy";
    public static final String LAST_COURSE = "last_course";
    public static final String LAST_MDT_TIME = "last_mdt_time";
    public static final String LAST_HR = "last_hr";
    public static final String LAST_HSI = "last_hsi";
    public static final String LAST_SUCCESSFUL_SERVER_PING = "last_successful_server_ping";
    public static final String END_SESSION_ENDPOINT = "end_session_endpoint";
    public static final String LOGOUT_ENDPOINT = "logoutEndpoint";
    public static final String SELECTED_MAP_STYLE = "selected_map_style";
    public static final String MAP_TYPE = "MAP_TYPE";
    public static final String MAP_TRAFFIC_ENABLED = "MAP_TRAFFIC_ENABLED";
    public static final String MAP_INDOOR_ENABLED = "MAP_INDOOR_ENABLED";
    public static final String MAP_BUILDINGS_ENABLED = "MAP_BUILDINGS_ENABLED";
    public static final String MAP_FULLSCREEN_ENABLED = "MAP_FULLSCREEN_ENABLED";
}
