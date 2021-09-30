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
package edu.mit.ll.nics.android.repository;

import android.content.SharedPreferences;
import android.location.Location;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.data.OrgCapabilities;
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.data.User;
import edu.mit.ll.nics.android.data.Workspace;
import edu.mit.ll.nics.android.data.messages.IncidentMessage;
import edu.mit.ll.nics.android.data.messages.OrganizationMessage;
import edu.mit.ll.nics.android.data.messages.UserMessage;
import edu.mit.ll.nics.android.data.messages.WorkspaceMessage;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import edu.mit.ll.nics.android.database.entities.MobileDeviceTracking;
import edu.mit.ll.nics.android.di.Qualifiers.SharedPrefs;
import edu.mit.ll.nics.android.maps.MapStyle;
import edu.mit.ll.nics.android.utils.MyOrgCapabilities;
import edu.mit.ll.nics.android.utils.livedata.NonNullMutableLiveData;
import edu.mit.ll.nics.android.utils.livedata.SharedPreferenceBooleanLiveData;
import edu.mit.ll.nics.android.utils.livedata.SharedPreferenceIntLiveData;
import edu.mit.ll.nics.android.utils.livedata.SharedPreferenceStringLiveData;
import edu.mit.ll.nics.android.utils.livedata.SharedPreferenceStringSetLiveData;

import static edu.mit.ll.nics.android.data.HostServerConfig.DEFAULT_SERVERS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_NO_RESULTS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NO_SELECTION;
import static edu.mit.ll.nics.android.utils.constants.Map.TRACKING_DESCRIPTION;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.Preferences.ALL_USER_DATA;
import static edu.mit.ll.nics.android.utils.constants.Preferences.API_SERVER;
import static edu.mit.ll.nics.android.utils.constants.Preferences.AUTH_SERVER;
import static edu.mit.ll.nics.android.utils.constants.Preferences.BASE_SERVER;
import static edu.mit.ll.nics.android.utils.constants.Preferences.CONFIG_HOST;
import static edu.mit.ll.nics.android.utils.constants.Preferences.CURRENT_USER_ORG;
import static edu.mit.ll.nics.android.utils.constants.Preferences.CUSTOM_SERVER_SET;
import static edu.mit.ll.nics.android.utils.constants.Preferences.GEO_SERVER;
import static edu.mit.ll.nics.android.utils.constants.Preferences.IMAGE_UPLOAD_URL;
import static edu.mit.ll.nics.android.utils.constants.Preferences.IS_ONLINE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_ACCURACY;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_ALTITUDE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_COURSE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_LATITUDE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_LONGITUDE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_MDT_TIME;
import static edu.mit.ll.nics.android.utils.constants.Preferences.LAST_SUCCESSFUL_SERVER_PING;
import static edu.mit.ll.nics.android.utils.constants.Preferences.MAP_BUILDINGS_ENABLED;
import static edu.mit.ll.nics.android.utils.constants.Preferences.MAP_FULLSCREEN_ENABLED;
import static edu.mit.ll.nics.android.utils.constants.Preferences.MAP_INDOOR_ENABLED;
import static edu.mit.ll.nics.android.utils.constants.Preferences.MAP_TRAFFIC_ENABLED;
import static edu.mit.ll.nics.android.utils.constants.Preferences.MAP_TYPE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SAVED_INCIDENTS;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SAVED_ORGANIZATIONS;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SAVED_ORGCAPABILITES;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SAVED_WORKSPACES;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SELECTED_COLLABROOM;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SELECTED_INCIDENT;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SELECTED_MAP_STYLE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SELECTED_WORKSPACE;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SHOW_DISTANCE_CALCULATOR;
import static edu.mit.ll.nics.android.utils.constants.Preferences.SHOW_HAZARDS;
import static edu.mit.ll.nics.android.utils.constants.Preferences.TABLET_LAYOUT;
import static edu.mit.ll.nics.android.utils.constants.Preferences.USER_DATA;
import static edu.mit.ll.nics.android.utils.constants.Preferences.USER_ID;
import static edu.mit.ll.nics.android.utils.constants.Preferences.USER_NAME;
import static edu.mit.ll.nics.android.utils.constants.Preferences.USER_ORG_ID;
import static edu.mit.ll.nics.android.utils.constants.Preferences.USER_SESSION_ID;
import static edu.mit.ll.nics.android.utils.constants.Preferences.WEB_SERVER;

