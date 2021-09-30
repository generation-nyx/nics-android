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
package edu.mit.ll.nics.android.maps.tileproviders;

import android.net.Uri;

import com.google.android.gms.maps.model.TileProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import edu.mit.ll.nics.android.api.DownloaderApiService;

public class TileProviderFactory {

    public static TileProvider buildWmsTileProvider(String baseUrl, String layerNames,
                                                    WmsOptions options, DownloaderApiService downloader) {
        String layers = Uri.encode(layerNames);

        StringBuilder url = new StringBuilder(baseUrl +
                "?SERVICE=WMS" +
                "&LAYERS=" + layers +
                "&REQUEST=GetMap" +
                "&TILED=" + options.tiled +
                "&VERSION=" + options.version +
                "&TRANSPARENT=" + options.transparent +
                "&STYLES=" + options.styles +
                "&FORMAT=" + options.format +
                "&CRS=" + options.crs +
                "&WIDTH=" + options.width +
                "&HEIGHT=" + options.height);

        if (options.attrs != null) {
            for(Map.Entry<String, String> entry : options.attrs.entrySet()) {
                try {
                    url.append(String.format("&%s=%s",
                            URLEncoder.encode(entry.getKey(), "UTF-8"),
                            URLEncoder.encode(entry.getValue(), "UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    // bury
                }
            }
        }

        return new WMSTileProvider(url.toString(), options.width, options.height, downloader);
    }

    public static TileProvider buildArcGISTileProvider(String baseUrl, ArcGISOptions options,
                                                       DownloaderApiService downloader) {
        String url = baseUrl +
                "/export?F=" + options.format +
                "&FORMAT=" + options.formatDesc +
                "&TRANSPARENT=" + options.transparent +
                "&LAYERS=show%3A0";
        return new ArcGISTileProvider(url, options.width, options.height, downloader);
    }

    public static class ArcGISOptions {

        private final String format = "image";
        private final String formatDesc = "PNG32";
        private final boolean transparent = true;
        private final int height = 512;
        private final int width = 512;
    }

    public static class WmsOptions {

        private String styles = "";
        private String version = "1.3.0";
        private String format = "image/png";
        private String crs = "EPSG:3857";
        private int height = 512;
        private int width = 512;
        private boolean transparent = true;
        private boolean tiled = true;
        private Map<String, String> attrs = Collections.emptyMap();

        public WmsOptions styles(String styles) {
            this.styles = styles;
            return this;
        }

        public WmsOptions version(String version) {
            this.version = version;
            return this;
        }

        public WmsOptions format(String format) {
            this.format = format;
            return this;
        }

        public WmsOptions crs(String crs) {
            this.crs = crs;
            return this;
        }

        public WmsOptions transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        public WmsOptions height(int height) {
            this.height = height;
            return this;
        }

        public WmsOptions width(int width) {
            this.width = width;
            return this;
        }

        public WmsOptions tiled(boolean tiled) {
            this.tiled = tiled;
            return this;
        }

        public WmsOptions attributes(Map<String, String> attributes) {
            Map<String, String> attrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            if (attributes != null) {
                attrs.putAll(attributes);
            }

            this.styles = attrs.getOrDefault("styles", this.styles);
            this.version = attrs.getOrDefault("version", this.version);
            this.format = attrs.getOrDefault("format", this.format);
            this.crs = attrs.getOrDefault("crs", this.crs);

            attrs.remove("styles");
            attrs.remove("version");
            attrs.remove("format");
            attrs.remove("crs");

            this.attrs = attrs;
            return this;
        }
    }
}
