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
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public class GeocodingLatLngViewModel extends ViewModel {

    private final MutableLiveData<LiveDataTriggerEvent<String>> mLatitude;
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLongitude;

    @AssistedInject
    public GeocodingLatLngViewModel(@Assisted LatLng point) {
        if (point != null) {
            mLatitude = new MutableLiveData<>(new LiveDataTriggerEvent<>(String.valueOf(point.latitude), INPUT));
            mLongitude = new MutableLiveData<>(new LiveDataTriggerEvent<>(String.valueOf(point.longitude), INPUT));
        } else {
            mLatitude = new MutableLiveData<>(null);
            mLongitude = new MutableLiveData<>(null);
        }
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLatitude() {
        return mLatitude;
    }

    public void setLatitude(LiveDataTriggerEvent<String> latitude) {
        mLatitude.postValue(latitude);
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLongitude() {
        return mLongitude;
    }

    public void setLongitude(LiveDataTriggerEvent<String> longitude) {
        mLongitude.postValue(longitude);
    }

    public void setTextFields(EnhancedLatLng point) {
        try {
            setLatitude(new LiveDataTriggerEvent<>(String.valueOf(point.getLatLng().latitude), MAP));
            setLongitude(new LiveDataTriggerEvent<>(String.valueOf(point.getLatLng().longitude), MAP));
        } catch (Exception e) {
            clearTextFields();
        }
    }

    public void clearTextFields() {
        setLatitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLongitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
    }

    public void refresh(LatLng point) {
        if (point != null) {
            mLatitude.postValue(new LiveDataTriggerEvent<>(String.valueOf(point.latitude), INPUT));
            mLongitude.postValue(new LiveDataTriggerEvent<>(String.valueOf(point.longitude), INPUT));
        } else {
            mLatitude.postValue(null);
            mLongitude.postValue(null);
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final LatLng mPoint;
        private final GeocodingLatLngViewModelFactory mAssistedFactory;

        public Factory(GeocodingLatLngViewModelFactory assistedFactory,
                       LatLng point) {
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
    public interface GeocodingLatLngViewModelFactory {
        GeocodingLatLngViewModel create(LatLng point);
    }
}