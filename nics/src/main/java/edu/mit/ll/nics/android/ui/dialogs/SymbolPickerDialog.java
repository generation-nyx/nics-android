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
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.databinding.DialogSymbolPickerBinding;
import edu.mit.ll.nics.android.databinding.DialogSymbolPickerGridBinding;
import edu.mit.ll.nics.android.interfaces.SymbolClickCallback;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SymbologyRepository;
import edu.mit.ll.nics.android.ui.adapters.SymbolImageAdapter;
import edu.mit.ll.nics.android.utils.ExtensionsKt;
import edu.mit.ll.nics.android.utils.Utils;
import edu.mit.ll.nics.android.ui.viewmodel.SymbolPickerViewModel;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_SYMBOL_REQUEST;

import javax.inject.Inject;

@AndroidEntryPoint
public class SymbolPickerDialog extends AppDialog {

    private NavController mNavController;
    private SymbolPickerViewModel mViewModel;
    private DialogSymbolPickerBinding mBinding;

    @Inject
    SymbologyRepository mRepository;

    @Inject
    PreferencesRepository mPreferences;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register on back pressed callback.
        mActivity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public void onStart() {
        super.onStart();
        setDimensionsPercent(95);
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/dialog_symbol_picker.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link SymbolPickerViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = NavHostFragment.findNavController(this);

        mViewModel = new ViewModelProvider(this).get(SymbolPickerViewModel.class);

        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);

        mBinding.symbolPickerPager.setAdapter(new TabAdapter(mRepository, mPreferences, mViewModel));

        new TabLayoutMediator(mBinding.symbolPickerLayout, mBinding.symbolPickerPager,
                (tab, position) -> tab.setText(mRepository.getSymbologyGroupNames().get(position))
        ).attach();

        subscribeToModel();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_symbol_picker, null, false);

        mBinding.symbolPickerPager.setOffscreenPageLimit(1);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.symbol_picker))
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
        // Observe the changes in symbology selection for the symbol picker and return result
        mViewModel.getSymbologySelection().observe(mLifecycleOwner, symbologySelection -> {
            if (symbologySelection != null) {
                dismissWithResult(symbologySelection);
            }
        });

    }

    /**
     * Callback method for the symbol that is clicked in the symbol picker dialog.
     */
    private void dismissWithResult (SymbologyGroup.Symbology symbology){
        ExtensionsKt.setNavigationResult(mNavController, PICK_SYMBOL_REQUEST,
                new String[]{ symbology.getFilename(), symbology.getDescription()});
        dismiss();
    }

    private void cancel() {
        ExtensionsKt.setNavigationResult(mNavController, PICK_SYMBOL_REQUEST, null);
        dismiss();
    }

    final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            cancel();
            Utils.popBackStack(mNavController);
        }
    };

    @Override
    public void onCancel(@NonNull @NotNull DialogInterface dialog) {
        ExtensionsKt.setNavigationResult(mNavController, PICK_SYMBOL_REQUEST, null);
        super.onCancel(dialog);
    }

    static class TabAdapter extends RecyclerView.Adapter<TabViewHolder> {

        private final SymbologyRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final SymbolPickerViewModel mViewModel;

        public TabAdapter(SymbologyRepository repository, PreferencesRepository preferences, SymbolPickerViewModel viewModel) {
            mRepository = repository;
            mPreferences = preferences;
            mViewModel = viewModel;
        }

        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            DialogSymbolPickerGridBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.dialog_symbol_picker_grid, parent, false);

            SymbolImageAdapter adapter = new SymbolImageAdapter(mPreferences, mViewModel);
            binding.setAdapter(adapter);
            binding.executePendingBindings();

            binding.symbolPickerGrid.addRecyclerListener(
                    holder -> {
                        SymbolImageAdapter.SymbolViewHolder viewHolder = (SymbolImageAdapter.SymbolViewHolder) holder;
                        Glide.with(parent.getContext()).clear(viewHolder.getBinding().markupSymbolView);
                    });

            return new TabViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
            String name = mRepository.getSymbologyGroupNames().get(position);
            SymbologyGroup symbology = mRepository.getSymbologyByName(name);
            Timber.d("BINDING %s:  %s %s", name, symbology.getName(), symbology.getListing().getListing().size());
            holder.bind(symbology);
        }

        @Override
        public int getItemCount() {
            return mRepository.getSymbologyGroupNames().size();
        }
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {

        private final DialogSymbolPickerGridBinding binding;
        public TabViewHolder(DialogSymbolPickerGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SymbologyGroup symbology) {
            binding.getAdapter().setSymbology(symbology);
            binding.getAdapter().notifyDataSetChanged();
        }
    }
}
