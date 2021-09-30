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

import java.util.UUID;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.utils.livedata.RefreshableMutableLiveData;

import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public class GeocodingViewModel extends ViewModel {

    private final RefreshableMutableLiveData<EnhancedLatLng> mPoint = new RefreshableMutableLiveData<>(null);

    @AssistedInject
    public GeocodingViewModel(@Assisted MarkupSymbol symbol) {
        if (symbol.getPoint() != null) {
            mPoint.setValue(new EnhancedLatLng(UUID.randomUUID(), symbol.getPoint(), MAP));
        }
    }

    public MutableLiveData<EnhancedLatLng> getPoint() {
        return mPoint;
    }

    public void setPoint(EnhancedLatLng point) {
        mPoint.postValue(point);
    }

    public void refresh() {
        mPoint.refresh();
    }

    public void reset() {
        mPoint.postValue(null);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final GeocodingViewModelFactory mAssistedFactory;
        private final MarkupSymbol mMarkup;

        public Factory(GeocodingViewModelFactory assistedFactory, MarkupSymbol markup) {
            mAssistedFactory = assistedFactory;
            mMarkup = markup;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) mAssistedFactory.create(mMarkup);
        }
    }

    @AssistedFactory
    public interface GeocodingViewModelFactory {
        GeocodingViewModel create(MarkupSymbol markup);
    }
}