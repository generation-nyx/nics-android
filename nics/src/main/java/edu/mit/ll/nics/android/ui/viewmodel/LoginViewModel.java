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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;

import static edu.mit.ll.nics.android.data.HostServerConfig.DEFAULT_SERVERS;
import static edu.mit.ll.nics.android.utils.Utils.getValueOrDefault;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final boolean mIsLargeScreen = false;
    private final LiveData<List<String>> mServers;
    private final MutableLiveData<String> mSelectedServer;
    private final NonNullMutableLiveData<Boolean> mIsTabletLayout = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Boolean> mIsLoading = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Boolean> mIsLoggingIn = new NonNullMutableLiveData<>(false);
    @Inject
    public LoginViewModel(PreferencesRepository preferences) {
        mServers = preferences.getServers();

        // Initialize the selected server to the last selected server or the first default server.
        mSelectedServer = new MutableLiveData<>(getValueOrDefault(preferences.getConfigHost(), DEFAULT_SERVERS.get(0)));

        // TODO need mediator here.
//        int screenSize = getApplication().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
//        mIsLargeScreen = (screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE);
//
//        SharedPreferenceBooleanLiveData tabletLayoutPref = new SharedPreferenceBooleanLiveData(dataManager.getSharedPreferences(), TABLET_LAYOUT, false);
//        mIsTabletLayout = preferences.getTabletLayout();
    }

    public NonNullMutableLiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public void setLoading(boolean isLoading) {
        mIsLoading.postValue(isLoading);
    }

    public NonNullMutableLiveData<Boolean> isLoggingIn() {
        return mIsLoggingIn;
    }

    public void setLoggingIn(boolean isLoggingIn) {
        mIsLoggingIn.postValue(isLoggingIn);
    }

    public LiveData<List<String>> getServers() {
        return mServers;
    }

    public NonNullMutableLiveData<Boolean> isTabletLayout() {
        return mIsTabletLayout; }

    public boolean isLargeScreen() { return mIsLargeScreen; }

    public MutableLiveData<String> getSelectedServer() {
        return mSelectedServer;
    }

    public void setSelectedServer(String server) {
        mSelectedServer.postValue(server);
    }
}
