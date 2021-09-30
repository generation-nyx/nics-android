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

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.Map;

public class NICS {

    public static final int MAX_RETRIES = 3;
    public static final int MAX_POST_IMAGE_SIZE = 1024;
    public static final int MAX_POST_IMAGE_QUALITY = 80;
    public static final int NICS_CONNECTION_LIMIT = 60000;
    public static final int RC_AUTH = 9998; //OID authorization request code
    public static final long TEN_MINUTES = 600000L;
    public static final String DEFAULT_BASE_URL = "https://localhost:8080/";
    public static final String OPEN_ELEVATION_BASE_URL = "https://api.open-elevation.com/api/v1/";
    public static final String DEBUG = "nicsDebug";
    public static final String LRF_DEVICE_NAME = "TP360B";
    public static final String BT_SERIALPORT_SERVICEID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String USER_KEY = "VAYHxGeIOY4lQ7J55mYoJw==:IUtPpNtl2yRmqGLqbE4QVBo6VID00J+7lc42oWSJdMY=:BxbKRpeWgehf3fK1yfbZcLbsZWw6Ec/7TKnetMOb5Bs=";
    public static final String GOOGLE_LEGAL_NOTICE = "https://www.google.com/mobile/legalnotices/";
    public static final String PRIVACY_POLICY = "https://public.nics.ll.mit.edu/privacy-policy/";
    public static final String NICS_HELP_OLD = "https://public.nics.ll.mit.edu/nicshelp/articles/frontpage.php";
    public static final String NICS_HELP = "https://help.nics.ll.mit.edu/nicshelp2/";
    public static final String DEFAULT_ICON_PATH = "/ics/x.png";
    public static final String DATE_PICKER = "DATE_PICKER";
    public static final String NICS_WAKE_LOCK = "nics:nics_WAKE";
    public static final String NICS_PACKAGE_NAME = "edu.mit.ll.nics.android";
    public static final String NICS_MAIN_ACTIVITY_PACKAGE_NAME = "edu.mit.ll.nics.android.ui.MainActivity";
    public static final String NICS_FILE_PROVIDER = "edu.mit.ll.nics.android.fileprovider";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String INCIDENT_MAP = "Incident Map";
    public static final String WORKING_MAP = "Working Map";
    public static final String NICS_TIME_FORMAT = "MM/dd kk:mm:ss";
    public static final String NO_SELECTION = "no_selection";
    public static final String NICS_NO_RESULTS = "nics_NO_RESULTS";
    public static final String NICS_LOGIN_ERROR = "Login Error";
    public static final String NICS_HTTP_RESPONSE_ERROR = "Http Response Error.";
    public static final String NICS_OID_CLIENT_ID = "nics-mobile";
    public static final String NICS_OID_REDIRECT_URI = "mobile.nics-api:/oauth2callback";
    public static final String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DATE_FORMAT_XML = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String NICS_LAYER_TYPE = "nics.collaborationfeed:R";

    public final static String NEW_GENERAL_MESSAGES = "NEW_GENERAL_MESSAGES";
    public final static String UNREAD_GENERAL_MESSAGES = "UNREAD_GENERAL_MESSAGES";
    public final static String NEW_EOD_REPORTS = "NEW_EOD_REPORTS";
    public final static String UNREAD_EOD_REPORTS = "UNREAD_EOD_REPORTS";
    public final static String NEW_CHATS = "NEW_CHATS";
    public final static String UNREAD_CHATS = "UNREAD_CHATS";

    // Temp folders
    public static final String NICS_TEMP_FOLDER = File.separator + "temp";
    public static final String NICS_ROOM_LAYERS_TEMP_FOLDER = NICS_TEMP_FOLDER + File.separator + "collabroom_layers";
    public static final String NICS_OVERLAPPING_LAYERS_TEMP_FOLDER = NICS_TEMP_FOLDER + File.separator + "overlapping_layers";

    // TODO need to use string resource for strings.
    public static final Map<Integer, String> frequencyValues = ImmutableMap.<Integer, String>builder()
            .put(5, "5 Seconds")
            .put(15, "15 Seconds")
            .put(30, "30 Seconds")
            .put(60, "1 Minute")
            .put(90, "1.5 Minutes")
            .put(120, "2 Minutes")
            .put(150, "2.5 Minutes")
            .put(180, "3 Minutes")
            .put(210, "3.5 Minutes")
            .put(240, "4 Minutes")
            .put(270, "4.5 Minutes")
            .put(300, "5 Minutes")
            .build();
}
