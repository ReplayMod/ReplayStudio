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
package com.replaymod.replaystudio.studio;

import com.google.common.collect.Iterators;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.collection.ReplayPartView;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.Validate;
import org.spacehq.packetlib.packet.Packet;

import java.util.Collection;
import java.util.ListIterator;

public class StudioReplayPartView implements ReplayPartView {

    private final ReplayPart viewed;

    private final long from;

    private final long to;

    public StudioReplayPartView(ReplayPart viewed, long from, long to) {
        this.viewed = viewed;
        this.from = from;
        this.to = to;
    }

    @Override
    public long length() {
        return to - from;
    }

    @Override
    public int size() {
        return Iterators.size(iterator());
    }

    @Override
    public ReplayPart copy() {
        return copyOf(0);
    }

    @Override
    public ReplayPart copyOf(long from) {
        return copyOf(from, length());
    }

    @Override
    public ReplayPart copyOf(long from, long to) {
        return viewed.copyOf(this.from + from, this.from + to);
    }

    @Override
    public ReplayPartView viewOf(long from) {
        return viewOf(from, length());
    }

    @Override
    public ReplayPartView viewOf(long from, long to) {
        Validate.isTrue(from <= to, "from (" + from + ") must not be greater than to (" + to + ")");
        return new StudioReplayPartView(this, from, to);
    }

    @Override
    public void add(long at, Packet packet) {
        viewed.add(this.from + at, packet);
    }

    @Override
    public void add(long at, Iterable<Packet> packets) {
        viewed.add(this.from + at, packets);
    }

    @Override
    public void add(Iterable<PacketData> packets) {
        viewed.addAt(this.from, packets);
    }

    @Override
    public void addAt(long offset, Iterable<PacketData> packets) {
        viewed.addAt(this.from + offset, packets);
    }

    @Override
    public ReplayPart append(ReplayPart part) {
        ReplayPart combined = copy();
        combined.addAt(length(), part);
        return combined;
    }

    @Override
    public Collection<PacketData> remove(long from, long to) {
        Validate.isTrue(from <= to, "from (" + from + ") must not be greater than to (" + to + ")");
        return viewed.remove(this.from + from, this.from + to);
    }

    @Override
    public ListIterator<PacketData> iterator() {
        return new ListIterator<PacketData>() {
            private final ListIterator<PacketData> org = IteratorUtils.filteredListIterator(viewed.iterator(),
                    (p) -> p.getTime() >= from && p.getTime() <= to);

            @Override
            public boolean hasNext() {
                return org.hasNext();
            }

            @Override
            public PacketData next() {
                PacketData next = org.next();
                return new PacketData(next.getTime() - from, next.getPacket());
            }

            @Override
            public boolean hasPrevious() {
                return org.hasPrevious();
            }

            @Override
            public PacketData previous() {
            PacketData next = org.previous();
            return new PacketData(next.getTime() - from, next.getPacket());
            }

            @Override
            public int nextIndex() {
                return org.nextIndex();
            }

            @Override
            public int previousIndex() {
                return org.previousIndex();
            }

            @Override
            public void remove() {
                org.remove();
            }

            @Override
            public void set(PacketData packetData) {
                org.set(new PacketData(packetData.getTime() + from, packetData.getPacket()));
            }

            @Override
            public void add(PacketData packetData) {
                org.add(new PacketData(packetData.getTime() + from, packetData.getPacket()));
            }
        };
    }

    public ReplayPart getViewed() {
        return this.viewed;
    }

    public long getFrom() {
        return this.from;
    }

    public long getTo() {
        return this.to;
    }
}
