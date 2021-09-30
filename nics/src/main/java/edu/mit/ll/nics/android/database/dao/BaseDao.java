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
package edu.mit.ll.nics.android.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long replace(T obj);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] replace(T[] obj);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> replace(List<T> obj);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(T obj);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long[] insert(T[] obj);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insert(List<T> obj);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int update(T obj);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int update(T[] obj);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int update(List<T> obj);

    @Delete
    int delete(T obj);

    @Delete
    int delete(T[] obj);

    @Delete
    int delete(List<T> obj);

    @Transaction
    default void upsert(T obj) {
        long id = insert(obj);
        if (id == -1L) update(obj);
    }

    @Transaction
    default void upsert(List<T> obj) {
        List<Long> ids = insert(obj);
        List<T> updateList = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) == -1L) updateList.add(obj.get(i));
        }

        if (!updateList.isEmpty()) update(updateList);
    }
}