@Singleton
public class PreferencesRepository {

    private final SharedPreferences mPreferences;
    private MyOrgCapabilities mMyOrgCapabilities;

    @Inject
    public PreferencesRepository(@SharedPrefs SharedPreferences sharedPreferences) {
        mPreferences = sharedPreferences;
    }

    /**
     * Gets the base url for the currently selected NICS server. Returns null if not found.
     * The server config is saved in the preferences {@link SharedPreferences} file.
     * @return String The base server url.
     *
     * @see SharedPreferences
     */
    public String getBaseServer() {
        return mPreferences.getString(BASE_SERVER, null);
    }

    /**
     * Saves the selected base url for the selected NICS server into the preferences
     * {@link SharedPreferences} file.
     *
     * @param baseUrl The base server url to save.
     * @return Returns true if the new values were successfully written
     * to persistent storage.
     */
    public boolean setBaseServer(String baseUrl) {
        return mPreferences.edit().putString(BASE_SERVER, baseUrl).commit();
    }

    public String getSymbologyURL() {
        String baseUrl = mPreferences.getString(BASE_SERVER, null);
        if (baseUrl == null) {
            return null;
        }
        return baseUrl + "/upload/symbology/";
    }

    public LiveData<String> getAPIServerLiveData() {
        return new SharedPreferenceStringLiveData(mPreferences, API_SERVER, null);
    }

    public String getAPIServer() {
        return mPreferences.getString(API_SERVER, null);
    }

    public boolean setAPIServer(String server) {
        return mPreferences.edit().putString(API_SERVER, server).commit();
    }

    /**
     * Gets the web api url for the currently selected server. Returns null if not found.
     * The server config is saved in the preferences {@link SharedPreferences} file.
     * @return String The server's api url.
     *
     * @see SharedPreferences
     */
    public String getWebServerURL() {
        return mPreferences.getString(WEB_SERVER, null);
    }

    public boolean setWebServerURL(String server) {
        return mPreferences.edit().putString(WEB_SERVER, server).commit();
    }

    public String getGeoServerURL() {
        return mPreferences.getString(GEO_SERVER, null);
    }

    public boolean setGeoServerURL(String server) {
        return mPreferences.edit().putString(GEO_SERVER, server).commit();
    }

    public String getImageUploadURL() {
        return mPreferences.getString(IMAGE_UPLOAD_URL, null);
    }

    public boolean setImageUploadURL(String url) {
        return mPreferences.edit().putString(IMAGE_UPLOAD_URL, url).commit();
    }

    public String getAuthServerURL() {
        return mPreferences.getString(AUTH_SERVER, null);
    }

    public boolean setAuthServerURL(String server) {
        return mPreferences.edit().putString(AUTH_SERVER, server).commit();
    }

    public String getConfigHost() {
        return mPreferences.getString(CONFIG_HOST, null);
    }

    public boolean setConfigHost(String host) {
        return mPreferences.edit().putString(CONFIG_HOST, host).commit();
    }

    public Set<String> getCustomServerSet() {
        return mPreferences.getStringSet(CUSTOM_SERVER_SET, new HashSet<>());
    }

    public boolean setCustomServerSet(Set<String> set) {
        return mPreferences.edit().putStringSet(CUSTOM_SERVER_SET, set).commit();
    }

    public boolean addCustomServer(String server) {
        Set<String> servers = new HashSet<>(getCustomServerSet());
        return servers.add(server) && setCustomServerSet(servers);
    }

    public boolean removeCustomServer(String server) {
        Set<String> servers = new HashSet<>(getCustomServerSet());
        return servers.remove(server) && setCustomServerSet(servers);
    }

    public void clearLoginState() {
        setSelectedCollabroom(null);
        setSelectedIncident(null);
        setSelectedOrganization(null);
        setSelectedWorkspace(null);
        setUserSessionId(-1);
        setUserName(NICS_NO_RESULTS);
    }

    public void setSelectedCollabroom(@Nullable Collabroom collabroom) {
        if (collabroom == null) {
            collabroom = new Collabroom();
            collabroom.setCollabRoomId(-1);
            collabroom.setName(NO_SELECTION);
        }
        mPreferences.edit().putString(SELECTED_COLLABROOM, collabroom.toJson()).apply();
    }

