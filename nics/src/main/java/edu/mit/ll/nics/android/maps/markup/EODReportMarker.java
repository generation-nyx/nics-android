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
package edu.mit.ll.nics.android.maps.markup;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.BitmapUtils.generateBitmap;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class EODReportMarker extends ReportMarker<EODReport>  {

    public EODReportMarker(EODReport report,
                           GoogleMap map,
                           PreferencesRepository preferences,
                           Activity activity) {
        super(report, map, preferences, activity);

        // Set the location based upon the value from the report.
        double latitude = getValueOrDefault(report.getLatitude(), 0.0d);
        double longitude = getValueOrDefault(report.getLongitude(), 0.0d);
        setPoint(new LatLng(latitude, longitude));

        // Set the title based upon the information from the general message report.
        JsonObject attr = new JsonObject();
        Resources resources = mActivity.getResources();

        // TODO add image from report and show in window.
        try {
            attr.addProperty("title", resources.getString(R.string.EODREPORT));
            attr.addProperty("reportId", report.getId());
            attr.addProperty("payload", report.toJson());
            attr.addProperty("type", "eod");
            attr.addProperty("icon", R.drawable.eod);
            attr.addProperty(resources.getString(R.string.markup_user), report.getUser());
            attr.addProperty(resources.getString(R.string.markup_timestamp), report.getSeqTime());
            attr.addProperty(resources.getString(R.string.markup_message), report.getDescription());
            attr.addProperty(resources.getString(R.string.markup_eod_team), report.getTeam());
            attr.addProperty(resources.getString(R.string.markup_task_type), report.getTaskType());
            setTitle(attr.toString());
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to add properties to general message layer title.");
        }

        // Set the icon to default report image for now.
        Bitmap bitmap = generateBitmap(R.drawable.report_small_black, mActivity);
        if (bitmap != null) {
            setIcon(bitmap);
        }
    }
}
