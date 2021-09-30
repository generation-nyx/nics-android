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

import edu.mit.ll.nics.android.R;

public enum MapStyle {

    STANDARD("Standard", -1),

    SILVER("Silver", R.raw.google_maps_styled_silver),

    RETRO("Retro", R.raw.google_maps_styled_retro),

    DARK("Dark", R.raw.google_maps_styled_dark),

    NIGHT("Night", R.raw.google_maps_styled_night),

    AUBERGINE("Aubergine", R.raw.google_maps_styled_aubergine);

    private final String name;

    private final int resourceId;

    MapStyle(String name, int resourceId) {
        this.name = name;
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    public static MapStyle lookUp(int id) {
        MapStyle value = null;

        for (MapStyle style : MapStyle.values()) {
            if (style.getResourceId() == id) {
                value = style;

                break;
            }
        }

        return value;
    }

    public static MapStyle lookUp(String text) {
        MapStyle value = null;

        for (MapStyle style : MapStyle.values()) {
            if (style.getName().equals(text)) {
                value = style;
                break;
            }
        }

        return value;
    }
}
