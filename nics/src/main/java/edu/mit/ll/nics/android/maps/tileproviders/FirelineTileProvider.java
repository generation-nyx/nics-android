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

import static edu.mit.ll.nics.android.utils.GeoUtils.intersects;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PathDashPathEffect;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import edu.mit.ll.nics.android.maps.markup.FirelineType;
import edu.mit.ll.nics.android.maps.markup.MarkupFireLine;
import edu.mit.ll.nics.android.maps.tileproviders.MarkupTileProjection.DoublePoint;

public class FirelineTileProvider extends MarkupCanvasTileProvider {

    //TODO need to work out the alpha value for the stroke color. May not need to change the paint color from here since we set it in the fireline object.
    private int mCurrentZoom;
    private float mZoomMin;
    private float mZoomMax;
    private MarkupFireLine mFireline;

    private final Paint mRedPaint;
    private final Paint mBlackPaint;
    private final Paint mOrangePaint;

    public FirelineTileProvider(float minZoomLevel, float maxZoomLevel, MarkupFireLine fireLine) {
        mFireline = fireLine;
        mZoomMin = minZoomLevel;
        mZoomMax = maxZoomLevel;

        mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRedPaint.setStyle(Style.STROKE);
        mRedPaint.setStrokeWidth(5);
        mRedPaint.setARGB(255, 255, 0, 0);

        mBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBlackPaint.setStyle(Style.STROKE);
        mBlackPaint.setStrokeWidth(5);
        mBlackPaint.setARGB(255, 0, 0, 0);

        mOrangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOrangePaint.setStyle(Style.STROKE);
        mOrangePaint.setStrokeWidth(5);
        mOrangePaint.setARGB(255, 247, 148, 30);
    }

    @Override
    boolean onDraw(Canvas canvas, MarkupTileProjection projection) {
       return drawFirelines(canvas, projection);
    }

    public void updateFireline(MarkupFireLine fireLine) {
        mFireline = fireLine;
    }

    private boolean drawFirelines(Canvas canvas, MarkupTileProjection projection) {
        if (mFireline.getPoints().size() > 0 &&
                intersects(projection.getTileBounds(), mFireline.getFirelineBounds())) {
            FirelinePath fp = createPath(mFireline, projection);
            canvas.drawPath(fp.getPath(), createPaint(canvas, mFireline, fp));
            return true;
        }
        return false;
    }

    /**
     * Creates a path to draw on the canvas from the LatLng coordinates of the given fireline.
     */
    private FirelinePath createPath(MarkupFireLine feature, MarkupTileProjection projection) {
        mCurrentZoom = projection.getZoom();

        ArrayList<LatLng> coordinates = new ArrayList<>(feature.getPoints());

        Path path = new Path();
        float[] points = new float[feature.getPoints().size() * 2];
        DoublePoint pt = new DoublePoint(0, 0);

        for (int i = 0; i < coordinates.size(); i++) {
            LatLng coord = coordinates.get(i);
            projection.latLngToPoint(coord, pt);
            points[(i * 2)] = (float) pt.x;
            points[(i * 2) + 1] = (float) pt.y;

            if (i == 0) {
                path.moveTo(points[0], points[1]);
            } else {
                path.lineTo(points[i * 2], points[(i * 2) + 1]);
            }
        }

        return new FirelinePath(path, points);
    }

