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

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;

import static edu.mit.ll.nics.android.utils.constants.NICS.UNREAD_CHATS;
import static edu.mit.ll.nics.android.utils.constants.NICS.UNREAD_EOD_REPORTS;
import static edu.mit.ll.nics.android.utils.constants.NICS.UNREAD_GENERAL_MESSAGES;

@HiltViewModel
public class OverviewViewModel extends ViewModel {

    private final SavedStateHandle mSavedStateHandler;
    private final LiveData<Incident> mActiveIncident;
    private final LiveData<Organization> mActiveOrganization;
    private final LiveData<Collabroom> mActiveCollabroom;
    private final MediatorLiveData<List<GeneralMessage>> mUnreadGeneralMessages = new MediatorLiveData<>();
    private final MediatorLiveData<List<EODReport>> mUnreadEODReports = new MediatorLiveData<>();
    private final MediatorLiveData<List<Chat>> mUnreadChats = new MediatorLiveData<>();
    private final NonNullMutableLiveData<Boolean> mIsLoadingCollabrooms = new NonNullMutableLiveData<>(false);
    private final NonNullMutableLiveData<Boolean> mIsLoadingIncidents = new NonNullMutableLiveData<>(false);

    @Inject
    public OverviewViewModel(SavedStateHandle savedStateHandle,
                             PreferencesRepository preferences,
                             GeneralMessageRepository generalMessageRepository,
                             EODReportRepository eodReportRepository,
                             ChatRepository chatRepository) {
        mSavedStateHandler = savedStateHandle;

        mActiveCollabroom = preferences.getSelectedCollabroomLiveData();
        mActiveIncident = preferences.getSelectedIncidentLiveData();
        mActiveOrganization = preferences.getSelectedOrganizationLiveData();

        mUnreadEODReports.addSource(mActiveIncident, incident -> refresh(UNREAD_EOD_REPORTS));
        mUnreadEODReports.addSource(mActiveCollabroom, collabroom -> refresh(UNREAD_EOD_REPORTS));
        mUnreadEODReports.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(UNREAD_EOD_REPORTS, null),
                (Function<CharSequence, LiveData<List<EODReport>>>) query -> {
                    long incidentId = -1L;
                    long collabroomId = -1L;
                    Incident incident = mActiveIncident.getValue();
                    if (incident != null) {
                        incidentId = incident.getIncidentId();
                    }

                    Collabroom collabroom = mActiveCollabroom.getValue();
                    if (collabroom != null) {
                        collabroomId = collabroom.getCollabRoomId();
                    }

                    return eodReportRepository.getUnreadEODReports(incidentId, collabroomId);
                }), mUnreadEODReports::postValue);

        mUnreadGeneralMessages.addSource(mActiveIncident, incident -> refresh(UNREAD_GENERAL_MESSAGES));
        mUnreadGeneralMessages.addSource(mActiveCollabroom, collabroom -> refresh(UNREAD_GENERAL_MESSAGES));
        mUnreadGeneralMessages.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(UNREAD_GENERAL_MESSAGES, null),
                (Function<CharSequence, LiveData<List<GeneralMessage>>>) query -> {
                    long incidentId = -1L;
                    long collabroomId = -1L;
                    Incident incident = mActiveIncident.getValue();
                    if (incident != null) {
                        incidentId = incident.getIncidentId();
                    }

                    Collabroom collabroom = mActiveCollabroom.getValue();
                    if (collabroom != null) {
                        collabroomId = collabroom.getCollabRoomId();
                    }

                    return generalMessageRepository.getUnreadGeneralMessages(incidentId, collabroomId);
                }), mUnreadGeneralMessages::postValue);

        mUnreadChats.addSource(mActiveIncident, incident -> refresh(UNREAD_CHATS));
        mUnreadChats.addSource(mActiveCollabroom, collabroom -> refresh(UNREAD_CHATS));
        mUnreadChats.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(UNREAD_CHATS, null),
                (Function<CharSequence, LiveData<List<Chat>>>) query -> {
                    long incidentId = -1L;
                    long collabroomId = -1L;
                    Incident incident = mActiveIncident.getValue();
                    if (incident != null) {
                        incidentId = incident.getIncidentId();
                    }

                    Collabroom collabroom = mActiveCollabroom.getValue();
                    if (collabroom != null) {
                        collabroomId = collabroom.getCollabRoomId();
                    }

                    return chatRepository.getUnreadChats(incidentId, collabroomId);
                }), mUnreadChats::postValue);
    }

    public LiveData<Boolean> isLoadingIncidents() {
        return mIsLoadingIncidents;
    }

    public void setLoadingIncidents(boolean isLoading) {
        mIsLoadingIncidents.postValue(isLoading);
    }

    public LiveData<Boolean> isLoadingCollabrooms() {
        return mIsLoadingCollabrooms;
    }

    public void setLoadingCollabrooms(boolean isLoading) {
        mIsLoadingCollabrooms.postValue(isLoading);
    }

    public LiveData<List<GeneralMessage>> getUnreadGeneralMessages() {
        return mUnreadGeneralMessages;
    }

    public LiveData<List<EODReport>> getUnreadEODReports() {
        return mUnreadEODReports;
    }

    public LiveData<List<Chat>> getUnreadChats() {
        return mUnreadChats;
    }

    public LiveData<Incident> getActiveIncident() {
        return mActiveIncident;
    }

    public LiveData<Collabroom> getActiveCollabroom() {
        return mActiveCollabroom;
    }

    public LiveData<Organization> getActiveOrganization() {
        return mActiveOrganization;
    }

    private void refresh(String key) {
        mSavedStateHandler.set(key, mSavedStateHandler.get(key));
    }
}
