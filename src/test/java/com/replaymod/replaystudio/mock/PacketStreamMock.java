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
package com.replaymod.replaystudio.mock;

import com.github.steveice10.packetlib.packet.Packet;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.stream.PacketStream;

import java.util.Collection;
import java.util.List;

public class PacketStreamMock implements PacketStream {
    @Override
    public void insert(PacketData packet) {

    }

    @Override
    public void insert(long time, Packet packet) {

    }

    @Override
    public void addFilter(StreamFilter filter) {

    }

    @Override
    public void addFilter(StreamFilter filter, long from, long to) {

    }

    @Override
    public void removeFilter(StreamFilter filter) {

    }

    @Override
    public Collection<FilterInfo> getFilters() {
        return null;
    }

    @Override
    public PacketData next() {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public List<PacketData> end() {
        return null;
    }
}
