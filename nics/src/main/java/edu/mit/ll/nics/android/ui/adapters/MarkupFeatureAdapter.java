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

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.common.primitives.Longs;

import java.util.Collections;
import java.util.List;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.MarkupFeatureItemBinding;
import edu.mit.ll.nics.android.interfaces.MarkupFeatureItemCallback;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

public class MarkupFeatureAdapter extends RecyclerView.Adapter<MarkupFeatureAdapter.MarkupFeatureViewHolder> {

    List<MarkupBaseShape> mMarkupFeatures;
    PreferencesRepository mPreferences;

    @Nullable
    private final MarkupFeatureItemCallback<MarkupBaseShape> mMarkupFeatureClickCallback;

    public MarkupFeatureAdapter(@Nullable MarkupFeatureItemCallback<MarkupBaseShape> clickCallback,
                                PreferencesRepository preferences) {
        mPreferences = preferences;
        mMarkupFeatureClickCallback = clickCallback;
        setHasStableIds(true);
    }

    public void setMarkupFeatures(List<MarkupBaseShape> features) {
        // Sort the features depending on the time they were created.
        if (!emptyCheck(features)) {
            Collections.sort(features, (p1, p2) -> Longs.compare(p2.getLastUpdate(), p1.getLastUpdate()));
        }

        if (mMarkupFeatures == null) {
            mMarkupFeatures = features;
            notifyItemRangeInserted(0, features.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mMarkupFeatures.size();
                }

                @Override
                public int getNewListSize() {
                    return features.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mMarkupFeatures.get(oldItemPosition).getId().equals(features.get(newItemPosition).getId());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    MarkupBaseShape newFeature = features.get(newItemPosition);
                    MarkupBaseShape oldFeature = mMarkupFeatures.get(oldItemPosition);
                    return newFeature.equals(oldFeature);
                }
            });
            mMarkupFeatures = features;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    @NonNull
    public MarkupFeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MarkupFeatureItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.markup_feature_item, parent, false);
        binding.setCallback(mMarkupFeatureClickCallback);
        binding.setReadOnly(!mPreferences.getSelectedCollabroom().doIHaveMarkupPermission(mPreferences.getUserId()));
        return new MarkupFeatureViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MarkupFeatureViewHolder holder, int position) {
        MarkupBaseShape feature = mMarkupFeatures.get(position);
        holder.binding.setFeature(feature);
        holder.binding.executePendingBindings();

        if (feature.getImagePath() != null) {
            Glide.with(holder.binding.getRoot())
                    .asDrawable()
                    .load(mPreferences.getSymbologyURL() + feature.getImagePath())
                    .placeholder(R.drawable.x)
                    .into(holder.binding.markupFeatureThumbnail);
        } else {
            Glide.with(holder.binding.getRoot()).clear(holder.binding.markupFeatureThumbnail);

            Bitmap icon = feature.getIcon();
            holder.binding.markupFeatureThumbnail.setImageBitmap(icon);
        }
    }

    @Override
    public int getItemCount() {
        return mMarkupFeatures == null ? 0 : mMarkupFeatures.size();
    }

    @Override
    public long getItemId(int position) {
        return mMarkupFeatures.get(position).getId();
    }

    public MarkupBaseShape getItem(int position) {
        return mMarkupFeatures.get(position);
    }

    public List<MarkupBaseShape> getMarkupFeatures() {
        return mMarkupFeatures;
    }

    public int getItem(MarkupBaseShape shape) {
        return mMarkupFeatures.indexOf(shape);
    }

    static class MarkupFeatureViewHolder extends RecyclerView.ViewHolder {

        final MarkupFeatureItemBinding binding;

        public MarkupFeatureViewHolder(MarkupFeatureItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

