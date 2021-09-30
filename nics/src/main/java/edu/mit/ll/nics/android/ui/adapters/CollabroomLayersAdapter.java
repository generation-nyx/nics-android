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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.databinding.CollabroomLayerItemBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

public class CollabroomLayersAdapter extends RecyclerView.Adapter<CollabroomLayersAdapter.CollabroomLayerViewHolder> {

    List<? extends CollabroomDataLayer> mCollabroomLayers;

    @Nullable
    private final RecyclerViewItemCallback<CollabroomDataLayer> mCollabroomLayerClickCallback;

    public CollabroomLayersAdapter(@Nullable RecyclerViewItemCallback<CollabroomDataLayer> clickCallback) {
        mCollabroomLayerClickCallback = clickCallback;
        setHasStableIds(true);
    }

    public void setCollabroomLayers(List<? extends CollabroomDataLayer> layers) {
        // Sort the layers depending on display name.
        if (!emptyCheck(layers)) {
            Collections.sort(layers, (Comparator<CollabroomDataLayer>) (t1, t2) -> t1.getDisplayName().compareToIgnoreCase(t2.getDisplayName()));
        }

        if (mCollabroomLayers == null) {
            mCollabroomLayers = layers;
            notifyItemRangeInserted(0, layers.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mCollabroomLayers.size();
                }

                @Override
                public int getNewListSize() {
                    return layers.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mCollabroomLayers.get(oldItemPosition).equals(layers.get(newItemPosition));
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return mCollabroomLayers.get(oldItemPosition).equals(layers.get(newItemPosition));
                }
            });
            mCollabroomLayers = layers;
            result.dispatchUpdatesTo(this);
            notifyDataSetChanged();
        }
    }

    @Override
    @NonNull
    public CollabroomLayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CollabroomLayerItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.collabroom_layer_item, parent, false);
        binding.setCallback(mCollabroomLayerClickCallback);
        return new CollabroomLayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CollabroomLayerViewHolder holder, int position) {
        holder.binding.setLayer(mCollabroomLayers.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mCollabroomLayers == null ? 0 : mCollabroomLayers.size();
    }

    @Override
    public long getItemId(int position) {
        return mCollabroomLayers.get(position).getId();
    }

    static class CollabroomLayerViewHolder extends RecyclerView.ViewHolder {

        final CollabroomLayerItemBinding binding;

        public CollabroomLayerViewHolder(CollabroomLayerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
