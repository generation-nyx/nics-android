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

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class SQLCipherUtils {

    /**
     * The detected state of the database, based on whether we can open it
     * without a passphrase.
     */
    public enum State {
        DOES_NOT_EXIST, UNENCRYPTED, ENCRYPTED
    }

    public static boolean isDatabaseEncrypted(File databaseFile) {
        return getDatabaseState(databaseFile).equals(State.ENCRYPTED);
    }

    /**
     * Determine whether or not this database appears to be encrypted, based
     * on whether we can open it without a passphrase.
     * <p>
     * NOTE: You are responsible for ensuring that net.sqlcipher.database.SQLiteDatabase.loadLibs()
     * is called before calling this method. This is handled automatically with the
     * getDatabaseState() method that takes a Context as a parameter.
     *
     * @param databaseFile a File pointing to the database
     * @return the detected state of the database
     */
    public static State getDatabaseState(File databaseFile) {
        if (databaseFile.exists()) {
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getAbsolutePath(), "", null, SQLiteDatabase.OPEN_READONLY)) {
                db.getVersion();
                return (State.UNENCRYPTED);
            } catch (Exception e) {
                return (State.ENCRYPTED);
            }
        }

        return (State.DOES_NOT_EXIST);
    }

    /**
     * Replaces this database with a version encrypted with the supplied
     * passphrase, deleting the original. Do not call this while the database
     * is open, which includes during any Room migrations.
     * <p>
     * The passphrase is untouched in this call. If you are going to turn around
     * and use it with SafeHelperFactory.fromUser(), fromUser() will clear the
     * passphrase. If not, please set all bytes of the passphrase to 0 or something
     * to clear out the passphrase.
     *
     * @param context      a Context
     * @param originalFile a File pointing to the database
     * @param passphrase   the passphrase from the user
     * @throws IOException Throws this exception if file operations fail.
     */
    public static void encrypt(Context context, String name, File originalFile, String passphrase, SQLiteDatabaseHook hook) throws IOException {
        SQLiteDatabase.loadLibs(context);

        if (originalFile.exists()) {
            if (!isDatabaseEncrypted(originalFile)) {
                File newFile = File.createTempFile("original_database", "db", context.getCacheDir());
                SQLiteDatabase db = SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(), "", null, SQLiteDatabase.OPEN_READWRITE);
                int version = db.getVersion();

                db.close();

                String path = originalFile.getAbsolutePath().replace("\\", "\\\\");

                db = SQLiteDatabase.openDatabase(newFile.getAbsolutePath(), passphrase.getBytes(),
                        null, SQLiteDatabase.OPEN_READWRITE, hook, null);

                db.rawExecSQL("ATTACH DATABASE '" + path + "' AS plaintext KEY ''");
                db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')");
                db.rawExecSQL("DETACH DATABASE plaintext");
                db.setVersion(version);
                db.close();

                if (originalFile.delete()) {
                    Timber.tag(DEBUG).d("Deleted original database %s", originalFile.getAbsolutePath());
                }

                if (newFile.renameTo(originalFile)) {
                    Timber.tag(DEBUG).d("Renamed temp database to %s", originalFile.getAbsolutePath());
                }
            }
        } else {
            throw new FileNotFoundException(originalFile.getAbsolutePath() + " not found");
        }
    }
}
