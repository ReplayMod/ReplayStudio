/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
