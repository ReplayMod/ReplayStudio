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
package com.replaymod.replaystudio.filter;

import com.google.common.collect.Ordering;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.io.WrappedPacket;
import com.replaymod.replaystudio.stream.PacketStream;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.HashMap;
import java.util.Map;

public class PacketCountFilter extends StreamFilterBase {

    private final Map<Class<?>, MutableInt> count = new HashMap<>();

    @Override
    public String getName() {
        return "packet_count";
    }

    @Override
    public void init(Studio studio, JsonObject config) {

    }

    @Override
    public void onStart(PacketStream stream) {
        count.clear();
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        Class<?> cls = WrappedPacket.getWrapped(data.getPacket());

        MutableInt counter = count.get(cls);
        if (counter == null) {
            counter = new MutableInt();
            count.put(cls, counter);
        }

        counter.increment();
        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
        System.out.println();
        System.out.println();

        Ordering<Map.Entry<Class<?>, MutableInt>> entryOrdering = Ordering.natural().reverse().onResultOf(Map.Entry::getValue);
        for (Map.Entry<Class<?>, MutableInt> e : entryOrdering.immutableSortedCopy(count.entrySet())) {
            System.out.println(String.format("[%dx] %s", e.getValue().intValue(), e.getKey().getSimpleName()));
        }

        System.out.println();

    }
}
