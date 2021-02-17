/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.util;

import com.google.common.base.Supplier;

import java.util.Map;

/**
 * Class containing "back-ported" java 8 API features, mainly interface default methods.
 */
public class Java8 {

    /**
     * Java 8 Map interface default methods.
     */
    public static class Map8 {

        /**
         * Note: Not in java 8 API.
         * Returns the value for the specified key from the map. If no such key exists in the map, this creates
         * a new value using the specified supplier, puts it in the map and returns the new value.
         * @param map The map
         * @param key The key
         * @param supplier The supplier
         * @param <K> The type of keys
         * @param <V> The type of values
         * @return The value from the map or a new value
         */
        public static <K, V> V getOrCreate(Map<K, V> map, K key, Supplier<V> supplier) {
            V value = map.get(key);
            if (value == null) {
                value = supplier.get();
                map.put(key, value);
            }
            return value;
        }

        /**
         * @see Map#putIfAbsent(Object, Object)
         */
        public static <K, V> void putIfAbsent(Map<K, V> map, K key, V value) {
            if (!map.containsKey(key)) {
                map.put(key, value);
            }
        }
    }

}
