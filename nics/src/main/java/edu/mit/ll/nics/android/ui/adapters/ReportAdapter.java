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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import edu.mit.ll.nics.android.database.entities.Report;
import edu.mit.ll.nics.android.interfaces.ReportClickCallback;

public abstract class ReportAdapter<T extends Report, VH extends RecyclerView.ViewHolder> extends PagingDataAdapter<T, VH> {

    @Nullable
    protected final ReportClickCallback<T> mReportClickCallback;

    public ReportAdapter(@Nullable ReportClickCallback<T> clickCallback) {
        super(getDiffCallback());
        mReportClickCallback = clickCallback;
    }

    private static <D extends Report> DiffUtil.ItemCallback<D> getDiffCallback() {
        return new DiffUtil.ItemCallback<D>() {
            @Override
            public boolean areItemsTheSame(@NotNull D oldReport, @NotNull D newReport) {
                return oldReport.getId() == newReport.getId();
            }

            @Override
            public boolean areContentsTheSame(@NotNull D oldReport, @NotNull D newReport) {
                return oldReport.equals(newReport);
            }
        };
    }

    static abstract class ReportViewHolder extends RecyclerView.ViewHolder {

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

