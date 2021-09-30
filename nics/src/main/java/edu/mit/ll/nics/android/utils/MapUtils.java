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

import android.app.Activity;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonObject;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.maps.markup.FirelineType;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.maps.markup.MarkupFireLine;
import edu.mit.ll.nics.android.maps.markup.MarkupPolygon;
import edu.mit.ll.nics.android.maps.markup.MarkupSegment;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.maps.markup.MarkupText;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

import static edu.mit.ll.nics.android.maps.markup.MarkupBaseShape.ANCHOR_CENTER;
import static edu.mit.ll.nics.android.utils.GeoUtils.convertPointsToLatLng;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLatLngBounds;
import static edu.mit.ll.nics.android.utils.UnitConverter.IMPERIAL;
import static edu.mit.ll.nics.android.utils.UnitConverter.METRIC;
import static edu.mit.ll.nics.android.utils.UnitConverter.NAUTICAL;

/**
 * Utility class for integrating NICS map features with a {@link GoogleMap} instance.
 */
public class MapUtils {

    /**
     * Convert a list of {@link Marker} markers to a list of {@link LatLng} points.
     *
     * @param markers The list of {@link Marker} markers to convert.
     * @return A list of {@link LatLng} points.
     */
    public static List<LatLng> markersToPoints(List<Marker> markers) {
        List<LatLng> points = new ArrayList<>();
        for (Marker marker : markers) {
            points.add(marker.getPosition());
        }

        return points;
    }

