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
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToDevice;
import static edu.mit.ll.nics.android.utils.NetworkUtils.urlValidator;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;

@HiltWorker
public class DownloadImageWorker extends AppWorker {

    private final DownloaderApiService mApiService;

    @AssistedInject
    public DownloadImageWorker(@Assisted @NonNull Context context,
                               @Assisted @NonNull WorkerParameters workerParams,
                               DownloaderApiService apiService) {
        super(context, workerParams);

        mApiService = apiService;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        // Initialize the progress to 0, so that any observers can be updated that the request has started.
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

        return CallbackToFutureAdapter.getFuture(completer -> {
            String url = getInputData().getString("url");
            Data.Builder output = new Data.Builder();

            if (!emptyCheck(url)) {
                Uri uri = Uri.parse(url);

                // Check if the file has already been stored locally.
                File file = new File(mAppContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), uri.getLastPathSegment());

                if (uri.getScheme() != null && !file.exists() && urlValidator(url)) {
                    Call<ResponseBody> call = mApiService.download(url);
                    call.enqueue(new AuthCallback<>(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                            if (response.body() != null) {
                                try (ResponseBody body = response.body(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                    InputStream is = body.byteStream();

                                    byte[] data = new byte[4096];

                                    int count;
                                    while ((count = is.read(data)) != -1) {
                                        baos.write(data, 0, count);
                                    }

                                    // Keep track of the image location and update the UI based upon it.
                                    Uri imageUri = saveImageToDevice(baos.toByteArray(), file, 60);
                                    completer.set(Result.success(output.putString("uri", imageUri.toString()).build()));
                                } catch (Exception e) {
                                    completer.set(Result.failure());
                                }
                            } else {
                                completer.set(Result.failure());
                            }
                        }

                        @Override
                        public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                            completer.set(Result.failure());
                        }
                    }));
                } else {
                    completer.set(Result.success(output.putString("uri", Uri.fromFile(file).toString()).build()));
                }
            } else {
                completer.set(Result.failure());
            }

            return Result.success();
        });
    }
}
