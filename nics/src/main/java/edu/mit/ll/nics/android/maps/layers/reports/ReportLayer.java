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
package edu.mit.ll.nics.android.maps.layers.reports;

import android.app.Activity;

import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.maps.GoogleMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.TransformerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.mit.ll.nics.android.database.entities.Report;
import edu.mit.ll.nics.android.maps.layers.Layer;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.maps.markup.ReportMarker;
import edu.mit.ll.nics.android.repository.PreferencesRepository;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public abstract class ReportLayer<T extends Report> extends Layer {

    protected final LifecycleOwner mLifecycleOwner;
    protected final PreferencesRepository mPreferences;
    protected final HashMap<Long, ReportMarker<T>> mReports = new HashMap<>();

   public ReportLayer(Activity activity,
                      GoogleMap map,
                      String name,
                      LifecycleOwner lifecycleOwner,
                      PreferencesRepository preferences) {
        super(activity, map, name);

        mLifecycleOwner = lifecycleOwner;
        mPreferences = preferences;
    }

    protected abstract void subscribeToUpdates();

    protected abstract void unsubscribeFromUpdates();

    protected abstract <R extends Report> void addReport(R report);

    protected abstract void removeReport(long id);

    @Override
    public List<MarkupBaseShape> getFeatures() {
        return new ArrayList<>(mReports.values());
    }

    @Override
    public void unregister() {
        unsubscribeFromUpdates();
    }

    @Override
    public void removeFromMap() {
        Timber.tag(DEBUG).d("Removing General Messages layer from map.");
        for (MarkupBaseShape shape : mReports.values()) {
            shape.removeFromMap();
            Timber.tag(DEBUG).d("Removed %s", shape.getTitle());
        }
        mReports.clear();
    }

    @Override
    public void addToMap() {
    }

    public void addToMap(MarkupBaseShape shape) {
        shape.addToMap();
    }

    protected <R extends Report> void updateMap(List<R> reports) {
        if (!emptyCheck(reports)) {
            for (R report : reports) {
                if (!mReports.containsKey(report.getId())) {
                    addReport(report);
                }
            }

            ArrayList<Long> ids = new ArrayList<>(CollectionUtils.collect(reports,
                    TransformerUtils.invokerTransformer("getId")));

            ArrayList<Long> toRemove = new ArrayList<>(CollectionUtils.subtract(mReports.keySet(), ids));

            for (Long id : toRemove) {
                removeReport(id);
            }
        } else {
            removeFromMap();
        }
    }
}
