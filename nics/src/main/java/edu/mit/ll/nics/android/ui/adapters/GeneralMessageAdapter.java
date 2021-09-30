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
package edu.mit.ll.nics.android.ui.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.databinding.GeneralMessageItemBinding;
import edu.mit.ll.nics.android.interfaces.ClickCallback;
import edu.mit.ll.nics.android.interfaces.ReportClickCallback;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.ui.viewmodel.GeneralMessageListViewModel;

public class GeneralMessageAdapter extends ReportAdapter<GeneralMessage, GeneralMessageAdapter.GeneralMessageViewHolder> {

    private final LifecycleOwner mLifecycleOwner;
    private final GeneralMessageListViewModel mViewModel;
    private final Picasso mPicasso;
    private final PreferencesRepository mPreferences;

    @Nullable
    protected final ClickCallback mRetryCallback;

    @AssistedInject
    public GeneralMessageAdapter(@Assisted @Nullable ReportClickCallback<GeneralMessage> clickCallback,
                                 @Assisted @Nullable ClickCallback retryCallback,
                                 @Assisted GeneralMessageListViewModel viewModel,
                                 @Assisted LifecycleOwner lifecycleOwner,
                                 Picasso picasso,
                                 PreferencesRepository preferences) {
        super(clickCallback);

        mViewModel = viewModel;
        mLifecycleOwner = lifecycleOwner;
        mPicasso = picasso;
        mRetryCallback = retryCallback;
        mPreferences = preferences;
    }

    @Override
    @NonNull
    public GeneralMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GeneralMessageItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.general_message_item, parent, false);
        binding.setLifecycleOwner(mLifecycleOwner);
        binding.setCallback(mReportClickCallback);
        binding.setViewModel(mViewModel);
        binding.setPicasso(mPicasso);
        binding.setRetry(mRetryCallback);
        binding.setPreferences(mPreferences);
        return new GeneralMessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GeneralMessageViewHolder holder, int position) {
        GeneralMessage report = getItem(position);
        if (report != null) {
            holder.bind(report);
        } else {
            holder.clear();
        }
    }

    public static class GeneralMessageViewHolder extends RecyclerView.ViewHolder {

        final GeneralMessageItemBinding binding;

        public GeneralMessageViewHolder(GeneralMessageItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GeneralMessage report) {
            if (report != null) {
                binding.setReport(report);
                binding.executePendingBindings();
            }
        }

        void clear() {
            binding.setReport(null);
        }

        public void refreshPicasso() {
            binding.messageImage.invalidate();
        }

        public void setProgress(int progress) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.sendProgress.setProgress(progress, true);
            } else {
                binding.sendProgress.setProgress(progress);
            }
        }
    }

    @AssistedFactory
    public interface GeneralMessageAdapterFactory {
        GeneralMessageAdapter create(ReportClickCallback<GeneralMessage> clickCallback,
                                     ClickCallback retryCallback,
                                     GeneralMessageListViewModel viewModel,
                                     LifecycleOwner lifecycleOwner);
    }
}
