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
package edu.mit.ll.nics.android.ui.fragments.reports;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentEodTaskDescriptionBinding;
import edu.mit.ll.nics.android.ui.fragments.TabFragment;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel;

@AndroidEntryPoint
public class EODReportTaskDescriptionFragment extends TabFragment {

    private FragmentEodTaskDescriptionBinding mBinding;

    public static EODReportTaskDescriptionFragment newInstance() {
        return new EODReportTaskDescriptionFragment();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_eod_task_description.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_eod_task_description, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the shared {@link EODReportViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Initialize dropdowns with options.
        initTaskTypeDropdown();

        // Get the EOD shared view model.
        EODReportViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(EODReportViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(viewModel);
        mBinding.setFragment(this);
    }

    /**
     * Unbind from all xml layouts and cancel any pending dialogs.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private void initTaskTypeDropdown() {
        List<String> options = Arrays.asList(
                getString(R.string.eod_tasktype_uxo_disposal),
                getString(R.string.eod_tasktype_emergency_uxo_response),
                getString(R.string.eod_tasktype_harvest_takeover),
                getString(R.string.eod_tasktype_mine_victim_rescue),
                getString(R.string.eod_tasktype_emergency_assistance),
                getString(R.string.eod_tasktype_blow_in_situ),
                getString(R.string.eod_tasktype_blow_on_range),
                getString(R.string.eod_tasktype_handovered),
                getString(R.string.eod_tasktype_other)
        );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, R.layout.dropdown_item, options);
        mBinding.setTaskTypeAdapter(adapter);
        mBinding.executePendingBindings();
    }

    @Override
    public void refresh() {

    }

    @Override
    public String getTabTitle(Context context) {
        return context.getString(R.string.eod_task_section_displayname);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.eod_task_section_displayname);
    }
}
