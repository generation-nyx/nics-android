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
package edu.mit.ll.nics.android.ui.viewmodel;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;

public class GeneralMessageViewModel extends ViewModel  {

    private Uri mPhotoUri;
    private int mCurrentImageRotation = 0;
    private final MutableLiveData<Bitmap> mBitmap;
    private final MutableLiveData<String> mDescription;
    private final MutableLiveData<String> mLatitude;
    private final MutableLiveData<String> mLongitude;
    private final MutableLiveData<Boolean> mIsDraft;
    private final LiveData<String> mCoordinateRepresentation;
    private final NonNullMutableLiveData<Boolean> mIsLoading = new NonNullMutableLiveData<>(false);

    @AssistedInject
    public GeneralMessageViewModel(@Assisted GeneralMessage generalMessage, @Assisted Bitmap bitmap,
                                   SettingsRepository settings) {
        mDescription = new MutableLiveData<>(getValueOrDefault(generalMessage.getDescription(), EMPTY));
        mLatitude = new MutableLiveData<>(getValueOrDefault(String.valueOf(generalMessage.getLatitude()), "0.0"));
        mLongitude = new MutableLiveData<>(getValueOrDefault(String.valueOf(generalMessage.getLongitude()), "0.0"));
        mIsDraft = new MutableLiveData<>(generalMessage.isDraft());
        mBitmap = new MutableLiveData<>(bitmap);
        mCoordinateRepresentation = settings.getCoordinateRepresentationLiveData();
    }

    public MutableLiveData<String> getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription.postValue(description);
    }

    public MutableLiveData<String> getLatitude() {
        return mLatitude;
    }

    public void setLatitude(String latitude) {
        mLatitude.postValue(latitude);
    }

    public MutableLiveData<String> getLongitude() {
        return mLongitude;
    }

    public void setLongitude(String longitude) {
        mLongitude.postValue(longitude);
    }

    public MutableLiveData<Boolean> isDraft() {
        return mIsDraft;
    }

    public void setDraft(boolean isDraft) {
        mIsDraft.postValue(isDraft);
    }

    public NonNullMutableLiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public void setLoading(boolean isLoading) {
        mIsLoading.postValue(isLoading);
    }

    public Uri getPhotoUri() {
        return mPhotoUri;
    }

    public void setPhotoUri(Uri uri) {
        mPhotoUri = uri;
    }

    public int getCurrentImageRotation() {
        return mCurrentImageRotation;
    }

    public void setCurrentImageRotation(int imageRotation) {
        mCurrentImageRotation = imageRotation;
    }

    public MutableLiveData<Bitmap> getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap.postValue(bitmap);
    }

    public LiveData<String> getCoordinateRepresentation() {
        return mCoordinateRepresentation;
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Bitmap mBitmap;
        private final GeneralMessage mGeneralMessage;
        private final GeneralMessageViewModelFactory mAssistedFactory;

        public Factory(GeneralMessageViewModelFactory assistedFactory,
                       GeneralMessage generalMessage,
                       Bitmap bitmap) {
            mBitmap = bitmap;
            mAssistedFactory = assistedFactory;
            mGeneralMessage = generalMessage;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) mAssistedFactory.create(mGeneralMessage, mBitmap);
        }
    }

    @AssistedFactory
    public interface GeneralMessageViewModelFactory {
        GeneralMessageViewModel create(GeneralMessage markup, Bitmap bitmap);
    }
}
