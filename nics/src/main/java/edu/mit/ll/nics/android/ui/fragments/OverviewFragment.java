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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentOverviewBinding;
import edu.mit.ll.nics.android.ui.viewmodel.OverviewViewModel;

import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_ALL_INCIDENTS_WORKER;
import static edu.mit.ll.nics.android.workers.Workers.GET_COLLABROOMS_WORKER;

import com.google.android.material.badge.BadgeDrawable;

@AndroidEntryPoint
public class OverviewFragment extends AppFragment {

    private OverviewViewModel mViewModel;
    private FragmentOverviewBinding mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the nav controller for this fragment.
        mNavController = mNavHostFragment.getNavController();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_overview.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_overview, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link OverviewViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        // Initialize the overview view model.
        mViewModel = new ViewModelProvider(this).get(OverviewViewModel.class);

        // Bind to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setPreferences(mPreferences);
        mBinding.setChatBadge(BadgeDrawable.create(requireContext()));
        mBinding.setEodBadge(BadgeDrawable.create(requireContext()));
        mBinding.setGeneralMessageBadge(BadgeDrawable.create(requireContext()));
        mBinding.setFragment(this);

        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            refresh();
            mBinding.swipeRefresh.setRefreshing(false);
        });

        subscribeToModel();
    }

    private void subscribeToModel() {
        // Observe the collabrooms GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosByTagLiveData(GET_COLLABROOMS_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoadingCollabrooms(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });

        //TODO add progress bar for incidents too.
        // Observe the incidents GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_ALL_INCIDENTS_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoadingIncidents(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        mServiceManager.getLocationService();
        mServiceManager.getGeofenceService();
        mServiceManager.getPollingService();
        refresh();
    }

    private void refresh() {
        mNetworkRepository.refreshAllContent();
    }

    public void openEODReportFragment() {
        navigateSafe(mNavController, OverviewFragmentDirections.openEODReportList());
    }

    public void openMapFragment() {
        navigateSafe(mNavController, OverviewFragmentDirections.openMap());
    }

    public void openChatFragment() {
        navigateSafe(mNavController, OverviewFragmentDirections.openChat());
    }

    public void openGeneralMessageFragment() {
        navigateSafe(mNavController, OverviewFragmentDirections.openGeneralMessageList());
    }

    public void showIncidentDialog() {
        navigateSafe(mNavController, OverviewFragmentDirections.openIncidentsDialog());
    }

    public void showRoomDialog() {
        navigateSafe(mNavController, OverviewFragmentDirections.openCollabroomsDialog());
    }
}