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
package edu.mit.ll.nics.android.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavAction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.data.Incident;
import edu.mit.ll.nics.android.data.Organization;
import edu.mit.ll.nics.android.data.Workspace;
import edu.mit.ll.nics.android.database.entities.Collabroom;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.DASH;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.StringUtils.SPACE;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.INCIDENT_MAP;
import static edu.mit.ll.nics.android.utils.constants.NICS.NO_SELECTION;
import static edu.mit.ll.nics.android.utils.constants.NICS.WORKING_MAP;

public class Utils {

    /**
     * If the value provided is null, return the provided default value. Otherwise, return the
     * original value.
     *
     * @param value The value to return if not null.
     * @param defaultValue The value to return if it is null.
     * @param <T> The type of object that the value is.
     * @return The value.
     */
    public static <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static double isPositiveOrDefault(double value, double defaultValue) {
        return value <= 0 ? defaultValue : value;
    }

    /**
     * Show a simple AlertDialog given a title and a message. It will use the default NICS
     * logo and have a simple "OK" button to close the dialog when clicked.
     *
     * @param activity The {@link Activity} to build this dialog to.
     * @param title The title for the dialog.
     * @param message The message for the dialog.
     */
    public static void showSimpleDialog(Activity activity, String title, String message) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setIcon(R.drawable.nics_logo)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    /**
     * Returns the current version of the app as a nicely formatted string.
     *
     * @param context The context to get the version name and strings to use for formatting.
     * @return The formatted NICS version string.
     */
    public static String getNicsVersion(Context context) {
        String version = null;
        try {
            String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            version = context.getString(R.string.version_login_activity).concat(SPACE).concat(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.tag(DEBUG).e(e, "NICS version number not found.");
        }

        return version;
    }

    /**
     * Pop the backstack of the {@link NavController} backstack provided a {@link NavHostFragment}.
     * Uses the {@link NavHostFragment} to get the {@link NavController}.
     *
     * @param navHostFragment The provided {@link NavHostFragment}.
     */
    public static void popBackStack(NavHostFragment navHostFragment) {
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.popBackStack();
        } else {
            Timber.tag(DEBUG).w("NavHostFragment is null.");
        }
    }

    /**
     * Pop the backstack of the {@link NavController} backstack.
     *
     * @param navController The provided {@link NavController}.
     */
    public static void popBackStack(NavController navController) {
        if (navController != null) {
            navController.popBackStack();
        } else {
            Timber.tag(DEBUG).w("NavHostFragment is null.");
        }
    }

