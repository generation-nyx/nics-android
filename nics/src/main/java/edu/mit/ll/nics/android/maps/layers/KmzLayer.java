package edu.mit.ll.nics.android.maps.layers;

import android.app.Activity;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.List;
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

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class KmzLayer extends Layer {

    private final CollabroomDataLayer mLayer;
    private final PreferencesRepository mPreferences;


    public KmzLayer(Activity activity, GoogleMap map, CollabroomDataLayer layer, PreferencesRepository preferencesRepository) {
        super(activity, map, layer.getDisplayName());
        mLayer = layer;
        mPreferences = preferencesRepository;
    }


    private void addFeaturesToMap() {
        if (mLayer.getFeatures() != null) {
            ExecutorService service = Executors.newCachedThreadPool();
            List<LayerFeature> features = new ArrayList<>(mLayer.getFeatures());
            for (LayerFeature feature : features) {
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

    @Override
    public void unregister() {
        Timber.tag(DEBUG).i("unregister");
    }

    @Override
    public void removeFromMap() {
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



