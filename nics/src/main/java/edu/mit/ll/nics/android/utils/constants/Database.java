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

import androidx.room.RoomDatabase;
import edu.mit.ll.nics.android.database.AppDatabase;

/**
 * Constants for anything related to the local database including database name, version,
 * table names, etc.
 *
 * @see RoomDatabase
 * @see AppDatabase
 */
public class Database {

    public static final int DATABASE_VERSION = 72;
    public static final String DATABASE_NAME = "nics.db";
    public static final String ALERT_TABLE = "alertTable";
    public static final String COLLABROOM_TABLE = "collabroomTable";
    public static final String ASSIGNMENT_REPORT_TABLE = "assignmentReportTable";
    public static final String CATAN_REQUEST_TABLE = "catanRequestTable";
    public static final String CHAT_TABLE = "chatTable";
    public static final String WORKSPACE_TABLE = "workspaceTable";
    public static final String PERSONAL_HISTORY_TABLE = "personalHistoryTable";
    public static final String COLLABROOM_LAYERS_TABLE = "collabroomLayersTable";
    public static final String EMBEDDED_LAYERS_TABLE = "embeddedLayersTable";
    public static final String DAMAGE_REPORT_TABLE = "damageReportTable";
    public static final String EOD_REPORT_TABLE = "eodReportTable";
    public static final String EOD_REPORT_FTS_TABLE = "eodReportFts";
    public static final String FIELD_REPORT_TABLE = "fieldReportTable";
    public static final String GAR_REPORT_TABLE = "garReportTable";
    public static final String GENERAL_MESSAGE_TABLE = "generalMessageTable";
    public static final String GENERAL_MESSAGE_FTS_TABLE = "generalMessageFts";
    public static final String MAP_MARKUP_TABLE = "mapMarkupTable";
    public static final String MOBILE_DEVICE_TRACKING_TABLE = "mobileDeviceTrackingTable";
    public static final String OVERLAPPING_ROOM_LAYERS_TABLE = "overlappingRoomLayersTable";
    public static final String RESOURCE_REQUEST_TABLE = "resourceRequestTable";
    public static final String UXO_REPORT_TABLE = "uxoReportTable";
    public static final String WEATHER_REPORT_TABLE = "weatherReportTable";
    public static final String HAZARD_TABLE = "hazardTable";
    public static final String LAYER_FEATURE_TABLE = "layerFeatureTable";
    public static final String OVERLAPPING_LAYER_FEATURE_TABLE = "overlappingLayerFeatureTable";
    public static final String TRACKING_LAYER_TABLE = "trackingLayerTable";
    public static final String TRACKING_LAYER_FEATURE_TABLE = "trackingLayerFeatureTable";
    public static final String SYMBOLOGY_TABLE = "symbologyTable";
}