    public Collabroom getSelectedCollabroom() {
        String json = mPreferences.getString(SELECTED_COLLABROOM, EMPTY);
        return parseCollabroom(json);
    }

    private Collabroom parseCollabroom(String json) {
        if (!json.equals(EMPTY)) {
            return new Gson().fromJson(json, Collabroom.class);
        } else {
            Collabroom collabroom = new Collabroom();
            collabroom.setCollabRoomId(-1);
            collabroom.setName(NO_SELECTION);
            return collabroom;
        }
    }

    public LiveData<Collabroom> getSelectedCollabroomLiveData() {
        SharedPreferenceStringLiveData pref = new SharedPreferenceStringLiveData(mPreferences, SELECTED_COLLABROOM, EMPTY);
        return Transformations.switchMap(pref, collabroom -> {
            MutableLiveData<Collabroom> liveData = new MutableLiveData<>();
            liveData.postValue(parseCollabroom(collabroom));
            return liveData;
        });
    }

    public String getSelectedCollabroomName() {
        return getSelectedCollabroom().getName();
    }

    public long getSelectedCollabroomId() {
        return getSelectedCollabroom().getCollabRoomId();
    }

    public MutableLiveData<List<String>> getServers() {
        SharedPreferenceStringSetLiveData pref = new SharedPreferenceStringSetLiveData(mPreferences, CUSTOM_SERVER_SET, new HashSet<>());
        return (MutableLiveData<List<String>>) Transformations.switchMap(pref, (servers) -> {
            MutableLiveData<List<String>> hosts = new MutableLiveData<>();

            // Create a list of the custom servers from the shared preference.
            ArrayList<String> customServers = new ArrayList<>(servers);

            // Add the default servers to the saved server list.
            customServers.addAll(DEFAULT_SERVERS);

            // Sort the list of servers alphabetically.
            Collections.sort(customServers, String.CASE_INSENSITIVE_ORDER);

            // Update the live data value.
            hosts.postValue(customServers);
            return hosts;
        });
    }

    public long getUserId() {
        return mPreferences.getLong(USER_ID, -1L);
    }

    public void setUserId(long userId) {
        mPreferences.edit().putLong(USER_ID, userId).apply();
    }

    public long getUserOrgId() {
        return mPreferences.getLong(USER_ORG_ID, -1L);
    }

    public void setUserOrgId(long id) {
        mPreferences.edit().putLong(USER_ORG_ID, id).apply();
    }

    public String getUserNickName() {
        User userData = getUser();

        if (userData != null) {
            return userData.getFirstName() + " " + userData.getLastName();
        } else {
            return "Unknown User";
        }
    }

    public String getUserNickName(String username) {
        User userData = getUser(username);

        if (userData != null) {
            return userData.getFirstName() + " " + userData.getLastName();
        } else {
            return "Unknown User";
        }
    }

    public User getUser() {
        return new Gson().fromJson(mPreferences.getString(USER_DATA, EMPTY), User.class);
    }

    public User getUser(String username) {
        UserMessage userMessage = null;
        User user = null;

        String userMessageString = mPreferences.getString(ALL_USER_DATA, null);

        if (userMessageString != null) {
            userMessage = new Gson().fromJson(userMessageString, UserMessage.class);
        }

        if (userMessage != null) {
            ArrayList<User> allUsers = userMessage.getUsers();

            for (User u : allUsers) {
                if (u.getUserName().equals(username)) {
                    user = u;
                    break;
                }
            }
        }

        return user;
    }

    public void setUserName(String userName) {
        mPreferences.edit().putString(USER_NAME, userName).apply();
    }

    public String getUserName() {
        return mPreferences.getString(USER_NAME, NICS_NO_RESULTS);
    }

    public void setUserData(User user) {
        mPreferences.edit().putString(USER_DATA, user.toJson()).apply();
    }

    public String getUserData() {
        return mPreferences.getString(USER_DATA, NICS_NO_RESULTS);
    }

    public String getAllUserData() {
        return mPreferences.getString(ALL_USER_DATA, NICS_NO_RESULTS);
    }

    public void setAllUserData(UserMessage userMessage) {
        mPreferences.edit().putString(ALL_USER_DATA, userMessage.toJson()).apply();
    }

    public void setUserSessionId(long userSessionID) {
        mPreferences.edit().putLong(USER_SESSION_ID, userSessionID).apply();
    }

