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

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.CollabroomLayerApiService;
import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.auth.AuthCallback;
import edu.mit.ll.nics.android.data.messages.CollabroomLayerMessage;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.HazardInfo;
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.repository.CollabroomLayerRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.utils.UnitConverter;
import edu.mit.ll.nics.android.utils.WfsUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.FileUtils.clearDirectory;
import static edu.mit.ll.nics.android.utils.FileUtils.copyFile;
import static edu.mit.ll.nics.android.utils.FileUtils.createTempFile;
import static edu.mit.ll.nics.android.utils.FileUtils.deleteFile;
import static edu.mit.ll.nics.android.utils.GeoUtils.bufferGeometry;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertCoordinatesToGeometryString;
import static edu.mit.ll.nics.android.utils.GeoUtils.getPolygonForCircle;
import static edu.mit.ll.nics.android.utils.GeoUtils.parseGeojson;
import static edu.mit.ll.nics.android.utils.GeoUtils.parseGeojsonFile;
import static edu.mit.ll.nics.android.utils.GeoUtils.parseKmlFile;
import static edu.mit.ll.nics.android.utils.StringUtils.httpToHttps;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_ROOM_LAYERS_TEMP_FOLDER;

@HiltWorker
public class CollabroomLayersWorker extends AppWorker {

    private final CollabroomLayerRepository mRepository;
    private final PreferencesRepository mPreferences;
    private final CollabroomLayerApiService mApiService;
    private final DownloaderApiService mDownloader;

    @AssistedInject
    public CollabroomLayersWorker(@Assisted @NonNull Context context,
                                  @Assisted @NonNull WorkerParameters workerParams,
                                  CollabroomLayerRepository repository,
                                  PreferencesRepository preferences,
                                  CollabroomLayerApiService apiService,
                                  DownloaderApiService downloader) {
        super(context, workerParams);

        mRepository = repository;
        mPreferences = preferences;
        mApiService = apiService;
        mDownloader = downloader;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        // Initialize the progress to 0, so that any observers can be updated that the request has started.
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());

