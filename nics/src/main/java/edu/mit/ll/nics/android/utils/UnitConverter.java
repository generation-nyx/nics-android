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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class UnitConverter {

    public static final String METRIC = "Metric";
    public static final String IMPERIAL = "Imperial";
    public static final String NAUTICAL = "Nautical";

    // TODO could make a fancier converter setup, but for now just do it manually.

    public static double kilometersToMeters(double kilometers) {
        return kilometers * 1000;
    }

    public static double kilometersToMiles(double kilometers) {
        return kilometers / 1.609;
    }

    public static double kilometersToNauticalMiles(double kilometers) {
        return kilometers / 1.852;
    }

    public static double metersToKilometers(double meters) {
        return meters / 1000;
    }

    public static double metersToMiles(double meters) {
        return meters / 1609.344;
    }

    public static double metersToNauticalMiles(double meters) {
        return meters / 1852;
    }

    public static String getAbbreviation(String unit) {
        return unitAbbreviations.get(unit);
    }

    public static final Map<String, String> unitAbbreviations = ImmutableMap.<String, String>builder()
            .put("kilometer", "km")
            .put("kilometers", "km")
            .put("meter", "m")
            .put("meters", "m")
            .build();
}
