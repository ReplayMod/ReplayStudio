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

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.collection.PacketList;
import com.replaymod.replaystudio.collection.PacketListIterator;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.collection.ReplayPartView;
import org.apache.commons.lang3.Validate;
import org.spacehq.packetlib.packet.Packet;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class StudioReplayPart implements ReplayPart {

    private final PacketList packets;

    public StudioReplayPart(PacketList packets) {
        this.packets = checkNotNull(packets);
    }

    @Override
    public long length() {
        return size() == 0 ? 0 : packets.get(size() - 1).getTime();
    }

    @Override
    public int size() {
        return packets.size();
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
        Validate.isTrue(from <= to, "from (" + from + ") must not be greater than to (" + to + ")");
        return new StudioReplayPart(new PacketList(this.packets,
                (d) -> d.getTime() >= from && d.getTime() <= to));
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
        packets.listIterator().skipTo(at).add(new PacketData(at, packet));
    }

    @Override
    public void add(long at, Iterable<Packet> packets) {
        ListIterator<PacketData> iter = this.packets.listIterator().skipTo(at);
        for (Packet packet : packets) {
            iter.add(new PacketData(at, packet));
        }
    }

    @Override
    public void add(Iterable<PacketData> packets) {
        addAt(0, packets);
    }

    @Override
    public void addAt(long offset, Iterable<PacketData> packets) {
        PacketListIterator iter = this.packets.listIterator().skipTo(offset);
        for (PacketData data : packets) {
            long at = data.getTime() + offset;
            iter.skipTo(at).add(new PacketData(at, data.getPacket()));
        }
    }

    @Override
    public ReplayPart append(ReplayPart part) {
        ReplayPart combined = copy();
        combined.addAt(length(), part);
        return combined;
    }

    @Override
    public Collection<PacketData> remove(long from, long to) {
        List<PacketData> removed = new LinkedList<>();
        Iterator<PacketData> iter = iterator();
        while (iter.hasNext()) {
            PacketData data = iter.next();
            if (data.getTime() >= from) {
                iter.remove();
                removed.add(data);
            }
            if (data.getTime() > to) {
                // Packets are ordered by time therefore we can stop here
                break;
            }
        }
        return removed;
    }

    @Override
    public ListIterator<PacketData> iterator() {
        return packets.iterator();
    }

}
