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
package edu.mit.ll.nics.android.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.ChatApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.data.Presence;
import edu.mit.ll.nics.android.data.messages.ChatMessage;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.enums.PresenceStatus;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.PersonalHistoryRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.NetworkUtils.createPartFromString;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class ChatWorkers {

    @HiltWorker
    public static class Get extends AppWorker {

        private final ChatRepository mChatRepository;
        private final PersonalHistoryRepository mPersonalHistory;
        private final PreferencesRepository mPreferences;
        private final ChatApiService mApiService;

        @AssistedInject
        public Get(@Assisted @NonNull Context context,
                   @Assisted @NonNull WorkerParameters workerParams,
                   ChatRepository chatRepository,
                   PersonalHistoryRepository personalHistory,
                   PreferencesRepository preferences,
                   ChatApiService chatApiService) {
            super(context, workerParams);

            mChatRepository = chatRepository;
            mPersonalHistory = personalHistory;
            mPreferences = preferences;
            mApiService = chatApiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            return CallbackToFutureAdapter.getFuture(completer -> {
                long collabroomId = mPreferences.getSelectedCollabroomId();
                long incidentId = mPreferences.getSelectedIncidentId();

                Call<ChatMessage> call = mApiService.getChats(collabroomId, mChatRepository.getLastChatTimestamp(collabroomId) + 1);
                call.enqueue(new AuthCallback<>(new Callback<ChatMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<ChatMessage> call, @NotNull Response<ChatMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                        ChatMessage message = response.body();
                        if (message != null && message.getChats() != null && message.getChats().size() > 0) {
                            parseChatMessages(message);
                            Timber.tag(DEBUG).i("Successfully received chat information for: %s - %s", incidentId, collabroomId);
                        } else {
                            Timber.tag(DEBUG).w("Received empty chat information. Status Code: %s", response.code());
                        }

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<ChatMessage> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to receive chat information for: %s - %s", incidentId, collabroomId);

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }

        private void parseChatMessages(ChatMessage message) {
            int numParsed = 0;
            for (Chat chat : message.getChats()) {
                chat.setIncidentId(mPreferences.getSelectedIncidentId());
                chat.setSendStatus(SendStatus.RECEIVED);
                chat.setNew(true);
                chat.setRead(false);
                mChatRepository.addChatToDatabase(chat);
                numParsed++;
            }

            if (numParsed > 0) {
                mPersonalHistory.addPersonalHistory("Successfully received " + numParsed + " chat messages from " + mPreferences.getSelectedCollabroom().getName(),
                        mPreferences.getUserId(), mPreferences.getUserNickName());
            }
        }
    }

    @HiltWorker
    public static class Post extends AppWorker {

        private final ChatRepository mRepository;
        private final PreferencesRepository mPreferences;
        private final ChatApiService mApiService;

        @AssistedInject
        public Post(@Assisted @NonNull Context context,
                    @Assisted @NonNull WorkerParameters workerParams,
                    ChatRepository chatRepository,
                    PreferencesRepository preferencesRepository,
                    ChatApiService chatApiService) {
            super(context, workerParams);

            mRepository = chatRepository;
            mPreferences = preferencesRepository;
            mApiService = chatApiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            Timber.tag(DEBUG).d("Starting Chat Post Worker.");

            return CallbackToFutureAdapter.getFuture(completer -> {
                long id = getInputData().getLong("id", -1L);
                Chat chat = mRepository.getChatById(id);
                long chatId = chat.getId();

                chat.setSendStatus(SendStatus.SENT);
                mRepository.addChatToDatabase(chat);

                Timber.tag(DEBUG).i("Adding chat " + id + " to send queue.");

                // TODO failed to send implementation.
                RequestBody body = createPartFromString(chat.toJson());
                Call<ChatMessage> call = mApiService.postChat(chat.getCollabroomId(), body);
                call.enqueue(new AuthCallback<>(new Callback<ChatMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<ChatMessage> call, @NotNull Response<ChatMessage> response) {
                        mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                        Timber.tag(DEBUG).i("Successfully posted chat messages");

                        ChatMessage message = response.body();
                        if (message != null && message.getChats() != null && message.getChats().size() > 0) {
                            ArrayList<Chat> chats = message.getChats();
                            Chat chat = chats.get(0);
                            chat.setId(chatId);
                            chat.setSendStatus(SendStatus.SAVED);
                            chat.setIncidentId(mPreferences.getSelectedIncidentId());
                            mRepository.addChatToDatabase(chat);
                        } else {
                            Timber.tag(DEBUG).w("Received empty chat information. Status Code: %s", response.code());
                        }

                        completer.set(Result.success());
                    }

                    @Override
                    public void onFailure(@NotNull Call<ChatMessage> call, @NotNull Throwable t) {
                        Timber.tag(DEBUG).e("Failed to post Chat Feature information: %s", t.getMessage());
                        completer.set(Result.failure());
                    }
                }));

                return Result.success();
            });
        }
    }

    @HiltWorker
    public static class ChatPresence extends AppWorker {

        private final PreferencesRepository mPreferences;
        private final ChatApiService mApiService;

        @AssistedInject
        public ChatPresence(@Assisted @NonNull Context context,
                            @Assisted @NonNull WorkerParameters workerParams,
                            PreferencesRepository preferences,
                            ChatApiService apiService) {
            super(context, workerParams);

            mPreferences = preferences;
            mApiService = apiService;
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            long collabroomId = mPreferences.getSelectedCollabroomId();
            long incidentId = mPreferences.getSelectedIncidentId();
            String status = getInputData().getString("status");

            return CallbackToFutureAdapter.getFuture(completer -> {
                Organization org = mPreferences.getSelectedOrganization();
                if (org != null) {
                    Presence presence = new Presence(mPreferences.getUserName(), mPreferences.getUserNickName());
                    presence.setOrganization(mPreferences.getSelectedOrganization().getName());
                    presence.setStatus(PresenceStatus.lookUp(status));

                    Call<ResponseBody> call = mApiService.postChatPresence(incidentId, collabroomId, presence);
                    call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                            mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());
                            Timber.tag(DEBUG).i("Successfully posted chat presence.");
                            completer.set(Result.success());
                        }

                        @Override
                        public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                            Timber.tag(DEBUG).e("Failed to post Chat Presence information: %s", t.getMessage());
                            completer.set(Result.failure());
                        }
                    }));
                } else {
                    completer.set(Result.failure());
                }

                return Result.success();
            });
        }
    }
}
