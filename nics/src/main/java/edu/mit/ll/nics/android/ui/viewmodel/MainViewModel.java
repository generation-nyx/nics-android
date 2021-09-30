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
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.ConfigRepository;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.Utils;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;

import static edu.mit.ll.nics.android.utils.Utils.isCollabroomSelected;
import static edu.mit.ll.nics.android.utils.Utils.isIncidentSelected;
import static edu.mit.ll.nics.android.utils.constants.NICS.NEW_CHATS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NEW_EOD_REPORTS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NEW_GENERAL_MESSAGES;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final SavedStateHandle mSavedStateHandler;
    private final PreferencesRepository mPreferences;
    private final ConfigRepository mConfigRepository;
    private final LiveData<Boolean> mIsOnline;
    private final LiveData<Incident> mActiveIncident;
    private final LiveData<Collabroom> mActiveCollabroom;
    private final LiveData<String> mHostSelection;
    private final NonNullMutableLiveData<Boolean> mPollingServiceBound = new NonNullMutableLiveData<>(false);
    private final MediatorLiveData<String> mBreadcrumbText = new MediatorLiveData<>();
    private final MediatorLiveData<List<GeneralMessage>> mNewGeneralMessages = new MediatorLiveData<>();
    private final MediatorLiveData<List<EODReport>> mNewEODReports = new MediatorLiveData<>();
    private final MediatorLiveData<List<Chat>> mNewChats = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> mPollIncident = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> mPollCollabroom = new MediatorLiveData<>();

    @Inject
    public MainViewModel(SavedStateHandle savedStateHandle,
                         PreferencesRepository preferences,
                         ConfigRepository configRepository,
                         ChatRepository chatRepository,
                         GeneralMessageRepository generalMessageRepository,
                         EODReportRepository eodReportRepository) {
        mSavedStateHandler = savedStateHandle;
        mPreferences = preferences;
        mConfigRepository = configRepository;

        mHostSelection = mPreferences.getAPIServerLiveData();
        mIsOnline = mPreferences.isOnline();
        mActiveCollabroom = mPreferences.getSelectedCollabroomLiveData();
        mActiveIncident = mPreferences.getSelectedIncidentLiveData();

        mPollIncident.addSource(mActiveIncident, incident -> {
            if (isIncidentSelected(incident) && mPollingServiceBound.getValue()) {
                mPollIncident.postValue(true);
            } else {
                mPollIncident.postValue(false);
            }
        });

        mPollIncident.addSource(mPollingServiceBound, isBound -> {
            if (isBound && isIncidentSelected(mActiveIncident.getValue())) {
                mPollIncident.postValue(true);
            } else {
                mPollIncident.postValue(false);
            }
        });

        mPollCollabroom.addSource(mActiveCollabroom, collabroom -> {
            if (isCollabroomSelected(collabroom) && mPollingServiceBound.getValue()) {
                mPollCollabroom.postValue(true);
            } else {
                mPollCollabroom.postValue(false);
            }
        });

        mPollCollabroom.addSource(mPollingServiceBound, isBound -> {
            if (isBound && isCollabroomSelected(mActiveCollabroom.getValue())) {
                mPollCollabroom.postValue(true);
            } else {
                mPollCollabroom.postValue(false);
            }
        });

        mNewGeneralMessages.addSource(mActiveIncident, incident -> refresh(NEW_GENERAL_MESSAGES));
        mNewGeneralMessages.addSource(mActiveCollabroom, collabroom -> refresh(NEW_GENERAL_MESSAGES));
        mNewGeneralMessages.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(NEW_GENERAL_MESSAGES, null),
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

                    return generalMessageRepository.getNewGeneralMessages(incidentId, collabroomId);
                }), mNewGeneralMessages::postValue);

        mNewEODReports.addSource(mActiveIncident, incident -> refresh(NEW_EOD_REPORTS));
        mNewEODReports.addSource(mActiveCollabroom, collabroom -> refresh(NEW_EOD_REPORTS));
        mNewEODReports.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(NEW_EOD_REPORTS, null),
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

                    return eodReportRepository.getNewEODReports(incidentId, collabroomId);
                }), mNewEODReports::postValue);

        mNewChats.addSource(mActiveIncident, incident -> refresh(NEW_CHATS));
        mNewChats.addSource(mActiveCollabroom, collabroom -> refresh(NEW_CHATS));
        mNewChats.addSource(Transformations.switchMap(
                mSavedStateHandler.getLiveData(NEW_CHATS, null),
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

                    return chatRepository.getNewChats(incidentId, collabroomId);
                }), mNewChats::postValue);

        mBreadcrumbText.addSource(mActiveIncident, incident -> updateBreadcrumbText());
        mBreadcrumbText.addSource(mActiveCollabroom, collabroom -> updateBreadcrumbText());
    }

    private void refresh(String key) {
        mSavedStateHandler.set(key, mSavedStateHandler.get(key));
    }

    private void updateBreadcrumbText() {
        Collabroom collabroom = mActiveCollabroom.getValue();
        Incident incident = mActiveIncident.getValue();

        StringBuilder sb = new StringBuilder();

        if (Utils.isIncidentSelected(incident)) {
            sb.append(incident.getIncidentName());

            if (Utils.isCollabroomSelected(collabroom)) {
                sb.append("\n");

                String name = collabroom.getName();
                Locale locale = mConfigRepository.getLocale();
                if (locale.getDisplayLanguage().equalsIgnoreCase("srpski") && name.equalsIgnoreCase("Incident Map")) {
                    name = "Mapa incidenta";
                } else if (locale.getDisplayLanguage().equalsIgnoreCase("srpski") && name.equalsIgnoreCase("Working Map")) {
                    name = "Radna mapa";
                }

                sb.append(name);

                if (!collabroom.doIHaveMarkupPermission(mPreferences.getUserId())) {
                    sb.append(" - READ ONLY");
                }
            }
        }

        mBreadcrumbText.postValue(sb.toString());
    }

    public LiveData<List<GeneralMessage>> getNewGeneralMessages() {
        return mNewGeneralMessages;
    }

    public LiveData<List<EODReport>> getNewEODReports() {
        return mNewEODReports;
    }


    public LiveData<List<Chat>> getNewChats() {
        return mNewChats;
    }

    public LiveData<String> getBreadcrumbText() {
        return mBreadcrumbText;
    }

    public LiveData<Boolean> isOnline() {
        return mIsOnline;
    }

    public LiveData<String> getHostSelection() {
        return mHostSelection;
    }

    public LiveData<Boolean> pollIncident() {
        return mPollIncident;
    }

    public LiveData<Boolean> pollCollabroom() {
        return mPollCollabroom;
    }

    public NonNullMutableLiveData<Boolean> isPollingServiceBound() {
        return mPollingServiceBound;
    }

    public void setPollingServiceBound(boolean isBound) {
        mPollingServiceBound.postValue(isBound);
    }
}
