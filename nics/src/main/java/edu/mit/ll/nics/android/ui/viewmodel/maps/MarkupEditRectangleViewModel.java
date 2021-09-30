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
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;


import java.util.UUID;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.MarkupPolygon;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import edu.mit.ll.nics.android.utils.livedata.RefreshableMutableLiveData;

import static edu.mit.ll.nics.android.utils.ColorUtils.colorArrayToInt;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public class MarkupEditRectangleViewModel extends ViewModel {

    private final NonNullMutableLiveData<String> mComment = new NonNullMutableLiveData<>(EMPTY);
    private final NonNullMutableLiveData<Float> mStrokeWidth = new NonNullMutableLiveData<>(6f);
    private final NonNullMutableLiveData<Integer> mColor = new NonNullMutableLiveData<>(Color.BLACK);
    private final NonNullMutableLiveData<Boolean> mIsMeasuring = new NonNullMutableLiveData<>(false);
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLatitude = new MutableLiveData<>(null);
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLongitude = new MutableLiveData<>(null);
    private final RefreshableMutableLiveData<EnhancedLatLng> mUpperLeftPoint = new RefreshableMutableLiveData<>(null);
    private final RefreshableMutableLiveData<EnhancedLatLng> mLowerRightPoint = new RefreshableMutableLiveData<>(null);
    private final RefreshableMutableLiveData<EnhancedLatLng> mSelectedPoint = new RefreshableMutableLiveData<>(null);

    @AssistedInject
    public MarkupEditRectangleViewModel(@Assisted MarkupPolygon markup) {
        mComment.setValue(getValueOrDefault(markup.getComments(), EMPTY));
        mStrokeWidth.setValue(getValueOrDefault((float) markup.getStrokeWidth(), 6f));
        mColor.setValue(getValueOrDefault(colorArrayToInt(markup.getStrokeColor()), Color.BLACK));

        if (markup.getPoints().size() > 3) {
            LatLng ul = markup.getPoints().get(0);
            EnhancedLatLng eUL = new EnhancedLatLng(UUID.randomUUID(), ul, MAP);
            mUpperLeftPoint.setValue(eUL);

            LatLng lr = markup.getPoints().get(2);
            EnhancedLatLng eLR = new EnhancedLatLng(UUID.randomUUID(), lr, MAP);
            mLowerRightPoint.setValue(eLR);

            mSelectedPoint.setValue(eUL);
        }
    }

    public NonNullMutableLiveData<String> getComment() {
        return mComment;
    }

    public void setComment(String comment) {
        mComment.setValue(comment);
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLatitude() {
        return mLatitude;
    }

    public void setLatitude(LiveDataTriggerEvent<String> latitude) {
        mLatitude.setValue(latitude);
    }

    public MutableLiveData<LiveDataTriggerEvent<String>> getLongitude() {
        return mLongitude;
    }

    public void setLongitude(LiveDataTriggerEvent<String> longitude) {
        mLongitude.setValue(longitude);
    }

    public NonNullMutableLiveData<Integer> getColor() {
        return mColor;
    }

    public void setColor(Integer color) {
        mColor.setValue(color);
    }

    public NonNullMutableLiveData<Float> getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(Float strokeWidth) {
        mStrokeWidth.setValue(strokeWidth);
    }

    public NonNullMutableLiveData<Boolean> isMeasuring() {
        return mIsMeasuring;
    }

    public void setMeasuring(Boolean isMeasuring) {
        mIsMeasuring.setValue(isMeasuring);
    }

    public void toggleMeasurementTool() {
        mIsMeasuring.setValue(!mIsMeasuring.getValue());
    }

    public MutableLiveData<EnhancedLatLng> getUpperLeft() {
        return mUpperLeftPoint;
    }

    public void setUpperLeft(EnhancedLatLng ul) {
        mUpperLeftPoint.setValue(ul);
    }

    public MutableLiveData<EnhancedLatLng> getLowerRight() {
        return mLowerRightPoint;
    }

    public void setLowerRight(EnhancedLatLng lr) {
        mLowerRightPoint.setValue(lr);
    }

    public MutableLiveData<EnhancedLatLng> getSelectedPoint() {
        return mSelectedPoint;
    }

    public void setSelectedPoint(EnhancedLatLng point) {
        mSelectedPoint.setValue(point);
    }

    public void updatePoint(LatLng point, LiveDataTrigger trigger) {
        EnhancedLatLng selected = mSelectedPoint.getValue();
        EnhancedLatLng upperLeft = mUpperLeftPoint.getValue();
        EnhancedLatLng lowerRight = mLowerRightPoint.getValue();

        // if we don't yet have an upper left point, use the new point
        if (upperLeft == null) {
            EnhancedLatLng uUL = new EnhancedLatLng(UUID.randomUUID(), point, trigger);
            mUpperLeftPoint.setValue(uUL);
            mSelectedPoint.setValue(uUL);
            return;
        }

        // if we don't yet have an lower right point, use the new point
        if (lowerRight == null) {
            EnhancedLatLng eLR = new EnhancedLatLng(UUID.randomUUID(), point, trigger);
            mLowerRightPoint.setValue(eLR);
            mSelectedPoint.setValue(eLR);
            return;
        }

        if (selected != null) {

            // If the upper left point is the selected point, move it and update selected
            if (upperLeft.equals(selected)) {
                EnhancedLatLng newUl = new EnhancedLatLng(upperLeft.getUUID(), point, trigger);
                mUpperLeftPoint.setValue(newUl);
                mSelectedPoint.setValue(newUl);
                return;
            }

            // If the lower right point is the selected point, move it and update selected
            if (lowerRight.equals(selected)) {
                EnhancedLatLng newLr = new EnhancedLatLng(lowerRight.getUUID(), point, trigger);
                mLowerRightPoint.setValue(newLr);
                mSelectedPoint.setValue(newLr);
                return;
            }
        }
    }

    public void clearMap() {
        clearTextFields();
        clearPoints();
    }

    public void clearTextFields() {
        setLatitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLongitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
    }

    public void clearPoints() {
        if (mUpperLeftPoint.getValue() != null) {
            mUpperLeftPoint.setValue(null);
        }

        if (mLowerRightPoint.getValue() != null) {
            mLowerRightPoint.setValue(null);
        }

        if (mSelectedPoint.getValue() != null) {
            mSelectedPoint.setValue(null);
        }
    }

    public void setTextFields(LatLng point) {
        try {
            setLatitude(new LiveDataTriggerEvent<>(String.valueOf(point.latitude), MAP));
            setLongitude(new LiveDataTriggerEvent<>(String.valueOf(point.longitude), MAP));
        } catch (Exception e) {
            clearTextFields();
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final MarkupPolygon mMarkup;
        private final MarkupEditRectangleViewModelFactory mAssistedFactory;

        public Factory(MarkupEditRectangleViewModelFactory assistedFactory, MarkupPolygon markup) {
            mMarkup = markup;
            mAssistedFactory = assistedFactory;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) mAssistedFactory.create(mMarkup);
        }
    }

    @AssistedFactory
    public interface MarkupEditRectangleViewModelFactory {
        MarkupEditRectangleViewModel create(MarkupPolygon markup);
    }
}