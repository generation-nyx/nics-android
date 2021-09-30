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

import androidx.annotation.NonNull;

public class WfsUrl {

    private final String server;
    private final String typeName;
    private final String outputFormat;
    private final String version;
    private final String request;
    private final String srsName;
    private final String maxFeatures;
    private final String token;
    private final String url;

    private WfsUrl(Builder builder) {
        this.server = builder.server;
        this.typeName = builder.typeName;
        this.outputFormat = builder.outputFormat;
        this.version = builder.version;
        this.request = builder.request;
        this.srsName = builder.srsName;
        this.maxFeatures = builder.maxFeatures;
        this.token = builder.token;
        this.url = builder.url;
    }

    public String getServer() {
        return server;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getVersion() {
        return version;
    }

    public String getRequest() {
        return request;
    }

    public String getSrsName() {
        return srsName;
    }

    public String getMaxFeatures() {
        return maxFeatures;
    }

    public String getToken() {
        return token;
    }

    public String getUrl() {
        return url;
    }

    public static class Builder {

        private final String server;
        private final String typeName;
        private String outputFormat = "json";
        private String version = "1.1.0";
        private String request = "GetFeature";
        private String srsName = "EPSG:4326";
        private String maxFeatures;
        private String token;
        private String url;

        public Builder(String server, String typeName) {
            this.server = server;
            this.typeName = typeName;
        }

        public Builder withOutputFormat(@NonNull String outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        public Builder withVersion(@NonNull String version) {
            this.version = version;
            return this;
        }

        public Builder withRequest(@NonNull String request) {
            this.request = request;
            return this;
        }

        public Builder withSRSName(@NonNull String srsName) {
            this.srsName = srsName;
            return this;
        }

        public Builder withMaxFeatures(@NonNull String maxFeatures) {
            this.maxFeatures = maxFeatures;
            return this;
        }

        public Builder withToken(@NonNull String token) {
            this.token = token;
            return this;
        }

        public WfsUrl build() {
            this.url = buildUrl();
            return new WfsUrl(this);
        }

        /**
         * Build out the full WFS request url from all of the builder properties.
         *
         * The maxFeatures and token properties are optional.
         *
         * @return The full WFS url.
         */
        private String buildUrl() {
            StringBuilder builder = new StringBuilder()
                    .append(server)
                    .append("?service=WFS")
                    .append("&outputFormat=")
                    .append(outputFormat)
                    .append("&version=")
                    .append(version)
                    .append("&request=")
                    .append(request)
                    .append("&srsName=")
                    .append(srsName)
                    .append((version.equals("2.0.0")) ? "&typeNames=" : "&typeName=")
                    .append(typeName);

            if (maxFeatures != null) {
                builder.append("&maxFeatures=").append(maxFeatures);
            }

            if (token != null) {
                builder.append("&token=").append(token);
            }

            return builder.toString();
        }
    }
}
