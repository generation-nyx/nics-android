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
package edu.mit.ll.nics.android.ui.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import edu.mit.ll.nics.android.di.Qualifiers.MainHandler;
import edu.mit.ll.nics.android.App;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.auth.AuthStateManager;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.interfaces.WorkerCallback;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.services.ServiceManager;

@AndroidEntryPoint
public abstract class AppFragment extends Fragment {

    protected App mApp;
    protected Resources mResources;
    protected FragmentActivity mActivity;
    protected NavBackStackEntry mNavBackStackEntry;
    protected NavController mNavController;
    protected NavHostFragment mNavHostFragment;
    protected LifecycleOwner mLifecycleOwner;

    @MainHandler
    @Inject
    protected Handler mMainHandler;

    @ApplicationContext
    @Inject
    protected Context mContext;

    @Inject
    protected WorkManager mWorkManager;

    @Inject
    protected PreferencesRepository mPreferences;

    @Inject
    protected SettingsRepository mSettings;

    @Inject
    protected NetworkRepository mNetworkRepository;

    @Inject
    AuthStateManager mAuthStateManager;

    @Inject
    protected ServiceManager mServiceManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = requireActivity();
        mApp = ((App) mActivity.getApplication());
        mResources = mActivity.getResources();
        mNavHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        setHasOptionsMenu(true);
    }

    /**
     * Special case for dialog fragments. More info:
     * https://developer.android.com/guide/navigation/navigation-programmatic#additional_considerations
     */
    protected <T> void subscribeToDestinationResponse(int destinationId, String requestKey, DestinationResponse<T> destinationResponse) {
        // Observe the result from the dialog.
        mNavBackStackEntry = mNavController.getBackStackEntry(destinationId);

        // Create our observer and add it to the NavBackStackEntry's lifecycle.
        LifecycleEventObserver observer = (source, event) -> {
            if (event.equals(Lifecycle.Event.ON_RESUME) && mNavBackStackEntry.getSavedStateHandle().contains(requestKey)) {
                try {
                    destinationResponse.onResponse(mNavBackStackEntry.getSavedStateHandle().get(requestKey));
                } catch (ClassCastException ignored) {
                }
            }
        };
        mNavBackStackEntry.getLifecycle().addObserver(observer);

        // As addObserver() does not automatically remove the observer, we
        // call removeObserver() manually when the view lifecycle is destroyed
        getViewLifecycleOwner().getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event.equals(Lifecycle.Event.ON_DESTROY)) {
                mNavBackStackEntry.getLifecycle().removeObserver(observer);
            }
        });
    }

    protected void subscribeToWorker(OneTimeWorkRequest request, WorkerCallback callback) {
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
