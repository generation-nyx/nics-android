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
import androidx.work.WorkInfo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.databinding.DialogTrackingLayersBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.ui.adapters.TrackingLayersAdapter;
import edu.mit.ll.nics.android.ui.viewmodel.TrackingLayersViewModel;

import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_TRACKING_LAYERS_WORKER;

@AndroidEntryPoint
public class TrackingLayersDialog extends AppDialog {

    private TrackingLayersAdapter mAdapter;
    private TrackingLayersViewModel mViewModel;
    private DialogTrackingLayersBinding mBinding;

    @Inject
    TrackingLayerRepository mRepository;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshLayers();
    }

    @Override
    public void onStart() {
        super.onStart();
        setDimensionsPercent(90);
    }

    /**
     * Bind to the layout for this dialog.
     *
     * The layout resource file for this dialog is located at
     * nics/src/main/res/layout/dialog_tracking_layers.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link TrackingLayersViewModel} for this dialog.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(TrackingLayersViewModel.class);

        mBinding.setLifecycleOwner(mLifecycleOwner);

        mAdapter = new TrackingLayersAdapter(mTrackingLayerClickCallback);
        mBinding.setAdapter(mAdapter);
        mBinding.setDialog(this);
        mBinding.setViewModel(mViewModel);

        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            refreshLayers();
            mBinding.swipeRefresh.setRefreshing(false);
        });

        subscribeToModel();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_tracking_layers, null, false);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.tracking_layer_title)
                .setIcon(R.drawable.nics_logo)
                .setView(mBinding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void subscribeToModel() {
        mViewModel.getTrackingLayers().observe(mLifecycleOwner, mAdapter::setTrackingLayers);

        // Observe the tracking layers GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_TRACKING_LAYERS_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoading(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });
    }

    /**
     * The {@link RecyclerViewItemCallback <Tracking>} that is registered for the binded {@link TrackingLayersAdapter}.
     *
     * When a tracking layer is selected, toggle the checkbox for displaying the tracking layer on the map.
     */
    private final RecyclerViewItemCallback<Tracking> mTrackingLayerClickCallback = trackingLayer -> {
        trackingLayer.toggle();
        mRepository.updateTrackingLayer(trackingLayer);
    };

    private void refreshLayers() {
        mNetworkRepository.getTrackingLayers();
    }
}
