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
package edu.mit.ll.nics.android.ui.fragments.maps;

import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingViewModel;
import edu.mit.ll.nics.android.ui.fragments.TabFragment;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;

import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public abstract class GeocodingPageFragment extends TabFragment {

    protected GeocodingViewModel mSharedViewModel;

    protected abstract void updateMap();

    protected abstract void updateText(EnhancedLatLng point);

    protected void setPoint(LatLng point, LiveDataTrigger trigger) {
        if (mSharedViewModel.getPoint().getValue() == null) {
            mSharedViewModel.setPoint(new EnhancedLatLng(UUID.randomUUID(), point, MAP));
        } else {
            EnhancedLatLng p = mSharedViewModel.getPoint().getValue();
            p.setLatLng(point);
            p.setTrigger(trigger);
            mSharedViewModel.setPoint(p);
        }
    }

    protected LatLng getSharedPoint() {
        LatLng sharedPoint = null;
        EnhancedLatLng point = mSharedViewModel.getPoint().getValue();

        if (point != null) {
            sharedPoint = point.getLatLng();
        }
        return sharedPoint;
    }
}
