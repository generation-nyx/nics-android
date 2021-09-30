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

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import edu.mit.ll.nics.android.BuildConfig;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class NetworkUtils {

    /**
     * Gets the file size of a remote file from the provided url.
     * Returns -1 if it can't be found or if the url isn't a file.
     *
     * @param url         The url of the remote file to get the file size of.
     * @param accessToken The authorization access token for connecting to the given url.
     * @return The file size of the remote file from the given url.
     */
    public static long getFileSize(String url, String accessToken) {
        URLConnection connection = null;
        long fileSize = -1L;

        try {
            connection = new URL(url).openConnection();

            if (accessToken != null && !accessToken.isEmpty()) {
                connection.addRequestProperty("Authorization", accessToken);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileSize = connection.getContentLengthLong();
            } else {
                fileSize = connection.getContentLength();
            }
        } catch (IOException | NullPointerException | OutOfMemoryError e) {
            Timber.tag(DEBUG).e(e, "Failed to get file size of remote file.");
        } finally {
            closeConnection(connection, url);
        }

        return fileSize;
    }

    /**
     * Returns the value of the {@code last-modified} header field from the provided url.
     *
     * @param url         The url to check for a {@code last-modified} header field.
     * @param accessToken The accessToken to authorize connection to the url.
     * @return Returns the {@code last-modified} header field value if found. Returns -1 if not found.
     * @see URLConnection#getLastModified()
     */
    public static long getLastModified(String url, String accessToken) {
        URLConnection connection = null;

        long lastModified = -1L;

        try {
            connection = new URL(url).openConnection();

            if (accessToken != null && !accessToken.isEmpty()) {
                connection.addRequestProperty("Authorization", accessToken);
            }

            lastModified = connection.getLastModified();
        } catch (IOException | NullPointerException | OutOfMemoryError e) {
            Timber.tag(DEBUG).e(e, "Failed to get last modified timestamp from url.");
        } finally {
            closeConnection(connection, url);
        }

        return lastModified;
    }

    /**
     * Closes the connection to the provided {@link URLConnection}.
     *
     * @param connection The {@link URLConnection} to disconnect.
     * @param url        The url that will be used to decipher whether or not it is a
     *                   {@link HttpsURLConnection} or a {@link HttpURLConnection}.
     */
    public static void closeConnection(URLConnection connection, String url) {
        if (connection != null) {
            if (url.startsWith("http://")) {
                ((HttpURLConnection) connection).disconnect();
            } else if (url.startsWith("https://")) {
                ((HttpsURLConnection) connection).disconnect();
            }
        }
    }

    /**
     * Check whether or not the provided {@link String} is a valid {@link URL}.
     *
     * @param url The {@link String} to check.
     * @return Returns true if valid, false if invalid.
     */
    public static boolean urlValidator(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    /**
     * Check to see if the active {@link Network} is connected to WIFI by checking it's
     * {@link NetworkCapabilities}.
     *
     * @param application The application that holds the {@link ConnectivityManager}.
     * @return Whether or not there is an active WIFI connection.
     */
    public static boolean isWifiConnected(Application application) {
        ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        return isWifiConnected(connectivityManager);
    }

    /**
     * Check to see if the active {@link Network} is connected to WIFI by checking it's
     * {@link NetworkCapabilities}.
     *
     * @param connectivityManager The {@link ConnectivityManager} to use to get the
     * {@link NetworkCapabilities}.
     * @return Whether or not there is an active WIFI connection.
     */
    public static boolean isWifiConnected(ConnectivityManager connectivityManager) {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
        return activeNetwork != null && (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
    }

    /**
     * Check to see if the active {@link Network} is connected at all by checking it's
     * {@link NetworkCapabilities}.
     * @return Whether or not there is an active network connection.
     */
    public static boolean isNetworkConnected(ConnectivityManager connectivityManager) {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
        return activeNetwork != null && (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    public static RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(descriptionString, MediaType.parse("text/plain"));
    }

    public static MultipartBody.Part prepareFilePart(String partName, File file, String mediaType) {
        // create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(file, MediaType.parse(mediaType));

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    public static HttpLoggingInterceptor getLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
            if(!message.contains("�")){
                Timber.d(message);
            }
        });

        if (BuildConfig.DEBUG) {
            // development build
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            // production build
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        }
        return logging;
    }

    public static boolean isSuccessStatusCode(int statusCode) {
        return (statusCode >= 200) && (statusCode <= 299);
    }

    public static boolean isPicassoLogging() {
        return BuildConfig.DEBUG;
    }
}
