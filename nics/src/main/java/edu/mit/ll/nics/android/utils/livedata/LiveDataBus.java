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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;

import timber.log.Timber;

/**
 * LiveDataBus is a {@link LiveData} implementation of an Event Bus. Since {@link LiveData} is
 * lifecycle aware, it avoids memory leaks and other lifecycle problems that come with using a
 * regular Event Bus. Also, we don't need to use {@link android.content.BroadcastReceiver} which
 * makes passing the data around self contained from outside applications.
 *
 * Based upon: https://dzone.com/articles/how-to-make-an-event-bus-with-googles-livedata.
 */
public final class LiveDataBus {

    private static final HashMap<String, EventLiveData> sEventMap = new HashMap<>();

    private LiveDataBus() {
    }

    /**
     * Gets the {@link EventLiveData} or creates it if it's not already in memory.
     *
     * @param event The event string for the specific event to observe.
     */
    @NonNull
    private static EventLiveData getLiveData(String event) {
        EventLiveData liveData = sEventMap.get(event);
        if (liveData == null) {
            liveData = new EventLiveData(event);
            sEventMap.put(event, liveData);
        }

        return liveData;
    }


    /**
     * Subscribe to the specific event and listen for updates on that event.
     *
     * @param event The event to subscribe to.
     * @param lifecycle The {@link LifecycleOwner}
     * @param observer The {@link Observer<Object>} observer to observe.
     */
    public static void subscribe(String event, @NonNull LifecycleOwner lifecycle, @NonNull Observer<Object> observer) {
        getLiveData(event).observe(lifecycle, observer);
    }

    /**
     * Removes this event when it has no observers.
     *
     * @param event The event to unregister.
     */
    public static void unregister(String event) {
        sEventMap.remove(event);
    }

    /**
     * Publish that an event has occurred.
     *
     * @param event The event to publish data to.
     */
    public static void publish(String event) {
        getLiveData(event).update(null);
    }

    /**
     * Publish an {@link Object} to the specified event for all subscribers of that event.
     *
     * @param event The event to publish data to.
     * @param message The message data to publish.
     */
    public static void publish(String event, @NonNull Object message) {
        getLiveData(event).update(message);
        Timber.tag("published").i(message.toString());
    }

    /**
     * Publish a {@link HashMap} to the specified event for all subscribers of that event.
     * This will allow to publish multiple objects with different keys.
     *
     * @param event The event to publish data to.
     * @param message The message data to publish.
     */
    public static void publish(String event, @NonNull HashMap<String, Object> message) {
        getLiveData(event).update(message);
    }
}
