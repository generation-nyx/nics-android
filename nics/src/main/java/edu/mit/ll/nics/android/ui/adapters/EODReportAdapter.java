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
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
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
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.databinding.EodReportItemBinding;
import edu.mit.ll.nics.android.interfaces.ClickCallback;
import edu.mit.ll.nics.android.interfaces.ReportClickCallback;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportListViewModel;

public class EODReportAdapter extends ReportAdapter<EODReport, EODReportAdapter.EODReportViewHolder> {

    private final LifecycleOwner mLifecycleOwner;
    private final EODReportListViewModel mViewModel;
    private final Picasso mPicasso;
    private final PreferencesRepository mPreferences;

    @Nullable
    protected final ClickCallback mRetryCallback;

    @AssistedInject
    public EODReportAdapter(@Assisted @Nullable ReportClickCallback<EODReport> clickCallback,
                            @Assisted @Nullable ClickCallback retryCallback,
                            @Assisted EODReportListViewModel viewModel,
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
    public EODReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EodReportItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.eod_report_item, parent, false);
        binding.setLifecycleOwner(mLifecycleOwner);
        binding.setCallback(mReportClickCallback);
        binding.setViewModel(mViewModel);
        binding.setPicasso(mPicasso);
        binding.setRetry(mRetryCallback);
        binding.setPreferences(mPreferences);
        return new EODReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EODReportViewHolder holder, int position) {
        EODReport report = getItem(position);
        if (report != null) {
            holder.bind(report);
        } else {
            holder.clear();
        }
    }

//    @Override
//    public void onBindViewHolder(@NonNull EODReportViewHolder holder, int position, List<Object> payloads) {
//        if(!payloads.isEmpty()) {
//            if (payloads.get(0) instanceof Integer) {
//                holder.setProgress((Integer) payloads.get(0));
//            }
//        } else {
//            super.onBindViewHolder(holder, position, payloads);
//        }
//    }

    public static class EODReportViewHolder extends RecyclerView.ViewHolder {

        final EodReportItemBinding binding;

        public EODReportViewHolder(EodReportItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EODReport report) {
            if (report != null) {
                binding.setReport(report);

                binding.arrowButton.setOnClickListener(view -> {
                    if (binding.extras.getVisibility() == View.VISIBLE) {
                        TransitionManager.beginDelayedTransition(binding.layout, new AutoTransition());
                        binding.extras.setVisibility(View.GONE);
                        binding.arrowButton.setIconResource(R.drawable.expand_more);
                    } else {
                        TransitionManager.beginDelayedTransition(binding.layout, null);
                        binding.extras.setVisibility(View.VISIBLE);
                        binding.arrowButton.setIconResource(R.drawable.expand_less);
                    }
                });

                binding.executePendingBindings();
            }
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

        void clear() {
            binding.setReport(null);
        }
    }

    @AssistedFactory
    public interface EODReportAdapterFactory {
        EODReportAdapter create(ReportClickCallback<EODReport> clickCallback,
                                ClickCallback retryCallback,
                                EODReportListViewModel viewModel,
                                LifecycleOwner lifecycleOwner);
    }
}