    /**
     * Checks whether all of the entries in an {@link Iterable} are true. If any of them are false,
     * then return false.
     *
     * @param entries The {@link Iterable} object to iterate over.
     * @return boolean Whether or not all of the entries in the iterable are true.
     */
    public static boolean isTrue(Iterable<Boolean> entries) {
        boolean result = true;
        for (Boolean entry : entries) {
            if (!entry) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Special check for whether not an {@link Object} is null, has an empty string, or is equal to
     * 0.0. Returns false if any of the checks are true.
     *
     * @param object The object to check.
     * @return boolean Whether or not the object has any value besides the ones in the check.
     */
    public static boolean hasValue(Object object) {
        return object != null && !object.equals(EMPTY) && !object.equals(0.0);
    }

    /**
     * Special check for whether not an {@link Object} is null, has an empty string, or is equal to
     * 0.0. Returns true if any of the checks are true.
     *
     * @param object The object to check.
     * @return boolean Returns true if the object is equal to one of the values in the check or null.
     */
    public static boolean hasNoValue(Object object) {
        return object == null || object.equals(EMPTY) || object.equals(0.0);
    }

    /**
     * Utility method to check if a {@link Bitmap} has been set in the image view. If the bitmap is
     * null or equal to the default bitmap (R.drawable.ic_photo_white), then it has not been set and
     * returns false. Otherwise returns true.
     *
     * @param bitmap The provided {@link Bitmap}.
     * @param context The {@link Context} to use to get the default bitmap.
     * @return boolean Whether or not the bitmap has been set.
     */
    public static boolean isBitmapSet(Bitmap bitmap, Context context) {
        return bitmap != null && !bitmap.sameAs(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_photo_white));
    }

    /**
     * A quick method for refreshing the android gallery.
     * @param context A {@link Context} to pass to {@link MediaScannerConnection} to do the refresh.
     */
    public static void refreshAndroidGallery(Context context) {
        MediaScannerConnection.scanFile(context, new String[]{Environment.getExternalStorageDirectory().toString()}, null, null);
    }

    public static void clearWorkers(WorkManager workManager) {
        workManager.cancelAllWork();
        workManager.pruneWork();
    }

    /**
     * Returns a list of the objects that are in the first {@link Collection}, but are missing from
     * the second. Returns an empty list if all of the required items are in the collection.
     *
     * @param c1 The collection of required items.
     * @param c2 The collection to check for missing items.
     * @param <T> The type of {@link Object}.
     * @return ArrayList of missing items.
     */
    public static <T> ArrayList<T> findMissingItems(Collection<T> c1, Collection<T> c2) {
        ArrayList<T> missingItems = new ArrayList<>();

        for (T item : c1) {
            if (!c2.contains(item)) {
                missingItems.add(item);
            }
        }

        return missingItems;
    }

    /**
     * Converts a timestamp to a date string.
     *
     * @param timestamp The provided timestamp.
     * @return String The date string.
     */
    public static String secondsTimestampToDateString(long timestamp){
        return new Date(timestamp * 1000).toString();
    }

    public static String millisecondsTimestampToDateString(long timestamp){
        return new Date(timestamp).toString();
    }

    /**
     * Checks whether any {@link Object} that is passed as a parameter is null. If any one of them
     * is null, return false.
     *
     * @param o Objects to check.
     * @return boolean Whether or not any {@link Object} that has been passed in is null.
     */
    public static boolean multiNullCheck(Object ... o) {
        for (Object object : o) {
            if (object == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether any {@link String} that is passed as a parameter is null or the empty string.
     * If any one of them is, return false.
     *
     * @param s Strings to check.
     * @return boolean Whether or not any {@link String} that has been passed in is null or empty.
     */
    public static boolean multiEmptyCheck(String ... s) {
        for (String string : s) {
            if (string == null || string.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the provided {@link String} is null or the empty string. Returns
     * false otherwise.
     *
     * @param s The {@link String} to check.
     * @return boolean Whether or not the provided {@link String} is empty.
     */
    public static boolean emptyCheck(String s) {
        return s == null || s.isEmpty();
    }

    public static <T> boolean emptyCheck(List<T> list) {
        return list == null || list.size() == 0;
    }

    /**
     * Hides the user's keyboard if it is showing.
     *
     * @param activity The {@link Activity} to gather the tools to hide the keyboard.
     */
    public static void hideKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    public static void removeSafe(SavedStateHandle savedStateHandle, String ... keys) {
        if (savedStateHandle != null) {
            for (String key : keys) {
                if (savedStateHandle.contains(key)) {
                    savedStateHandle.remove(key);
                }
            }
        }
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static void forceHideKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getRootView().getWindowToken(), 0);
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to hide keyboard.");
        }
    }

    /**
     * Utility method to return a {@link OkHttp3Downloader} that a {@link Picasso} instance can use
     * to download the image files.
     *
     * @param token The authorization token to add as a header in the request.
     * @param userName The user name to add as a header in the request.
     * @return OkHttp3Downloader to use with Picasso.
     */
    public static OkHttp3Downloader getPicassoDownloader(String token, String userName) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("Authorization", token)
                            .addHeader("CUSTOM-uid", userName)
                            .build();
                    return chain.proceed(request);
                })
                .build();
        return new OkHttp3Downloader(client);
    }

    /**
     * Utility method to get a formatted label for the selected organization.
     *
     * @param context {@link Context} to get resource strings.
     * @param organization {@link Organization} to get organization information.
     * @param userNickName User's nickname to use in the formatted string.
     * @return {@link String} A formatted label for the selected organization.
     */
    public static String getOrganizationLabel(Context context, Organization organization, String userNickName) {
        // If the selected org can't be found, just display the user's name.
        return isOrganizationSelected(organization) ? context.getString(R.string.selected_org, userNickName, organization.getName()) : userNickName;
    }

    /**
     * Utility method to get a formatted label for the selected collaboration room.
     * @param context {@link Context} to get resource strings.
     * @param collabroom {@link Collabroom} to get collabroom information.
     * @param incidentName Incident name to add to the collabroom room.
     * @return {@link String} A formatted label for the selected collaboration room.
     */
    public static String getRoomLabel(Context context, Collabroom collabroom, String incidentName) {
            // Set the room label to the selected collabroom's name.
            if (collabroom != null && !collabroom.getName().equals(NO_SELECTION)) {
                String room = collabroom.getName().replace(incidentName + DASH, EMPTY);

                // Manually translating the default room names.
                if (room.equals(INCIDENT_MAP)) {
                    room = context.getString(R.string.incident_map);
                } else if (room.equals(WORKING_MAP)) {
                    room = context.getString(R.string.working_map);
                }

                return context.getString(R.string.room_active, room);
            } else {
                // Set the room label to "Join Room" because no collabroom is selected."
                return context.getString(R.string.room_join);
            }
    }

    /**
     * Utility method to check if both the incident and collaboration room are currently selected.
     *
     * @param incident The {@link Incident} to check.
     * @param collabroom The {@link Collabroom} to check.
     * @return boolean Whether or not both the incident and collaboration room are selected.
     */
    public static boolean isIncidentAndCollabroomSelected(Incident incident, Collabroom collabroom) {
        return isIncidentSelected(incident) && isCollabroomSelected(collabroom);
    }

    /**
     * Utility method to check if the incident has been selected.
     *
     * @param incident The {@link Incident} to check.
     * @return boolean Whether or not the incident has been selected.
     */
    public static boolean isIncidentSelected(Incident incident) {
        return incident != null && !incident.getIncidentName().equals(NO_SELECTION);
    }

    /**
     * Utility method to check if the collaboration room has been selected.
     *
     * @param collabroom The {@link Collabroom} to check.
     * @return boolean Whether or not the collaboration room has been selected.
     */
    public static boolean isCollabroomSelected(Collabroom collabroom) {
        return collabroom != null && !collabroom.getName().equals(NO_SELECTION);
    }

    /**
     * Utility method to check if the organization has been selected.
     *
     * @param organization The {@link Organization} to check.
     * @return boolean Whether or not the organization has been selected.
     */
    public static boolean isOrganizationSelected(Organization organization) {
        return organization != null && !organization.getName().equals(NO_SELECTION);
    }

    /**
     * Utility method to check if the workspace has been selected.
     *
     * @param workspace The {@link Workspace} to check.
     * @return boolean Whether or not the workspace has been selected.
     */
    public static boolean isWorkspaceSelected(Workspace workspace) {
        return workspace != null && !workspace.getWorkspaceName().equals(NO_SELECTION);
    }

    public static boolean isValidUsername(String username) {
        return !emptyCheck(username) && !username.equalsIgnoreCase("Unknown User");
    }

    public static <T> boolean itemExists(List<T> list, int position) {
        return list != null && position >= 0 && position < list.size() && list.get(position) != null;
    }

    /**
     * Dismisses the provided {@link Dialog} instance only if it's not already null.
     *
     * @param dialog The {@link Dialog} to dismiss.
     * @param <T> The dialog type that extends {@link Dialog}.
     */
    public static <T extends Dialog> void safeDismiss(T dialog) {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Shows the provided {@link Dialog} instance only if it's not already null and not already
     * showing.
     *
     * @param dialog The {@link Dialog} to show.
     * @param <T> The dialog type that extends {@link Dialog}.
     */
    public static <T extends Dialog> void safeShow(T dialog) {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public static <T> void safeAdd(List<T> list, T item) {
        if (item != null) {
            list.add(item);
        }
    }

    /**
     * If the navigation controller is called multiple times to navigate to a destination, the app
     * will crash if it's not handled correctly. For example, if a button is clicked multiple times
     * then, the new destination will be set as the current destination. On the second button
     * callback, it will try to navigate to the destination but the current destination is already
     * set, so it won't find it and will throw an error.
     *
     * In this method we check make sure that the current destination is not the same as the nav
     * directions destination is. We also catch all other failures to make sure the app doesn't
     * crash.
     *
     * @param controller The {@link NavController} to navigate with.
     * @param navDirections THe {@link NavDirections} to use for navigation.
     */
    public static void navigateSafe(NavController controller,
                                    NavDirections navDirections) {
        try {
            NavDestination current = controller.getCurrentDestination();
            NavAction action = Objects.requireNonNull(current).getAction(navDirections.getActionId());

            if (current.getId() != Objects.requireNonNull(action).getDestinationId()) {
                controller.navigate(navDirections);
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to navigate to fragment.");
        }
    }

    public static void setGraph(NavHostFragment hostFragment, int graphResId) throws NullPointerException {
        NavController controller = Objects.requireNonNull(hostFragment).getNavController();
        NavInflater inflater = controller.getNavInflater();
        NavGraph graph = inflater.inflate(graphResId);
        controller.setGraph(graph);
    }

    public static void setGraph(NavHostFragment hostFragment,
                                int graphResId,
                                int startingDestination,
                                Bundle args) throws NullPointerException {
        NavController controller = Objects.requireNonNull(hostFragment).getNavController();
        NavInflater inflater = controller.getNavInflater();
        NavGraph graph = inflater.inflate(graphResId);
        graph.setStartDestination(startingDestination);
        controller.setGraph(graph, args);
    }

    public static Fragment findFragment(NavHostFragment hostFragment, Class<?> cls) {
        Fragment fragment = null;
        List<Fragment> fragments = hostFragment.getChildFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (cls.isInstance(f)) {
                fragment = f;
                break;
            }
        }
        return fragment;
    }

    /**
     * Navigate to the specified fragment destination provided a {@link NavHostFragment} to get
     * the {@link NavController} and navigate using the navId of the action.
     *
     * @deprecated use SafeArgs instead.
     *
     * @param navHostFragment The {@link NavHostFragment} to get the {@link NavController} from.
     * @param navId The id of the action.
     */
    @Deprecated
    public static void navigateToFragment(NavHostFragment navHostFragment, int navId) {
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.navigate(navId);
        } else {
            Timber.tag(DEBUG).w("NavHostFragment is null.");
        }
    }

    /**
     * Navigate to the specified fragment destination provided a {@link NavHostFragment} to get
     * the {@link NavController} and navigate using the navId of the action.
     *
     * @deprecated use SafeArgs instead.
     *
     * @param navHostFragment The {@link NavHostFragment} to get the {@link NavController} from.
     * @param navId The id of the action.
     * @param bundle The {@link Bundle} to pass alongside the action to the destination.
     */
    @Deprecated
    public static void navigateToFragment(NavHostFragment navHostFragment, int navId, Bundle bundle) {
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.navigate(navId, bundle);
        } else {
            Timber.tag(DEBUG).w("NavHostFragment is null.");
        }
    }

    /**
     * Navigate to the specified fragment destination provided a {@link NavController} and
     * navigate using the navId of the action.
     *
     * @deprecated use SafeArgs instead.
     *
     * @param navController The {@link NavController}.
     * @param navId The id of the action.
     */
    @Deprecated
    public static void navigateToFragment(NavController navController, int navId) {
        if (navController != null) {
            navController.navigate(navId);
        }
    }

    /**
     * Navigate to the specified fragment destination provided a {@link NavController} and
     * navigate using the navId of the action.
     *
     * @deprecated use SafeArgs instead.
     *
     * @param navController The {@link NavController}.
     * @param navId The id of the action.
     * @param bundle The {@link Bundle} to pass alongside the action to the destination.
     */
    @Deprecated
    public static void navigateToFragment(NavController navController, int navId, Bundle bundle) {
        if (navController != null) {
            navController.navigate(navId, bundle);
        }
    }
}
