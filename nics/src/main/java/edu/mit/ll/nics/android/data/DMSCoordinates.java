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
package edu.mit.ll.nics.android.data;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import static android.location.Location.FORMAT_SECONDS;

public class DMSCoordinates {

    private final String mLatDegrees;
    private final String mLatMinutes;
    private final String mLatSeconds;
    private final String mLonDegrees;
    private final String mLonMinutes;
    private final String mLonSeconds;

    public DMSCoordinates(LatLng latLng) throws IllegalArgumentException {
        try {
            String[] latitude = Location.convert(latLng.latitude, FORMAT_SECONDS).split(":");
            String[] longitude = Location.convert(latLng.longitude, FORMAT_SECONDS).split(":");

            mLatDegrees = latitude[0];
            mLatMinutes = latitude[1];
            mLatSeconds = latitude[2];
            mLonDegrees = longitude[0];
            mLonMinutes = longitude[1];
            mLonSeconds = longitude[2];
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Degrees Minutes coordinates from LatLng.");
        }
    }

    public DMSCoordinates(String latDeg, String latMin, String latSec, String lonDeg, String lonMin,
                          String lonSec) throws IllegalArgumentException {
        mLatDegrees = latDeg;
        mLatMinutes = latMin;
        mLatSeconds = latSec;
        mLonDegrees = lonDeg;
        mLonMinutes = lonMin;
        mLonSeconds = lonSec;
    }

    public String getLatDegrees() {
        return mLatDegrees;
    }

    public String getLatMinutes() {
        return mLatMinutes;
    }

    public String getLatSeconds() {
        return mLatSeconds;
    }

    public String getLonDegrees() {
        return mLonDegrees;
    }

    public String getLonMinutes() {
        return mLonMinutes;
    }

    public String getLonSeconds() {
        return mLonSeconds;
    }

    public LatLng toLatLng() {
        String lat = mLatDegrees.concat(":").concat(mLatMinutes).concat(":").concat(mLatSeconds);
        String lon = mLonDegrees.concat(":").concat(mLonMinutes).concat(":").concat(mLonSeconds);
        return new LatLng(Location.convert(lat), Location.convert(lon));
    }

    @Override
    public String toString() {
        return mLatDegrees
                .concat(":")
                .concat(mLatMinutes)
                .concat(":")
                .concat(mLatSeconds)
                .concat(",")
                .concat(mLonDegrees)
                .concat(":")
                .concat(mLonMinutes)
                .concat(":")
                .concat(mLonSeconds);
    }
}
