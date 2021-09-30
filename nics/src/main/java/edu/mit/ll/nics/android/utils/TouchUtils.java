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
package edu.mit.ll.nics.android.utils;

import android.graphics.Point;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

public class TouchUtils {

    // Android Material Design guidelines suggest touch targets should be at least 48dp^2.
    public static final int dpTolerance = 24;

    public static boolean isGeoMarkerTouched(Marker marker, LatLng touch, Projection projection) {
        // WARNING: This math assumes the default pushpin Marker icon is being used.
        // At present CNS doesn't see a way to get the icon dimensions from the Marker.
        // MORE WARNING: This math also assumes the Marker is not rotated.
        // IMPORTANT: We don't reuse isLocationInBox because the icon for Marker is not centered on its LatLng.

        int baseDim = 40; // Marker is a little wider than the recommended minimum touch target

        // Experience says make the box a little taller because people like to touch the pin itself,
        // not its insertion point.
        Point pinPt = projection.toScreenLocation(marker.getPosition());
        Point northeast = new Point(pinPt.x+baseDim, (int) (pinPt.y-(baseDim*3.5)));
        Point southwest = new Point(pinPt.x-baseDim, pinPt.y+baseDim);

        LatLngBounds markerBox = new LatLngBounds(projection.fromScreenLocation(southwest),
                projection.fromScreenLocation(northeast));
/*
        // use to visualize while testing
        Point northwest = new Point(pinPt.x-baseDim, (int) (pinPt.y-(baseDim*3.5)));
        Point southeast = new Point(pinPt.x+baseDim, pinPt.y+baseDim);
        Polygon polygon = map.addPolygon(new PolygonOptions()
                .add(projection.fromScreenLocation(southwest), projection.fromScreenLocation(northwest),
                        projection.fromScreenLocation(northeast), projection.fromScreenLocation(southeast),
                        projection.fromScreenLocation(southwest))
                .strokeColor(Color.BLUE)
                .fillColor(Color.TRANSPARENT));
*/
        return markerBox.contains(touch);
    }

    public static boolean isLocationInBox(LatLng touch, LatLng center, int width, int height,
                                          Projection projection) {
        // not going to sweat half a dp
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        Point touchPt = projection.toScreenLocation(touch);
        Point northeast = new Point(touchPt.x+(halfWidth+dpTolerance), touchPt.y-(halfHeight+dpTolerance));
        Point southwest = new Point(touchPt.x-(halfWidth+dpTolerance), touchPt.y+(halfHeight+dpTolerance));

        LatLngBounds box = new LatLngBounds(projection.fromScreenLocation(southwest),
                projection.fromScreenLocation(northeast));
/*
        // use to visualize while testing
        Point northwest = new Point(touchPt.x-(halfWidth+dpTolerance), touchPt.y-(halfHeight+dpTolerance));
        Point southeast = new Point(touchPt.x+(halfWidth+dpTolerance), (int) (touchPt.y+(halfHeight+dpTolerance)));
        Polygon polygon = mMap.addPolygon(new PolygonOptions()
                .add(projection.fromScreenLocation(southwest), projection.fromScreenLocation(northwest),
                        projection.fromScreenLocation(northeast), projection.fromScreenLocation(southeast),
                        projection.fromScreenLocation(southwest))
                .strokeColor(Color.BLUE)
                .fillColor(Color.TRANSPARENT));
*/
        return box.contains(center);
    }


    // Use for Polygon
    public static boolean isLocationOnEdge(LatLng touch, ArrayList<LatLng> points,
                                           Projection projection) {

        // Let's figure out how big the touch target is in meters, starting with 48dp^2
        Point touchPt = projection.toScreenLocation(touch);
        Point northeast = new Point(touchPt.x+dpTolerance, touchPt.y-dpTolerance);
        Point southwest = new Point(touchPt.x-dpTolerance, touchPt.y+dpTolerance);

        double distanceInMeters =
                SphericalUtil.computeDistanceBetween(projection.fromScreenLocation(northeast),
                                                     projection.fromScreenLocation(southwest));

        return PolyUtil.isLocationOnEdge(touch, points, true, distanceInMeters/2);

    }

    // Use for Polyline
    public static boolean isLocationOnPath(LatLng touch, ArrayList<LatLng> points,
                                           Projection projection) {

        // Let's figure out how big the touch target is in meters, starting with 48dp^2
        Point touchPt = projection.toScreenLocation(touch);
        Point northeast = new Point(touchPt.x+dpTolerance, touchPt.y-dpTolerance);
        Point southwest = new Point(touchPt.x-dpTolerance, touchPt.y+dpTolerance);

        double distanceInMeters =
                SphericalUtil.computeDistanceBetween(projection.fromScreenLocation(northeast),
                        projection.fromScreenLocation(southwest));

        return PolyUtil.isLocationOnPath(touch, points, true, distanceInMeters/2);

    }

    private static ArrayList<Point> toScreenLocation(Projection projection, ArrayList<LatLng> points) {
        ArrayList<Point> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            result.add(i, projection.toScreenLocation(points.get(i)));
        }
        return result;
    }
}
