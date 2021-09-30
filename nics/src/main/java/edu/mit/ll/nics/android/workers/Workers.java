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
package edu.mit.ll.nics.android.workers;

public class Workers {

    // Worker IDs/Tags
    public static final String GET_ALERTS_WORKER = "GET_ALERTS_WORKER";
    public static final String GET_GENERAL_MESSAGES_WORKER = "GET_GENERAL_MESSAGES_WORKER";
    public static final String POST_GENERAL_MESSAGES_WORKER = "POST_GENERAL_MESSAGES_WORKER";
    public static final String GET_EOD_REPORTS_WORKER = "GET_EOD_REPORTS_WORKER";
    public static final String POST_EOD_REPORTS_WORKER = "POST_EOD_REPORTS_WORKER";
    public static final String GET_CHAT_MESSAGES_WORKER = "GET_CHAT_HISTORY_WORKER";
    public static final String POST_CHAT_MESSAGES_WORKER = "POST_CHAT_HISTORY_WORKER";
    public static final String POST_CHAT_PRESENCE_WORKER = "POST_CHAT_PRESENCE_WORKER";
    public static final String GET_COLLABROOM_LAYERS_WORKER = "GET_COLLABROOM_LAYERS_WORKER";
    public static final String GET_MARKUP_FEATURES_WORKER = "GET_MARKUP_FEATURES_WORKER";
    public static final String POST_MARKUP_FEATURES_WORKER = "POST_MARKUP_FEATURES_WORKER";
    public static final String UPDATE_MARKUP_FEATURES_WORKER = "UPDATE_MARKUP_FEATURES_WORKER";
    public static final String DELETE_MARKUP_FEATURES_WORKER = "DELETE_MARKUP_FEATURES_WORKER";
    public static final String POST_MOBILE_DEVICE_TRACKS_WORKER = "POST_MOBILE_DEVICE_TRACKS_WORKER";
    public static final String DELETE_MOBILE_DEVICE_TRACKS_WORKER = "DELETE_MOBILE_DEVICE_TRACKS_WORKER";
    public static final String GET_ORG_CAPABILITIES_WORKER = "GET_ORG_CAPABILITIES_WORKER";
    public static final String GET_USER_ORGS_WORKER = "GET_USER_ORGS_WORKER";
    public static final String GET_USER_DATA_WORKER = "GET_USER_DATA_WORKER";
    public static final String GET_USER_WORKSPACES_WORKER = "GET_USER_WORKSPACES_WORKER";
    public static final String GET_ALL_INCIDENTS_WORKER = "GET_ALL_INCIDENTS_WORKER";
    public static final String GET_ALL_USER_DATA_WORKER = "GET_ALL_USER_DATA_WORKER";
    public static final String GET_SERVER_HOST_CONFIG_WORKER = "GET_SERVER_HOST_CONFIG";
    public static final String GET_SYMBOLOGY_WORKER = "GET_SYMBOLOGY_WORKER";
    public static final String LOGIN_WORKER = "LOGIN_WORKER";
    public static final String LOGOUT_WORKER = "LOGOUT_WORKER";
    public static final String GET_TRACKING_LAYERS_WORKER = "GET_TRACKING_LAYERS_WORKER";
    public static final String GET_TRACKING_LAYER_WFS_WORKER = "GET_TRACKING_LAYER_WFS_WORKER";
    public static final String GET_COLLABROOMS_WORKER = "GET_COLLABROOMS_WORKER";
    public static final String GET_OVERLAPPING_COLLABROOMS_WORKER = "GET_OVERLAPPING_COLLABROOMS_WORKER";
    public static final String DOWNLOAD_IMAGE_WORKER = "DOWNLOAD_IMAGE_WORKER";
    public static final String GEOCODE_COORDINATE_WORKER = "GEOCODE_LOCATION_WORKER";
    public static final String GEOCODE_ADDRESS_WORKER = "GEOCODE_ADDRESS_WORKER";
    public static final String OPEN_ELEVATION_WORKER = "OPEN_ELEVATION_WORKER";

    // Chains
    public static final String GET_COLLABROOMS_AND_ROOM_LAYERS_WORKER_CHAIN = "GET_COLLABROOMS_AND_ROOM_LAYERS_WORKER_CHAIN";
}
