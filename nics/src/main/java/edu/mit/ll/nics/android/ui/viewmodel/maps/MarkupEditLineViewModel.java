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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.TransformerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.MarkupSegment;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import edu.mit.ll.nics.android.utils.livedata.RefreshableMutableLiveData;

import static edu.mit.ll.nics.android.utils.ColorUtils.colorArrayToInt;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

public class MarkupEditLineViewModel extends ViewModel {

    private final NonNullMutableLiveData<String> mComment = new NonNullMutableLiveData<>(EMPTY);
    private final NonNullMutableLiveData<Float> mStrokeWidth = new NonNullMutableLiveData<>(6f);
    private final NonNullMutableLiveData<Integer> mColor = new NonNullMutableLiveData<>(Color.BLACK);
    private final NonNullMutableLiveData<Boolean> mIsMeasuring = new NonNullMutableLiveData<>(false);
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLatitude = new MutableLiveData<>(null);
    private final MutableLiveData<LiveDataTriggerEvent<String>> mLongitude = new MutableLiveData<>(null);
    private final NonNullMutableLiveData<ArrayList<EnhancedLatLng>> mPoints = new NonNullMutableLiveData<>(new ArrayList<>());
    private final RefreshableMutableLiveData<EnhancedLatLng> mSelectedPoint = new RefreshableMutableLiveData<>(null);

    @AssistedInject
    public MarkupEditLineViewModel(@Assisted MarkupSegment markup) {
        mComment.setValue(getValueOrDefault(markup.getComments(), EMPTY));
        mStrokeWidth.setValue(getValueOrDefault((float) markup.getStrokeWidth(), 6f));
        mColor.setValue(getValueOrDefault(colorArrayToInt(markup.getStrokeColor()), Color.BLACK));

        ArrayList<EnhancedLatLng> points = new ArrayList<>();
        List<LatLng> markupPoints = markup.getPoints();
        for (LatLng point : markupPoints) {
            points.add(new EnhancedLatLng(UUID.randomUUID(), point, MAP));
        }
        mPoints.setValue(points);
    }

    public NonNullMutableLiveData<String> getComment() {
        return mComment;
    }

    public void setComment(String comment) {
        mComment.postValue(comment);
    }

    public NonNullMutableLiveData<Integer> getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor.postValue(color);
    }

    public NonNullMutableLiveData<Float> getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float width) {
        mStrokeWidth.postValue(width);
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

    public NonNullMutableLiveData<Boolean> isMeasuring() {
        return mIsMeasuring;
    }

    public void setMeasuring(boolean isMeasuring) {
        mIsMeasuring.postValue(isMeasuring);
    }

    public void toggleMeasurementTool() {
        mIsMeasuring.setValue(!mIsMeasuring.getValue());
    }

    public NonNullMutableLiveData<ArrayList<EnhancedLatLng>> getPoints() {
        return mPoints;
    }

    public ArrayList<LatLng> getLatLngs() {
        return new ArrayList<>(CollectionUtils.collect(mPoints.getValue(),
                TransformerUtils.invokerTransformer("getLatLng")));
    }

    public MutableLiveData<EnhancedLatLng> getSelectedPoint() {
        return mSelectedPoint;
    }

    public void setSelectedPoint(EnhancedLatLng point) {
        mSelectedPoint.postValue(point);
    }

    public void addPoint(EnhancedLatLng point) {
        ArrayList<EnhancedLatLng> points = mPoints.getValue();

        if (mSelectedPoint.getValue() != null) {
            int index = points.indexOf(mSelectedPoint.getValue());

            if (index != -1) {
                if (index == 0) {
                    if (points.size() > 1) {
                        points.add(0, point);
                    } else {
                        points.add(point);
                    }
                } else {
                    points.add(index + 1, point);
                }
            } else {
                points.add(point);
            }
        } else {
            points.add(point);
        }

        mPoints.postValue(points);
    }

    public void clearPoints() {
        mPoints.setValue(new ArrayList<>());
    }

    public void movePoint(EnhancedLatLng point) {
        ArrayList<EnhancedLatLng> points = mPoints.getValue();
        int index = -1;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getUUID().equals(point.getUUID())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            points.get(index).setLatLng(point.getLatLng());
            points.get(index).setTrigger(point.getTrigger());
        }
    }

    public void removePoint(EnhancedLatLng point) {
        ArrayList<EnhancedLatLng> points = mPoints.getValue();
        points.remove(point);
        mPoints.postValue(points);
    }

    public void clearTextFields() {
        setLatitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
        setLongitude(new LiveDataTriggerEvent<>(EMPTY, MAP));
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

        private final MarkupSegment mMarkup;
        private final MarkupEditLineViewModelFactory mAssistedFactory;

        public Factory(MarkupEditLineViewModelFactory assistedFactory, MarkupSegment markup) {
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
    public interface MarkupEditLineViewModelFactory {
        MarkupEditLineViewModel create(MarkupSegment markup);
    }
}
