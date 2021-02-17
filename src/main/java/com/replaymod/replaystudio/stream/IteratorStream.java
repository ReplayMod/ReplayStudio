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
package com.replaymod.replaystudio.stream;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.protocol.Packet;

import java.io.IOException;
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

    public void processNext() throws IOException {
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
            next.getPacket().getBuf().release();
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

    public void processAll() throws IOException {
        while (hasNext()) {
            processNext();
        }

        end();
    }

    @Override
    public void start() {

    }

    @Override
    public List<PacketData> end() throws IOException {
        if (filterActive) {
            filterActive = false;
            filter.getFilter().onEnd(this, lastTimestamp);
        }
        return Collections.unmodifiableList(added);
    }
}
