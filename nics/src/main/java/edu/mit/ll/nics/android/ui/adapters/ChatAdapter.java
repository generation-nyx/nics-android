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
import androidx.lifecycle.LifecycleOwner;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.databinding.ChatItemBinding;
import edu.mit.ll.nics.android.interfaces.ChatClickCallback;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.ui.viewmodel.ChatViewModel;

public class ChatAdapter extends PagingDataAdapter<Chat, ChatAdapter.ChatViewHolder> {

    private final PreferencesRepository mPreferences;
    private final LifecycleOwner mLifecycleOwner;
    private final ChatClickCallback mClickCallback;
    private final ChatViewModel mViewModel;

    public ChatAdapter(@Nullable ChatClickCallback clickCallback,
                       @NonNull ChatViewModel viewModel,
                       @NonNull LifecycleOwner lifecycleOwner,
                       @NonNull PreferencesRepository preferences) {
        super(DIFF_CALLBACK);
        mClickCallback = clickCallback;
        mPreferences = preferences;
        mLifecycleOwner = lifecycleOwner;
        mViewModel = viewModel;
    }

    @Override
    @NonNull
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ChatItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.chat_item, parent, false);
        binding.setLifecycleOwner(mLifecycleOwner);
        binding.setCallback(mClickCallback);
        binding.setViewModel(mViewModel);
        binding.setPreferences(mPreferences);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = getItem(position);
        if (chat != null) {
            holder.bind(chat);
        } else {
            holder.clear();
        }
    }

    private static final DiffUtil.ItemCallback<Chat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Chat>() {
                @Override
                public boolean areItemsTheSame(Chat oldChat, Chat newChat) {
                    return oldChat.getChatId() == newChat.getChatId();
                }

                @Override
                public boolean areContentsTheSame(Chat oldChat, @NotNull Chat newChat) {
                    return oldChat.equals(newChat);
                }
            };

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        final ChatItemBinding binding;

        public ChatViewHolder(ChatItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Chat chat) {
            if (chat != null) {
                binding.setChat(chat);
                binding.executePendingBindings();
            }
        }

        void clear() {
            binding.setChat(null);
        }
    }
}