    /**
     * Creates the paint to use when drawing the path on the canvas. The path effect/style is dependant upon the type of fireline.
     */
    private Paint createPaint(Canvas canvas, MarkupFireLine feature, FirelinePath fp) {
        Path path = fp.getPath();
        float[] points = fp.getPoints();

        FirelineType type = FirelineType.lookUp(feature.getDashStyle());
        switch (type) {
            case PRIMARY_FIRELINE:
                mBlackPaint.setPathEffect(new PathDashPathEffect(makeRectangle(scaleBasedOnZoom(6, 10), Math.round(scaleBasedOnZoom(6, 10))),
                        scaleBasedOnZoom(15, 30), 0, PathDashPathEffect.Style.ROTATE));
                return mBlackPaint;
            case SECONDARY_FIRELINE:
                mBlackPaint.setPathEffect(new PathDashPathEffect(makeCircle(scaleBasedOnZoom(2, 4)), scaleBasedOnZoom(15, 30), 0, PathDashPathEffect.Style.ROTATE));
                return mBlackPaint;
            case COMPLETED_DOZER_LINE:
                mBlackPaint.setPathEffect(new PathDashPathEffect(makeCross(scaleBasedOnZoom(1.3f, 2), Math.round(scaleBasedOnZoom(4, 5))),
                        scaleBasedOnZoom(11, 15), 0, PathDashPathEffect.Style.ROTATE));
                return mBlackPaint;
            case PROPOSED_DOZER_LINE:
                mBlackPaint.setPathEffect(new PathDashPathEffect(makeCrossWithCircle(scaleBasedOnZoom(1.3f, 2), Math.round(scaleBasedOnZoom(4, 5))),
                        scaleBasedOnZoom(26, 30), 0, PathDashPathEffect.Style.ROTATE));
                return mBlackPaint;
            case FIRE_EDGE_LINE:
                mRedPaint.setPathEffect(null);
                canvas.drawPath(path, mRedPaint);
                mRedPaint.setPathEffect(new PathDashPathEffect(makeDash(scaleBasedOnZoom(15, 25)), scaleBasedOnZoom(8, 12), 0, PathDashPathEffect.Style.ROTATE));
                return mRedPaint;
            case MANAGEMENT_ACTION_POINT:
                mOrangePaint.setPathEffect(null);
                path.addCircle(points[0], points[1] + 2, 4, Direction.CCW);
                path.addCircle(points[points.length - 2], points[points.length - 1] - 2, 4, Direction.CCW);
                canvas.drawPath(path, mOrangePaint);
                return mOrangePaint;
            case FIRE_SPREAD_PREDICTION:
                mOrangePaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
                return mOrangePaint;
            default:
                mBlackPaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
                return mBlackPaint;
        }
    }

    /**
     * Scales a range of values to match the range of zoom values depending on the current zoom.
     */
    private float scaleBasedOnZoom(float min, float max) {
        float zoomRange = mZoomMax - mZoomMin;
        float newRange = max - min;
        return ((mCurrentZoom - mZoomMin) * (newRange)) / (zoomRange) + min;
    }

    private Path makeCircle(float radius) {
        Path p = new Path();
        p.addCircle(0, 0, radius, Direction.CCW);
        return p;
    }

    private Path makeRectangle(float width, float length) {
        Path p = new Path();
        p.addRect(0 - width / 2, length / 2, width / 2, -length / 2, Direction.CCW);
        p.close();
        return p;
    }

    private Path makeDash(float width) {
        Path p = new Path();
        p.addRect(5, -4, 1, -width + 5, Direction.CCW);
        p.close();
        return p;
    }

    private Path makeCross(float thickness, int size) {
        Path p = new Path();
        int i = -size;
        p.moveTo(5, 5);
        while (i < size) {
            p.addCircle(i, -i, thickness, Direction.CCW);
            p.addCircle(-i, -i, thickness, Direction.CCW);
            i++;
        }

        return p;
    }

    private Path makeCrossWithCircle(float thickness, int size) {
        Path p = new Path();
        int i = -size;
        p.moveTo(5, 5);
        while (i < size) {
            p.addCircle(i, -i, thickness, Direction.CCW);
            p.addCircle(-i, -i, thickness, Direction.CCW);
            i++;
        }

        p.addCircle(scaleBasedOnZoom(13, 15), 0, thickness * 2, Direction.CCW);
        return p;
    }

    private static class FirelinePath {

        private Path path;
        private float[] points;

        public FirelinePath(Path path, float[] points) {
            this.points = points;
            this.path = path;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public float[] getPoints() {
            return points;
        }

        public void setPoints(float[] points) {
            this.points = points;
        }
    }
}
