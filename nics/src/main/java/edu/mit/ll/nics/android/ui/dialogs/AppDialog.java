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
package edu.mit.ll.nics.android.ui.dialogs;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

@AndroidEntryPoint
public class AppDialog extends DialogFragment {

    protected Context mContext;
    protected FragmentActivity mActivity;
    protected LifecycleOwner mLifecycleOwner;
    protected NavHostFragment mNavHostFragment;
    protected NavController mNavController;

    @ApplicationContext
    @Inject
    protected Context mAppContext;

    @Inject
    protected PreferencesRepository mPreferences;

    @Inject
    protected NetworkRepository mNetworkRepository;

    @Inject
    protected WorkManager mWorkManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = requireActivity();
        mContext = getContext();
        mNavHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    }

    protected void setWidthPercent(float percentage) {
        float percent = percentage / 100;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Rect rect = new Rect(0, 0, dm.widthPixels, dm.heightPixels);
        float percentWidth = rect.width() * percent;
        requireDialog().getWindow().setLayout((int) percentWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    protected void setHeightPercent(float percentage) {
        float percent = percentage / 100;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Rect rect = new Rect(0, 0, dm.widthPixels, dm.heightPixels);
        float percentHeight = rect.height() * percent;
        requireDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, (int) percentHeight);
    }

    protected void setDimensionsPercent(float percentage) {
        setDimensionsPercent(percentage, percentage);
    }

    protected void setDimensionsPercent(float widthPercentage, float heightPercentage) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Rect rect = new Rect(0, 0, dm.widthPixels, dm.heightPixels);
        float percentWidth = rect.width() * (widthPercentage / 100);
        float percentHeight = rect.height() * (heightPercentage / 100);
        requireDialog().getWindow().setLayout((int) percentWidth, (int) percentHeight);
    }

    protected void setFullScreen() {
        requireDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    protected void subscribeToWorker(@Nullable OneTimeWorkRequest request, WorkerCallback callback) {
        if (request != null) {
            LiveData<WorkInfo> workInfo = mWorkManager.getWorkInfoByIdLiveData(request.getId());
            workInfo.observe(getViewLifecycleOwner(), info -> {
                if (info != null) {
                    WorkInfo.State state = info.getState();
                    if (state.isFinished()) {
                        if (state == WorkInfo.State.SUCCEEDED) {
                            callback.onSuccess(info);
                        } else if (state == WorkInfo.State.CANCELLED || state == WorkInfo.State.FAILED) {
                            callback.onFailure(info);
                        }

                        // Stop listening to this worker when finished.
                        workInfo.removeObservers(getViewLifecycleOwner());
                    } else {
                        callback.onWorking();
                    }
                }
            });
        }
    }
}
