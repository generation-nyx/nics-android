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
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_TEMP_FOLDER;

public class FileUtils {

    /**
     * Deletes all files within the app's temp folder.
     *
     * @param context The application's context to find where the temp folder is located.
     */
    public static void clearTempFolder(Context context) {
        clearDirectory(context.getCacheDir() + NICS_TEMP_FOLDER);
    }

    /**
     * Deletes all files within a directory.
     *
     * @param directory The directory to clear.
     */
    public static void clearDirectory(String directory) {
        File dir = new File(directory);

        try {
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (!file.isDirectory()) {
                            if (file.delete()) {
                                Timber.tag(DEBUG).i("Cleared directory: %s", directory);
                            } else {
                                Timber.tag(DEBUG).w("Failed to clear directory: %s", directory);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).w("Failed to clear directory: %s", directory);
        }
    }

    /**
     * Removes the file extension from the provided file name.
     *
     * @param filename The file name to remove the extension from.
     * @param removeAllExtensions Whether or not remove all the extensions or just the last one.
     *                            For example: filename.tar.gz
     *                            If true => filename
     *                            If false => filename.tar
     * @return The file name without the file extension(s).
     */
    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, EMPTY);
    }

    public static String getFileNameFromUri(ContentResolver resolver, Uri uri) {
        Cursor returnCursor = resolver.query(uri, null, null, null, null);
        String name = EMPTY;

        if (returnCursor != null) {
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            name = returnCursor.getString(nameIndex);
            returnCursor.close();
        }

        return removeFileExtension(name, true);
    }

    public static String saveBytesToFile(File file, byte[] data) {
        String path = EMPTY;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            path = file.getAbsolutePath();
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to write stream to file.");
        }

        return path;
    }

    public static File createDirectory(String dir) throws NullPointerException, SecurityException {
        File directory = new File(dir);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Timber.tag(DEBUG).i("Created directory at " + directory + ".");
            }
        }

        return directory;
    }

    /**
     * Creates a file for the image to be stored in when capturing it using the camera.
     *
     * @return The {@link File} object that is created.
     */
    public static File createJpegFile(File parent) {
        return createFile(parent, ".jpg");
    }

    public static File createTempFile(String tempDirectory) {
        return createFile(createDirectory(tempDirectory), ".tmp");
    }

    public static File createFile(File parent, String ext) {
        return new File(parent, UUID.randomUUID().toString() + ext);
    }

    public static void deleteFile(File file) {
        try {
             if (file.delete()) {
                 Timber.tag(DEBUG).i("Successfully deleted file: %s", file);
             } else {
                 Timber.tag(DEBUG).i("Failed to delete file: %s", file);
             }
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to delete file.");
        }
    }

    /**
     * Based upon answer at https://stackoverflow.com/questions/4447477/how-to-copy-files-from-assets-folder-to-sdcard.
     *
     * @param fileName     The name of the file to copy.
     * @param assetManager The AssetManager to use to get the file.
     * @param output       The output location to copy the file to.
     */
    public static void copyFromAssets(String fileName, AssetManager assetManager,
                                      String output) {
        try (InputStream is = assetManager.open(fileName); FileOutputStream fos = new FileOutputStream(new File(output))) {
            copyFile(is, fos);
        } catch (Exception e) {
            Timber.tag(DEBUG).e(e, "Failed to copy asset file: %s to output file %s.", fileName, output);
        }
    }

    /**
     * Copies data from one stream to another.
     *
     * @param in The {@link InputStream} stream to copy from.
     * @param out The {@link OutputStream} stream to copy to.
     * @throws IOException If reading or writing from the streams causes errors.
     */
    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}

