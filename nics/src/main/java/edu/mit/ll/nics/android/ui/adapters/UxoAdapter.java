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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Uxo;
import edu.mit.ll.nics.android.databinding.UxoItemBinding;
import edu.mit.ll.nics.android.interfaces.UxoClickCallback;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel;

public class UxoAdapter extends RecyclerView.Adapter<UxoAdapter.UxoViewHolder> {

    private List<Uxo> mUxos;
    private RecyclerView mRecyclerView;
    private final LifecycleOwner mLifecycleOwner;
    private final EODReportViewModel mViewModel;
    private final ArrayAdapter<String> mUxoTypes;
    protected final UxoClickCallback mDeleteCallback;

    public UxoAdapter(@NonNull EODReportViewModel viewModel,
                      @NonNull LifecycleOwner lifecycleOwner,
                      @NonNull UxoClickCallback deleteCallback,
                      @NonNull ArrayAdapter<String> uxoTypes) {
        mViewModel = viewModel;
        mLifecycleOwner = lifecycleOwner;
        mDeleteCallback = deleteCallback;
        mUxoTypes = uxoTypes;
        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public void setUxos(List<Uxo> uxos) {
        if (mUxos == null) {
            mUxos = uxos;
            notifyItemRangeInserted(0, uxos.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mUxos.size();
                }

                @Override
                public int getNewListSize() {
                    return uxos.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mUxos.get(oldItemPosition).getUxoId() == uxos.get(newItemPosition).getUxoId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Uxo newUxo = uxos.get(newItemPosition);
                    Uxo oldUxo = mUxos.get(oldItemPosition);
                    return newUxo.equals(oldUxo) && newUxo.getUxoId() == oldUxo.getUxoId();
                }
            });
            mUxos = uxos;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    @NonNull
    public UxoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        UxoItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.uxo_item, parent, false);
        return new UxoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UxoViewHolder holder, int position) {
        holder.binding.setLifecycleOwner(mLifecycleOwner);
        holder.binding.setUxo(mUxos.get(position));
        holder.binding.setPosition(position);
        holder.binding.setViewModel(mViewModel);
        holder.binding.setUxoTypeAdapter(mUxoTypes);
        holder.binding.setCallback(mDeleteCallback);
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mUxos == null ? 0 : mUxos.size();
    }

    static class UxoViewHolder extends RecyclerView.ViewHolder {

        final UxoItemBinding binding;

        public UxoViewHolder(UxoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
