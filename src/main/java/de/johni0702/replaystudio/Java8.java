package de.johni0702.replaystudio;

import com.google.common.base.Supplier;

import java.util.Map;

public class Java8 {

    public static class Map8 {

        public static <K, V> V getOrCreate(Map<K, V> map, K key, Supplier<V> supplier) {
            V value = map.get(key);
            if (value == null) {
                value = supplier.get();
                map.put(key, value);
            }
            return value;
        }
    }

}
