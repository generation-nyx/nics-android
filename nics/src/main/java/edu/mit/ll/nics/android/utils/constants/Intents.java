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


public class Intents {

    public static String HAZARD_BOUNDS = "hazardBounds";

    public static final int CAMERA_REQUEST = 100;
    public static final int END_SESSION_REQUEST_CODE = 1002;

    public static final String PICK_SYMBOL_REQUEST = "pickSymbol";
    public static final String PICK_COLOR_REQUEST = "pickColor";
    public static final String PICK_WORKSPACE_REQUEST = "pickWorkspace";
    public static final String PICK_ORGANIZATION_REQUEST = "pickOrganization";
    public static final String PICK_INCIDENT_REQUEST = "pickIncident";
    public static final String PICK_COLLABROOM_REQUEST = "pickCollabroom";
    public static final String PICK_LOCATION_REQUEST = "pickLocation";

    public static final String NICS_BT_CONNECT = "NICS_BT_CONNECT";
    public static final String NICS_BT_DISCONNECT = "NICS_BT_DISCONNECT";

    public static final String NICS_GEOFENCING_CHANGED = "NICS_GEOFENCING_CHANGED";

    public static final String NICS_VIEW_OVERVIEW = "NICS_VIEW_OVERVIEW";
    public static final String NICS_VIEW_MAP = "NICS_VIEW_MAP";
    public static final String NICS_VIEW_HAZARDS = "NICS_VIEW_HAZARDS";
    public static final String NICS_VIEW_GENERAL_MESSAGES_LIST = "NICS_VIEW_GENERAL_MESSAGES_LIST";
    public static final String NICS_VIEW_EOD_REPORTS_LIST = "NICS_VIEW_EOD_REPORTS_LIST";
    public static final String NICS_VIEW_CHAT_LIST = "NICS_VIEW_CHAT_LIST";
    public static final String NICS_VIEW_FIELD_REPORTS_LIST = "NICS_VIEW_FIELD_REPORTS_LIST";
    public static final String NICS_VIEW_DAMAGE_REPORTS_LIST = "NICS_VIEW_DAMAGE_REPORTS_LIST";
    public static final String NICS_VIEW_RESOURCE_REQUESTS_LIST = "NICS_VIEW_RESOURCE_REQUESTS_LIST";
    public static final String NICS_VIEW_WEATHER_REPORTS_LIST = "NICS_VIEW_WEATHER_REPORTS_LIST";
    public static final String NICS_VIEW_UXO_REPORTS_LIST = "NICS_VIEW_UXO_REPORTS_LIST";
    public static final String NICS_VIEW_CATAN_REQUESTS_LIST = "NICS_VIEW_CATAN_REQUESTS_LIST";
    public static final String NICS_VIEW_GAR_REPORTS_LIST = "NICS_VIEW_GAR_REPORTS_LIST";
    public static final String NICS_VIEW_ASSIGN_REPORTS_LIST = "NICS_VIEW_ASSIGN_REPORTS_LIST";

    public static final String NICS_POLLING_TASK_GENERAL_MESSAGE = "NICS_POLLING_TASK_GENERAL_MESSAGE";
    public static final String NICS_POLLING_TASK_EOD_REPORT = "NICS_POLLING_TASK_EOD_REPORT";
    public static final String NICS_POLLING_TASK_CHAT_MESSAGES = "NICS_POLLING_TASK_CHAT_MESSAGES";
    public static final String NICS_POLLING_TASK_CHAT_PRESENCE = "NICS_POLLING_TASK_CHAT_PRESENCE";
    public static final String NICS_POLLING_MARKUP_REQUEST = "NICS_POLLING_MARKUP_REQUEST";
    public static final String NICS_POLLING_COLLABROOM_LAYERS = "NICS_POLLING_COLLABROOM_LAYERS";
    public static final String NICS_POLLING_TRACKING_LAYER = "NICS_POLLING_TRACKING_LAYER";
    public static final String NICS_POLLING_COLLABROOMS = "NICS_POLLING_COLLABROOMS";
    public static final String NICS_POLLING_INCIDENTS = "NICS_POLLING_INCIDENTS";
    public static final String NICS_POLLING_ORGANIZATIONS = "NICS_POLLING_INCIDENTS";
    public static final String NICS_POLLING_ALERTS = "NICS_POLLING_ALERTS";
    public static final String NICS_POLLING_TRACKING_LAYERS = "NICS_POLLING_TRACKING_LAYERS";

    public static final String NICS_SUCCESSFUL_GET_USER_ORGANIZATION_INFO = "NICS_SUCCESSFUL_GET_USER_ORGANIZATION_INFO";
    public static final String NICS_FAILED_GET_USER_ORGANIZATION_INFO = "NICS_FAILED_GET_USER_ORGANIZATION_INFO";
}
