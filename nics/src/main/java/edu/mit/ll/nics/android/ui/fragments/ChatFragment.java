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
package edu.mit.ll.nics.android.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.databinding.FragmentChatBinding;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.interfaces.ChatClickCallback;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.ui.adapters.ChatAdapter;
import edu.mit.ll.nics.android.ui.viewmodel.ChatViewModel;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.NICS.DATE_PICKER;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_CHAT_MESSAGES_WORKER;

@AndroidEntryPoint
public class ChatFragment extends AppFragment {

    private ChatViewModel mViewModel;
    private FragmentChatBinding mBinding;

    @Inject
    ChatRepository mRepository;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the nav controller for this fragment.
        mNavController = mNavHostFragment.getNavController();

        // Request an update of the most recent chat messages.
        refresh();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_chat.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link ChatViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Bind the lifecycle owner, viewmodel, and the fragment to the xml layout.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setReadOnly(!mPreferences.getSelectedCollabroom().doIHaveMarkupPermission(mPreferences.getUserId()));
        mBinding.setFragment(this);

        // Initialize and bind the paged list adapter to the xml layout.
        ChatAdapter adapter = new ChatAdapter(mClickCallback, mViewModel, mLifecycleOwner, mPreferences);
        mBinding.setAdapter(adapter);
        mBinding.executePendingBindings();

        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            refresh();
            mBinding.swipeRefresh.setRefreshing(false);
        });

        subscribeToModel(adapter);
    }

    /**
     * Subscribe this fragment to the {@link ChatViewModel} to observe changes and dynamically
     * update the UI components.
     *
     * @param adapter The {@link ChatAdapter} for the chat {@link androidx.recyclerview.widget.RecyclerView}
     */
    private void subscribeToModel(ChatAdapter adapter) {
        mViewModel.getChat().observe(mLifecycleOwner, chats -> {
            // Update the list with the chats.
            adapter.submitData(mLifecycleOwner.getLifecycle(), chats);

            // mark all unread chats as read
            mRepository.markAllRead(mPreferences.getSelectedIncidentId(), mPreferences.getSelectedCollabroomId());
        });

        // Observe the chat GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_CHAT_MESSAGES_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoading(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.details) {

        } else if (id == R.id.search) {
            // Toggle showing the search bar.
            mViewModel.toggleSearching();
        } else if (id == R.id.filter) {
            showDateRangePicker();
        } else if (id == R.id.reverse) {
            // Show a popup menu with different sorting options.
//            showOrderOptions();
        } else if (id == R.id.refresh) {
            // Request an update of the most recent chat messages.
            refresh();
        }

        return super.onOptionsItemSelected(item);
    }

    private final ChatClickCallback mClickCallback = chat -> {

    };

    private void refresh() {
        mNetworkRepository.getChatMessages();
        mNetworkRepository.postChatMessages();
    }

    private void showDateRangePicker() {
        Long startDate = mViewModel.getStartDate().getValue();
        Long endDate = mViewModel.getEndDate().getValue();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startDate)
                .setEnd(endDate)
                .setOpenAt(new DateTime(new Instant(endDate), DateTimeZone.UTC).getMonthOfYear())
                .build();

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select dates")
                        .setSelection(new Pair<>(startDate, endDate))
                        .setCalendarConstraints(constraints)
                        .build();
        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            mViewModel.setStartDate(selection.first);
            mViewModel.setEndDate(selection.second);
        });
        dateRangePicker.show(mActivity.getSupportFragmentManager(), DATE_PICKER);
    }

    public void sendChat() {
        try {
            String message = mViewModel.getChatMessage().getValue().trim();

            if (!message.isEmpty()) {
                Chat chat = new Chat();
                long currentTime = System.currentTimeMillis();
                chat.setMessage(message);
                chat.setCreated(currentTime);
                chat.setLastUpdated(currentTime);
                chat.setUserOrgId(mPreferences.getSelectedOrganization().getUserOrgs().get(0).getUserOrgId());
                chat.setIncidentId(mPreferences.getSelectedIncidentId());
                chat.setUserOrganization(mPreferences.getSelectedOrganization().getUserOrgs().get(0));
                chat.setCollabroomId(mPreferences.getSelectedCollabroomId());
                chat.setSeqNum(currentTime);
                chat.setChatId(currentTime);
                chat.setSendStatus(SendStatus.WAITING_TO_SEND);

                mRepository.addChatToDatabase(chat, result -> mMainHandler.post(() -> mNetworkRepository.postChatMessages()));

                mViewModel.setChatMessage(EMPTY);
            } else {
                Snackbar.make(requireView(), "No chat message to send.", Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to send the chat message.");
        }
    }
}