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

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.enums.LabelSize;
import edu.mit.ll.nics.android.maps.markup.MarkupText;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import edu.mit.ll.nics.android.utils.livedata.RefreshableMutableLiveData;

import static edu.mit.ll.nics.android.utils.ColorUtils.colorArrayToInt;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public class MarkupEditTextViewModel extends ViewModel {

    private final NonNullMutableLiveData<String> mComment = new NonNullMutableLiveData<>(EMPTY);
    private final NonNullMutableLiveData<String> mLabel = new NonNullMutableLiveData<>(EMPTY);
    private final NonNullMutableLiveData<LabelSize> mLabelSize = new NonNullMutableLiveData<>(LabelSize.LARGE);
    private final NonNullMutableLiveData<Integer> mColor = new NonNullMutableLiveData<>(Color.BLACK);
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLatitude = new MutableLiveData<>(null);
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLongitude = new MutableLiveData<>(null);
    private final RefreshableMutableLiveData<LiveDataTriggerEvent<LatLng>> mPoint = new RefreshableMutableLiveData<>(null);

    @AssistedInject
    public MarkupEditTextViewModel(@Assisted MarkupText markup) {
        mComment.setValue(getValueOrDefault(markup.getComments(), EMPTY));
        mLabelSize.setValue(getValueOrDefault(LabelSize.lookUp(markup.getLabelSize()), LabelSize.LARGE));
        mLabel.setValue(getValueOrDefault(markup.getLabelText(), EMPTY));
        mColor.setValue(getValueOrDefault(colorArrayToInt(markup.getStrokeColor()), Color.BLACK));

        LatLng point = markup.getPoint();
        if (point != null) {
            markup.addToMap(point);
            setPoint(new LiveDataTriggerEvent<>(point, MAP));
        }
    }

    public MutableLiveData<String> getComment() {
        return mComment;
    }

    public void setComment(String comment) {
        mComment.postValue(comment);
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

    public MutableLiveData<String> getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel.postValue(label);
    }

    public MutableLiveData<LabelSize> getLabelSize() {
        return mLabelSize;
    }

    public void setLabelSize(LabelSize labelSize) {
        mLabelSize.postValue(labelSize);
    }

    public NonNullMutableLiveData<Integer> getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor.postValue(color);
    }

    public MutableLiveData<LiveDataTriggerEvent<LatLng>> getPoint() {
        return mPoint;
    }

    public void setPoint(LiveDataTriggerEvent<LatLng> event) {
        mPoint.postValue(event);
    }

    public void setTextFields(LiveDataTriggerEvent<LatLng> point) {
        try {
            setLatitude(new LiveDataTriggerEvent<>(String.valueOf(point.getData().latitude), MAP));
            setLongitude(new LiveDataTriggerEvent<>(String.valueOf(point.getData().longitude), MAP));
        } catch (Exception e) {
            clearTextFields();
        }
    }

    public void clearTextFields() {
        setLatitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLongitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final MarkupEditTextViewModelFactory mAssistedFactory;

        private final MarkupText mMarkup;

        public Factory(MarkupEditTextViewModelFactory assistedFactory, MarkupText markup) {
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
    public interface MarkupEditTextViewModelFactory {
        MarkupEditTextViewModel create(MarkupText markup);
    }
}
