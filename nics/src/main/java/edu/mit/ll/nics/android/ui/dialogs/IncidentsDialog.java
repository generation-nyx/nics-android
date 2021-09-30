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

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.WorkInfo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.databinding.DialogIncidentsBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;
import edu.mit.ll.nics.android.ui.adapters.IncidentAdapter;
import edu.mit.ll.nics.android.ui.viewmodel.IncidentsViewModel;

import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_ALL_INCIDENTS_WORKER;

@AndroidEntryPoint
public class IncidentsDialog extends AppDialog {

    private IncidentsViewModel mViewModel;
    private DialogIncidentsBinding mBinding;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshIncidents();
    }

    @Override
    public void onStart() {
        super.onStart();
        setDimensionsPercent(90);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view.
        mNavController = NavHostFragment.findNavController(this);
        mViewModel = new ViewModelProvider(this).get(IncidentsViewModel.class);

        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setDialog(this);

        IncidentAdapter adapter = new IncidentAdapter(mClickCallback);
        mBinding.setAdapter(adapter);

        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            refreshIncidents();
            mBinding.swipeRefresh.setRefreshing(false);
        });

        subscribeToModel(adapter);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_incidents, null, false);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.select_an_incident)
                .setIcon(R.drawable.nics_logo)
                .setView(mBinding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void subscribeToModel(IncidentAdapter adapter) {
        mViewModel.getIncidents().observe(mLifecycleOwner, incidents -> {
            adapter.setIncidents(incidents);
            mBinding.executePendingBindings();
        });

        // Observe the incidents GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_ALL_INCIDENTS_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoading(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });
    }

    /**
     * The {@link RecyclerViewItemCallback <Incident>} that is registered for the binded {@link IncidentAdapter}.
     * When an incident is selected, set that incident in the preferences.
     */
    private final RecyclerViewItemCallback<Incident> mClickCallback = incident -> {
        mPreferences.setSelectedIncident(incident);
        mNetworkRepository.getCollabrooms(mPreferences.getSelectedIncidentId());
        mPreferences.setSelectedCollabroom(null);
        dismiss();
    };

    public void leaveIncident() {
        mPreferences.setSelectedIncident(null);
        mPreferences.setSelectedCollabroom(null);
        dismiss();
    }

    private void refreshIncidents() {
        mNetworkRepository.getIncidents();
    }
}
