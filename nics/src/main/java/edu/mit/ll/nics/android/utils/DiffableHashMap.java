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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.TransformerUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@SuppressWarnings("unchecked")
public class DiffableHashMap<K, V, T> extends ConcurrentHashMap<K, V> {

    public Diff<K, V, T> mDiffCallback;

    public DiffableHashMap(Diff<K, V, T> diffCallback) {
        super();
        mDiffCallback = diffCallback;
    }

    public void diff(List<T> items, String methodName, String compareMethodName) {
        diff(items, methodName, compareMethodName, null);
    }

    public void diff(List<T> items,
                     String methodName,
                     String compareMethodName,
                     String preConditionName) {
        // If the new set of items is null, remove all the elements.
        if (items == null || items.size() == 0) {
            mDiffCallback.removeAll();
        } else {
            for (T item : items) {
                try {
                    boolean preCondition = true;
                    if (preConditionName != null) {
                        Method preConditionMethod = item.getClass().getDeclaredMethod(preConditionName);
                        Boolean result = (Boolean) preConditionMethod.invoke(item);
                        if (result != null) {
                            preCondition = result;
                        }
                    }

                    Method method = item.getClass().getDeclaredMethod(methodName);
                    K key = (K) method.invoke(item);
                    if (preCondition) {
                        if (containsKey(Objects.requireNonNull(key))) {
                            V value = get(key);
                            if (value != null) {
                                Method compareMethod = value.getClass().getMethod(compareMethodName);
                                T itemToCompare = (T) compareMethod.invoke(value);

                                if (itemToCompare != null && !itemToCompare.equals(item)) {
                                    mDiffCallback.replace(key, item);
                                }
                            }
                        } else {
                            mDiffCallback.add(key, item);
                        }
                    } else {
                        if (containsKey(key)) {
                            mDiffCallback.remove(key);
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(DEBUG).e(e, "Failed performing diff on new items.");
                }
            }

            List<K> keys = (List<K>) CollectionUtils.collect(items, TransformerUtils.invokerTransformer(methodName));
            List<K> keysToRemove = (List<K>) CollectionUtils.subtract(keySet(), keys);

            for (K k : keysToRemove) {
                mDiffCallback.remove(k);
            }
        }
    }
}
