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
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.databinding.TrackingLayerItemBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

public class TrackingLayersAdapter extends RecyclerView.Adapter<TrackingLayersAdapter.TrackingLayerViewHolder> {

    List<? extends Tracking> mTrackingLayers;

    @Nullable
    private final RecyclerViewItemCallback<Tracking> mTrackingLayerClickCallback;

    public TrackingLayersAdapter(@Nullable RecyclerViewItemCallback<Tracking> clickCallback) {
        mTrackingLayerClickCallback = clickCallback;
        setHasStableIds(true);
    }

    public void setTrackingLayers(List<? extends Tracking> trackingLayers) {
        // Sort the layers depending on display name.
        if (!emptyCheck(trackingLayers)) {
            Collections.sort(trackingLayers, (Comparator<Tracking>) (t1, t2) -> t1.getDisplayName().compareToIgnoreCase(t2.getDisplayName()));
        }

        if (mTrackingLayers == null) {
            mTrackingLayers = trackingLayers;
            notifyItemRangeInserted(0, trackingLayers.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mTrackingLayers.size();
                }

                @Override
                public int getNewListSize() {
                    return trackingLayers.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mTrackingLayers.get(oldItemPosition).equals(trackingLayers.get(newItemPosition));
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return mTrackingLayers.get(oldItemPosition).equals(trackingLayers.get(newItemPosition));
                }
            });
            mTrackingLayers = trackingLayers;
            result.dispatchUpdatesTo(this);
            notifyDataSetChanged();
        }
    }

    @Override
    @NonNull
    public TrackingLayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TrackingLayerItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.tracking_layer_item, parent, false);
        binding.setCallback(mTrackingLayerClickCallback);
        return new TrackingLayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackingLayerViewHolder holder, int position) {
        holder.binding.setTracking(mTrackingLayers.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mTrackingLayers == null ? 0 : mTrackingLayers.size();
    }

    @Override
    public long getItemId(int position) {
        return mTrackingLayers.get(position).getId();
    }

    static class TrackingLayerViewHolder extends RecyclerView.ViewHolder {

        final TrackingLayerItemBinding binding;

        public TrackingLayerViewHolder(TrackingLayerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
