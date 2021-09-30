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

import com.google.common.primitives.Longs;

import java.util.Collections;
import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.databinding.CollabroomItemBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

public class CollabroomAdapter extends RecyclerView.Adapter<CollabroomAdapter.CollabroomViewHolder> {

    List<? extends Collabroom> mCollabrooms;

    @Nullable
    private final RecyclerViewItemCallback<Collabroom> mCollabroomClickCallback;

    public CollabroomAdapter(@Nullable RecyclerViewItemCallback<Collabroom> clickCallback) {
        mCollabroomClickCallback = clickCallback;
        setHasStableIds(true);
    }

    public void setCollabrooms(List<? extends Collabroom> collabrooms) {
        // Sort the collabrooms by their creation date.
        if (!emptyCheck(collabrooms)) {
            Collections.sort(collabrooms, (p1, p2) -> Longs.compare(Long.parseLong(p2.getCreated()), Long.parseLong(p1.getCreated())));
        }

        if (mCollabrooms == null) {
            mCollabrooms = collabrooms;
            notifyItemRangeInserted(0, collabrooms.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mCollabrooms.size();
                }

                @Override
                public int getNewListSize() {
                    return collabrooms.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mCollabrooms.get(oldItemPosition).getCollabRoomId() == collabrooms.get(newItemPosition).getCollabRoomId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Collabroom newCollabroom = collabrooms.get(newItemPosition);
                    Collabroom oldCollabroom = mCollabrooms.get(oldItemPosition);
                    return newCollabroom.equals(oldCollabroom);
                }
            });
            mCollabrooms = collabrooms;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    @NonNull
    public CollabroomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CollabroomItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.collabroom_item, parent, false);
        binding.setCallback(mCollabroomClickCallback);
        return new CollabroomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CollabroomViewHolder holder, int position) {
        holder.binding.setCollabroom(mCollabrooms.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mCollabrooms == null ? 0 : mCollabrooms.size();
    }

    @Override
    public long getItemId(int position) {
        return mCollabrooms.get(position).getCollabRoomId();
    }

    static class CollabroomViewHolder extends RecyclerView.ViewHolder {

        final CollabroomItemBinding binding;

        public CollabroomViewHolder(CollabroomItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

