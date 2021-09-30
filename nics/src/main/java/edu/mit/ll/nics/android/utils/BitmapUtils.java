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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;

import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;

import edu.mit.ll.nics.android.R;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

/**
 * Utility class with helpers for operations on {@link Bitmap} bitmaps.
 */
public class BitmapUtils {

    private static final int MAX_SIZE = 128;
    private static final int STANDARD_SIZE = 64;

    /**
     * Converts a {@link Bitmap} bitmap to raw bytes.
     *
     * @param bitmap The {@link Bitmap} to convert.
     * @return byte[] The array of bytes representing the bitmap.
     * @throws NullPointerException Throws null exception if bitmap stream is null.
     * @throws IllegalArgumentException Throws illegal argument exception if quality argument is
     * not between 0 and 100.
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) throws NullPointerException, IllegalArgumentException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    /**
     * Generates a {@link Bitmap} bitmap from an array of bytes that represent a bitmap image in
     * raw bytes.
     *
     * @param bytes The raw bytes that represent a bitmap.
     * @return A {@link Bitmap} bitmap result from decoding the bytes and scaling appropriately.
     * @throws IllegalArgumentException Throws an except if the bytes do not represent a bitmap.
     */
    public static Bitmap generateBitmapFromBytes(byte[] bytes) throws IllegalArgumentException {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (bitmap != null) {
            if (bitmap.getWidth() > MAX_SIZE || bitmap.getHeight() > MAX_SIZE) {
                bitmap = Bitmap.createScaledBitmap(bitmap, MAX_SIZE, MAX_SIZE, false);
            } else {
                bitmap = Bitmap.createScaledBitmap(bitmap, STANDARD_SIZE, STANDARD_SIZE, false);
            }
        }

        return bitmap;
    }

    /**
     * Generate a {@link Bitmap} bitmap from a resource. Generate the bitmap accordingly depending
     * on what type of {@link Drawable} drawable it is.
     *
     * @param resourceId The resource id of the drawable that will be converted to a {@link Bitmap}
     *                   bitmap.
     * @param context The {@link Context} to use to get the {@link Drawable} from the resources.
     * @return The converted bitmap {@link Bitmap}.
     */
    public static Bitmap generateBitmap(int resourceId, Context context) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, resourceId);
            if (drawable instanceof BitmapDrawable) {
                return generateBitmap(context.getResources(), resourceId);
            } else if (drawable instanceof VectorDrawable) {
                return generateBitmap(drawable);
            } else {
                // Return default if fails. will do better function later.
                return generateBitmap(context.getResources(), R.drawable.symbol);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get or build a Bitmap from a drawable
     *
     * @param drawable The {@link Drawable} to try and get a Bitmap representation of.
     * @return A bitmap {@link Bitmap} representation of the Drawable
     */
    public static Bitmap fromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        } else {
            return generateBitmap(drawable);
        }
    }

    /**
     * Generate a bitmap {@link Bitmap} from a provided resource id.
     *
     * @param resources {@link Resources} to get the resource from.
     * @param resourceId The resource id of the {@link Drawable} to try and decode to bitmap.
     * @return A bitmap {@link Bitmap} decoded from the resource.
     */
    public static Bitmap generateBitmap(Resources resources, int resourceId) throws IllegalArgumentException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;
        return BitmapFactory.decodeResource(resources, resourceId, opts);
    }

    public static Bitmap generateBitmap(Drawable vectorDrawable) throws IllegalArgumentException {
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        vectorDrawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap generateScaledBitmap(int resourceId, Resources resources,
                                              int width, int height) throws IllegalArgumentException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, opts);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        return bitmap;
    }

    public static Bitmap generateTintedBitmap(int resourceId,
                                              int[] colorArray,
                                              Resources resources) throws IllegalArgumentException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 2;
        opts.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, opts);

        if (bitmap != null) {
            Canvas test = new Canvas(bitmap);
            Paint paint = new Paint();

            int color = Color.argb(255, colorArray[1], colorArray[2], colorArray[3]);

            if (color != -1) {
                paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            } else {
                paint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            }
            test.drawBitmap(bitmap, 0, 0, paint);
        }

        return bitmap;
    }

    public static Bitmap generateTintedBitmap(int resourceId,
                                              int color,
                                              Resources resources) throws IllegalArgumentException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 2;
        opts.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, opts);

        if (bitmap != null) {
            Canvas test = new Canvas(bitmap);
            Paint paint = new Paint();

            if (color != -1) {
                paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            } else {
                paint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            }
            test.drawBitmap(bitmap, 0, 0, paint);
        }

        return bitmap;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, double maxDimension) {
        double scale = Math.min( bitmap.getWidth() / maxDimension, bitmap.getHeight() / maxDimension);
        if (scale > 1) {
            int height = (int) (bitmap.getHeight() / scale);
            int width = (int) (bitmap.getWidth() / scale);
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        return bitmap;
    }

    public static Bitmap getScaledBitmap(String path, int targetH, int targetW) throws IllegalArgumentException {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeFile(path, bmOptions);
    }

    /**
     *
     *
     * @param resourceId
     * @param degrees
     * @param resources
     * @return
     */
    public static Bitmap generateRotatedBitmap(int resourceId, float degrees, Resources resources) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId, opts);

        try {
            Matrix rotator = new Matrix();
            rotator.postRotate(degrees, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotator, true);
        } catch (Exception e) {
            Timber.tag(DEBUG).w(e, "Failed to create rotated bitmap.");
        }

        return bitmap;
    }

    /**
     * Generate a {@link Bitmap} bitmap that represents text of the provided {@link String} and
     * style options.
     *
     * @param text The text to draw and save as a {@link Bitmap}.
     * @param textSize The size of the text.
     * @param textColor The color of the text.
     * @param typeface The {@link Typeface} typeface of the text.
     * @return A {@link Bitmap} bitmap representing text.
     */
    public static Bitmap generateText(String text, int textSize, int textColor,
                                      Typeface typeface) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);

        if (textColor == 0) {
            paint.setColor(Color.BLACK);
        } else {
            paint.setColor(textColor);
        }

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTypeface(typeface);

        int width = (int) (paint.measureText(text, 0, text.length()) + 0.5f);
        int height = textSize + 24;

        // ensure height and width are never 0 or bitmap creation fails
        if (width == 0) {
            width = 1;
        }
        if (height == 0) {
            height = 24;
        }

        Bitmap textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(textBitmap);

        try {
            width = width / 2;
            height = height / 2;

            canvas.drawText(text, width, height, paint);
        } catch (Exception ignored) {
            // width or height is 0
        }

        return textBitmap;
    }
}
