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

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLineString;
import com.google.maps.android.data.geojson.GeoJsonMultiLineString;
import com.google.maps.android.data.geojson.GeoJsonMultiPoint;
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.data.geojson.GeoJsonParser;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPolygon;

import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.Proj4jException;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import edu.mit.ll.nics.android.database.entities.LayerFeature;
import edu.mit.ll.nics.android.database.entities.OverlappingLayerFeature;
import edu.mit.ll.nics.android.database.entities.Vector2;
import edu.mit.ll.nics.android.enums.EPSG;
import timber.log.Timber;

import static edu.mit.ll.nics.android.enums.EPSG.EPSG_3857;
import static edu.mit.ll.nics.android.enums.EPSG.EPSG_4326;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.Geometry.GEOJSON_FEATURES;
import static edu.mit.ll.nics.android.utils.constants.Geometry.GEOJSON_GEOMETRIES;
import static edu.mit.ll.nics.android.utils.constants.Geometry.LINESTRING;
import static edu.mit.ll.nics.android.utils.constants.Geometry.MULTI_LINESTRING;
import static edu.mit.ll.nics.android.utils.constants.Geometry.MULTI_POINT;
import static edu.mit.ll.nics.android.utils.constants.Geometry.MULTI_POLYGON;
import static edu.mit.ll.nics.android.utils.constants.Geometry.POINT;
import static edu.mit.ll.nics.android.utils.constants.Geometry.POLYGON;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class GeoUtils {

    /**
     * Calculate the radius of the smallest circle that encloses the provided {@link LatLng} coordinates.
     *
     * @param points The {@link LatLng} geometry coordinate points to enclose.
     * @return The radius of the smallest circle that encloses the coordinates.
     * @throws IllegalStateException Geometry points can't be empty or LatLngBounds.Builder.build() will throw.
     * @see com.google.android.gms.maps.model.LatLngBounds.Builder#build().
     */
    public static int calculateSmallestEnclosingCircleRadius(ArrayList<LatLng> points) throws IllegalStateException {
        LatLng midPoint = calculateGeometryMidpoint(points);

        double radius = 0.0;
        for (LatLng point : points) {
            double distance = SphericalUtil.computeDistanceBetween(point, midPoint);
            if (distance > radius) {
                radius = distance;
            }
        }

        return Math.round((float) radius);
    }

    public static LatLng getCenterPoint(ArrayList<Vector2> points) {
        return calculateGeometryMidpoint(convertPointsToLatLng(points));
    }

    /**
     * Calculate the geometry's {@link LatLng} midpoint.
     *
     * @param geometry The geometry given as a list of {@link LatLng} coordinate points.
     * @return The {@link LatLng} coordinate of the geometry's midpoint.
     * @throws IllegalStateException Geometry points can't be empty or LatLngBounds.Builder.build() will throw.
     * @see com.google.android.gms.maps.model.LatLngBounds.Builder#build().
     */
    public static LatLng calculateGeometryMidpoint(List<LatLng> geometry) throws IllegalStateException {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : geometry) {
            builder.include(point);
        }

        return builder.build().getCenter();
    }

    /**
     * Convert the provided geometry string to {@link LatLng} coordinates using WKTReader().read()
     * to create a {@link Geometry} object out of the geometry string.
     *
     * @param geometryString The geometry string to convert.
     * @return The list of {@link LatLng} coordinates that make up the {@link Geometry} of the geometry string.
     * @throws ParseException Thrown by WKTReader.read().
     * @see org.locationtech.jts.io.WKTReader#read(Reader).
     */
    public static ArrayList<LatLng> geometryStringToCoordinates(String geometryString) throws ParseException {
        ArrayList<LatLng> coordinates = new ArrayList<>();

        Geometry geometry = new WKTReader().read(geometryString);
        for (Coordinate coordinate : geometry.getCoordinates()) {
            coordinates.add(new LatLng(coordinate.getY(), coordinate.getX()));
        }

        return coordinates;
    }

    /**
     * Convert the provided {@link Vector2} coordinates to a {@link Geometry} object.
     *
     * @param coordinates The {@link Vector2} coordinates to convert.
     * @param type        The geometry type that the coordinates represent.
     * @return The {@link Geometry} of the coordinates.
     * @throws ParseException Thrown if geometry string is invalid or the parsing fails.
     * @see org.locationtech.jts.io.WKTReader#read(Reader)
     */
    public static Geometry pointsToGeometry(ArrayList<Vector2> coordinates, String type) throws ParseException {
        return new WKTReader().read(convertPointsToGeometryString(coordinates, type));
    }

    /**
     * Convert a geometry string to a {@link Geometry} object.
     *
     * @param geometryString The geometry string to convert.
     * @return {@link Geometry} object that was create using the {@link WKTReader} to read the geometry string.
     * @throws ParseException Throws {@link ParseException} if the {@link WKTReader} fails to read the geometry string.
     */
    public static Geometry geometryStringToGeometry(String geometryString) throws ParseException {
        return new WKTReader().read(geometryString);
    }

    /**
     * Convert single {@link LatLng} coordinate to a geometry string.
     *
     * @param coordinate The {@link LatLng} to convert.
     * @return The geometry string.
     */
    public static String coordinateToGeometryString(LatLng coordinate) {
        return "POINT(" + coordinate.longitude + " " + coordinate.latitude + ")";
    }

    /**
     * Converts an {@link ArrayList} of {@link LatLng} coordinates into a geometry string.
     *
     * @param coordinates The {@link ArrayList<LatLng>} of coordinates.
     * @param type        The type of geometry that this {@link ArrayList<LatLng>} represents.
     * @return The geometry string.
     */
    public static String convertCoordinatesToGeometryString(ArrayList<LatLng> coordinates, String type) {
        return convertPointsToGeometryString(convertLatLngToPoints(coordinates), type);
    }

    /**
     * Converts an {@link ArrayList} of {@link Vector2} coordinates into a geometry string.
     *
     * @param points The {@link ArrayList<Vector2>} of coordinates.
     * @param type   The type of geometry that this {@link ArrayList<Vector2>} represents.
     * @return The geometry string.
     */
    public static String convertPointsToGeometryString(ArrayList<Vector2> points, String type) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (Vector2 coordinate : points) {
            double x = coordinate.x;
            double y = coordinate.y;

            if (first) {
                sb.append(x).append(" ").append(y);
                first = false;
            } else {
                sb.append(",").append(x).append(" ").append(y);
            }
        }

        String geometryString = sb.toString();

        switch (type) {
            case "marker":
            case "label":
                geometryString = "POINT(" + geometryString + ")";
                break;
            case "square":
            case "triangle":
            case "hexagon":
            case "polygon":
            case "circle":
                geometryString = "POLYGON((" + geometryString + "))";
                break;
            case "sketch":
                geometryString = "LINESTRING(" + geometryString + ")";
                break;
        }

        return geometryString;
    }

    public static Vector2 projectionTransformation(EPSG destination, LatLng latLng) throws Proj4jException {
        return projectionTransformation(EPSG_4326, destination, latLng);
    }

    public static Vector2 projectionTransformation(EPSG source, EPSG destination, LatLng latLng) throws Proj4jException {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem srcCrs = factory.createFromParameters(source.getName(), source.getParams());
        CoordinateReferenceSystem dstCrs = factory.createFromParameters(destination.getName(), destination.getParams());

        CoordinateTransform transform = new CoordinateTransformFactory().createTransform(srcCrs, dstCrs);
        ProjCoordinate dstCoord = transformCoordinate(transform, latLng.latitude, latLng.longitude);

        return new Vector2(dstCoord.y, dstCoord.x);
    }

    public static LatLng projectionTransformation(EPSG source, double latitude, double longitude) throws Proj4jException {
        return projectionTransformation(source, EPSG_4326, latitude, longitude);
    }

    public static LatLng projectionTransformation(EPSG source, EPSG destination, double latitude, double longitude) throws Proj4jException {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem srcCrs = factory.createFromParameters(source.getName(), source.getParams());
        CoordinateReferenceSystem dstCrs = factory.createFromParameters(destination.getName(), destination.getParams());

        // x,y = lon, lat so need to check if it's 4326 as input
        CoordinateTransform transform = new CoordinateTransformFactory().createTransform(srcCrs, dstCrs);
        ProjCoordinate dstCoord = transformCoordinate(transform, latitude, longitude);

        return new LatLng(dstCoord.y, dstCoord.x);
    }

    /**
     * Projects an {@link ArrayList} of {@link Vector2} coordinates from one EPSG Coordinate Reference System (CRS) to another CRS.
     *
     * @param source      The source CRS to project from.
     * @param destination The destination CRS to project to.
     * @param coordinates The {@link ArrayList<Vector2>} of coordinates to project.
     * @return The projected coordinates.
     * @throws Proj4jException     Throws a {@link Proj4jException} if the projection fails.
     */
    public static ArrayList<Vector2> projectionTransformation(EPSG source, EPSG destination, ArrayList<Vector2> coordinates) throws Proj4jException {
        CRSFactory factory = new CRSFactory();

        CoordinateReferenceSystem srcCrs = factory.createFromParameters(source.getName(), source.getParams());
        CoordinateReferenceSystem dstCrs = factory.createFromParameters(destination.getName(), destination.getParams());

        CoordinateTransform transform = new CoordinateTransformFactory().createTransform(srcCrs, dstCrs);

        ArrayList<Vector2> transformedCoordinates = new ArrayList<>();
        for (Vector2 coordinate : coordinates) {
            transformedCoordinates.add(projectionTransformation(transform, coordinate));
        }

        return transformedCoordinates;
    }

    /**
     * Transform the {@link Vector2} coordinate point using the {@link CoordinateTransform} projection definition.
     *
     * @param transform The projection definition that will be used to transform the coordinate.
     * @param point     The {@link Vector2} coordinate point to transform.
     * @return The {@link Vector2} coordinate point after transformation.
     * @throws Proj4jException Throws a {@link Proj4jException} if the transformation fails.
     */
    public static Vector2 projectionTransformation(CoordinateTransform transform, Vector2 point) throws Proj4jException {
        ProjCoordinate transformedCoordinate = transformCoordinate(transform, point.x, point.y);
        return new Vector2(transformedCoordinate.x, transformedCoordinate.y);
    }

    /**
     * Transform the coordinate points using the {@link CoordinateTransform} definition and return the {@link ProjCoordinate}.
     *
     * @param transform The {@link CoordinateTransform} that defines the projection transformation.S
     * @param x         The x coordinate value.
     * @param y         The y coordinate value.
     * @return The {@link ProjCoordinate} that is the result of the projection transformation.
     */
    public static ProjCoordinate transformCoordinate(CoordinateTransform transform, double x, double y) {
        ProjCoordinate srcCoord = new ProjCoordinate(x, y);
        ProjCoordinate dstCoord = new ProjCoordinate();

        transform.transform(srcCoord, dstCoord);

        return dstCoord;
    }

    /**
     * Buffer the provided geometry string with the given buffer (in meters). Assume the input geometry string is EPSG 4326
     * and project the coordinates to EPSG 3857, so that the unit of measurement is in meters, so that we can buffer the geometry
     * with the correct unit of measurement.
     *
     * @param geometryString The geometry string to
     * @param markupType     The type of geometry that the geometryString is, so that we can create the correct projected geometry string.
     * @param buffer         The buffer to apply to the geometryString in meters.
     * @return The {@link ArrayList<LatLng>} coordinates of the buffered geometry.
     * @throws ParseException       Throws {@link ParseException} if the geometry string can't be parsed into {@link ArrayList<LatLng>} coordinates.
     * @throws Proj4jException      Throws {@link Proj4jException} if the geometry can't be projected between the different Coordinate Reference Systems.
     * @throws NullPointerException Throws {@link NullPointerException} if the resulting {@link Geometry} object is null.
     */
    public static ArrayList<LatLng> bufferGeometry(String geometryString, String markupType, double buffer) throws ParseException, Proj4jException, NullPointerException {
        // Get the LatLng coordinates from the provided geometryString.
        ArrayList<LatLng> coordinates = geometryStringToCoordinates(geometryString);

        // Project the coordinates to EPSG: 3857 to get a projection that uses meters as a unit of measurement.
        ArrayList<Vector2> points = projectionTransformation(EPSG_4326, EPSG_3857, convertLatLngToPoints(coordinates));

        // Get the geometry object from the transformed coordinates.
        Geometry geometry = pointsToGeometry(points, markupType);

        // Buffers the geometry with the provided buffer distance given in meters.
        geometry = Objects.requireNonNull(geometry).buffer(buffer);

        // Get a list of Vector2 coordinates from the buffered geometry.
        ArrayList<Vector2> bufferedGeometry = new ArrayList<>();
        for (Coordinate coordinate : geometry.getCoordinates()) {
            bufferedGeometry.add(new Vector2(coordinate.getX(), coordinate.getY()));
        }

        // Project the coordinates back to EPSG: 4327 to actually plot the points on the Android Google map.
        bufferedGeometry = projectionTransformation(EPSG_3857, EPSG_4326, bufferedGeometry);

        coordinates = convertPointsToLatLng(bufferedGeometry, false);

//        if (PolyUtil.isClosedPolygon(coordinates)) {
//            coordinates = (ArrayList<LatLng>) PolyUtil.simplify(coordinates, PolyUtil.DEFAULT_TOLERANCE);
//        }

        return coordinates;
    }

    /**
     * Convert {@link ArrayList<LatLng>} coordinates to {@link ArrayList<Vector2>} coordinates.
     *
     * @param coordinates The {@link ArrayList<LatLng>} coordinates to convert.
     * @return The {@link ArrayList<Vector2>} coordinates after conversion.
     */
    public static ArrayList<Vector2> convertLatLngToPoints(ArrayList<LatLng> coordinates) {
        ArrayList<Vector2> points = new ArrayList<>();
        for (LatLng coordinate : coordinates) {
            points.add(new Vector2(coordinate.longitude, coordinate.latitude));
        }
        return points;
    }

    /**
     * Convert {@link ArrayList<Vector2>} coordinates to {@link ArrayList<LatLng>} coordinates.
     *
     * @param points     The {@link ArrayList<Vector2>} coordinates to convert.
     * @param isReversed Typically the Lat Lon coordinates would map to y and x respectively.
     *                   Some projections have the x and y reversed, so if this boolean is set to true,
     *                   it will set the Lat Lon to x and y respectively.
     * @return The {@link ArrayList<LatLng>} coordinates after conversion.
     */
    public static ArrayList<LatLng> convertPointsToLatLng(ArrayList<Vector2> points, boolean isReversed) {
        ArrayList<LatLng> coordinates = new ArrayList<>();
        for (Vector2 point : points) {
            if (isReversed) {
                coordinates.add(new LatLng(point.x, point.y));
            } else {
                coordinates.add(new LatLng(point.y, point.x));
            }
        }
        return coordinates;
    }

    public static ArrayList<LatLng> convertPointsToLatLng(ArrayList<Vector2> points) {
        return convertPointsToLatLng(points, true);
    }

    /**
     * Get the {@link ArrayList<LatLng>} coordinates of a polygon representation of a circle based upon
     * the center of the circle and the radius of the circle.
     *
     * @param center The center {@link LatLng} point of the circle.
     * @param radius The radius of the circle.
     * @return Return the {@link ArrayList<LatLng>} coordinate representation of the circle.
     */
    public static ArrayList<LatLng> getPolygonForCircle(LatLng center, Double radius) {
        ArrayList<LatLng> circleGeom = new ArrayList<>();

        int points = 40;
        int p = 360 / points, d = 0;
        for (int i = 0; i < points; ++i, d += p) {
            circleGeom.add(SphericalUtil.computeOffset(center, radius, d));
        }

        circleGeom.add(circleGeom.get(0));
        return circleGeom;
    }

    public static ArrayList<LatLng> getSimplifiedPolygonForCircle(LatLng center, Double radius) {
        ArrayList<LatLng> coordinates = getPolygonForCircle(center, radius);
//
//        if (PolyUtil.isClosedPolygon(coordinates)) {
//            coordinates = (ArrayList<LatLng>) PolyUtil.simplify(coordinates, PolyUtil.DEFAULT_TOLERANCE);
//        }

        return coordinates;
    }

    /**
     * Get the cardinal direction between two {@link LatLng} coordinate points.
     *
     * @param from The first {@link LatLng} coordinate.
     * @param to   The second {@link LatLng} coordinate.
     * @return The string of the cardinal direction between the two {@link LatLng} coordinate points.
     */
    public static String getCardinalDirection(LatLng from, LatLng to) {
        double heading = SphericalUtil.computeHeading(from, to);
        if (heading < 0) {
            heading += 360;
        }

        return calculateCardinalDirection(heading);
    }

    /**
     * Calculate the cardinal direction based upon the heading value of two {@link LatLng} coordinates.
     * <p>
     * Based upon:
     * https://stackoverflow.com/questions/2131195/cardinal-direction-algorithm-in-java
     *
     * @param heading The heading value of the direction.
     * @return The string value of the cardinal direction.
     */
    public static String calculateCardinalDirection(double heading) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.round(((heading % 360) / 45)) % 8];
    }

    /**
     * Compute the shortest distance between the input {@link LatLng} and the {@link ArrayList<LatLng>} coordinate geometry.
     *
     * @param from The {@link LatLng} coordinate to get the distance from.
     * @param to   The {@link ArrayList<LatLng>} coordinates to get the distance to.
     * @return The distance.
     */
    public static double computeDistance(LatLng from, ArrayList<LatLng> to) {
        return SphericalUtil.computeDistanceBetween(from, findNearestPoint(from, to));
    }

    /**
     * Find the nearest {@link LatLng} coordinate from the provided {@link LatLng} coordinate to the
     * {@link ArrayList<LatLng>} coordinates geometry.
     * <p>
     * Based upon:
     * https://stackoverflow.com/questions/36104809/find-the-closest-point-on-polygon-to-user-location
     *
     * @param coordinate The {@link LatLng} coordinate to use to find the closest point.
     * @param target     The {@link ArrayList<LatLng>} coordinates target geometry.
     * @return The closest {@link LatLng} coordinate from the input coordinate to the target coordinate geometry.
     */
    public static LatLng findNearestPoint(LatLng coordinate, ArrayList<LatLng> target) {
        double distance = -1;
        LatLng minimumDistancePoint = coordinate;

        if (coordinate == null || target == null) {
            return minimumDistancePoint;
        }

        for (int i = 0; i < target.size(); i++) {
            LatLng point = target.get(i);

            int segmentPoint = i + 1;
            if (segmentPoint >= target.size()) {
                segmentPoint = 0;
            }

            double currentDistance = PolyUtil.distanceToLine(coordinate, point, target.get(segmentPoint));
            if (distance == -1 || currentDistance < distance) {
                distance = currentDistance;
                minimumDistancePoint = findNearestPoint(coordinate, point, target.get(segmentPoint));
            }
        }

        return minimumDistancePoint;
    }

    /**
     * Find the nearest point of the {@link LatLng} point between the range of the start and end {@link LatLng} points.
     *
     * @param p     The {@link LatLng} point to use to find the nearest point.
     * @param start The starting {@link LatLng} point to search for the nearest point.
     * @param end   The end {@link LatLng} point to search for the nearest point.
     */
    public static LatLng findNearestPoint(LatLng p, LatLng start, LatLng end) {
        if (start.equals(end)) {
            return start;
        }

        double s0lat = Math.toRadians(p.latitude);
        double s0lng = Math.toRadians(p.longitude);
        double s1lat = Math.toRadians(start.latitude);
        double s1lng = Math.toRadians(start.longitude);
        double s2lat = Math.toRadians(end.latitude);
        double s2lng = Math.toRadians(end.longitude);

        double s2s1lat = s2lat - s1lat;
        double s2s1lng = s2lng - s1lng;
        double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
        if (u <= 0) {
            return start;
        }
        if (u >= 1) {
            return end;
        }

        return new LatLng(start.latitude + (u * (end.latitude - start.latitude)),
                start.longitude + (u * (end.longitude - start.longitude)));
    }

    /**
     * Compute the distance between two geometry strings.
     *
     * @param g1 The first geometry string.
     * @param g2 The second geometry string.
     * @return The {@link Double} distance between the two geometry strings.
     * @throws ParseException           Throws {@link ParseException} if parsing the geometry string to a {@link Geometry} object fails.
     * @throws IllegalArgumentException Throws {@link IllegalArgumentException} if the {@link Geometry} objects are null.
     */
    public static Double computeDistance(String g1, String g2) throws ParseException, IllegalArgumentException {
        Geometry geometry1 = geometryStringToGeometry(g1);
        Geometry geometry2 = geometryStringToGeometry(g2);

        return DistanceOp.distance(geometry1, geometry2);
    }

