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
package edu.mit.ll.nics.android.ui.viewmodel.maps;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.data.DDMCoordinates;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public class GeocodingDegMinViewModel extends ViewModel {

    private final MutableLiveData<LiveDataTriggerEvent<String>> mLatDegrees;
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLatMinutes;
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLonDegrees;
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLonMinutes;

    @AssistedInject
    public GeocodingDegMinViewModel(@Assisted LatLng point) {
        DDMCoordinates ddm = null;
        try {
            ddm = new DDMCoordinates(point);
        } catch (IllegalArgumentException ignored) {
        }

        if (ddm != null) {
            mLatDegrees = new MutableLiveData<>(new LiveDataTriggerEvent<>(ddm.getLatDegrees(), INPUT));
            mLatMinutes = new MutableLiveData<>(new LiveDataTriggerEvent<>(ddm.getLatMinutes(), INPUT));
            mLonDegrees = new MutableLiveData<>(new LiveDataTriggerEvent<>(ddm.getLonDegrees(), INPUT));
            mLonMinutes = new MutableLiveData<>(new LiveDataTriggerEvent<>(ddm.getLonMinutes(), INPUT));
        } else {
            mLatDegrees = new MutableLiveData<>(null);
            mLatMinutes = new MutableLiveData<>(null);
            mLonDegrees = new MutableLiveData<>(null);
            mLonMinutes = new MutableLiveData<>(null);
        }
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLatDegrees() {
        return mLatDegrees;
    }

    public void setLatDegrees(LiveDataTriggerEvent<String> latDegrees) {
        mLatDegrees.postValue(latDegrees);
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLatMinutes() {
        return mLatMinutes;
    }

    public void setLatMinutes(LiveDataTriggerEvent<String> latMinutes) {
        mLatMinutes.postValue(latMinutes);
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLonDegrees() {
        return mLonDegrees;
    }

    public void setLonDegrees(LiveDataTriggerEvent<String> lonDegrees) {
        mLonDegrees.postValue(lonDegrees);
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLonMinutes() {
        return mLonMinutes;
    }

    public void setLonMinutes(LiveDataTriggerEvent<String> lonMinutes) {
        mLonMinutes.postValue(lonMinutes);
    }

    public void setTextFields(EnhancedLatLng point) {
        try {
            DDMCoordinates ddm = new DDMCoordinates(point.getLatLng());
            setLatDegrees(new LiveDataTriggerEvent<>(ddm.getLatDegrees(), MAP));
            setLatMinutes(new LiveDataTriggerEvent<>(ddm.getLatMinutes(), MAP));
            setLonDegrees(new LiveDataTriggerEvent<>(ddm.getLonDegrees(), MAP));
            setLonMinutes(new LiveDataTriggerEvent<>(ddm.getLonMinutes(), MAP));
        } catch (Exception e) {
            clearTextFields();
        }
    }

    private void clearTextFields() {
        setLatDegrees(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLatMinutes(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLonDegrees(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLonMinutes(new LiveDataTriggerEvent<>(EMPTY, MAP));
    }

    public void refresh(LatLng point) {
        DDMCoordinates ddm = null;
        try {
            ddm = new DDMCoordinates(point);
        } catch (IllegalArgumentException ignored) {
        }

        if (ddm != null) {
            mLatDegrees.postValue(new LiveDataTriggerEvent<>(ddm.getLatDegrees(), INPUT));
            mLatMinutes.postValue(new LiveDataTriggerEvent<>(ddm.getLatMinutes(), INPUT));
            mLonDegrees.postValue(new LiveDataTriggerEvent<>(ddm.getLonDegrees(), INPUT));
            mLonMinutes.postValue(new LiveDataTriggerEvent<>(ddm.getLonMinutes(), INPUT));
        } else {
            mLatDegrees.postValue(null);
            mLatMinutes.postValue(null);
            mLonDegrees.postValue(null);
            mLonMinutes.postValue(null);
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final LatLng mPoint;
        private final GeocodingDegMinViewModelFactory mAssistedFactory;

        public Factory(GeocodingDegMinViewModelFactory assistedFactory, LatLng point) {
            mPoint = point;
            mAssistedFactory = assistedFactory;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) mAssistedFactory.create(mPoint);
        }
    }

    @AssistedFactory
    public interface GeocodingDegMinViewModelFactory {
        GeocodingDegMinViewModel create(LatLng point);
    }
}