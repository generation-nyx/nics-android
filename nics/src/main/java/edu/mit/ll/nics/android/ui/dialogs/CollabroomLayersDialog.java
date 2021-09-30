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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.databinding.DialogCollabroomLayersBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;
import edu.mit.ll.nics.android.repository.CollabroomLayerRepository;
import edu.mit.ll.nics.android.ui.adapters.CollabroomLayersAdapter;
import edu.mit.ll.nics.android.ui.fragments.MapFragment;
import edu.mit.ll.nics.android.ui.viewmodel.CollabroomLayersViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;

import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngBoundsFromLayerFeatures;
import static edu.mit.ll.nics.android.utils.Utils.findFragment;
import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_COLLABROOM_LAYERS_WORKER;

@AndroidEntryPoint
public class CollabroomLayersDialog extends AppDialog {

    private GoogleMap mMap;
    private CollabroomLayersAdapter mAdapter;
    private CollabroomLayersViewModel mViewModel;
    private DialogCollabroomLayersBinding mBinding;

    @Inject
    CollabroomLayerRepository mRepository;

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
     * nics/src/main/res/layout/dialog_collabroom_layers.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link CollabroomLayersViewModel} for this dialog.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(CollabroomLayersViewModel.class);

        // Get the view model for this fragment as well as the shared MapViewModel.
        Fragment mapFragment = findFragment(mNavHostFragment, MapFragment.class);
        MapViewModel mapViewModel = new ViewModelProvider(Objects.requireNonNull(mapFragment)).get(MapViewModel.class);
        mMap = mapViewModel.getMap();

        mBinding.setLifecycleOwner(mLifecycleOwner);

        mAdapter = new CollabroomLayersAdapter(mCollabroomLayerClickCallback);
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
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_collabroom_layers, null, false);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.data_layer_title)
                .setIcon(R.drawable.nics_logo)
                .setView(mBinding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private final RecyclerViewItemCallback<CollabroomDataLayer> mCollabroomLayerClickCallback = layer -> {
        layer.toggle();

        if (layer.isActive() && layer.hasFeatures()) {
            try {
                LatLngBounds bounds = getLatLngBoundsFromLayerFeatures(layer.getFeatures());
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
            } catch (IllegalStateException ignored) {
                // Layer doesn't have features.
                Snackbar.make(requireView(), "Layer does not have any features.", Snackbar.LENGTH_SHORT).show();
            }
        }

        mRepository.updateCollabroomLayer(layer);
    };

    private void subscribeToModel() {
        mViewModel.getCollabroomLayers().observe(mLifecycleOwner, mAdapter::setCollabroomLayers);

        // Observe the collabroom layers GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_COLLABROOM_LAYERS_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoading(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });
    }

    private void refreshLayers() {
        mNetworkRepository.getCollabroomLayers();
    }
}
