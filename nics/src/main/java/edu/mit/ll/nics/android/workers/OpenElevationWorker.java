package edu.mit.ll.nics.android.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.OpenElevationApiService;
import edu.mit.ll.nics.android.auth.RetryCallback;
import edu.mit.ll.nics.android.data.messages.OpenElevationMessage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static edu.mit.ll.nics.android.utils.StringUtils.coordinateToString;

@HiltWorker
public class OpenElevationWorker extends AppWorker {

    private final OpenElevationApiService mApiService;

    @AssistedInject
    public OpenElevationWorker(@Assisted @NonNull Context context,
                               @Assisted @NonNull WorkerParameters workerParams,
                               OpenElevationApiService apiService) {
        super(context, workerParams);

        mApiService = apiService;
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
                LatLng coordinate = new LatLng(latitude, longitude);
                Call<OpenElevationMessage> call = mApiService.getElevation(coordinateToString(coordinate));
                call.enqueue(new RetryCallback<>(new Callback<OpenElevationMessage>() {
                    @Override
                    public void onResponse(@NotNull Call<OpenElevationMessage> call, @NotNull Response<OpenElevationMessage> response) {
                        if (response.body() != null && response.body().getResults().size() > 0) {
                            Data output = new Data.Builder()
                                    .putDouble("elevation", response.body().getResults().get(0).getElevation())
                                    .build();
                            completer.set(Result.success(output));
                        } else {
                            completer.set(Result.success());
                        }

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    }

                    @Override
                    public void onFailure(@NotNull Call<OpenElevationMessage> call, @NotNull Throwable t) {

                        // Set progress to 100 after you are done doing your work.
                        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                        completer.set(Result.failure());
                    }
                }));
            } catch (Exception e) {

            }

            return Result.success();
        });
    }
}
