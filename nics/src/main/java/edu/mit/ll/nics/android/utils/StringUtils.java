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

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.HTTP;

public class StringUtils {

    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String DASH = "-";
    public static final String SPACED_DASH = " - ";
    public static final String COMMA = ",";

    /**
     * Checks to see if the string is valid JSON by trying to parse it into a {@link JSONObject}
     * or a {@link JSONArray}. If it fails, then it isn't valid JSON.
     *
     * @param test The string to check validity over.
     * @return Whether or not the string is valid json.
     */
    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Separates each string in a list by commas.
     *
     * @param list The list of strings to combine and separate by commas.
     * @return Returns the combined string separated by commas.
     */
    public static String commaSeparateList(ArrayList<String> list) {
        return separateListByDelimiter(list, COMMA);
    }

    /**
     * A generic method to separate a list by a provided delimiter.
     *
     * @param list      The list of strings to combine and separate by the delimiter.
     * @param delimiter The delimiter to separate by.
     * @return Returns the combined string separated by the delimiter.
     */
    public static String separateListByDelimiter(ArrayList<String> list, String delimiter) {
        if (list.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(list.get(0));

            for (int i = 1; i < list.size(); i++) {
                sb.append(delimiter);
                sb.append(SPACE);
                sb.append(list.get(i));
            }

            return sb.toString();
        } else {
            return EMPTY;
        }
    }

    /**
     * Builds an SQL Where argument string depending on the length of arguments
     *
     * @param length The amount of arguments in the clause.
     * @return Returns the SQL argument string.
     */
    public static String buildWhereArgument(int length) {
        StringBuilder sb = new StringBuilder();
        sb.append("?");

        for (int i = 1; i < length; i++) {
            sb.append(",?");
        }

        return sb.toString();
    }

    public static String valueOrEmpty(String s) {
        return s != null ? s : EMPTY;
    }


    /**
     * Removes the protocol from a URL and returns the protocol-relative URL.
     *
     * @param url The URL string to clean.
     * @return The protocol-relative URL string.
     */
    public static String cleanUrlString(String url) {
        url = url.trim();
        if (url.toLowerCase().startsWith("https://www.")) {
            url = url.replace("https://www.", "");
        } else if (url.toLowerCase().startsWith("http://www.")) {
            url = url.replace("http://www.", "");
        } else if (url.toLowerCase().startsWith("https://")) {
            url = url.replace("https://", "");
        } else if (url.toLowerCase().startsWith("http://")) {
            url = url.replace("http://", "");
        }

        return url;
    }

    public static Double toDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

    public static Double toLatitude(String s) {
        try {
            double latitude = Double.parseDouble(s);
            if (latitude <= 90 && latitude >= -90) {
                return latitude;
            } else {
                return null;
            }
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

    public static Double toLongitude(String s) {
        try {
            double longitude = Double.parseDouble(s);
            if (longitude <= 180 && longitude >= -180) {
                return longitude;
            } else {
                return null;
            }
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

    public static String formatDecimalString(String s) {
        return formatDecimalString(s, "#.#########");
    }

    public static String formatDecimalString(String s, String pattern) {
        try {
            DecimalFormat formatter = new DecimalFormat(pattern);
            return formatter.format(s);
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to format string. ");
        }
        return EMPTY;
    }

    public static String safeTrim(String s) {
        if (s != null) {
            s = s.trim();
        }
        return s;
    }

    public static String dateToString(long timestamp) {
        return new Date(timestamp).toString();
    }

    public static boolean isValidUrl(String value) {
        return UrlValidator.getInstance().isValid(value)
                || UrlValidator.getInstance().isValid("http://" + value)
                || UrlValidator.getInstance().isValid("https://" + value);
    }

    public static String encodeUrl(String url) {
        return url.replace(" ", "%20");
    }

    public static String httpToHttps(String s) throws MalformedURLException {
        URL url = new URL(s);
        String protocol = url.getProtocol();
        if (protocol.equals(HTTP)) {
            return s.replaceFirst("(?i)^http://", "https://");
        } else {
            return s;
        }
    }

    public static String coordinateToString(LatLng coordinate) {
        return coordinate == null ? EMPTY :
                String.valueOf(coordinate.latitude)
                        .concat(",")
                        .concat(String.valueOf(coordinate.longitude));
    }
}
