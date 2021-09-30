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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.Uxo;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class EODReportViewModel extends ViewModel  {

    private Uri mPhotoUri;
    private long mCurrentId = 0;
    private int mCurrentImageRotation = 0;
    private final EODReport mEODReport;
    private final MutableLiveData<Bitmap> mBitmap;
    private final MutableLiveData<String> mLatitude;
    private final MutableLiveData<String> mLongitude;
    private final MutableLiveData<Boolean> mIsDraft;
    private final MutableLiveData<String> mSelectedTeam;
    private final MutableLiveData<String> mSelectedCanton;
    private final MutableLiveData<String> mSelectedTown;
    private final MutableLiveData<String> mSelectedTaskType;
    private final MutableLiveData<String> mMacID;
    private final MutableLiveData<String> mMedevacPointTimeDistance;
    private final MutableLiveData<String> mContactPerson;
    private final MutableLiveData<String> mContactPhone;
    private final MutableLiveData<String> mContactAddress;
    private final MutableLiveData<String> mRemarks;
    private final MutableLiveData<String> mExpendedResources;
    private final MutableLiveData<String> mDirectlyInvolved;
    private final NonNullMutableLiveData<List<Uxo>> mUxos;
    private final MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);

    @AssistedInject
    public EODReportViewModel(@Assisted EODReport eodReport, @Assisted Bitmap bitmap) {
        mEODReport = eodReport;

        // Initialize the live data objects with the EOD report properties.
        mLatitude = new MutableLiveData<>(getValueOrDefault(String.valueOf(eodReport.getLatitude()), "0.0"));
        mLongitude = new MutableLiveData<>(getValueOrDefault(String.valueOf(eodReport.getLongitude()), "0.0"));
        mIsDraft = new MutableLiveData<>(eodReport.isDraft());
        mSelectedTeam = new MutableLiveData<>(getValueOrDefault(eodReport.getTeam(), null));
        mSelectedCanton = new MutableLiveData<>(getValueOrDefault(eodReport.getCanton(), null));
        mSelectedTown = new MutableLiveData<>(getValueOrDefault(eodReport.getTown(), null));
        mSelectedTaskType = new MutableLiveData<>(getValueOrDefault(eodReport.getTaskType(), null));
        mMacID = new MutableLiveData<>(getValueOrDefault(eodReport.getMacID(), EMPTY));
        mMedevacPointTimeDistance = new MutableLiveData<>(getValueOrDefault(eodReport.getMedevacPointTimeDistance(), EMPTY));
        mContactPerson = new MutableLiveData<>(getValueOrDefault(eodReport.getContactPerson(), EMPTY));
        mContactPhone = new MutableLiveData<>(getValueOrDefault(eodReport.getContactPhone(), EMPTY));
        mContactAddress = new MutableLiveData<>(getValueOrDefault(eodReport.getContactAddress(), EMPTY));
        mRemarks = new MutableLiveData<>(getValueOrDefault(eodReport.getRemarks(), EMPTY));
        mExpendedResources = new MutableLiveData<>(getValueOrDefault(eodReport.getExpendedResources(), EMPTY));
        mDirectlyInvolved = new MutableLiveData<>(getValueOrDefault(eodReport.getDirectlyInvolved(), EMPTY));
        mBitmap = new MutableLiveData<>(bitmap);
        mUxos = new NonNullMutableLiveData<>(getValueOrDefault(eodReport.getUxo(), new ArrayList<>()));
    }

    public EODReport getEODReport() {
        return mEODReport;
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

    public MutableLiveData<Boolean> isLoading() {
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

    public MutableLiveData<String> getSelectedTeam() {
        return mSelectedTeam;
    }

    public void setSelectedTeam(String team) {
        mSelectedTeam.postValue(team);
    }

    public MutableLiveData<String> getSelectedCanton() {
        return mSelectedCanton;
    }

    public void setSelectedCanton(String canton) {
        mSelectedCanton.postValue(canton);
    }

    public MutableLiveData<String> getSelectedTown() {
        return mSelectedTown;
    }

    public void setSelectedTown(String town) {
        mSelectedTown.postValue(town);
    }

    public MutableLiveData<String> getSelectedTaskType() {
        return mSelectedTaskType;
    }

    public void setSelectedTaskType(String taskType) {
        mSelectedTaskType.postValue(taskType);
    }

    public MutableLiveData<String> getMacID() {
        return mMacID;
    }

    public void setMacID(String macID) {
        mMacID.postValue(macID);
    }

    public MutableLiveData<String> getMedevacPointTimeDistance() {
        return mMedevacPointTimeDistance;
    }

    public void setMedevacPointTimeDistance(String medevacPointTimeDistance) {
        mMedevacPointTimeDistance.postValue(medevacPointTimeDistance);
    }

    public MutableLiveData<String> getContactPerson() {
        return mContactPerson;
    }

    public void setContactPerson(String contactPerson) {
        mContactPerson.postValue(contactPerson);
    }

    public MutableLiveData<String> getContactPhone() {
        return mContactPhone;
    }

    public void setContactPhone(String contactPhone) {
        mContactPhone.postValue(contactPhone);
    }

    public MutableLiveData<String> getContactAddress() {
        return mContactAddress;
    }

    public void setContactAddress(String contactAddress) {
        mContactAddress.postValue(contactAddress);
    }

    public MutableLiveData<String> getRemarks() {
        return mRemarks;
    }

    public void setRemarks(String remarks) {
        mRemarks.postValue(remarks);
    }

    public MutableLiveData<String> getExpendedResources() {
        return mExpendedResources;
    }

    public void setExpendedResources(String expendedResources) {
        mExpendedResources.postValue(expendedResources);
    }

    public MutableLiveData<String> getDirectlyInvolved() {
        return mDirectlyInvolved;
    }

    public void setDirectlyInvolved(String directlyInvolved) {
        mDirectlyInvolved.postValue(directlyInvolved);
    }

    public NonNullMutableLiveData<List<Uxo>> getUxos() {
        return mUxos;
    }

    public void setUxos(ArrayList<Uxo> uxos) {
        mUxos.postValue(uxos);
    }

    public void deleteUxo(Uxo uxo) {
        List<Uxo> uxos = mUxos.getValue();
        if (!emptyCheck(uxos)) {
            uxos.remove(uxo);
        }
        mUxos.postValue(uxos);
    }

    public void addUxo() {
        List<Uxo> uxos = mUxos.getValue();
        uxos.add(new Uxo(mCurrentId));
        mCurrentId++;
        mUxos.postValue(uxos);
    }

    public void setUxoType(View view, int position) {
        try {
            List<Uxo> uxos = mUxos.getValue();
            if (uxos.size() > position) {
                uxos.get(position).setUxoType(((MaterialTextView) view).getText().toString());
            }
            mUxos.postValue(uxos);
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to set uxo type.");
        }
    }

    public void setUxoModel(CharSequence text, int position) {
        List<Uxo> uxos = mUxos.getValue();
        if (uxos.size() > position) {
            uxos.get(position).setModel(text.toString());
        }
        mUxos.postValue(uxos);
    }

    public void setUxoCal(CharSequence text, int position) {
        List<Uxo> uxos = mUxos.getValue();
        if (uxos.size() > position) {
            uxos.get(position).setCal(text.toString());
        }
        mUxos.postValue(uxos);
    }

    public void setUxoQuantity(CharSequence text, int position) {
        List<Uxo> uxos = mUxos.getValue();
        if (uxos.size() > position) {
            uxos.get(position).setQuantity(text.toString());
        }
        mUxos.postValue(uxos);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Bitmap mBitmap;
        private final EODReport mEODReport;
        private final EODReportViewModelFactory mAssistedFactory;

        public Factory(EODReportViewModelFactory assistedFactory,
                       EODReport eodReport,
                       Bitmap bitmap) {
            mBitmap = bitmap;
            mAssistedFactory = assistedFactory;
            mEODReport = eodReport;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) mAssistedFactory.create(mEODReport, mBitmap);
        }
    }

    @AssistedFactory
    public interface EODReportViewModelFactory {
        EODReportViewModel create(EODReport markup, Bitmap bitmap);
    }
}