    public long getUserSessionId() {
        return mPreferences.getLong(USER_SESSION_ID, -1L);
    }

    public WorkspaceMessage getWorkspaces() {
        String savedWorkspaces = mPreferences.getString(SAVED_WORKSPACES, NICS_NO_RESULTS);
        if (savedWorkspaces.equals(NICS_NO_RESULTS)) {
            return null;
        }

        return new GsonBuilder().create().fromJson(savedWorkspaces, WorkspaceMessage.class);
    }

    public void setWorkspaces(WorkspaceMessage workspaces) {
        mPreferences.edit().putString(SAVED_WORKSPACES, workspaces.toJson()).apply();
    }

    public LiveData<List<Workspace>> getWorkspacesLiveData() {
        SharedPreferenceStringLiveData workspacePref = new SharedPreferenceStringLiveData(mPreferences, SAVED_WORKSPACES, NICS_NO_RESULTS);
        return Transformations.switchMap(workspacePref, (savedWorkspaces) -> {
            MutableLiveData<List<Workspace>> workspaces = new MutableLiveData<>();

            if (savedWorkspaces.equals(NICS_NO_RESULTS)) {
                return null;
            }

            WorkspaceMessage message = new GsonBuilder().create().fromJson(savedWorkspaces, WorkspaceMessage.class);

            workspaces.postValue(message.getWorkspaces());
            return workspaces;
        });
    }

    public String getSelectedWorkspaceName() {
        return getSelectedWorkspace().getWorkspaceName();
    }

    public long getSelectedWorkspaceId() {
        return getSelectedWorkspace().getWorkspaceId();
    }

    public LiveData<Workspace> getSelectedWorkspaceLiveData() {
        SharedPreferenceStringLiveData pref = new SharedPreferenceStringLiveData(mPreferences, SELECTED_WORKSPACE, EMPTY);
        return Transformations.switchMap(pref, workspace -> {
            MutableLiveData<Workspace> liveData = new MutableLiveData<>();
            liveData.postValue(parseWorkspace(workspace));
            return liveData;
        });
    }

    public Workspace getSelectedWorkspace() {
        String json = mPreferences.getString(SELECTED_WORKSPACE, EMPTY);
        return parseWorkspace(json);
    }

    public void setSelectedWorkspace(@Nullable Workspace workspace) {
        if (workspace == null) {
            workspace = new Workspace();
        }
        mPreferences.edit().putString(SELECTED_WORKSPACE, workspace.toJson()).apply();
    }

    private Workspace parseWorkspace(String json) {
        if (!json.equals(EMPTY)) {
            return new Gson().fromJson(json, Workspace.class);
        } else {
            return new Workspace();
        }
    }

    public HashMap<String, Incident> getIncidents() {
        String json = mPreferences.getString(SAVED_INCIDENTS, NICS_NO_RESULTS);
        if (json.equals(NICS_NO_RESULTS)) {
            return null;
        }

        IncidentMessage message = new Gson().fromJson(json, IncidentMessage.class);

        HashMap<String, Incident> incidents = new HashMap<>();
        for (Incident incident : message.getIncidents()) {
            incidents.put(incident.getIncidentName(), incident);
        }
        return incidents;
    }

    public void setIncidents(IncidentMessage message) {
        mPreferences.edit().putString(SAVED_INCIDENTS, message.toJson()).apply();
    }

    public void setSelectedIncident(Incident incident) {
        if (incident == null) {
            incident = new Incident();
            incident.setIncidentId(-1);
            incident.setIncidentName(NO_SELECTION);
        }
        mPreferences.edit().putString(SELECTED_INCIDENT, incident.toJson()).apply();
    }

    public Incident getSelectedIncident() {
        String json = mPreferences.getString(SELECTED_INCIDENT, EMPTY);
        return parseIncident(json);
    }

    public LiveData<Incident> getSelectedIncidentLiveData() {
        SharedPreferenceStringLiveData pref = new SharedPreferenceStringLiveData(mPreferences, SELECTED_INCIDENT, EMPTY);
        return Transformations.switchMap(pref, incident -> {
            MutableLiveData<Incident> liveData = new MutableLiveData<>();
            liveData.postValue(parseIncident(incident));
            return liveData;
        });
    }

    private Incident parseIncident(String json) {
        if (!json.equals(EMPTY)) {
            return new Gson().fromJson(json, Incident.class);
        } else {
            Incident incident = new Incident();
            incident.setIncidentId(-1);
            incident.setIncidentName(NO_SELECTION);
            return incident;
        }
    }

