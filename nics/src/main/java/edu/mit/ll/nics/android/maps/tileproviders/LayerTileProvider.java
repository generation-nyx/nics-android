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
package edu.mit.ll.nics.android.maps.tileproviders;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import edu.mit.ll.nics.android.api.DownloaderApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.httpToHttps;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public abstract class LayerTileProvider extends BBOXTileProvider implements TileProvider {

    protected final int mWidth;
    protected final int mHeight;
    protected final String mBaseUrl;
    protected final DownloaderApiService mDownloader;

    // Construct with tile size in pixels, normally 256, see parent class.
    LayerTileProvider(String baseUrl, int width, int height,
                      DownloaderApiService downloader) {
        mWidth = width;
        mHeight = height;
        mBaseUrl = baseUrl;
        mDownloader = downloader;
    }

    protected abstract String getTileUrl(int x, int y, int zoom);

    @Override
    public Tile getTile(int x, int y, int zoom) {
        String url = getTileUrl(x, y, zoom);
        if (!url.isEmpty()) {
            try {
                url = httpToHttps(url);
                Call<ResponseBody> call = mDownloader.download(url);
                Response<ResponseBody> response = call.execute();
                if (response.body() != null) {
                    try {
                        return new Tile(mWidth, mHeight, response.body().bytes());
                    } catch (Exception e) {
                        Timber.tag(DEBUG).w(e, "Failed to get tile byte data.");
                    }
                } else {
                    Timber.tag(DEBUG).w("Empty tile data response for tile.");
                }
            } catch (Exception e) {
                Timber.tag(DEBUG).w(e, "getTile Failed.");
            }
        }

        return NO_TILE;
    }
}