//    public static void parseKmlFile(final File file) {
//        try {
//            KmlInputStream kis = new KmlInputStream(new FileInputStream(file));
//            IGISObject obj;
//            DecimalFormat df = new DecimalFormat("0.0#####");
//            while((obj = kis.read()) != null) {
//                if (obj instanceof Feature) {
//                    Feature f = (Feature)obj;
//                    org.opensextant.giscore.geometry.Geometry g = f.getGeometry();
//                    if (g instanceof Polygon) {
//                        System.out.println("Points");
//                        for(Point p : ((Polygon)g).getOuterRing().getPoints()) {
//                            // do something with the points (e.g. insert in database, etc.)
//                            Geodetic2DPoint pt = p.asGeodetic2DPoint();
//                            System.out.printf("%s,%s%n",
//                                    df.format(pt.getLatitudeAsDegrees()),
//                                    df.format(pt.getLongitudeAsDegrees()));
//                        }
//                    }
//                }
//            }
//            kis.close();
//        } catch (Exception e) {
//
//        }
//    }

    public static ArrayList<String> parseGeojsonFile(File file) {
        ArrayList<String> features = new ArrayList<>();

        try (FileInputStream stream = new FileInputStream(file);
             JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
             reader.setLenient(true);
             Gson gson = new Gson();

             reader.beginObject();

             // Extract the features and geometries out of the geojson.
             while (reader.hasNext()) {
                 String key = reader.nextName();

                 switch (key) {
                     case GEOJSON_FEATURES:
                     case GEOJSON_GEOMETRIES:
                         reader.beginArray();

                         while (reader.hasNext()) {
                             try {
                                 JsonObject feature = gson.fromJson(reader, JsonObject.class);
                                 features.add(feature.toString());
                             } catch (Exception e) {
                                 Timber.tag(DEBUG).e("Failed to parse GeoJson feature from file.");
                             }
                         }

                         while (reader.hasNext()) {
                             reader.skipValue();
                         }

                         reader.endArray();
                         break;
                     default:
                         reader.skipValue();
                         break;
                 }
             }
        } catch (Exception ignored) {
        }

        return features;
    }

    /**
     * Parses through an {@link ArrayList} of Geojson features and pulls out each individual features and it's properties to
     * create a {@link LayerFeature} for each parsed feature.
     *
     * @param features The {@link ArrayList<String>} of Geojson features represented as strings.
     * @return An {@link ArrayList<LayerFeature>} of all of the parsed features.
     */
    public static ArrayList<LayerFeature> parseGeojson(ArrayList<String> features) {
        ArrayList<LayerFeature> layerFeatures = new ArrayList<>();

        for (String feature : features) {
            try {
                // Use Google's GeoJsonParser to get the proper LatLng coordinates.
                GeoJsonParser parser = new GeoJsonParser(new JSONObject(feature));
                ArrayList<GeoJsonFeature> geoJsonFeatures = parser.getFeatures();

                for (GeoJsonFeature f : geoJsonFeatures) {
                    // Feature properties are private, so need to create a new hashmap of them.
                    HashMap<String, Object> properties = new HashMap<>();
                    for (String key : f.getPropertyKeys()) {
                        properties.put(key, f.getProperty(key));
                    }

                    // Get the coordinates of each geometry.
                    String type = f.getGeometry().getGeometryType();

                    switch (type) {
                        case POINT:
                            ArrayList<LatLng> coordinates = new ArrayList<>();
                            coordinates.add(((GeoJsonPoint) f.getGeometry()).getCoordinates());
                            layerFeatures.add(new LayerFeature(LayerFeature.hash(coordinates, properties), coordinates, properties, "marker"));
                            break;
                        case POLYGON:
                            coordinates = ((GeoJsonPolygon) f.getGeometry()).getOuterBoundaryCoordinates();

//                            if (PolyUtil.isClosedPolygon(coordinates)) {
//                                coordinates = (ArrayList<LatLng>) PolyUtil.simplify(coordinates, PolyUtil.DEFAULT_TOLERANCE);
//                            }

                            layerFeatures.add(new LayerFeature(LayerFeature.hash(coordinates, properties), coordinates, properties, "polygon"));
                            break;
                        case LINESTRING:
                            coordinates = (ArrayList<LatLng>) ((GeoJsonLineString) f.getGeometry()).getCoordinates();
                            layerFeatures.add(new LayerFeature(LayerFeature.hash(coordinates, properties), coordinates, properties, "sketch"));
                            break;
                        case MULTI_POINT:
                            ArrayList<GeoJsonPoint> points = (ArrayList<GeoJsonPoint>) ((GeoJsonMultiPoint) f.getGeometry()).getPoints();

                            int count = 0;
                            for (GeoJsonPoint point : points) {
                                coordinates = new ArrayList<>();
                                coordinates.add(((GeoJsonPoint) f.getGeometry()).getCoordinates());
                                layerFeatures.add(new LayerFeature(LayerFeature.hash(coordinates, properties) + "_" + count, coordinates, properties, "marker"));
                            }
                            break;
                        case MULTI_POLYGON:
                            ArrayList<GeoJsonPolygon> polygons = (ArrayList<GeoJsonPolygon>) ((GeoJsonMultiPolygon) f.getGeometry()).getPolygons();

                            count = 0;
                            for (GeoJsonPolygon polygon : polygons) {
                                coordinates = ((GeoJsonPolygon) f.getGeometry()).getOuterBoundaryCoordinates();

//                                if (PolyUtil.isClosedPolygon(coordinates)) {
//                                    coordinates = (ArrayList<LatLng>) PolyUtil.simplify(coordinates, PolyUtil.DEFAULT_TOLERANCE);
//                                }

                                layerFeatures.add(new LayerFeature(LayerFeature.hash(coordinates, properties) + "_" + count, coordinates, properties, "polygon"));
                            }
                            break;
                        case MULTI_LINESTRING:
                            ArrayList<GeoJsonLineString> lines = (ArrayList<GeoJsonLineString>) ((GeoJsonMultiLineString) f.getGeometry()).getLineStrings();

                            count = 0;
                            for (GeoJsonLineString line : lines) {
                                coordinates = (ArrayList<LatLng>) ((GeoJsonLineString) f.getGeometry()).getCoordinates();
                                layerFeatures.add(new LayerFeature(LayerFeature.hash(coordinates, properties) + "_" + count, coordinates, properties, "sketch"));
                            }
                            break;
                        default:
                            break;
                    }
                }

            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to parse GeoJson feature.");
            }
        }

        return layerFeatures;
    }

    /**
     * Parses through an {@link ArrayList} of Geojson features and pulls out each individual features and it's properties to
     * create a {@link LayerFeature} for each parsed feature.
     *
     * @param features The {@link ArrayList<String>} of Geojson features represented as strings.
     * @return An {@link ArrayList<LayerFeature>} of all of the parsed features.
     */
    public static ArrayList<OverlappingLayerFeature> parseGeojsonOverlapping(ArrayList<String> features, long collabroomId) {
        ArrayList<OverlappingLayerFeature> layerFeatures = new ArrayList<>();

        for (String feature : features) {
            try {
                // Use Google's GeoJsonParser to get the proper LatLng coordinates.
                GeoJsonParser parser = new GeoJsonParser(new JSONObject(feature));
                ArrayList<GeoJsonFeature> geoJsonFeatures = parser.getFeatures();

                for (GeoJsonFeature f : geoJsonFeatures) {
                    // Feature properties are private, so need to create a new hashmap of them.
                    HashMap<String, Object> properties = new HashMap<>();
                    for (String key : f.getPropertyKeys()) {
                        properties.put(key, f.getProperty(key));
                    }

                    // Get the coordinates of each geometry.
                    String type = f.getGeometry().getGeometryType();

                    switch (type) {
                        case POINT:
                            ArrayList<LatLng> coordinates = new ArrayList<>();
                            coordinates.add(((GeoJsonPoint) f.getGeometry()).getCoordinates());
                            layerFeatures.add(new OverlappingLayerFeature(collabroomId,
                                    OverlappingLayerFeature.hash(collabroomId, coordinates, properties),
                                    coordinates, properties, "marker"));
                            break;
                        case POLYGON:
                            coordinates = ((GeoJsonPolygon) f.getGeometry()).getOuterBoundaryCoordinates();

//                            if (PolyUtil.isClosedPolygon(coordinates)) {
//                                coordinates = (ArrayList<LatLng>) PolyUtil.simplify(coordinates, PolyUtil.DEFAULT_TOLERANCE);
//                            }

                            layerFeatures.add(new OverlappingLayerFeature(collabroomId,
                                    OverlappingLayerFeature.hash(collabroomId, coordinates, properties),
                                    coordinates, properties, "polygon"));
                            break;
                        case LINESTRING:
                            coordinates = (ArrayList<LatLng>) ((GeoJsonLineString) f.getGeometry()).getCoordinates();

                            layerFeatures.add(new OverlappingLayerFeature(collabroomId,
                                    OverlappingLayerFeature.hash(collabroomId, coordinates, properties),
                                    coordinates, properties, "sketch"));
                            break;
                        case MULTI_POINT:
                            ArrayList<GeoJsonPoint> points = (ArrayList<GeoJsonPoint>) ((GeoJsonMultiPoint) f.getGeometry()).getPoints();

                            int count = 0;
                            for (GeoJsonPoint point : points) {
                                coordinates = new ArrayList<>();
                                coordinates.add(((GeoJsonPoint) f.getGeometry()).getCoordinates());

                                layerFeatures.add(new OverlappingLayerFeature(collabroomId,
                                        OverlappingLayerFeature.hash(collabroomId, coordinates, properties),
                                        coordinates, properties, "marker"));
                            }
                            break;
                        case MULTI_POLYGON:
                            ArrayList<GeoJsonPolygon> polygons = (ArrayList<GeoJsonPolygon>) ((GeoJsonMultiPolygon) f.getGeometry()).getPolygons();

                            count = 0;
                            for (GeoJsonPolygon polygon : polygons) {
                                coordinates = ((GeoJsonPolygon) f.getGeometry()).getOuterBoundaryCoordinates();

//                                if (PolyUtil.isClosedPolygon(coordinates)) {
//                                    coordinates = (ArrayList<LatLng>) PolyUtil.simplify(coordinates, PolyUtil.DEFAULT_TOLERANCE);
//                                }

                                layerFeatures.add(new OverlappingLayerFeature(collabroomId,
                                        OverlappingLayerFeature.hash(collabroomId, coordinates, properties),
                                        coordinates, properties, "polygon"));
                            }
                            break;
                        case MULTI_LINESTRING:
                            ArrayList<GeoJsonLineString> lines = (ArrayList<GeoJsonLineString>) ((GeoJsonMultiLineString) f.getGeometry()).getLineStrings();

                            count = 0;
                            for (GeoJsonLineString line : lines) {
                                coordinates = (ArrayList<LatLng>) ((GeoJsonLineString) f.getGeometry()).getCoordinates();
                                layerFeatures.add(new OverlappingLayerFeature(collabroomId,
                                        OverlappingLayerFeature.hash(collabroomId, coordinates, properties),
                                        coordinates, properties, "sketch"));
                            }
                            break;
                        default:
                            break;
                    }
                }

            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to parse GeoJson feature.");
            }
        }

        return layerFeatures;
    }

    public static boolean intersects(String g1, String g2) throws ParseException {
        return intersects(geometryStringToGeometry(g1), geometryStringToGeometry(g2));
    }

    public static boolean intersects(Geometry g1, Geometry g2) {
        return g1.intersects(g2);
    }

    public static boolean intersects(LatLngBounds b1, LatLngBounds b2) {
        return b2.southwest.longitude < b1.northeast.longitude &&
               b1.southwest.longitude < b2.northeast.longitude &&
               b2.southwest.latitude < b1.northeast.latitude &&
               b1.southwest.latitude < b2.northeast.latitude;
    }

    public static LatLngBounds getLatLngBounds(List<LatLng> coordinates) {
        LatLngBounds.Builder t = LatLngBounds.builder();
        for (LatLng point : coordinates) {
            t.include(point);
        }

        return t.build();
    }

    public static LatLngBounds getLatLngBoundsFromLayerFeatures(List<LayerFeature> features) throws IllegalStateException {
        LatLngBounds.Builder t = LatLngBounds.builder();
        for (LayerFeature feature : features) {
            for (LatLng point : feature.getCoordinates()) {
                t.include(point);
            }
        }
        return t.build();
    }

    public static LatLngBounds getLatLngBoundsFromOverlappingFeatures(List<OverlappingLayerFeature> features) throws IllegalStateException {
        LatLngBounds.Builder t = LatLngBounds.builder();
        for (OverlappingLayerFeature feature : features) {
            for (LatLng point : feature.getCoordinates()) {
                t.include(point);
            }
        }
        return t.build();
    }

    private String formatLatLngString(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();

        builder.append(latitude);

        if (latitude < 0) {
            builder.append(" S");
        } else {
            builder.append(" N");
        }

        builder.append(", ");

        builder.append(longitude);
        if (longitude < 0) {
            builder.append(" W ");
        } else {
            builder.append(" E ");
        }

        return builder.toString();
    }

    public static double getLocationFromString(String location) {
        try {
            return Location.convert(location);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new
     *                            one
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Determine location quality using a combination of timeliness and
        // accuracy
        if (currentBestLocation.getLatitude() == location.getLatitude() && currentBestLocation.getLongitude() == location.getLongitude() && currentBestLocation.getAltitude() == location.getAltitude() && accuracyDelta == 0) {
            return false;
        } else return isMoreAccurate || !isLessAccurate || !isSignificantlyLessAccurate;
    }

    public static LatLng getLatLngPoint(ArrayList<Vector2> coordinates) {
        LatLng latLng = null;
        if (coordinates.size() > 0) {
            Vector2 point = coordinates.get(0);
            latLng = new LatLng(point.x, point.y);
        }

        return latLng;
    }

    public static Location latLngToLocation(LatLng point) {
        Location location = new Location(EMPTY);
        location.setLatitude(point.latitude);
        location.setLongitude(point.longitude);
        return location;
    }

    public static LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static boolean areLocationsEqual(Location l1, Location l2) {
        if (l1 == null && l2 == null) {
            return true;
        }


        if (l1 == null || l2 == null) {
            return false;
        }

        return l1.getLatitude() == l2.getLatitude() && l1.getLongitude() == l2.getLongitude() && l1.getAltitude() == l2.getAltitude();
    }

    public static double computeHeading(LatLng from, LatLng to) {
        double fromLat = toRadians(from.latitude);
        double fromLng = toRadians(from.longitude);
        double toLat = toRadians(to.latitude);
        double toLng = toRadians(to.longitude);
        double dLng = toLng - fromLng;
        double heading = atan2(
                sin(dLng) * cos(toLat),
                cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLng));
        return wrap(toDegrees(heading), 0, 360);
    }

    static double wrap(double n, double min, double max) {
        return (n >= min && n < max) ? n : (mod(n - min, max - min) + min);
    }

    static double mod(double x, double m) {
        return ((x % m) + m) % m;
    }

//    private void createBitMap() {
//        // Create a mutable bitmap
//        Bitmap bitMap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//
//        bitMap = bitMap.copy(bitMap.getConfig(), true);
//        // Construct a canvas with the specified bitmap to draw into
//        Canvas canvas = new Canvas(bitMap);
//        // Create a new paint with default settings.
//        Paint paint = new Paint();
//        // smooths out the edges of what is being drawn
//        paint.setAntiAlias(true);
//        // set color
//        paint.setColor(Color.BLACK);
//        // set style
//        paint.setStyle(Paint.Style.STROKE);
//        // set stroke
//        paint.setStrokeWidth(4.5f);
//
//        canvas.drawPath();
//
//
//        // draw circle with radius 30
//        canvas.drawCircle(50, 50, 30, paint);
//    }
}
