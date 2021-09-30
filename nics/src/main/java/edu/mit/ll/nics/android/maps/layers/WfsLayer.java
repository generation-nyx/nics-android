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
package edu.mit.ll.nics.android.maps.layers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;

import com.google.android.gms.maps.GoogleMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.maps.markup.MarkupFireLine;
import edu.mit.ll.nics.android.maps.markup.MarkupPolygon;
import edu.mit.ll.nics.android.maps.markup.MarkupSegment;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.BitmapUtils.generateBitmap;
import static edu.mit.ll.nics.android.utils.BitmapUtils.generateText;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class WfsLayer extends Layer {

    private final CollabroomDataLayer mLayer;
    private final PreferencesRepository mPreferences;

    public WfsLayer(Activity activity,
                    GoogleMap map,
                    CollabroomDataLayer layer,
                    PreferencesRepository preferences) {
        super(activity, map, layer.getDisplayName());

        mLayer = layer;
        mPreferences = preferences;
    }

    private void addFeaturesToMap() {
        if (mLayer.getFeatures() != null) {
            ExecutorService service = Executors.newCachedThreadPool();

            for (LayerFeature feature : mLayer.getFeatures()) {
                service.execute(() -> addFeature(feature));
            }

            service.shutdown();

            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                service.shutdown();
            }
        }
    }

    private void addFeature(LayerFeature feature) {
        MarkupType type = MarkupType.valueOf(feature.getType());

        switch (type) {
            case marker:
                addMarker(feature);
                break;
            case polygon:
                addPolygon(feature);
                break;
            case sketch:
                String dashStyle = feature.getDashStyle();
                if (dashStyle == null || dashStyle.isEmpty() || dashStyle.equals("solid")) {
                    addPolyline(feature);
                } else {
                    addFireline(feature);
                }
                break;
        }
    }

    private void addMarker(LayerFeature feature) {
        MarkupSymbol symbol = new MarkupSymbol(mMap, mPreferences, mActivity, feature);
        mFeatures.add(symbol);
        mActivity.runOnUiThread(symbol::addToMap);
    }

    private void addPolygon(LayerFeature feature) {
        MarkupPolygon polygon = new MarkupPolygon(mMap, mPreferences, mActivity, feature);
        mFeatures.add(polygon);
        mActivity.runOnUiThread(polygon::addToMap);
    }

    private void addPolyline(LayerFeature feature) {
        MarkupSegment polyline = new MarkupSegment(mMap, mPreferences, mActivity, feature);
        mFeatures.add(polyline);
        mActivity.runOnUiThread(polyline::addToMap);
    }

    private void addFireline(LayerFeature feature) {
        MarkupFireLine fireLine = new MarkupFireLine(mMap, mPreferences, mActivity, feature);
        mFeatures.add(fireLine);
        mActivity.runOnUiThread(fireLine::addToMap);
    }

    private Bitmap getMarkerIcon(LayerFeature feature) {
        // Check to see if the label text is set and create a text label bitmap from it.
        if (!feature.getLabelText().isEmpty()) {
            return generateText(feature.getLabelText(), feature.getLabelSize(), feature.getFillColor(), Typeface.DEFAULT);
        }
        return null;
    }

    @Override
    public void unregister() {
        Timber.tag(DEBUG).i("unregister");
    }

    @Override
    public void removeFromMap() {
        Timber.tag(DEBUG).i("clearFromMap");
        for (MarkupBaseShape feature : mFeatures) {
            feature.removeFromMap();
        }
        mFeatures.clear();
    }

    @Override
    public void addToMap() {
        addFeaturesToMap();
    }

    @Override
    public CollabroomDataLayer getLayer() {
        return mLayer;
    }
}