    public LatLng getSelectedIncidentLocation() {
        return new LatLng(getSelectedIncident().getLatitude(), getSelectedIncident().getLongitude());
    }

    public String getSelectedIncidentName() {
        return getSelectedIncident().getIncidentName();
    }

    public long getSelectedIncidentId() { return getSelectedIncident().getIncidentId(); }

    public LiveData<List<Incident>> getIncidentsLiveData() {
        SharedPreferenceStringLiveData incidentPref = new SharedPreferenceStringLiveData(mPreferences, SAVED_INCIDENTS, NICS_NO_RESULTS);
        return Transformations.switchMap(incidentPref, (savedIncidents) -> {
            MutableLiveData<List<Incident>> incidents = new MutableLiveData<>();

            if (savedIncidents.equals(NICS_NO_RESULTS)) {
                return null;
            }

            IncidentMessage message = new GsonBuilder().create().fromJson(savedIncidents, IncidentMessage.class);

            incidents.postValue(message.getIncidents());
            return incidents;
        });
    }

    public MyOrgCapabilities getMyOrgCapabilities() {
        if (mMyOrgCapabilities == null) {
            mMyOrgCapabilities = new MyOrgCapabilities();
            mMyOrgCapabilities.setCapabilities(getOrgCapabilites());
        }

        return mMyOrgCapabilities;
    }

    public void setOrgCapabilites(OrgCapabilities newOrgCap) {
        if (newOrgCap != null) {
            mPreferences.edit().putString(SAVED_ORGCAPABILITES, newOrgCap.toJson()).apply();
            getMyOrgCapabilities().setCapabilities(newOrgCap);
        } else {
            getMyOrgCapabilities().resetCapabilitiesToOff();
        }
    }

    public OrgCapabilities getOrgCapabilites() {
        String collabroomString = mPreferences.getString(SAVED_ORGCAPABILITES, EMPTY);
        if (!collabroomString.equals(EMPTY)) {
            return new Gson().fromJson(collabroomString, OrgCapabilities.class);
        } else {
            return new OrgCapabilities();
        }
    }

    public LiveData<List<Organization>> getOrganizationsLiveData() {
        SharedPreferenceStringLiveData pref = new SharedPreferenceStringLiveData(mPreferences, SAVED_ORGANIZATIONS, NICS_NO_RESULTS);
        return Transformations.switchMap(pref, (savedOrganizations) -> {
            MutableLiveData<List<Organization>> organizations = new MutableLiveData<>();

            if (savedOrganizations.equals(NICS_NO_RESULTS)) {
                return null;
            }

            OrganizationMessage message = new GsonBuilder().create().fromJson(savedOrganizations, OrganizationMessage.class);

            organizations.postValue(message.getOrgs());
            return organizations;
        });
    }

    public List<Organization> getOrganizations() {
        String json = mPreferences.getString(SAVED_ORGANIZATIONS, NICS_NO_RESULTS);
        if (json.equals(NICS_NO_RESULTS)) {
            return null;
        }

        OrganizationMessage message = new Gson().fromJson(json, OrganizationMessage.class);
        return message.getOrgs();
    }

    public void setOrganizations(OrganizationMessage orgs) {
        for (Organization org : orgs.getOrgs()) {
            org.getUserOrgs().get(0).setUser(getUser());
        }

        mPreferences.edit().putString(SAVED_ORGANIZATIONS, orgs.toJson()).apply();

        if (getUserOrgId() == -1) {
            setUserOrgId(orgs.getOrgs().get(0).getOrgId());
        }
    }

    public LiveData<Organization> getSelectedOrganizationLiveData() {
        SharedPreferenceStringLiveData pref = new SharedPreferenceStringLiveData(mPreferences, CURRENT_USER_ORG, EMPTY);
        return Transformations.switchMap(pref, organization -> {
            MutableLiveData<Organization> liveData = new MutableLiveData<>();
            liveData.postValue(parseOrganization(organization));
            return liveData;
        });
    }

    public Organization getSelectedOrganization() {
        String json = mPreferences.getString(CURRENT_USER_ORG, EMPTY);
        return parseOrganization(json);
    }

