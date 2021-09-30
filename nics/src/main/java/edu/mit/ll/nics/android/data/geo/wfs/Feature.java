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
package edu.mit.ll.nics.android.data.geo.wfs;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.util.ArrayList;

import edu.mit.ll.nics.android.data.geo.wfs.geom.Geometry;
import edu.mit.ll.nics.android.database.entities.LayerProperties;
import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class Feature {

    @Expose
    private String type;

    @Expose
    private String id;

    private Geometry<?> geometry;

    @Expose
    private String geometry_name;

    @Expose
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private LayerProperties properties;

    private boolean rendered;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Geometry<?> getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry<?> geometry) {
        this.geometry = geometry;
    }

    public String getGeometryName() {
        return geometry_name;
    }

    public void setGeometryName(String geometryName) {
        this.geometry_name = geometryName;
    }

    public LayerProperties getProperties() {
        if (properties == null) {
            properties = new LayerProperties();
        }
        return properties;
    }

    public void setProperties(LayerProperties properties) {
        this.properties = properties;
    }

    public boolean isRendered() {
        return rendered;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public LatLng geometryToLatLng() {
        ArrayList<Double> coordinates = new ArrayList<>();

        try {
            Object o = getGeometry().getCoordinates();

            if (o instanceof ArrayList<?>) {
                ArrayList<?> items = (ArrayList<?>) o;
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) instanceof Double) {
                        Double coordinate = (Double) items.get(i);
                        coordinates.add(coordinate);
                    }
                }
            }

            return new LatLng(coordinates.get(1), coordinates.get(0));
        } catch (Exception e) {
            Timber.tag(DEBUG).e("Parse tracking feature should be a Point geometry type.");
            return null;
        }
    }

    public String getUniqueId() {
        return String.valueOf(getProperties().getLayerId());
    }
}