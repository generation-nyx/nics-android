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
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.di.Qualifiers.NetworkExecutor;
import edu.mit.ll.nics.android.repository.ConfigRepository;

public class GeocodingWorkers {

    @HiltWorker
    public static class GeocodeCoordinate extends AppWorker {

        private final Geocoder mGeocoder;
        private final ExecutorService mExecutor;

        @AssistedInject
        public GeocodeCoordinate(@Assisted @NonNull Context context,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 @NetworkExecutor ExecutorService executor,
                                 ConfigRepository configRepository) {
            super(context, workerParams);

            mExecutor = executor;
            mGeocoder = new Geocoder(mAppContext, configRepository.getLocale());
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            double latitude = getInputData().getDouble("latitude", -1D);
            double longitude = getInputData().getDouble("longitude", -1D);

            return CallbackToFutureAdapter.getFuture(completer -> {
                try {
                    geocodeCoordinate(new LatLng(latitude, longitude), result -> {
                        if (result instanceof ThreadResult.Success) {
                            Data output = new Data.Builder()
                                    .putString("address", ((ThreadResult.Success<String>) result).data)
                                    .build();
                            completer.set(Result.success(output));
                        } else {
                            completer.set(Result.failure());
                        }

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    });
                } catch (Exception e) {
                    // Failed to get create a LatLng object.
                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    completer.set(Result.failure());
                }

                return Result.success();
            });
        }

        private void geocodeCoordinate(LatLng coordinate, ThreadCallback<String> callback) {
            mExecutor.execute(() -> {
                try {
                    // Attempt to get an address from the provided coordinate.
                    Address geocoded = mGeocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1).get(0);
                    String address = geocoded.getAddressLine(0);

                    ThreadResult<String> result = new ThreadResult.Success<>(address);
                    callback.onComplete(result);
                } catch (Exception e) {
                    ThreadResult<String> result = new ThreadResult.Error<>(e);
                    callback.onComplete(result);
                }
            });
        }
    }

    @HiltWorker
    public static class GeocodeAddress extends AppWorker {

        private final Geocoder mGeocoder;
        private final ExecutorService mExecutor;

        @AssistedInject
        public GeocodeAddress(@Assisted @NonNull Context context,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 @NetworkExecutor ExecutorService executor,
                                 ConfigRepository configRepository) {
            super(context, workerParams);

            mExecutor = executor;
            mGeocoder = new Geocoder(mAppContext, configRepository.getLocale());
        }

        @NonNull
        @Override
        public ListenableFuture<Result> startWork() {
            // Initialize the progress to 0, so that any observers can be updated that the request has started.
            setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

            String address = getInputData().getString("address");

            return CallbackToFutureAdapter.getFuture(completer -> {
                geocodeAddress(address, result -> {
                    if (result instanceof ThreadResult.Success) {
                        Data output = new Data.Builder()
                                .putDouble("latitude", ((ThreadResult.Success<LatLng>) result).data.latitude)
                                .putDouble("longitude", ((ThreadResult.Success<LatLng>) result).data.longitude)
                                .build();
                        completer.set(Result.success(output));
                    } else {
                        completer.set(Result.failure());
                    }

                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                });

                return Result.success();
            });
        }

        private void geocodeAddress(String address, ThreadCallback<LatLng> callback) {
            mExecutor.execute(() -> {
                try {
                    // Attempt to get an address from the provided text.
                    Address a = mGeocoder.getFromLocationName(address, 1).get(0);
                    LatLng point = new LatLng(a.getLatitude(), a.getLongitude());

                    ThreadResult<LatLng> result = new ThreadResult.Success<>(point);
                    callback.onComplete(result);
                } catch (Exception e) {
                    ThreadResult<LatLng> result = new ThreadResult.Error<>(e);
                    callback.onComplete(result);
                }
            });
        }
    }
}