        return CallbackToFutureAdapter.getFuture(completer -> {
            long collabroomId = mPreferences.getSelectedCollabroomId();

            Call<CollabroomLayerMessage> call = mApiService.getCollabroomLayers(mPreferences.getSelectedWorkspaceId(), collabroomId);
            call.enqueue(new AuthCallback<>(new Callback<CollabroomLayerMessage>() {
                @Override
                public void onResponse(@NotNull Call<CollabroomLayerMessage> call, @NotNull Response<CollabroomLayerMessage> response) {
                    mPreferences.setLastSuccessfulServerCommsTimestamp(System.currentTimeMillis());

                    CollabroomLayerMessage message = response.body();
                    if (message != null && message.getLayers().size() > 0) {
                        parseCollabroomLayers(collabroomId, message.getLayers());
                        Timber.tag(DEBUG).i("Successfully received Collabroom Layers: %s", message.getCount());
                    } else {
                        Timber.tag(DEBUG).w("Received empty Collabroom Layers. Status Code: %s", response.code());
                    }

                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    completer.set(Result.success());
                }

                @Override
                public void onFailure(@NotNull Call<CollabroomLayerMessage> call, @NotNull Throwable t) {
                    Timber.tag(DEBUG).e("Failed to receive Collabroom Layers: %s", t.getMessage());

                    // Set progress to 100 after you are done doing your work.
                    setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());
                    completer.set(Result.failure());
                }
            }));

            return Result.success();
        });
    }

    private void parseCollabroomLayers(long collabroomId, ArrayList<CollabroomDataLayer> collabroomDataLayers) {
        int numParsed;
        List<CollabroomDataLayer> storedLayers = mRepository.getCollabroomLayers(collabroomId);

        HashMap<String, CollabroomDataLayer> layers = new HashMap<>();

        for (CollabroomDataLayer layer : collabroomDataLayers) {
            layers.put(String.valueOf(layer.getCollabroomDatalayerId()), layer);
        }

        // Remove any layers from local db that are no longer in the collabroom.
        if (storedLayers != null) {
            for (CollabroomDataLayer layer : storedLayers) {
                if (!layers.containsKey(String.valueOf(layer.getCollabroomDatalayerId()))) {
                    mRepository.deleteCollabroomLayer(collabroomId, layer.getCollabroomDatalayerId());
                }
            }
        }

        // For each layer, set the embedded collabroom datalayer id to be the same as the parent datalayer so that we can perform a join.
        for (CollabroomDataLayer layer : collabroomDataLayers) {
            if (layer.getCollabroomDatalayers().size() > 0) {
                layer.getCollabroomDatalayers().get(0).setDatalayerId(layer.getDatalayerId());
            }
        }

        ExecutorService service = Executors.newCachedThreadPool();

        for (CollabroomDataLayer dataLayer : collabroomDataLayers) {
            service.execute(() -> {
                try {
                    ArrayList<LayerFeature> features = downloadLayerFile(dataLayer);

                    if (features != null) {
                        for (LayerFeature feature : features) {
                            feature.setDatalayerid(dataLayer.getDatalayerId());
                        }

                        HazardInfo hazardInfo = dataLayer.getHazardInfo();

                        try {
                            for (LayerFeature feature : features) {
                                feature.setDatalayerid(dataLayer.getDatalayerId());

                                if (dataLayer.getHazardInfo() != null && dataLayer.getHazardInfo().getRadius() > 0.0d) {
                                    double radius = hazardInfo.getRadius();

                                    if (hazardInfo.getMetric().equalsIgnoreCase("kilometer")) {
                                        radius = UnitConverter.kilometersToMeters(radius);
                                    }

                                    ArrayList<LatLng> points = feature.getCoordinates();
                                    ArrayList<LatLng> coordinates = new ArrayList<>();
                                    // If there is only one point, assume it's a marker, otherwise, assume it's a polygon.
                                    if (points.size() == 1) {
                                        coordinates = getPolygonForCircle(points.get(0), radius);
                                    } else if (points.size() > 1) {
                                        try {
                                            coordinates = bufferGeometry(convertCoordinatesToGeometryString(points, feature.getType()), feature.getType(), radius);
                                        } catch (Exception e) {
                                            Timber.tag(DEBUG).e(e, "Failed to buffer geometry for geofence boundary.");
                                        }
                                    }

                                    Hazard hazard = new Hazard(feature.getLayerFeatureId(), hazardInfo.getHazardLabel(), hazardInfo.getHazardType(), hazardInfo.getRadius(), hazardInfo.getMetric(),
                                            convertCoordinatesToGeometryString(coordinates, "polygon"), collabroomId, coordinates);

                                    hazard.setHazardLayerId(feature.getLayerFeatureId());
                                    feature.setHazard(hazard);
                                }
                            }
                        } catch (Exception e) {
                            Timber.tag(DEBUG).e("Failed to add layer feature.");
                        }
                    }

                    dataLayer.setFeatures(features);
                    dataLayer.setCollabroomId(collabroomId);
                    mRepository.addCollabroomLayerToDatabase(dataLayer);
                    Timber.tag(DEBUG).i("Downloaded %s", dataLayer.getDisplayName());
                } catch (AssertionError e) {
                    Timber.tag(DEBUG).e(e, "Failed to add collabroom layer.");
                }
            });
        }

        service.shutdown();

        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Timber.tag(DEBUG).e(e, "Error awaiting for service termination.");
        }

        numParsed = collabroomDataLayers.size();

        Timber.tag(DEBUG).i("Fetched %s collabroom layers.", numParsed);

        // Clean out any remaining temp files if for some reason they weren't removed.
        clearDirectory(mContext.getCacheDir() + NICS_ROOM_LAYERS_TEMP_FOLDER);
    }

    private ArrayList<LayerFeature> downloadLayerFile(CollabroomDataLayer layer) {
        String url;
        try {
            url = httpToHttps(layer.getInternalUrl());
        } catch (MalformedURLException e) {
            return null;
        }

        String name = layer.getLayername();
        String type = layer.getTypeName();

        Timber.tag(DEBUG).d("Layer name: %s", name);
        Timber.tag(DEBUG).d("Layer type: %s", type);
        Timber.tag(DEBUG).d("Layer URL: %s", url);

        switch (type) {
            case "wfs":
                url = new WfsUrl.Builder(url, name).build().getUrl();
                break;
            case "kmz":
                url = url + "/" + name;
                int lastSlashIndex = url.lastIndexOf('/');
                if (lastSlashIndex != -1) {
                    url = url.substring(0, lastSlashIndex);
                }
                url = url + ".kmz";
                Timber.tag(DEBUG).d("Layer URL Again: %s", url);
                break;
            case "geojson":
            case "gpx":
            case "kml":
                url = url + "/" + name;
                break;
            default:
                return null;
        }

        String tempDirectory = mContext.getCacheDir() + NICS_ROOM_LAYERS_TEMP_FOLDER;
        if (type.equals("wfs") || type.equals("geojson")) {
            Call<ResponseBody> call = mDownloader.download(url);
            try {
                Response<ResponseBody> response = call.execute();
                if (response.body() != null) {
                    try (InputStream stream = response.body().byteStream()) {
                        File file = createTempFile(tempDirectory);
                        Files.asByteSink(file).writeFrom(stream);
                        ArrayList<String> features = parseGeojsonFile(file);
                        deleteFile(file);
                        return parseGeojson(features);
                    } catch (IOException e) {
                        Timber.tag(DEBUG).e(e, "Failed to save and parse geojson response.");
                    }
                }
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Failed to execute wfs/geojson download call.");
            }
        } else if (type.equals("kml")) {
        } else if (type.equals("kmz")) {
           return downloadKmzFile(url, tempDirectory);
        }

        return null;
    }


    private ArrayList<LayerFeature> downloadKmzFile(String url, String tempDirectory) {
        Call<ResponseBody> call = mDownloader.download(url);
        try {
            Response<ResponseBody> response = call.execute();
            if (response.body() != null) {
                try (InputStream stream = response.body().byteStream()) {
                    File downloadedFile = createTempFile(tempDirectory);
                    Files.asByteSink(downloadedFile).writeFrom(stream);
                    ArrayList<LayerFeature> features = extractKmlFile(downloadedFile);
                    deleteFile(downloadedFile);
                    return features;
                } catch (IOException e) {
                    Timber.tag(DEBUG).e(e, "Failed to save and parse KML/KMZ response.");
                }
            }
        } catch (IOException e) {
            Timber.tag(DEBUG).e(e, "Failed to execute KML/KMZ download call.");
        }

        return null;
    }

    private ArrayList<LayerFeature> extractKmlFile(File kmzFile) {
        ArrayList<LayerFeature> features = new ArrayList<>();
        String uniqueTempDirName = UUID.randomUUID().toString();
        File kmzTempDir = new File(mContext.getCacheDir(), "kmz_temp_dir" + uniqueTempDirName);
        try {
            clearDirectory(kmzTempDir.getPath());
            unzip(kmzFile, kmzTempDir);
            File kmlFile = findKmlFile(kmzTempDir);
            if (kmlFile != null) {
                File nonKmlDir = cacheNonKmlFiles(kmzTempDir);
                features = parseKmlFile(kmlFile, nonKmlDir);
            }
        } catch (IOException e) {
            Timber.tag(DEBUG).e(e, "Failed to extract KMZ file.");
        } finally {
            clearDirectory(kmzTempDir.getPath());
        }

        return features;
    }

    private void unzip(File zipFile, File targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outputFile = new File(targetDirectory, entry.getName());
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }

    private File findKmlFile(File directory) {
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(".kml")) {
                return file;
            }
        }
        return null;
    }

    private File cacheNonKmlFiles(File directory) {
        File cacheDir = new File(mContext.getCacheDir(), "kmz_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            cacheFilesRecursively(directory, cacheDir);
        } catch (IOException e) {
            Timber.e(e, "Failed to cache non-KML files from directory: %s", directory.getName());
            return null;
        }
        return cacheDir;
    }

    private void cacheFilesRecursively(File sourceDir, File destinationDir) throws IOException {
        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                File newDir = new File(destinationDir, file.getName());
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
                cacheFilesRecursively(file, newDir);
            } else if (!file.getName().endsWith(".kml")) {
                File destFile = new File(destinationDir, file.getName());
                try (InputStream in = new FileInputStream(file);
                     OutputStream out = new FileOutputStream(destFile)) {
                    copyFile(in, out);
                } catch (IOException e) {
                    Timber.e(e, "Failed to cache file: %s", file.getName());
                    throw e;
                }
            }
        }
    }

}
