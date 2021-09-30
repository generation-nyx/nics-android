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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class ImageUtils {

    private static final int MAX_SIZE = 128;
    private static final int STANDARD_SIZE = 64;
    public static final String IMAGE_FILE_REGEX = "(?i).*\\.(tiff|pjp|pjpeg|jfif|webp|tif|bmp|png|jpg|svgz|jpeg|gif|svg|ico|xbm|dib)$";

    public static Matrix getTaggedRotationMatrix(Uri imageUri, Context context) {
        ExifInterface exifInterface;
        int orientation;

        try {
            exifInterface = new ExifInterface(getRealPathFromURI(imageUri, context));

            String attribute = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);

            if (attribute != null) {
                orientation = Integer.parseInt(attribute);
            } else {
                orientation = ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (Exception e) {
            orientation = ExifInterface.ORIENTATION_UNDEFINED;
        }

        if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            String[] orientationColumn = new String[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                orientationColumn = new String[]{MediaStore.Images.Media.ORIENTATION};
            }
            Cursor cur = context.getContentResolver().query(imageUri, orientationColumn, null, null, null);
            orientation = -1;

            if (cur != null && cur.moveToFirst()) {
                orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
                cur.close();
            }
        }

        float degrees = 0;

        switch (orientation) {
            case (ExifInterface.ORIENTATION_ROTATE_90):
                degrees = 90;
                break;

            case (ExifInterface.ORIENTATION_ROTATE_180):
                degrees = 180;
                break;

            case (ExifInterface.ORIENTATION_ROTATE_270):
                degrees = 270;
                break;

            default:
                break;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return matrix;
    }

    public static Matrix getTaggedRotationMatrix(String path) {
        ExifInterface exifInterface;
        int orientation;

        try {
            exifInterface = new ExifInterface(path);

            String attribute = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);

            if (attribute != null) {
                orientation = Integer.parseInt(attribute);
            } else {
                orientation = ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (Exception e) {
            orientation = ExifInterface.ORIENTATION_UNDEFINED;
        }

        float degrees = 0;

        switch (orientation) {
            case (ExifInterface.ORIENTATION_ROTATE_90):
                degrees = 90;
                break;
            case (ExifInterface.ORIENTATION_ROTATE_180):
                degrees = 180;
                break;
            case (ExifInterface.ORIENTATION_ROTATE_270):
                degrees = 270;
                break;
            default:
                break;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return matrix;
    }

    public static Bitmap rotateBitmap(Bitmap image, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        inImage.compress(Bitmap.CompressFormat.JPEG, 60, bytes);

        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);

        return Uri.parse(path);
    }

    public static FileDescriptor getFileDescriptorFromURI(Uri contentUri, Context context) {
        FileDescriptor fileDescriptor = null;
        try {
            if (contentUri != null) {
                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(contentUri, "rwt");
                if (parcelFileDescriptor != null) {
                    fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to get real path from Uri");
        }

        return fileDescriptor;
    }

    public static Bitmap getBitmapFromUri(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public static String getRealPathFromURI(Uri contentURI, Context context) {
        String path = EMPTY;
        try {
            if (contentURI != null) {
                Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
                if (cursor == null)
                    path = contentURI.getPath();
                else {
                    cursor.moveToFirst();
                    int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    path = cursor.getString(idx);
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to get the path from the provided uri. URI: %s", contentURI);
        }

        return path;
    }

    /**
     * Fill all colored pixels to a solid color while retaining transparency on pixels.
     * Warning: this could be slow to render on larger images, Recommend running this in a separate
     * thread to not stall the UI at all.
     *
     * @param bitmap image to change color of
     * @param red    red value 0-255
     * @param green  green value 0-255
     * @param blue   blue value 0-255
     */
    public static void setImageToSolidColor(Bitmap bitmap, int red, int green, int blue) {
        int[] pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int x = 0; x < pixels.length; x++) {
            int a = Color.alpha(pixels[x]);
            if (a > 0) {
                pixels[x] = Color.argb(a, red, green, blue);
            }
        }
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    public static Bitmap generateBitmapFromBytes(byte[] imageBytes) {
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public static Uri saveImageToDevice(byte[] bytes, File file, int quality) {
        return saveImageToDevice(generateBitmapFromBytes(bytes), file, quality);
    }

    /**
     * Save bitmap to location on local device
     *
     * @param image bitmap to be saved
     * @param file  file path to be saved at
     * @return Uri location of saved image
     */
    public static Uri saveImageToDevice(Bitmap image, File file, int quality) {
        try {
            File parent = file.getParentFile();

            if (parent != null) {
                parent.mkdirs();
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Error making directory to save image to file.");
        }

        FileWriter writer = null;
        FileOutputStream out = null;

        try {
            writer = new FileWriter(file);
            out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, quality, out);
            return Uri.fromFile(file);
        } catch (IOException e) {
            Timber.tag(DEBUG).e(e, "Error saving image to file.");
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Error closing FileWriter.");
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Error closing FileOutputStream.");
            }
        }

        return Uri.EMPTY;
    }

    public static byte[] compressImage(byte[] bytes, Context context) {
        FileWriter writer = null;
        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            Bitmap bitmap;
            String fileName = UUID.nameUUIDFromBytes(bytes).toString();

            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (bitmap != null) {
                if (bitmap.getWidth() > 256 || bitmap.getHeight() > 256) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, false);
                } else {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                }

                // Create temporary file.
                File tempFile = File.createTempFile(fileName, ".jpeg", context.getCacheDir());
                writer = new FileWriter(tempFile);

                // Compress image and store into temporary file.
                out = new FileOutputStream(tempFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
                out.close();

                // Get byte array from temporary file.
                byte[] data = new byte[(int) tempFile.length()];
                in = new FileInputStream(tempFile);

                int count = in.read(data);
                Timber.tag(DEBUG).i("Read %s bytes into temp file %s.jpeg", count, fileName);

                tempFile.delete();

                return data;
            }
        } catch (IOException e) {
            Timber.tag(DEBUG).e(e, "Error compressing image.");
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Error closing FileWriter.");
            }

            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Error closing FileInputStream.");
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Timber.tag(DEBUG).e(e, "Error closing FileOutputStream.");
            }
        }

        return null;
    }

    public static void decodeScaledImage(ImageView imageView, FileDescriptor fileDescriptor) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW/targetW, photoH/targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public static Bitmap decodeScaledImage(ImageView imageView, String currentPhotoPath) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW/targetW, photoH/targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
    }

    public static Uri saveImageToStorage(Bitmap bitmap, ContentResolver cr) {
        return saveImageToStorage(bitmap, cr, 100);
    }

    public static Uri saveImageToStorage(Bitmap bitmap, ContentResolver cr, int quality) {
        String fileName = UUID.randomUUID().toString().concat(".jpg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

            Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try (OutputStream out = cr.openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to save image to external storage.");
            }
            return uri;
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File image = new File(imagesDir, fileName);

            try (OutputStream out = new FileOutputStream(image)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            } catch (Exception e) {
                Timber.tag(DEBUG).e(e, "Failed to save image to external storage.");
            }
            return Uri.fromFile(image);
        }
    }
}
