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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import edu.mit.ll.nics.android.R;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEFAULT_ICON_PATH;

public class Symbols {
    public static final BiMap<String, Integer> ESI = null;
    public static final BiMap<String, Integer> FRIENDLYUNIT = null;
    public static final BiMap<String, Integer> ICS = null;
    public static final BiMap<String, Integer> INCIDENT = null;
    public static final BiMap<String, Integer> CST = null;
    public static final BiMap<String, Integer> RESOURCES = null;
    public static final BiMap<String, Integer> MISSION = null;
    public static final BiMap<String, Integer> USAR = null;
    public static final BiMap<String, Integer> USCG = null;
    public static final BiMap<String, Integer> NG = null;
    public static final BiMap<String, Integer> WILDFIRE = null;
    public static final BiMap<String, Integer> WEATHER = null;
    public static final BiMap<String, Integer> UNOCHA = null;
    public static final BiMap<String, Integer> MNE = null;
    public static final BiMap<String, Integer> IFRC = null;

    public static Integer getSymbolId(String path) {
        if (path != null) {
            if (ICS.containsKey(path)) {
                return ICS.get(path);
            } else if (INCIDENT.containsKey(path)) {
                return INCIDENT.get(path);
            } else if (CST.containsKey(path)) {
                return CST.get(path);
            } else if (FRIENDLYUNIT.containsKey(path)) {
                return FRIENDLYUNIT.get(path);
            } else if (RESOURCES.containsKey(path)) {
                return RESOURCES.get(path);
            } else if (MISSION.containsKey(path)) {
                return MISSION.get(path);
            } else if (USAR.containsKey(path)) {
                return USAR.get(path);
            } else if (USCG.containsKey(path)) {
                return USCG.get(path);
            } else if (ESI.containsKey(path)) {
                return ESI.get(path);
            } else if (WEATHER.containsKey(path)) {
                return WEATHER.get(path);
            } else if (NG.containsKey(path)) {
                return NG.get(path);
            } else if (WILDFIRE.containsKey(path)) {
                return WILDFIRE.get(path);
            } else if (UNOCHA.containsKey(path)) {
                return UNOCHA.get(path);
            } else if (MNE.containsKey(path)) {
                return MNE.get(path);
            } else if (IFRC.containsKey(path)) {
                return IFRC.get(path);
            }
        }
        return R.drawable.x;
    }

    public static String getSymbolPath(int id) {
        if (ICS.containsValue(id)) {
            return ICS.inverse().get(id);
        } else if (INCIDENT.containsValue(id)) {
            return INCIDENT.inverse().get(id);
        } else if (CST.containsValue(id)) {
            return CST.inverse().get(id);
        } else if (FRIENDLYUNIT.containsValue(id)) {
            return FRIENDLYUNIT.inverse().get(id);
        } else if (RESOURCES.containsValue(id)) {
            return RESOURCES.inverse().get(id);
        } else if (MISSION.containsValue(id)) {
            return MISSION.inverse().get(id);
        } else if (USAR.containsValue(id)) {
            return USAR.inverse().get(id);
        } else if (USCG.containsValue(id)) {
            return USCG.inverse().get(id);
        } else if (ESI.containsValue(id)) {
            return ESI.inverse().get(id);
        } else if (WEATHER.containsValue(id)) {
            return WEATHER.inverse().get(id);
        } else if (NG.containsValue(id)) {
            return NG.inverse().get(id);
        } else if (WILDFIRE.containsValue(id)) {
            return WILDFIRE.inverse().get(id);
        } else if (UNOCHA.containsValue(id)) {
            return UNOCHA.inverse().get(id);
        } else if (MNE.containsValue(id)) {
            return MNE.inverse().get(id);
        } else if (IFRC.containsValue(id)) {
            return IFRC.inverse().get(id);
        }

        return DEFAULT_ICON_PATH;
    }
}