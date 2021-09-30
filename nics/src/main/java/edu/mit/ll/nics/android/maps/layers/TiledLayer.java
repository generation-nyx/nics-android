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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import edu.mit.ll.nics.android.api.DownloaderApiService;
import edu.mit.ll.nics.android.database.entities.CollabroomDataLayer;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public abstract class TiledLayer extends Layer {

    protected final String mType;
    protected final float mOpacity;
    protected TileOverlay mTileOverlay;
    protected final TileProvider mTileProvider;
    protected final CollabroomDataLayer mLayer;
    protected final DownloaderApiService mDownloader;

    public TiledLayer(Activity activity,
                      CollabroomDataLayer layer,
                      GoogleMap map,
                      DownloaderApiService downloader,
                      String type) {
        super(activity, map, layer.getDisplayName());

        mType = type;
        mLayer = layer;
        mDownloader = downloader;
        mOpacity = 1 - (float) layer.getOpacity();
        mTileProvider = buildTileProvider();
        Timber.tag(DEBUG).i("%s constructor.", mType);
    }

    @Override
    public void unregister() {
        Timber.tag(DEBUG).i("%s unregister.", mType);
    }

    @Override
    public void removeFromMap() {
        Timber.tag(DEBUG).i("%s clearFromMap.", mType);
        if (mTileOverlay != null) {
            mTileOverlay.remove();
        }
    }

    @Override
    public void addToMap() {
        mActivity.runOnUiThread(() -> mTileOverlay = mMap.addTileOverlay(
                new TileOverlayOptions()
                        .tileProvider(mTileProvider)
                        .transparency(mOpacity)
                        .zIndex(1)
                        .visible(true)));
    }

    @Override
    public CollabroomDataLayer getLayer() {
        return mLayer;
    }

    public abstract TileProvider buildTileProvider();
}
