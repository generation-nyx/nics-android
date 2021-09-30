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
package edu.mit.ll.nics.android.maps.markup;

import android.app.Activity;
import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;

import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.maps.tags.HazardTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

public class HazardPolygon extends MarkupPolygon {

    private final Hazard mHazard;

    public HazardPolygon(Hazard hazard,
                         GoogleMap map,
                         PreferencesRepository preferences,
                         Activity activity) {
        super(map, preferences, activity);

        setId(hazard.getId());
        mHazard = hazard;
        setPoints(new ArrayList<>(hazard.getCoordinates()));
        setStrokeWidth(1f);
        mPolygonOptions.zIndex(-10f);
        mPolygonOptions.strokeColor(Color.YELLOW);
        mPolygonOptions.fillColor(Color.argb((int) (0.3 * 255), 255, 255, 0));
        setClickable(true);
        super.setTag(new HazardTag(mHazard));
    }

    public Hazard getHazard() {
        return mHazard;
    }
}
