package edu.mit.ll.nics.android.maps.tileproviders;

import android.util.LruCache;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CachingTileProvider implements TileProvider {

    private static final String TILE_URL = "https://mt1.google.com/vt/lyrs=m&x=%d&y=%d&z=%d";
    private LruCache<String, byte[]> tileCache;

    public CachingTileProvider() {
        tileCache = new LruCache<>(1024 * 1024 * 10); // 10MB cache
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        String key = x + "_" + y + "_" + zoom;
        byte[] tileData = tileCache.get(key);

        if (tileData == null) {
            tileData = downloadTile(x, y, zoom);
            if (tileData != null) {
                tileCache.put(key, tileData);
            }
        }

        return tileData == null ? NO_TILE : new Tile(256, 256, tileData);
    }

    private byte[] downloadTile(int x, int y, int zoom) {
        String url = String.format(TILE_URL, x, y, zoom);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
