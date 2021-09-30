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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.databinding.MarkupSymbolItemBinding;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.ui.viewmodel.SymbolPickerViewModel;
import timber.log.Timber;

public class SymbolImageAdapter extends RecyclerView.Adapter<SymbolImageAdapter.SymbolViewHolder> {

    private Context mContext;
    private final PreferencesRepository mPreferences;
    private final SymbolPickerViewModel mViewModel;

    private SymbologyGroup mSymbology = null;

    public SymbolImageAdapter(PreferencesRepository preferences, SymbolPickerViewModel viewModel) {
        mPreferences = preferences;
        mViewModel = viewModel;
    }

    public void setSymbology(SymbologyGroup symbology) {
        mSymbology = symbology;
    }

    @NonNull
    @Override
    public SymbolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();

        MarkupSymbolItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.markup_symbol_item, parent, false);
        binding.markupSymbolView.setOnClickListener(v -> {
            int id = (Integer) v.getTag();
            SymbologyGroup.Symbology pickedSymbology = getItem(id);

            // create a new symbology with the parent path prepended so it can stand alone
            SymbologyGroup.Symbology symbology = new SymbologyGroup.Symbology(
                    pickedSymbology.getDescription(),
                    mSymbology.getListing().getParentPath() + '/' + pickedSymbology.getFilename());
            mViewModel.setSymbologySelection(symbology);
        });
        return new SymbolImageAdapter.SymbolViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SymbolViewHolder holder, int position) {
        holder.binding.setId(position);
        holder.binding.setText(getItem(position).getDescription());
        holder.binding.executePendingBindings();

        Timber.d("FETCHING SYMBOLOGY URL: %s", getItemPath(position));

        //Glide.with(mContext).clear(holder.binding.markupSymbolView);
        //holder.binding.markupSymbolView.setImageResource(R.drawable.x);

        Glide.with(mContext)
                .asDrawable()
                .fitCenter()
                .placeholder(R.drawable.x)
                .error(R.drawable.x)
                .load(getItemPath(position))
                .into(holder.binding.markupSymbolView);

    }

    public String getItemPath(int idx) {
        return mPreferences.getSymbologyURL()
                + mSymbology.getListing().getParentPath()
                + "/"
                + getItem(idx).getFilename();
    }

    public SymbologyGroup.Symbology getItem(int idx) {
        return mSymbology.getListing().getListing().get(idx);
    }

    @Override
    public long getItemId(int idx) {
        return (mSymbology.getSymbologyid() * 1000) + idx;
    }

    @Override
    public int getItemCount() {
        if (mSymbology == null) {
            return 0;
        }
        return mSymbology.getListing().getListing().size();
    }

    public static class SymbolViewHolder extends RecyclerView.ViewHolder {

        private final MarkupSymbolItemBinding binding;

        SymbolViewHolder(MarkupSymbolItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public MarkupSymbolItemBinding getBinding() {
            return binding;
        }
    }
}
