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
package edu.mit.ll.nics.android.utils.gson;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

import timber.log.Timber;

/**
 * Json Deserializer for handling JSON sent from the server as a String.
 * Handles removing the escape values from the string and nested parsing.
 *
 * @param <T>
 */
public final class NestedJSONTypeAdapter<T> implements JsonDeserializer<T> {

    // Let Gson instantiate it itself
    private NestedJSONTypeAdapter() {
    }

    @Override
    public T deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        // if the annotated field has a primitive string value
        if (jsonElement.isJsonPrimitive() && ((JsonPrimitive) jsonElement).isString()) {
            // Get element value as string
            String jsonString = jsonElement.getAsString();
            try {
                //attempt continued parsing with string value
                JsonElement cleanElement = JsonParser.parseString(jsonString);
                return context.deserialize(cleanElement, type);
            } catch (Exception e) {
                Timber.e(e, "Failed to parse nested value to class %s: %s", type.getClass().getTypeName(), jsonString);
                return null;
            }
        }

        // if not a primitive string value, continue parsing as normal
        return context.deserialize(jsonElement, type);
    }
}
