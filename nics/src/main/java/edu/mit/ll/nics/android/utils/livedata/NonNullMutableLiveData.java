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
package edu.mit.ll.nics.android.utils.livedata;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

/**
 * Implementation based upon answer found at: https://stackoverflow.com/a/50154853
 * @param <T>
 */
public class NonNullMutableLiveData<T> extends MutableLiveData<T> implements NonNullLiveData<T> {

    private final @NonNull
    T initialValue;

    public NonNullMutableLiveData(@NonNull T initialValue) {
        this.initialValue = initialValue;
    }

    @Override
    public void postValue(@NonNull T value) {
        super.postValue(value);
    }

    @Override
    public void setValue(@NonNull T value) {
        super.setValue(value);
    }

    @NonNull
    @Override
    public T getValue() {
        //the only way value can be null is if the value hasn't been set yet.
        //for the other cases the set and post methods perform nullability checks.
        T value = super.getValue();
        return value != null ? value : initialValue;
    }

    //convenience method
    //call this method if T is a collection and you modify it's content
    public void notifyContentChanged() {
        postValue(getValue());
    }

    public void observe(@NonNull LifecycleOwner owner, @NonNull NonNullObserver<T> observer) {
        super.observe(owner, observer.getObserver());
    }
}
