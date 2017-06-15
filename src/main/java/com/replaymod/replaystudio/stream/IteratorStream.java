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
package com.replaymod.replaystudio.stream;

import com.github.steveice10.packetlib.packet.Packet;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.StreamFilter;

import java.util.*;

/**
 * A stream wrapper for list iterators. Only supports a single filter.
 */
public class IteratorStream implements PacketStream {

    private final ListIterator<PacketData> iterator;
    private final List<PacketData> added = new ArrayList<>();
    private final FilterInfo filter;
    private boolean filterActive;
    private boolean processing;
    private long lastTimestamp = -1;

    public IteratorStream(ListIterator<PacketData> iterator, StreamFilter filter) {
        this(iterator, new FilterInfo(filter, -1, -1));
    }

    public IteratorStream(ListIterator<PacketData> iterator, FilterInfo filter) {
        this.iterator = iterator;
        this.filter = filter;
    }

    @Override
    public void insert(PacketData packet) {
        if (processing) {
            added.add(packet);
        } else {
            iterator.add(packet);
        }
        if (packet.getTime() > lastTimestamp) {
            lastTimestamp = packet.getTime();
        }
    }

    @Override
    public void insert(long time, Packet packet) {
        insert(new PacketData(time, packet));
    }

    @Override
    public void addFilter(StreamFilter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFilter(StreamFilter filter, long from, long to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFilter(StreamFilter filter) {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public PacketData next() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<FilterInfo> getFilters() {
        return Arrays.asList(filter);
    }

    public void processNext() {
        processing = true;

        PacketData next = iterator.next();
        boolean keep = true;
        if ((filter.getFrom() == -1 || filter.getFrom() <= next.getTime())
                && (filter.getTo() == -1 || filter.getFrom() >= next.getTime())) {
            if (!filterActive) {
                filter.getFilter().onStart(this);
                filterActive = true;
            }
            keep = filter.getFilter().onPacket(this, next);
        } else if (filterActive) {
            filter.getFilter().onEnd(this, lastTimestamp);
            filterActive = false;
        }
        if (!keep) {
            iterator.remove();
            if (lastTimestamp == -1) {
                lastTimestamp = next.getTime();
            }
        } else {
            if (next.getTime() > lastTimestamp) {
                lastTimestamp = next.getTime();
            }
        }

        for (PacketData data : added) {
            iterator.add(data);
        }
        added.clear();
        processing = false;
    }

    public void processAll() {
        while (hasNext()) {
            processNext();
        }

        end();
    }

    @Override
    public void start() {

    }

    @Override
    public List<PacketData> end() {
        if (filterActive) {
            filterActive = false;
            filter.getFilter().onEnd(this, lastTimestamp);
        }
        return Collections.unmodifiableList(added);
    }
}
