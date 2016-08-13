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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.io.WrappedPacket;
import com.replaymod.replaystudio.stream.PacketStream;

public class RemoveFilter extends StreamFilterBase {

    private Predicate<PacketData> filter = Predicates.alwaysTrue();

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        if (config.has("type")) {
            String name = config.get("type").getAsString();
            System.out.println("ServerChatPacket".equals(name));
            filter = (d) -> WrappedPacket.getWrapped(d.getPacket()).getSimpleName().equals(name);
        }
    }

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        return !filter.apply(data);
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {

    }
}
