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
package edu.mit.ll.nics.android.utils;

import android.location.Location;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class ExifUtils {
    private static final StringBuilder sb = new StringBuilder(20);

    /**
     * Found at https://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android by Fabyen
     */
    private static String latitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    /**
     * Found at https://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android by Fabyen
     */
    private static String longitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    /**
     * Found at https://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android by Fabyen
     */
    private synchronized static String convert(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 1000.0d);

        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000");
        return sb.toString();
    }

    public static void saveGpsExif(File file, Location location) {
        if (location != null) {
            ExifInterface exifInterface;
            try {
                exifInterface = new ExifInterface(file);
                exifInterface.setGpsInfo(location);
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Failed to save gps exif data to image.");
            }
        }
    }

    public static void saveGpsExif(String fileName, Location location) {
        if (location != null) {
            ExifInterface exifInterface;
            try {
                exifInterface = new ExifInterface(fileName);
                exifInterface.setGpsInfo(location);
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Failed to save gps exif data to " + fileName);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasGPSData(String absolutePath) {
        try {
            ExifInterface exif = new ExifInterface(absolutePath);

            String lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String lat_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String lon_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if (lat != null && lat_ref != null && lon != null && lon_ref != null) {
                return true;
            }
        } catch (IOException e) {
            Timber.tag(DEBUG).e(e, "Failed getting GPS exif data from file. ");
        }

        return false;
    }

    public static HashMap<String, String> getGPSData(InputStream inputStream) {
        try {
            ExifInterface exif = new ExifInterface(inputStream);

            return checkForGPSExif(exif);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static HashMap<String, String> getGPSData(String absolutePath) {
        try {
            ExifInterface exif = new ExifInterface(absolutePath);

            return checkForGPSExif(exif);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static HashMap<String, String> checkForGPSExif(ExifInterface exif) {
        String lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String lat_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String lon_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

        if (lat != null && lat_ref != null && lon != null && lon_ref != null) {
            HashMap<String, String> gpsAttributes = new HashMap<>();
            gpsAttributes.put("lat", lat);
            gpsAttributes.put("lat_ref", lat_ref);
            gpsAttributes.put("lon", lon);
            gpsAttributes.put("lon_ref", lon_ref);
            return gpsAttributes;
        }

        return null;
    }

    public static void setGPSData(String absolutePath, HashMap<String, String> gpsAttributes) {
        try {
            ExifInterface exif = new ExifInterface(absolutePath);

            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, gpsAttributes.get("lat"));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, gpsAttributes.get("lat_ref"));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, gpsAttributes.get("lon"));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, gpsAttributes.get("lon_ref"));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setGPSData(String absolutePath, Location location) {
        try {
            ExifInterface exif = new ExifInterface(absolutePath);

            if (location != null) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, ExifUtils.convert(location.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, ExifUtils.latitudeRef(location.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, ExifUtils.convert(location.getLongitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, ExifUtils.longitudeRef(location.getLongitude()));
                exif.saveAttributes();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setGPSData(String absolutePath, double longitude, double latitude) {
        try {
            ExifInterface exif = new ExifInterface(absolutePath);

            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, ExifUtils.convert(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, ExifUtils.latitudeRef(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, ExifUtils.convert(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, ExifUtils.longitudeRef(longitude));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
