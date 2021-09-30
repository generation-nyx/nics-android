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
package edu.mit.ll.nics.android.ui.adapters;

import android.app.Activity;
import android.os.Handler;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.work.WorkManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.TransformerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingRoomLayer;
import edu.mit.ll.nics.android.database.entities.Tracking;
import edu.mit.ll.nics.android.di.Qualifiers.DiskExecutor;
import edu.mit.ll.nics.android.di.Qualifiers.MainHandler;
import edu.mit.ll.nics.android.di.Qualifiers.NetworkExecutor;
import edu.mit.ll.nics.android.maps.LocationSegment;
import edu.mit.ll.nics.android.maps.layers.ArcGISLayer;
import edu.mit.ll.nics.android.maps.layers.Layer;
import edu.mit.ll.nics.android.maps.layers.LayerType;
import edu.mit.ll.nics.android.maps.layers.RoomLayer;
import edu.mit.ll.nics.android.maps.layers.TrackingLayer;
import edu.mit.ll.nics.android.maps.layers.WfsLayer;
import edu.mit.ll.nics.android.maps.layers.WmsLayer;
import edu.mit.ll.nics.android.maps.layers.reports.EODReportLayer;
import edu.mit.ll.nics.android.maps.layers.reports.GeneralMessageLayer;
import edu.mit.ll.nics.android.maps.markup.DistancePolyline;
import edu.mit.ll.nics.android.maps.markup.HazardPolygon;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.repository.TrackingLayerRepository;
import edu.mit.ll.nics.android.utils.Diff;
import edu.mit.ll.nics.android.utils.DiffableHashMap;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.MapUtils.getShapeFromFeature;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class MapAdapter {

    private final GoogleMap mMap;
    private final View mRootView;
    private final Activity mActivity;
    private final LifecycleOwner mLifecycleOwner;
    private final PreferencesRepository mPreferences;
    private final WorkManager mWorkManager;
    private final SettingsRepository mSettings;
    private final NetworkRepository mNetworkRepository;
    private final TrackingLayerRepository mTrackingLayerRepository;
    private final GeneralMessageRepository mGeneralMessageRepository;
    private final EODReportRepository mEODRepository;
    private final Handler mMainHandler;
    private final ExecutorService mExecutor;
    private final ExecutorService mDiskExecutor;
    private final DownloaderApiService mDownloader;

    private long mEditingFeature;
    private boolean mIsEditing = false;
    private MarkupSymbol mReportMarker;
    private DistancePolyline mDistancePolyline;
    private final DiffableHashMap<Long, MarkupBaseShape, MarkupFeature> mMarkupFeatures;
    private final DiffableHashMap<Long, HazardPolygon, Hazard> mHazards;
    private final ConcurrentHashMap<String, Layer> mTrackingLayers = new ConcurrentHashMap<>();
    private final DiffableHashMap<String, Layer, CollabroomDataLayer> mCollabroomLayers;
    private final DiffableHashMap<String, RoomLayer, OverlappingRoomLayer> mOverlappingRoomLayers;

    @AssistedInject
    public MapAdapter(@Assisted Activity activity,
                      @Assisted LifecycleOwner lifecycleOwner,
                      @Assisted GoogleMap map,
                      @Assisted View rootView,
                      PreferencesRepository preferences,
                      WorkManager workManager,
                      SettingsRepository settings,
                      NetworkRepository networkRepository,
                      TrackingLayerRepository trackingLayerRepository,
                      GeneralMessageRepository generalMessageRepository,
                      EODReportRepository eodReportRepository,
                      @MainHandler Handler mainHandler,
                      @NetworkExecutor ExecutorService executor,
                      @DiskExecutor ExecutorService diskExecutor,
                      DownloaderApiService downloader) {

        mActivity = activity;
        mLifecycleOwner = lifecycleOwner;
        mMap = map;
        mPreferences = preferences;
        mWorkManager = workManager;
        mSettings = settings;
        mNetworkRepository = networkRepository;
        mTrackingLayerRepository = trackingLayerRepository;
        mGeneralMessageRepository = generalMessageRepository;
        mEODRepository = eodReportRepository;
        mMainHandler = mainHandler;
        mExecutor = executor;
        mDiskExecutor = diskExecutor;
        mDownloader = downloader;
        mRootView = rootView;

        mMarkupFeatures = new DiffableHashMap<>(new Diff<Long, MarkupBaseShape, MarkupFeature>() {
            @Override
            public void add(Long key, MarkupFeature item) {
                MarkupBaseShape shape = getShapeFromFeature(item, mMap, mPreferences, mActivity);
                mActivity.runOnUiThread(() -> {
                    try {
                        shape.addToMap();
                        shape.setClickable(!mIsEditing);
                        mMarkupFeatures.put(key, shape);
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void replace(Long key, MarkupFeature item) {
                mActivity.runOnUiThread(() -> {
                    remove(key);
                    add(key, item);
                });
            }

            @Override
            public void remove(Long key) {
                mActivity.runOnUiThread(() -> {
                    try {
                        MarkupBaseShape shape = mMarkupFeatures.get(key);
                        if (shape != null) {
                            shape.removeFromMap();
                        }
                        mMarkupFeatures.remove(key);
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void removeAll() {
                mActivity.runOnUiThread(() -> {
                    try {
                        ArrayList<MarkupBaseShape> shapes = new ArrayList<>(mMarkupFeatures.values());
                        for (MarkupBaseShape shape : shapes) {
                            if (shape != null) {
                                shape.removeFromMap();
                            }
                        }
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                    mMarkupFeatures.clear();
                });
            }
        });

        mHazards = new DiffableHashMap<>(new Diff<Long, HazardPolygon, Hazard>() {
            @Override
            public void add(Long key, Hazard item) {
                mActivity.runOnUiThread(() -> {
                    try {
                        HazardPolygon hazard = new HazardPolygon(item, mMap, mPreferences, mActivity);
                        hazard.addToMap();
                        hazard.setClickable(!mIsEditing);
                        mHazards.put(key, hazard);
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void replace(Long key, Hazard item) {
                mActivity.runOnUiThread(() -> {
                    remove(key);
                    add(key, item);
                });
            }

            @Override
            public void remove(Long key) {
                mActivity.runOnUiThread(() -> {
                    try {
                        HazardPolygon hazard = mHazards.get(key);
                        if (hazard != null) {
                            hazard.removeFromMap();
                        }
                        mHazards.remove(key);
                    } catch (Exception e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void removeAll() {
                mActivity.runOnUiThread(() -> {
                    try {
                        for (HazardPolygon shape : mHazards.values()) {
                            if (shape != null) {
                                shape.removeFromMap();
                            }
                        }
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                    mHazards.clear();
                });
            }
        });

        mCollabroomLayers = new DiffableHashMap<>(new Diff<String, Layer, CollabroomDataLayer>() {
            @Override
            public void add(String key, CollabroomDataLayer item) {
                LayerType type = LayerType.lookUp(item.getTypeName());

                Layer tempLayer = null;
                if (LayerType.WMS.equals(type)) {
                    tempLayer = new WmsLayer(mActivity, item, mMap, mDownloader);
                } else if (LayerType.ARCGIS_REST.equals(type)) {
                    tempLayer = new ArcGISLayer(mActivity, item, mMap, mDownloader);
                } else if (LayerType.GEOJSON.equals(type) || LayerType.WFS.equals(type)) {
                    tempLayer = new WfsLayer(mActivity, mMap, item, mPreferences);
                } else {
                    Snackbar.make(mRootView, String.format("%s layer type is not yet available.", item.getTypeName()), Snackbar.LENGTH_SHORT).show();
                }

                Layer layer = tempLayer;
                mActivity.runOnUiThread(() -> {
                    try {
                        if (layer != null) {
                            layer.addToMap();
                            for (MarkupBaseShape shape : layer.getFeatures()) {
                                shape.setClickable(!mIsEditing);
                            }
                        }
                        mCollabroomLayers.put(key, layer);
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void replace(String key, CollabroomDataLayer item) {
                mActivity.runOnUiThread(() -> {
                    remove(key);
                    add(key, item);
                });
            }

            @Override
            public void remove(String key) {
                mActivity.runOnUiThread(() -> {
                    try {
                        Layer layer = mCollabroomLayers.get(key);
                        if (layer != null) {
                            layer.unregister();
                            layer.removeFromMap();
                            mCollabroomLayers.remove(key);
                        }
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void removeAll() {
                ArrayList<Layer> layers = new ArrayList<>(mCollabroomLayers.values());
                mActivity.runOnUiThread(() -> {
                    try {
                        for (Layer layer : layers) {
                            if (layer != null) {
                                layer.unregister();
                                layer.removeFromMap();
                            }
                        }
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                    mCollabroomLayers.clear();
                });
            }
        });

        mOverlappingRoomLayers = new DiffableHashMap<>(new Diff<String, RoomLayer, OverlappingRoomLayer>() {
            @Override
            public void add(String key, OverlappingRoomLayer item) {
                mActivity.runOnUiThread(() -> {
                    try {
                        RoomLayer layer = new RoomLayer(mActivity, mMap, item, mPreferences);
                        layer.addToMap();
                        for (MarkupBaseShape shape : layer.getFeatures()) {
                            shape.setClickable(!mIsEditing);
                        }
                        mOverlappingRoomLayers.put(key, layer);
                    } catch (Exception e) {
                        Timber.tag(DEBUG).d(e);
                    }
                });
            }

            @Override
            public void replace(String key, OverlappingRoomLayer item) {
                mActivity.runOnUiThread(() -> {
                    remove(key);
                    add(key, item);
                });
            }

            @Override
            public void remove(String key) {
                Layer layer = mOverlappingRoomLayers.get(key);
                if (layer != null) {
                    mActivity.runOnUiThread(() -> {
                        try {
                            layer.unregister();
                            layer.removeFromMap();
                            mOverlappingRoomLayers.remove(key);
                        } catch (NullPointerException e) {
                            Timber.tag(DEBUG).d(e);
                        }
                    });
                }
            }

            @Override
            public void removeAll() {
                ArrayList<RoomLayer> layers = new ArrayList<>(mOverlappingRoomLayers.values());
                mActivity.runOnUiThread(() -> {
                    try {
                        for (Layer layer : layers) {
                            if (layer != null) {
                                layer.unregister();
                                layer.removeFromMap();
                            }
                        }
                    } catch (NullPointerException e) {
                        Timber.tag(DEBUG).d(e);
                    }
                    mOverlappingRoomLayers.clear();
                });
            }
        });
    }

    public void setMarkupFeatures(List<MarkupFeature> features) {
        mDiskExecutor.submit(() -> mMarkupFeatures.diff(features, "getId", "getFeature"));
    }

    public Set<Map.Entry<Long, MarkupBaseShape>> getMarkupFeatures() {
        return mMarkupFeatures.entrySet();
    }

    public void setHazards(List<Hazard> hazards) {
        mDiskExecutor.submit(() -> mHazards.diff(hazards, "getId", "getHazard"));
    }

    public void setOverlappingRoomLayers(List<OverlappingRoomLayer> layers) {
        mDiskExecutor.submit(() -> mOverlappingRoomLayers.diff(layers, "getCollabroomName", "getLayer", "isActive"));
    }

    public void setCollabroomLayers(List<CollabroomDataLayer> layers) {
        mDiskExecutor.submit(() -> mCollabroomLayers.diff(layers, "getDisplayName", "getLayer", "isActive"));
    }

    public void toggleHazards(boolean isShowHazards) {
        for (HazardPolygon hazard : mHazards.values()) {
            hazard.setVisibility(isShowHazards);
        }
    }

    public void setEditing(boolean editing) {
        mIsEditing = editing;

        for (HazardPolygon hazard : mHazards.values()) {
            hazard.setClickable(!editing);
        }

        for (MarkupBaseShape shape : mMarkupFeatures.values()) {
            shape.setClickable(!editing);
        }

        for (Layer trackingLayer : mTrackingLayers.values()) {
            for (MarkupBaseShape shape : trackingLayer.getFeatures()) {
                shape.setClickable(!editing);
            }
        }

        for (Layer layer : mCollabroomLayers.values()) {
            for (MarkupBaseShape shape : layer.getFeatures()) {
                shape.setClickable(!editing);
            }
        }

        for (Layer layer : mOverlappingRoomLayers.values()) {
            for (MarkupBaseShape shape : layer.getFeatures()) {
                shape.setClickable(!editing);
            }
        }
    }

    public void setEditingFeature(long id) {
        if (id == -1L) {
            if (mEditingFeature != id && mMarkupFeatures.containsKey(mEditingFeature)) {
                MarkupBaseShape shape = mMarkupFeatures.get(mEditingFeature);
                if (shape != null) {
                    shape.addToMap();
                }
            }
        } else {
            if (mMarkupFeatures.containsKey(id)) {
                MarkupBaseShape shape = mMarkupFeatures.get(id);
                if (shape != null) {
                    shape.removeFromMap();
                }
            }
        }

        mEditingFeature = id;
    }

    /**
     * Sets the tracking layers that are currently showing on the map.
     * Add the tracking layer to the map if it's marked as active and hasn't already been added.
     * Remove the layer if it's not active and it's still on the map.
     * Remove any layers that are on the map, but aren't in this updated trackingLayers list.
     *
     * @param layers The list of {@link Tracking} payloads from the database that triggered this
     *                  update.
     */
    public void setTrackingLayers(List<Tracking> layers) {
        if (layers == null || layers.size() == 0) {
            removeAllTrackingLayers();
        } else {
            for (Tracking tracking : layers) {
                if (tracking.isActive()) {
                    if (mTrackingLayers.containsKey(tracking.getLayerName())) {
                        removeTrackingLayer(tracking.getLayerName());
                    }
                    addTrackingLayer(tracking, mMap);
                } else {
                    removeTrackingLayer(tracking.getLayerName());
                }
            }

            ArrayList<String> layerNames = new ArrayList<>(CollectionUtils.collect(layers,
                    TransformerUtils.invokerTransformer("getLayerName")));

            ArrayList<String> toRemove = new ArrayList<>(CollectionUtils.subtract(mTrackingLayers.keySet(), layerNames));

            for (String name : toRemove) {
                removeTrackingLayer(name);
            }
        }
    }

    // TODO can just use mMap instead of passing parameter.
    private void addTrackingLayer(Tracking tracking, GoogleMap map) {
        if (tracking.isGeneralMessageLayer()) {
            GeneralMessageLayer layer = new GeneralMessageLayer(mActivity, map, tracking, mLifecycleOwner, mPreferences, mGeneralMessageRepository);
            for (MarkupBaseShape shape : layer.getFeatures()) {
                shape.setClickable(!mIsEditing);
            }
            mTrackingLayers.put(tracking.getLayerName(), layer);
        } else if (tracking.isEODReportLayer()) {
            EODReportLayer layer = new EODReportLayer(mActivity, map, tracking, mLifecycleOwner, mPreferences, mEODRepository);
            for (MarkupBaseShape shape : layer.getFeatures()) {
                shape.setClickable(!mIsEditing);
            }
            mTrackingLayers.put(tracking.getLayerName(), layer);
        } else {
            TrackingLayer trackingLayer = new TrackingLayer(mActivity, map, tracking,
                    mLifecycleOwner, mPreferences, mWorkManager, mSettings,
                    mNetworkRepository, mTrackingLayerRepository, mMainHandler, mExecutor, mDownloader);
            for (MarkupBaseShape shape : trackingLayer.getFeatures()) {
                shape.setClickable(!mIsEditing);
            }
            mTrackingLayers.put(tracking.getLayerName(), trackingLayer);
        }
    }

    private void removeTrackingLayer(String name) {
        Layer trackingLayer = mTrackingLayers.get(name);
        if (trackingLayer != null) {
            trackingLayer.unregister();
            trackingLayer.removeFromMap();
        }
        mTrackingLayers.remove(name);
    }

    private void removeAllTrackingLayers() {
        for (Layer layer : mTrackingLayers.values()) {
            if (layer != null) {
                layer.unregister();
                layer.removeFromMap();
            }
        }
        mTrackingLayers.clear();
    }

    // TODO I think we can remove the report marker, as I just use the report layers instead.
    public void addReportMarker(MarkupSymbol symbol) {
        symbol.addToMap();
        mReportMarker = symbol;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(symbol.getPoint(), 12));
    }

    public void removeReportMarker() {
        if (mReportMarker != null) {
            mReportMarker.removeFromMap();
            mReportMarker = null;
        }
    }

    public void setDistancePolyline(LocationSegment distancePolyline) {
        removeDistancePolyline();

        if (distancePolyline != null) {
            addDistancePolyline(distancePolyline);
        }
    }

    public void addDistancePolyline(LocationSegment distancePolyline) {
        mDistancePolyline = new DistancePolyline(distancePolyline, mMap, mPreferences, mActivity);
        mDistancePolyline.addToMap();
    }

    public void removeDistancePolyline() {
        if (mDistancePolyline != null) {
            mDistancePolyline.removeFromMap();
            mDistancePolyline = null;
        }
    }

    @AssistedFactory
    public interface MapAdapterFactory {
        MapAdapter create(Activity activity, LifecycleOwner lifecycleOwner, GoogleMap map, View rootView);
    }
}
