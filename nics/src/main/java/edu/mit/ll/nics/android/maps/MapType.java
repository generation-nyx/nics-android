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
package edu.mit.ll.nics.android.maps;

import com.google.android.gms.maps.GoogleMap;

import edu.mit.ll.nics.android.R;

public enum MapType {

    NORMAL(GoogleMap.MAP_TYPE_NORMAL, R.id.normalMapOption),

    SATELLITE(GoogleMap.MAP_TYPE_SATELLITE, R.id.satelliteMapOption),

    HYBRID(GoogleMap.MAP_TYPE_HYBRID, R.id.hybridMapOption),

    TERRAIN(GoogleMap.MAP_TYPE_TERRAIN, R.id.terrainMapOption),

    NONE(GoogleMap.MAP_TYPE_NONE, R.id.offlineMapOption);

    private final int type;
    private final int resourceId;

    MapType(int type, int resourceId) {
        this.type = type;
        this.resourceId = resourceId;
    }

    public int getType() {
        return type;
    }

    public int getResourceId() {
        return resourceId;
    }

    public static MapType lookupByType(int type) {
        MapType value = NORMAL;

        for (MapType mapType : MapType.values()) {
            if (mapType.getType() == type) {
                value = mapType;
                break;
            }
        }

        return value;
    }

    public static MapType lookUpById(int id) {
        MapType value = NORMAL;

        for (MapType mapType : MapType.values()) {
            if (mapType.getResourceId() == id) {
                value = mapType;
                break;
            }
        }

        return value;
    }
}