    /**
     * Zoom to the provided NICS markup feature {@link MarkupFeature}. If the feature is a
     * {@link Marker}, zoom to the coordinates, otherwise, zoom to the {@link LatLngBounds} of the
     * polygon.
     *
     * @param map The {@link GoogleMap} Google map instance to interact with.
     * @param feature The {@link MarkupFeature} to zoom to.
     */
    public static void zoomToFeature(GoogleMap map, MarkupFeature feature) {
        if (feature != null && map != null) {
            String type = feature.getType();

            ArrayList<LatLng> coordinates = convertPointsToLatLng(feature.getGeometryVector2());
            if (coordinates.size() > 0) {
                if (type.equals(MarkupType.marker.toString()) || type.equals(MarkupType.label.toString())) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates.get(0), 13));
                } else {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(coordinates), 200));
                }
            }
        }
    }

    /**
     * Zoom to the provided NICS markup feature {@link MarkupBaseShape}. If the feature is a
     * {@link Marker}, zoom to the coordinates, otherwise, zoom to the {@link LatLngBounds} of the
     * polygon.
     *
     * @param map The {@link GoogleMap} Google map instance to interact with.
     * @param feature The {@link MarkupBaseShape} to zoom to.
     */
    public static void zoomToFeature(GoogleMap map, MarkupBaseShape feature) {
        if (feature != null && map != null) {
            if (feature.getType().equals(MarkupType.marker) || feature.getType().equals(MarkupType.label)) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(feature.getPoints().get(0), 13));
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(feature.getPoints()), 200));
            }
        }
    }

    /**
     * Converts NICS markup features {@link MarkupFeature} to markup shapes {@link MarkupBaseShape}
     * to be used with a {@link GoogleMap} Google map instance.
     *
     * @param features The list of {@link MarkupFeature} markup features to convert.
     * @param map The {@link GoogleMap} Google map instance that the shapes with be associated with.
     * @param preferences The {@link PreferencesRepository} that the shapes will be associated with.
     * @param activity The {@link Activity} that the shapes will be associated with.
     * @return A list of {@link MarkupBaseShape} markup shapes.
     */
    public static ArrayList<MarkupBaseShape> getShapesFromFeatures(List<MarkupFeature> features,
                                                                   GoogleMap map,
                                                                   PreferencesRepository preferences,
                                                                   Activity activity) {
        ArrayList<MarkupBaseShape> shapes = new ArrayList<>();
        for (MarkupFeature feature : features) {
            shapes.add(getShapeFromFeature(feature, map, preferences, activity));
        }

        return shapes;
    }

    /**
     * Converts a NICS markup feature {@link MarkupFeature} to a markup shape {@link MarkupBaseShape}
     * to be used with a {@link GoogleMap} Google map instance.
     *
     * @param feature The {@link MarkupFeature} markup feature to convert.
     * @param map The {@link GoogleMap} Google map instance that the shape with be associated with.
     * @param preferences The {@link PreferencesRepository} that the shape will be associated with.
     * @param activity The {@link Activity} that the shape will be associated with.
     * @return A {@link MarkupBaseShape} markup shape.
     */
    public static MarkupBaseShape getShapeFromFeature(MarkupFeature feature,
                                                      GoogleMap map,
                                                      PreferencesRepository preferences,
                                                      Activity activity) {
        MarkupBaseShape shape = null;
        MarkupType type = MarkupType.valueOf(feature.getType());
        switch (type) {
            case marker:
                shape = new MarkupSymbol(map, preferences, activity, feature, false);
                break;
            case label:
                shape = new MarkupText(map, preferences, activity, feature, false);
                break;
            case sketch:
                if (feature.getDashStyle() == null || feature.getDashStyle().equals("solid")) {
                    MarkupFeature.Attributes attributes = feature.getAttributes();
                    if (attributes.getDescription() != null && attributes.getDescription().equals(FirelineType.FIRE_SPREAD_PREDICTION.getName())) {
                        feature.setDashStyle(FirelineType.FIRE_SPREAD_PREDICTION.getType());
                        shape = new MarkupFireLine(map, preferences, activity, feature);
                    } else if (attributes.getDescription() != null && attributes.getDescription().equals(FirelineType.COMPLETED_FIRELINE.getName())) {
                        feature.setDashStyle(FirelineType.COMPLETED_FIRELINE.getType());
                        shape = new MarkupFireLine(map, preferences, activity, feature);
                    } else {
                        feature.setDashStyle("solid");
                        shape = new MarkupSegment(map, preferences, activity, feature);
                    }
                } else {
                    shape = new MarkupFireLine(map, preferences, activity, feature);
                }
                break;
            case circle:
            case square:
            case hexagon:
            case polygon:
            case triangle:
                shape = new MarkupPolygon(map, preferences, activity, feature);
                break;
        }

        return shape;
    }

    /**
     * Computes the total distance over a list of {@link LatLng} coordinate points. The value is
     * calculated based upon the provided coordinate system.
     *
     * @param points The list of {@link LatLng} points to calculate over.
     * @param system The coordinate representation system to use in the calculation.
     * @return The total distance of the points.
     */
    public static double computeDistance(List<LatLng> points, String system) {
        double distance = computeDistance(points);
        if (distance != 0) {
            switch (system) {
                case METRIC:
                    distance = distance / 1000;
                    break;
                case IMPERIAL:
                    distance = distance / 1609.344;
                    break;
                case NAUTICAL:
                    distance = distance / 1852;
                    break;
            }
        }
        return distance;
    }

    /**
     * Computes the distance using the {@link SphericalUtil#computeLength(List)} utility method
     * provided by Google.
     *
     * @param points The list of {@link LatLng} points to calculate over.
     * @return The total distance of the points.
     */
    public static double computeDistance(List<LatLng> points) {
        double distance = 0d;
        if (points.size() > 0) {
            distance = SphericalUtil.computeLength(points);
        }
        return distance;
    }

    /**
     * Computes the total area over a list of {@link LatLng} coordinate points. The value is
     * calculated based upon the provided coordinate system.
     *
     * @param points The list of {@link LatLng} points to calculate over.
     * @param system The coordinate representation system to use in the calculation.
     * @return The total area of the points.
     */
    public static double computeArea(List<LatLng> points, String system) {
        double area = computeArea(points);
        if (area != 0) {
            switch (system) {
                case METRIC:
                    area = area / 1000000;
                    break;
                case IMPERIAL:
                    area = area / 4046.856;
                    break;
                case NAUTICAL:
                    area = area / 3430000;
                    break;
            }
        }
        return area;
    }

    /**
     * Computes the area using the {@link SphericalUtil#computeArea(List)} (List)} utility
     * method provided by Google.
     *
     * @param points The list of {@link LatLng} points to calculate over.
     * @return The total area of the points.
     */
    public static double computeArea(List<LatLng> points) {
        double area = 0d;
        if (points.size() > 0) {
            area = SphericalUtil.computeArea(points);
        }
        return area;
    }

    /**
     * Create a basic marker {@link MarkerOptions} provided the {@link LatLng} coordinate point.
     *
     * @param latLng The {@link LatLng} coordinate point of where the marker be located.
     * @return {@link MarkerOptions} Marker options to be used to add a {@link Marker} to the map.
     */
    public static MarkerOptions createMarker(LatLng latLng) {
        return new MarkerOptions()
                .anchor(ANCHOR_CENTER, ANCHOR_CENTER)
                .position(latLng)
                .draggable(true);
    }

    public static MarkerOptions createMarker(LatLng latLng, BitmapDescriptor bitmapDescriptor) {
        return new MarkerOptions()
                .anchor(ANCHOR_CENTER, ANCHOR_CENTER)
                .position(latLng)
                .icon(bitmapDescriptor)
                .draggable(true);
    }

    public static MarkerOptions createInfoMarker(LatLng midPoint, JsonObject attr) {
        return new MarkerOptions()
                .alpha(0.0f)
                .infoWindowAnchor(0.6f, 1.0f)
                .position(midPoint)
                .title(attr.toString());
    }

    /**
     * Gets a radius point at a default heading of 0 depending on the center {@link LatLng} point.
     *
     * @param center The center {@link LatLng} point.
     * @param radius The radius of the circle.
     * @return The radius point at a heading value of 0.
     */
    public static LatLng getRadiusPoint(LatLng center, double radius) {
        return getRadiusPoint(center, radius, 0);
    }

    /**
     * Gets a radius point at the provided heading depending on the center {@link LatLng} point.
     *
     * @param center The center {@link LatLng} point.
     * @param radius The radius of the circle.
     * @param heading The heading angle of the circle.
     * @return The radius point at a heading value.
     */
    public static LatLng getRadiusPoint(LatLng center, double radius, double heading) {
        return SphericalUtil.computeOffset(center, radius, heading);
    }

    /**
     * Gets the radius value based upon two {@link LatLng} points.
     *
     * @param start The starting point.
     * @param end The ending point.
     * @return The radius value.
     */
    public static float getRadius(LatLng start, LatLng end) {
        float[] distance = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, distance);
        return distance[0];
    }
}
