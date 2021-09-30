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
import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.databinding.IncidentItemBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    List<? extends Incident> mIncidents;

    @Nullable
    private final RecyclerViewItemCallback<Incident> mIncidentClickCallback;

    public IncidentAdapter(@Nullable RecyclerViewItemCallback<Incident> clickCallback) {
        mIncidentClickCallback = clickCallback;
        setHasStableIds(true);
    }

    public void setIncidents(List<? extends Incident> incidents) {
        // Sort the incidents by their creation date.
        if (!emptyCheck(incidents)) {
            Collections.sort(incidents, (p1, p2) -> Longs.compare(p2.getCreated(), p1.getCreated()));
        }

        if (mIncidents == null) {
            mIncidents = incidents;
            notifyItemRangeInserted(0, incidents.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mIncidents.size();
                }

                @Override
                public int getNewListSize() {
                    return incidents.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mIncidents.get(oldItemPosition).getIncidentId() == incidents.get(newItemPosition).getIncidentId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Incident newIncident = incidents.get(newItemPosition);
                    Incident oldIncident = mIncidents.get(oldItemPosition);
                    return newIncident.equals(oldIncident);
                }
            });
            mIncidents = incidents;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    @NonNull
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        IncidentItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.incident_item, parent, false);
        binding.setCallback(mIncidentClickCallback);
        return new IncidentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        holder.binding.setIncident(mIncidents.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mIncidents == null ? 0 : mIncidents.size();
    }

    @Override
    public long getItemId(int position) {
        return mIncidents.get(position).getIncidentId();
    }

    static class IncidentViewHolder extends RecyclerView.ViewHolder {

        final IncidentItemBinding binding;

        public IncidentViewHolder(IncidentItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
