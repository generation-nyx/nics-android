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
package edu.mit.ll.nics.android.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.di.Qualifiers.PagedListConfig;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import kotlinx.coroutines.CoroutineScope;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;

@HiltViewModel
public class ChatViewModel extends ViewModel {

    // TODO have two sets of dates. one start and end is actually selections. one start and end is the min and max based upon database.
    // The two selections will update the database query.

    private final NonNullMutableLiveData<Boolean> mIsDateFiltered = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Long> mStartDate;
    private final NonNullMutableLiveData<Long> mEndDate;
    private final MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> mIsSearching = new MutableLiveData<>(false);
    private final NonNullMutableLiveData<String> mSearch = new NonNullMutableLiveData<>(EMPTY);
    private final NonNullMutableLiveData<String> mChatMessage = new NonNullMutableLiveData<>(EMPTY);
    private final MediatorLiveData<PagingData<Chat>> mChat = new MediatorLiveData<>();

    @Inject
    public ChatViewModel(@PagedListConfig PagingConfig pagingConfig,
                         PreferencesRepository preferences,
                         ChatRepository repository) {
        long incidentId = preferences.getSelectedIncidentId();
        long collabroomId = preferences.getSelectedCollabroomId();

        CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);

        mStartDate = new NonNullMutableLiveData<>(repository.getOldestChatTimestamp(collabroomId));
        mEndDate = new NonNullMutableLiveData<>(DateTime.now(DateTimeZone.UTC).getMillis());

        Pager<Integer, Chat> pager = new Pager<>(pagingConfig, () -> repository.getChats(incidentId, collabroomId));
        mChat.addSource(PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), viewModelScope), mChat::postValue);
    }

    public LiveData<PagingData<Chat>> getChat() {
        return mChat;
    }

    public MutableLiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public void setLoading(boolean isLoading) {
        mIsLoading.postValue(isLoading);
    }

    public MutableLiveData<Boolean> isSearching() {
        return mIsSearching;
    }

    public void setSearching(boolean isSearching) {
        mIsSearching.postValue(isSearching);
    }

    public void toggleSearching() {
        Boolean isSearching = mIsSearching.getValue();

        if (isSearching != null) {
            isSearching = !isSearching;
            mIsSearching.postValue(isSearching);

            if (!mSearch.getValue().equals(EMPTY)) {
                mSearch.postValue(EMPTY);
            }
        }
    }

    public NonNullMutableLiveData<String> getChatMessage() {
        return mChatMessage;
    }

    public void setChatMessage(String chatMessage) {
        mChatMessage.postValue(chatMessage);
    }

    public MutableLiveData<String> getSearch() {
        return mSearch;
    }

    public NonNullMutableLiveData<Long> getStartDate() {
        return mStartDate;
    }

    public void setStartDate(long startDate) {
        mStartDate.postValue(startDate);
    }

    public NonNullMutableLiveData<Long> getEndDate() {
        return mEndDate;
    }

    public void setEndDate(long startDate) {
        mEndDate.postValue(startDate);
    }
}
