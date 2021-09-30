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

public class Notifications {

    public static final String NICS_TITLE = "NICS";

    public static final int ALERTS_REQUEST_CODE = 1;
    public static final int COLLABROOM_LAYERS_REQUEST_CODE = 2;
    public static final int COLLABROOMS_REQUEST_CODE = 3;
    public static final int GENERAL_MESSAGES_REQUEST_CODE = 4;
    public static final int EOD_REPORTS_REQUEST_CODE = 5;
    public static final int CHATS_REQUEST_CODE = 6;
    public static final int CHAT_PRESENCE_REQUEST_CODE = 7;
    public static final int MARKUP_FEATURES_REQUEST_CODE = 8;
    public static final int TRACKING_LAYERS_REQUEST_CODE = 9;
    public static final int INCIDENTS_REQUEST_CODE = 10;
    public static final int ORGANIZATIONS_REQUEST_CODE = 11;

    public static final String GENERAL_MESSAGES_GROUP = "edu.mit.ll.nics.android.generalmessages";
    public static final String EOD_REPORTS_GROUP = "edu.mit.ll.nics.android.eodreports";
    public static final String CHATS_GROUP = "edu.mit.ll.nics.android.chats";

    public static final String EXTRA_STARTED_FROM_NOTIFICATION = "EXTRA_STARTED_FROM_NOTIFICATION";
    public static final String EXTRA_NEW_TRACKING_STATE = "EXTRA_NEW_TRACKING_STATE";

    public static final int GENERAL_MESSAGE_NOTIFICATION_ID = 0;
    public static final int ALERTS_NOTIFICATION_ID = 10;
    public static final int CHAT_NOTIFICATION_ID = 11;
    public static final int EOD_REPORT_NOTIFICATION_ID = 12;

    public static final String NICS_NOTIFICATION_CHANNEL_ID_SERVICE = "12221";
    public static final String NICS_NOTIFICATION_CHANNEL_NAME = "NICS NOTIFICATION CHANNEL";

    public static final String REPORT_BIG_CONTENT_TITLE = "Report Details: ";
    public static final String ALERT_BIG_CONTENT_TITLE = "Alert Message: ";
    public static final String CHAT_BIG_CONTENT_TITLE = "Chat Message: ";
    public static final String HAZARD_BIG_CONTENT_TITLE = "Entered Hazard Zone(s)";

    public static final String GENERAL_MESSAGE_CONTENT_TEXT = "General Message(s) Received";
    public static final String EOD_REPORT_CONTENT_TEXT = "EOD Report(s) Received";
    public static final String CHAT_CONTENT_TEXT = "Chat(s) Received";
    public static final String ALERT_CONTENT_TEXT = "Alert(s) Receieved";

    public static final int LOCATION_NOTIFICATION_ID = 1234567890;
    public static final String LOCATION_NOTIFICATION_CHANNEL_ID_SERVICE = "12222";
    public static final String LOCATION_NOTIFICATION_CHANNEL_NAME = "LOCATION NOTIFICATION CHANNEL";

    public static final int GEOFENCE_SERVICE_NOTIFICATION_ID = 234567891;
    public static final String GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_ID_SERVICE = "12249";
    public static final String GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_NAME = "GEOFENCE NOTIFICATION CHANNEL";

    public static final int HAZARD_NOTIFICATION_ID = 234567892;
    public static final String HAZARD_NOTIFICATION_CHANNEL_ID_SERVICE = "12277";
    public static final String HAZARD_NOTIFICATION_CHANNEL_NAME = "HAZARD NOTIFICATION CHANNEL";
}
