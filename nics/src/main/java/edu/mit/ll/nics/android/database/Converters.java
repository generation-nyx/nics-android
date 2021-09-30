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
package edu.mit.ll.nics.android.database;

import androidx.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.ll.nics.android.data.UserOrg;
import edu.mit.ll.nics.android.database.entities.HazardInfo;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.database.entities.Uxo;
import edu.mit.ll.nics.android.database.entities.Vector2;
import edu.mit.ll.nics.android.enums.SendStatus;

public class Converters {

    @TypeConverter
    public static Date toDate(Long dateLong){
        return dateLong == null ? null: new Date(dateLong);
    }

    @TypeConverter
    public static Long fromDate(Date date){
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static SendStatus fromId(int id) {
        return SendStatus.lookUp(id);
    }

    @TypeConverter
    public static int statusToInt(SendStatus status) {
        if (status != null) {
            return status.getId();
        } else {
            return 0;
        }
    }

    @TypeConverter
    public static long[] fromLongArrayString(String value) {
        Type mapType = new TypeToken<long[]>() {
        }.getType();
        return new GsonBuilder().serializeNulls().create().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromLongArray(long[] array) {
        Gson gson = new Gson();
        return gson.toJson(array);
    }

    @TypeConverter
    public static HashMap<String, Object> fromStringObjectKeyHashMap(String value) {
        Type mapType = new TypeToken<HashMap<String, Object>>() {
        }.getType();
        return new GsonBuilder().serializeNulls().create().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromStringObjectHashMap(HashMap<String, Object> map) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, Object> fromStringObjectKey(String value) {
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        return new GsonBuilder().serializeNulls().create().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromStringObjectMap(Map<String, Object> map) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, String> fromStringKey(String value) {
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return new Gson().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromStringMap(Map<String, String> map) {
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    @TypeConverter
    public static ArrayList<String> fromStringToArrayList(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromStringArrayList(ArrayList<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<Vector2> fromStringToVectorArrayList(String value) {
        Type listType = new TypeToken<ArrayList<Vector2>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromVectorArrayList(ArrayList<Vector2> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<Uxo> fromUXOString(String value) {
        Type listType = new TypeToken<ArrayList<Uxo>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromUXOArrayList(ArrayList<Uxo> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<MarkupFeature> fromMarkupFeatureString(String value) {
        Type listType = new TypeToken<ArrayList<MarkupFeature>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromMarkupFeatureArrayList(ArrayList<MarkupFeature> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static JSONObject fromJsonString(String value) {
        if (value != null) {
            try {
                return new JSONObject(value);
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @TypeConverter
    public static String fromJsonObject(JSONObject object) {
        if (object != null) {
            return object.toString();
        } else {
            return null;
        }
    }

    @TypeConverter
    public static String fromLatLng(LatLng latLng) {
        if (latLng != null) {
            return new Gson().toJson(latLng);
        } else {
            return null;
        }
    }

    @TypeConverter
    public static LatLng toLatLng(String json) {
        return new Gson().fromJson(json, LatLng.class);
    }

    @TypeConverter
    public static ArrayList<LatLng> fromLatLngArrayString(String value) {
        Type listType = new TypeToken<ArrayList<LatLng>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromLatLngArrayList(ArrayList<LatLng> list) {
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static ArrayList<HazardInfo> fromHazardInfoArrayString(String value) {
        Type listType = new TypeToken<ArrayList<HazardInfo>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromHazardInfoArrayList(ArrayList<HazardInfo> list) {
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static ArrayList<UserOrg> fromUserOrgsString(String value) {
        Type listType = new TypeToken<ArrayList<UserOrg>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromUserOrgsArrayList(ArrayList<UserOrg> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<Integer> fromIntegersString(String value) {
        Type listType = new TypeToken<ArrayList<Integer>>() {
        }.getType();
        return new GsonBuilder().serializeNulls().create().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromIntegersList(ArrayList<Integer> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<SymbologyGroup.Symbology> fromSymbologyString(String value) {
        Type listType = new TypeToken<List<SymbologyGroup.Symbology>>() {}.getType();
        return new GsonBuilder().serializeNulls().create().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromSymbologyList(List<SymbologyGroup.Symbology> list) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<SymbologyGroup.Symbology>>() {}.getType();
        return gson.toJson(list, listType);
    }
}
