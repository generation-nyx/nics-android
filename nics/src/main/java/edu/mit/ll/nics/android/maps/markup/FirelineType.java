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

import edu.mit.ll.nics.android.R;

public enum FirelineType {

    COMPLETED_FIRELINE("completed-fire-line", "Completed Fire Line", R.drawable.completed_fire_line_white, R.drawable.completed_fire_line, R.drawable.completed_fire_line_glow),

    PRIMARY_FIRELINE("primary-fire-line", "Planned Fire Line", R.drawable.planned_fire_line_white, R.drawable.planned_fire_line, R.drawable.planned_fire_line_glow),

    SECONDARY_FIRELINE("secondary-fire-line", "Secondary Fire Line", R.drawable.secondary_fire_line_white, R.drawable.secondary_fire_line, R.drawable.secondary_fire_line_glow),

    FIRE_SPREAD_PREDICTION("fire-spread-prediction", "Fire Spread Prediction", R.drawable.fire_spread_prediction, R.drawable.fire_spread_prediction, R.drawable.fire_spread_prediction_glow),

    COMPLETED_DOZER_LINE("completed-dozer-line", "Completed Dozer Line", R.drawable.completed_dozer_line_white, R.drawable.completed_dozer_line, R.drawable.completed_dozer_line_glow),

    PROPOSED_DOZER_LINE("proposed-dozer-line", "Proposed Dozer Line", R.drawable.proposed_dozer_line_white, R.drawable.proposed_dozer_line, R.drawable.proposed_dozer_line_glow),

    FIRE_EDGE_LINE("fire-edge-line", "Uncontrolled Fire Edge Line", R.drawable.fire_edge_line, R.drawable.fire_edge_line, R.drawable.fire_edge_line_glow),

    MANAGEMENT_ACTION_POINT("action-point", "Management Action Point", R.drawable.management_action_point, R.drawable.management_action_point, R.drawable.management_action_point_glow);

    private final String type;
    private final String name;
    private final int lightId;
    private final int darkId;
    private final int glowId;

    FirelineType(String type, String name, int lightId, int darkId, int glowId) {
        this.type = type;
        this.name = name;
        this.lightId = lightId;
        this.darkId = darkId;
        this.glowId = glowId;
    }

    public int getLightId() {
        return lightId;
    }

    public int getDarkId() {
        return darkId;
    }

    public int getGlowId() {
        return glowId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static FirelineType lookUp(String dashStyle) {
        FirelineType value = PRIMARY_FIRELINE;

        for (FirelineType style : FirelineType.values()) {
            if (style.getType().equals(dashStyle)) {
                value = style;
                break;
            }
        }

        return value;
    }
}