    public void setSelectedOrganization(@Nullable Organization organization) {
        if (organization == null) {
            organization = new Organization();
            organization.setOrgId(-1L);
            organization.setName(NO_SELECTION);
        }
        mPreferences.edit().putString(CURRENT_USER_ORG, organization.toJson()).apply();
        mPreferences.edit().putLong(USER_ORG_ID, organization.getOrgId()).apply();
    }

    private Organization parseOrganization(String json) {
        if (!json.equals(EMPTY)) {
            return new Gson().fromJson(json, Organization.class);
        } else {
            Organization organization = new Organization();
            organization.setOrgId(-1L);
            organization.setName(NO_SELECTION);
            return organization;
        }
    }

    public boolean isShowDistanceCalculator() {
        return mPreferences.getBoolean(SHOW_DISTANCE_CALCULATOR, false);
    }

    public void setShowDistanceCalculator(boolean show) {
        mPreferences.edit().putBoolean(SHOW_DISTANCE_CALCULATOR, show).apply();
    }

    public NonNullMutableLiveData<Boolean> isShowDistanceCalculatorLiveData() {
        return new SharedPreferenceBooleanLiveData(mPreferences, SHOW_DISTANCE_CALCULATOR, false);
    }

    public boolean isShowHazards() {
        return mPreferences.getBoolean(SHOW_HAZARDS, true);
    }

    public void setShowHazards(boolean show) {
        mPreferences.edit().putBoolean(SHOW_HAZARDS, show).apply();
    }

    public NonNullMutableLiveData<Boolean> isShowHazardsLiveData() {
        return new SharedPreferenceBooleanLiveData(mPreferences, SHOW_HAZARDS, true);
    }

    public LiveData<Boolean> isOnline() {
        return new SharedPreferenceBooleanLiveData(mPreferences, IS_ONLINE, false);
    }

    public void switchToOfflineMode() {
        mPreferences.edit().putBoolean(IS_ONLINE, false).apply();
    }

    public void switchToOnlineMode() {
        mPreferences.edit().putBoolean(IS_ONLINE, true).apply();
    }

    public long getMDTTime() {
        return mPreferences.getLong(LAST_MDT_TIME, -1L);
    }

    public void setMDTTime(long time) {
        mPreferences.edit().putLong(LAST_MDT_TIME, time).apply();
    }

    public double getMDTLatitude() {
        return Double.parseDouble(mPreferences.getString(LAST_LATITUDE, "NaN"));
    }

    public void setMDTLatitude(String latitude) {
        mPreferences.edit().putString(LAST_LATITUDE, latitude).apply();
    }

    public double getMDTLongitude() {
        return Double.parseDouble(mPreferences.getString(LAST_LONGITUDE, "NaN"));
    }

    public void setMDTLongitude(String longitude) {
        mPreferences.edit().putString(LAST_LONGITUDE, longitude).apply();
    }

    public double getMDTAltitude() {
        return Double.parseDouble(mPreferences.getString(LAST_ALTITUDE, "NaN"));
    }

    public void setMDTAltitude(String altitude) {
        mPreferences.edit().putString(LAST_ALTITUDE, altitude).apply();
    }

    public double getMDTCourse() {
        return Double.parseDouble(mPreferences.getString(LAST_COURSE, "NaN"));
    }

    public void setMDTCourse(String course) {
        mPreferences.edit().putString(LAST_COURSE, course).apply();
    }

    public double getMDTAccuracy() {
        return Double.parseDouble(mPreferences.getString(LAST_ACCURACY, "NaN"));
    }

    public void setMDTAccuracy(String accuracy) {
        mPreferences.edit().putString(LAST_ACCURACY, accuracy).apply();
    }

    public MobileDeviceTracking setMDT(Location location) {
        setMDTTime(location.getTime());
        setMDTLatitude(String.valueOf(location.getLatitude()));
        setMDTLongitude(String.valueOf(location.getLongitude()));
        setMDTAltitude(String.valueOf(location.getAltitude()));
        setMDTCourse(String.valueOf(location.getBearing()));
        setMDTAccuracy(String.valueOf(location.getAccuracy()));

        MobileDeviceTracking mdt = new MobileDeviceTracking();

        long timeNow = System.currentTimeMillis();
        mdt.setCreatedUTC(timeNow);
        mdt.setTimestamp(timeNow);

        mdt.setDeviceId(NetworkRepository.mDeviceId);
        mdt.setUsername(getUserName());
        mdt.setName(getUserNickName());
        mdt.setDescription(TRACKING_DESCRIPTION);

        mdt.setLatitude(location.getLatitude());
        mdt.setLongitude(location.getLongitude());

        mdt.setCourse(location.getBearing());
        mdt.setSpeed(location.getSpeed());
        mdt.setAltitude(location.getAltitude());
        mdt.setAccuracy(location.getAccuracy());
        return mdt;
    }

