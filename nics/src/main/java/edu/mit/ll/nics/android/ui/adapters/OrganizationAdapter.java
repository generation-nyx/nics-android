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
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.databinding.OrganizationItemBinding;
import edu.mit.ll.nics.android.interfaces.RecyclerViewItemCallback;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

public class OrganizationAdapter extends RecyclerView.Adapter<OrganizationAdapter.OrganizationViewHolder> {

    List<? extends Organization> mOrganizations;

    @Nullable
    private final RecyclerViewItemCallback<Organization> mClickCallback;

    public OrganizationAdapter(@Nullable RecyclerViewItemCallback<Organization> clickCallback) {
        mClickCallback = clickCallback;
        setHasStableIds(true);
    }

    public void setOrganizations(List<? extends Organization> organizations) {
        // Sort the organizations depending on name.
        if (!emptyCheck(organizations)) {
            Collections.sort(organizations, (Comparator<Organization>) (w1, w2) -> w1.getName().compareToIgnoreCase(w2.getName()));
        }

        if (mOrganizations == null) {
            mOrganizations = organizations;
            notifyItemRangeInserted(0, organizations.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mOrganizations.size();
                }

                @Override
                public int getNewListSize() {
                    return organizations.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mOrganizations.get(oldItemPosition).getOrgId() == organizations.get(newItemPosition).getOrgId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Organization newOrganization = organizations.get(newItemPosition);
                    Organization oldOrganization = mOrganizations.get(oldItemPosition);
                    return newOrganization.equals(oldOrganization);
                }
            });
            mOrganizations = organizations;
            result.dispatchUpdatesTo(this);
            notifyDataSetChanged();
        }
    }

    @Override
    @NonNull
    public OrganizationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OrganizationItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.organization_item, parent, false);
        binding.setCallback(mClickCallback);
        return new OrganizationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrganizationViewHolder holder, int position) {
        holder.binding.setOrganization(mOrganizations.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mOrganizations == null ? 0 : mOrganizations.size();
    }

    @Override
    public long getItemId(int position) {
        return mOrganizations.get(position).getOrgId();
    }

    static class OrganizationViewHolder extends RecyclerView.ViewHolder {

        final OrganizationItemBinding binding;

        public OrganizationViewHolder(OrganizationItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
