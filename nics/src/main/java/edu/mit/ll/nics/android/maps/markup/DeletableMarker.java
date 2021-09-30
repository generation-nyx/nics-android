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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import edu.mit.ll.nics.android.R;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class DeletableMarker implements Deletable {

    private final Marker mMarker;
    private final Resources mResources;

    public DeletableMarker(Marker marker, Resources resources) {
        mMarker = marker;
        mResources = resources;
    }

    public Marker getMarker() {
        return mMarker;
    }

    public String getDescription() {
        return String.format("Pin at %s", mMarker.getPosition());
    }

    // REALLY would prefer to get from Marker, but the API gods didn't permit it
    @Override
    public Bitmap getIcon() {
        int bitmapId = R.drawable.ic_location_on_black_24dp;
        try {
            JSONObject tmp = new JSONObject(mMarker.getTitle());
            bitmapId = tmp.getInt("icon");
        } catch (JSONException e) {
            Timber.tag(DEBUG).d("Couldn't get icon id from mMarker.getTitle(): %s", e.toString());
        }
        return BitmapFactory.decodeResource(mResources, bitmapId); // just doesn't want to draw. hmmmm.
    }

    @Override
    public void removeFromMap() {
        mMarker.remove();
    }
}
