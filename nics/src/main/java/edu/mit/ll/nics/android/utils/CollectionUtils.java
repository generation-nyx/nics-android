package edu.mit.ll.nics.android.utils;

import androidx.annotation.NonNull;

import java.util.Map;

public class CollectionUtils {

    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        V v;
        return (((v = map.get(key)) != null) || map.containsKey(key))
                ? v
                : defaultValue;
    }

    public static <K, V> V getNonNullOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        V v;
        return (v = map.get(key)) != null
                ? v
                : defaultValue;
    }
}
