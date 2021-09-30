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

import android.text.TextUtils;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.di.Qualifiers.PagedListConfig;
import edu.mit.ll.nics.android.enums.SortBy;
import edu.mit.ll.nics.android.enums.SortOrder;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import kotlinx.coroutines.CoroutineScope;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;

@HiltViewModel
public class GeneralMessageListViewModel extends ViewModel {

    private static final String QUERY_KEY = "QUERY";

    private final SavedStateHandle mSavedStateHandler;
    private final MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> mSearch = new MutableLiveData<>(EMPTY);
    private final NonNullMutableLiveData<Boolean> mIsSearching = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<SortOrder> mSortOrder = new NonNullMutableLiveData<>(SortOrder.DESC);
    private final NonNullMutableLiveData<SortBy> mSortBy = new NonNullMutableLiveData<>(SortBy.DATE);
    private final MutableLiveData<GeneralMessage> mSelectedReport = new MutableLiveData<>(null);
    private final MediatorLiveData<PagingData<GeneralMessage>> mGeneralMessages = new MediatorLiveData<>();

    @Inject
    public GeneralMessageListViewModel(@PagedListConfig PagingConfig pagingConfig,
                                       SavedStateHandle savedStateHandle,
                                       GeneralMessageRepository repository) {
        CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);

        mSavedStateHandler = savedStateHandle;

        // When the user's query changes, update the query in the saved state live data.
        mGeneralMessages.addSource(mSearch, query -> mSavedStateHandler.set(QUERY_KEY, query));

        // When the sort order is changed, refreshModel the messages.
        mGeneralMessages.addSource(mSortOrder, order -> refresh());

        // When the sort order is changed, refreshModel the messages.
        mGeneralMessages.addSource(mSortBy, by -> refresh());

        mGeneralMessages.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(QUERY_KEY, null),
                (Function<CharSequence, LiveData<PagingData<GeneralMessage>>>) query -> {
                    if (TextUtils.isEmpty(query)) {
                        Pager<Integer, GeneralMessage> pager = new Pager<>(pagingConfig, () -> repository.getGeneralMessages(mSortOrder.getValue(), mSortBy.getValue()));
                        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), viewModelScope);
                    }
                    Pager<Integer, GeneralMessage> pager = new Pager<>(pagingConfig, () ->
                            repository.searchGeneralMessages("*" + query + "*", mSortOrder.getValue(), mSortBy.getValue()));
                    return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), viewModelScope);
                }), mGeneralMessages::postValue);
    }

    public LiveData<PagingData<GeneralMessage>> getGeneralMessages() {
        return mGeneralMessages;
    }

    public MutableLiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public void setLoading(boolean isLoading) {
        mIsLoading.postValue(isLoading);
    }

    public NonNullMutableLiveData<Boolean> isSearching() {
        return mIsSearching;
    }

    public void setSearching(boolean isSearching) {
        mIsSearching.postValue(isSearching);
    }

    public void toggleSearching() {
        mIsSearching.postValue(!mIsSearching.getValue());
        mSearch.postValue(EMPTY);
    }

    public MutableLiveData<GeneralMessage> getSelectedReport() {
        return mSelectedReport;
    }

    public NonNullMutableLiveData<SortOrder> getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(SortOrder order) {
        mSortOrder.setValue(order);
    }

    public NonNullMutableLiveData<SortBy> getSortBy() {
        return mSortBy;
    }

    public void setSortBy(SortBy by) {
        mSortBy.setValue(by);
    }

    public void setSelectedReport(GeneralMessage generalMessage) {
        if (generalMessage != mSelectedReport.getValue()) {
            mSelectedReport.postValue(generalMessage);
        } else {
            mSelectedReport.postValue(null);
        }
    }

    public MutableLiveData<String> getSearch() {
        return mSearch;
    }

    private void refresh() {
        mSavedStateHandler.set(QUERY_KEY, mSavedStateHandler.get(QUERY_KEY));
    }

    /**
     * Save the user's query into the SavedStateHandle.
     * This ensures that we retain the value across process death
     * and is used as the input into the Transformations.switchMap above.
     *
     * @param query The search query to use for the {@link androidx.room.Fts4} table.
     */
    public void setQuery(CharSequence query) {
        mSavedStateHandler.set(QUERY_KEY, query);
    }
}
