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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.databinding.DialogOrganizationsBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;
import edu.mit.ll.nics.android.ui.adapters.OrganizationAdapter;
import edu.mit.ll.nics.android.ui.viewmodel.OrganizationsViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;

import static edu.mit.ll.nics.android.utils.constants.Events.LOGOUT;

@AndroidEntryPoint
public class OrganizationsGlobalDialog extends AppDialog {

    private OrganizationsViewModel mViewModel;
    private DialogOrganizationsBinding mBinding;
    private boolean mEventsRegistered = false;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        mViewModel = new ViewModelProvider(this).get(OrganizationsViewModel.class);

        mBinding.setLifecycleOwner(mLifecycleOwner);

        OrganizationAdapter adapter = new OrganizationAdapter(mClickCallback);
        mBinding.setAdapter(adapter);

        subscribeToModel(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mEventsRegistered) {
            LiveDataBus.subscribe(LOGOUT, this, data -> dismiss());
            mEventsRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mEventsRegistered) {
            LiveDataBus.unregister(LOGOUT);
            mEventsRegistered = false;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_organizations, null, false);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.select_an_organization)
                .setIcon(R.drawable.nics_logo)
                .setView(mBinding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void subscribeToModel(OrganizationAdapter adapter) {
        mViewModel.getOrganizations().observe(mLifecycleOwner, organizations -> {
            adapter.setOrganizations(organizations);
            mBinding.executePendingBindings();
        });
    }

    private final RecyclerViewItemCallback<Organization> mClickCallback = organization -> {
        mPreferences.setSelectedOrganization(organization);
        dismiss();
    };
}
