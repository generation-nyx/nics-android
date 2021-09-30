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
import edu.mit.ll.nics.android.database.entities.Uxo;
import edu.mit.ll.nics.android.databinding.FragmentEodUxoReportsBinding;
import edu.mit.ll.nics.android.interfaces.UxoClickCallback;
import edu.mit.ll.nics.android.ui.adapters.UxoAdapter;
import edu.mit.ll.nics.android.ui.fragments.TabFragment;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel;

@AndroidEntryPoint
public class EODReportUxoReportsFragment extends TabFragment {

    private UxoAdapter mAdapter;
    private EODReportViewModel mViewModel;
    private FragmentEodUxoReportsBinding mBinding;

    public static EODReportUxoReportsFragment newInstance() {
        return new EODReportUxoReportsFragment();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_eod_uxo_reports.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_eod_uxo_reports, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the shared {@link EODReportViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the EOD shared view model.
        mViewModel = new ViewModelProvider(requireParentFragment()).get(EODReportViewModel.class);

        mAdapter = new UxoAdapter(mViewModel, mLifecycleOwner, mDeleteCallback, initUxoTypeDropdown());

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setAdapter(mAdapter);
        mBinding.setFragment(this);

        subscribeToModel();
    }

    private void subscribeToModel() {
        mViewModel.getUxos().observe(mLifecycleOwner, uxos -> {
            mAdapter.setUxos(uxos);
            mAdapter.notifyDataSetChanged();
            mBinding.executePendingBindings();
        });
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private ArrayAdapter<String> initUxoTypeDropdown() {
        List<String> options = Arrays.asList(
                getString(R.string.uxo_type_anti_personnel_mine),
                getString(R.string.uxo_type_anti_tank_mine),
                getString(R.string.uxo_type_hand_grenade),
                getString(R.string.uxo_type_rifle_grenade_heat),
                getString(R.string.uxo_type_anti_personal_rifle_grenade),
                getString(R.string.uxo_type_improvised_devices),
                getString(R.string.uxo_type_mortar_mines),
                getString(R.string.uxo_type_artillery_shells),
                getString(R.string.uxo_type_hand_rocket_launchers),
                getString(R.string.uxo_type_rockets),
                getString(R.string.uxo_type_guided_missiles),
                getString(R.string.uxo_type_aircraft_bombs),
                getString(R.string.uxo_type_cluster_ammunition),
                getString(R.string.uxo_type_infantry_ammunition),
                getString(R.string.uxo_type_anti_aircraft_ammunition),
                getString(R.string.uxo_type_ammunition_for_anti_aircraft_gun),
                getString(R.string.uxo_type_explosive_stic_fuses_in_m),
                getString(R.string.uxo_type_explosive_in_gr),
                getString(R.string.uxo_type_fuses_and_detonators),
                getString(R.string.uxo_type_other)
        );
        return new ArrayAdapter<>(mActivity, R.layout.dropdown_item, options);
    }

    private final UxoClickCallback mDeleteCallback = new UxoClickCallback() {
        @Override
        public void onClick(Uxo uxo) {
            mViewModel.deleteUxo(uxo);
        }
    };

    public void addUxo() {
        mViewModel.addUxo();
    }

    @Override
    public void refresh() {
        mAdapter.setUxos(mViewModel.getUxos().getValue());
        mAdapter.notifyDataSetChanged();
        mBinding.executePendingBindings();
    }

    @Override
    public String getTabTitle(Context context) {
        return context.getString(R.string.eod_uxo_reports_displayname);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.eod_uxo_reports_displayname);
    }
}
