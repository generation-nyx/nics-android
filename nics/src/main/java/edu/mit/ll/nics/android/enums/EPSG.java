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
package edu.mit.ll.nics.android.enums;

public enum EPSG {

    EPSG_4326(4326, "EPSG:4326", "EPSG:4326 - WGS 84",
            "+proj=longlat +datum=WGS84 +no_defs"),
    EPSG_3857(3857, "EPSG:3857", "EPSG:3857 - WGS 84 / Pseudo-Mercator",
            "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext +no_defs"),
    EPSG_27700(27700, "EPSG:27700", "EPSG:27700 - OSGB 1936 / British National Grid",
            "+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 +y_0=-100000 +ellps=airy +towgs84=446.448,-125.157,542.06,0.15,0.247,0.842,-20.489 +units=m +no_defs"),
    EPSG_2154(2154, "EPSG:2154", "EPSG:2154 - RGF93 / Lambert-93",
            "+proj=lcc +lat_1=49 +lat_2=44 +lat_0=46.5 +lon_0=3 +x_0=700000 +y_0=6600000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"),
    EPSG_31370(31370, "EPSG:31370", "EPSG:31370 - Belge 1972 / Belgian Lambert 72",
            "+proj=lcc +lat_1=51.16666723333333 +lat_2=49.8333339 +lat_0=90 +lon_0=4.367486666666666 +x_0=150000.013 +y_0=5400088.438 +ellps=intl +towgs84=-106.869,52.2978,-103.724,0.3366,-0.457,1.8422,-1.2747 +units=m +no_defs"),
    EPSG_28992(28992, "EPSG:28992", "EPSG:28992 - Amersfoort / RD New",
            "+proj=sterea +lat_0=52.15616055555555 +lon_0=5.38763888888889 +k=0.9999079 +x_0=155000 +y_0=463000 +ellps=bessel +towgs84=565.417,50.3319,465.552,-0.398957,0.343988,-1.8774,4.0725 +units=m +no_defs"),
    EPSG_3067(3067, "EPSG:3067", "EPSG:3067 - ETRS89 / TM35FIN(E,N)",
            "+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"),
    EPSG_21781(21781, "EPSG:21781", "EPSG:21781 - CH1903 / LV03",
            "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=600000 +y_0=200000 +ellps=bessel +towgs84=674.4,15.1,405.3,0,0,0,0 +units=m +no_defs");

    // These two don't work currently.
    public static final String EPSG_5514_PARAMS = "+proj=krovak +lat_0=49.5 +lon_0=24.83333333333333 +alpha=30.28813972222222 +k=0.9999 +x_0=0 +y_0=0 +ellps=bessel +towgs84=589,76,480,0,0,0,0 +units=m +no_defs";
    public static final String EPSG_27200_PARAMS = "+proj=nzmg +lat_0=-41 +lon_0=173 +x_0=2510000 +y_0=6023150 +ellps=intl +datum=nzgd49 +units=m +no_defs";

    private final int code;
    private final String name;
    private final String title;
    private final String params;

    EPSG(int code, String name, String title, String params) {
        this.code = code;
        this.name = name;
        this.title = title;
        this.params = params;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getParams() {
        return params;
    }

    public static EPSG getByCode(int code) {
        EPSG value = null;

        for (EPSG epsg : EPSG.values()) {
            if (epsg.getCode() == code) {
                value = epsg;
                break;
            }
        }

        return value;
    }

    public static EPSG getByName(String name) {
        EPSG value = null;

        for (EPSG epsg : EPSG.values()) {
            if (epsg.getName().equals(name)) {
                value = epsg;
                break;
            }
        }

        return value;
    }

    public static EPSG getByTitle(String title) {
        EPSG value = null;

        for (EPSG epsg : EPSG.values()) {
            if (epsg.getTitle().equals(title)) {
                value = epsg;
                break;
            }
        }

        return value;
    }

    public static EPSG getByParams(String params) {
        EPSG value = null;

        for (EPSG epsg : EPSG.values()) {
            if (epsg.getParams().equals(params)) {
                value = epsg;
                break;
            }
        }

        return value;
    }
}