    public MutableLiveData<Boolean> getTabletLayout() {
        SharedPreferenceBooleanLiveData tabletLayoutPref = new SharedPreferenceBooleanLiveData(mPreferences, TABLET_LAYOUT, false);
        return (MutableLiveData<Boolean>) Transformations.switchMap(tabletLayoutPref, layout -> {
            MutableLiveData<Boolean> isTabletLayout = new MutableLiveData<>();

            isTabletLayout.postValue(layout);
//            isTabletLayout.postValue(layout && isLargeScreen());
            return isTabletLayout;
        });
    }

    public boolean getTabletLayoutOn() {
        return mPreferences.getBoolean(TABLET_LAYOUT, false);
    }

    public void setTabletLayoutOn(boolean value) {
        mPreferences.edit().putBoolean(TABLET_LAYOUT, value).apply();
    }

    public NonNullMutableLiveData<Boolean> isIndoorEnabledLiveData() {
        return new SharedPreferenceBooleanLiveData(mPreferences, MAP_INDOOR_ENABLED, false);
    }

    public boolean isIndoorEnabled() {
        return mPreferences.getBoolean(MAP_INDOOR_ENABLED, false);
    }

    public void setIndoorEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(MAP_INDOOR_ENABLED, enabled).apply();
    }

    public NonNullMutableLiveData<Boolean> isMapFullscreenLiveData() {
        return new SharedPreferenceBooleanLiveData(mPreferences, MAP_FULLSCREEN_ENABLED, false);
    }

    public boolean isMapFullScreen() {
        return mPreferences.getBoolean(MAP_FULLSCREEN_ENABLED, false);
    }

    public void setMapFullscreen(boolean fullscreen) {
        mPreferences.edit().putBoolean(MAP_FULLSCREEN_ENABLED, fullscreen).apply();
    }

    public NonNullMutableLiveData<Boolean> isBuildingsEnabledLiveData() {
        return new SharedPreferenceBooleanLiveData(mPreferences, MAP_BUILDINGS_ENABLED, false);
    }

    public boolean isBuildingsEnabled() {
        return mPreferences.getBoolean(MAP_BUILDINGS_ENABLED, false);
    }

    public void setBuildingsEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(MAP_BUILDINGS_ENABLED, enabled).apply();
    }

    public NonNullMutableLiveData<Boolean> isTrafficEnabledLiveData() {
        return new SharedPreferenceBooleanLiveData(mPreferences, MAP_TRAFFIC_ENABLED, false);
    }

    public boolean isTrafficEnabled() {
        return mPreferences.getBoolean(MAP_TRAFFIC_ENABLED, false);
    }

    public void setTrafficEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(MAP_TRAFFIC_ENABLED, enabled).apply();
    }

    public LiveData<Integer> getMapTypeLiveData() {
        return new SharedPreferenceIntLiveData(mPreferences, MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
    }

    public int getMapType() {
        return mPreferences.getInt(MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
    }

    public void setMapType(int mapType) {
        mPreferences.edit().putInt(MAP_TYPE, mapType).apply();
    }

    public void setMapStyle(String mapStyle) {
        mPreferences.edit().putString(SELECTED_MAP_STYLE, mapStyle).apply();
    }

    public String getMapStyle() {
        return mPreferences.getString(SELECTED_MAP_STYLE, MapStyle.STANDARD.getName());
    }

    public LiveData<String> getMapStyleLiveData() {
        return new SharedPreferenceStringLiveData(mPreferences, SELECTED_MAP_STYLE, MapStyle.STANDARD.getName());
    }

    public long getLastSuccessfulServerCommsTimestamp() {
        return mPreferences.getLong(LAST_SUCCESSFUL_SERVER_PING, 0);
    }

    public void setLastSuccessfulServerCommsTimestamp(long timestamp) {
        mPreferences.edit().putLong(LAST_SUCCESSFUL_SERVER_PING, timestamp).apply();
    }
}
