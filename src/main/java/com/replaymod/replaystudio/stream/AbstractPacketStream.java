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

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.protocol.Packet;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractPacketStream implements PacketStream {

    public static AbstractPacketStream of(Supplier<PacketData> supplier) {
        return new AbstractPacketStreamImpl(supplier);
    }

    private static final class AbstractPacketStreamImpl extends AbstractPacketStream {
        private final Supplier<PacketData> supplier;

        public AbstractPacketStreamImpl(Supplier<PacketData> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void start() {

        }

        @Override
        protected void cleanup() {

        }

        @Override
        protected PacketData nextInput() {
            return supplier.get();
        }
    }

    private class PacketStreamContext implements PacketStream {
        private final StreamElement element;

        public PacketStreamContext(StreamElement element) {
            this.element = checkNotNull(element);
        }

        @Override
        public void insert(PacketData packet) {
            element.inserted.add(packet);
        }

        @Override
        public void insert(long time, Packet packet) {
            element.inserted.add(new PacketData(time, packet));
        }

        @Override
        public void addFilter(StreamFilter filter) {
            AbstractPacketStream.this.addFilter(filter);
        }

        @Override
        public void addFilter(StreamFilter filter, long from, long to) {
            AbstractPacketStream.this.addFilter(filter, from, to);
        }

        @Override
        public void removeFilter(StreamFilter filter) {
            AbstractPacketStream.this.removeFilter(filter);
        }

        @Override
        public Collection<FilterInfo> getFilters() {
            return AbstractPacketStream.this.getFilters();
        }

        @Override
        public PacketData next() {
            throw new IllegalStateException("Cannot get next data from within stream pipeline");
        }

        @Override
        public void start() {
            throw new IllegalStateException("Cannot start from within stream pipeline");
        }

        @Override
        public List<PacketData> end() {
            throw new IllegalStateException("Cannot end from within stream pipeline");
        }
    }

    private class StreamElement {
        private final FilterInfo filter;
        private final PacketStreamContext context = new PacketStreamContext(this);
        private final Queue<PacketData> inserted = new LinkedList<>();
        private boolean active;
        private long lastTimestamp;
        private StreamElement next;

        /**
         * When using this constructor make sure to override {@link #process(PacketData)} as it will throw NPE otherwise.
         */
        protected StreamElement() {
            filter = null;
        }

        public StreamElement(FilterInfo filter) {
            this.filter = checkNotNull(filter);
        }

        public void process(PacketData data) throws IOException {
            boolean keep = true;
            if (data != null && filter.applies(data.getTime())) {
                if (!active) {
                    filter.getFilter().onStart(context);
                    active = true;
                }
                keep = filter.getFilter().onPacket(context, data);
                if (!keep) {
                    data.getPacket().getBuf().release();
                }
            } else if (active) {
                filter.getFilter().onEnd(context, lastTimestamp);
                active = false;
                for (PacketData d : inserted) {
                    if (d.getTime() > lastTimestamp) {
                        lastTimestamp = d.getTime();
                    }
                    next.process(d);
                }
                inserted.clear();
            }
            if (data != null && keep) {
                if (data.getTime() > lastTimestamp) {
                    lastTimestamp = data.getTime();
                }
                next.process(data);
            }
            for (PacketData d : inserted) {
                if (d.getTime() > lastTimestamp) {
                    lastTimestamp = d.getTime();
                }
                next.process(d);
            }
            inserted.clear();
            if (data == null) {
                next.process(null);
            }
        }

        @Override
        public String toString() {
            return (active ? "" : "in") + "active " + filter;
        }
    }

    private class StreamElementEnd extends StreamElement {
        public StreamElementEnd() {
            super();
        }

        @Override
        public void process(PacketData data) {
            if (data != null) {
                AbstractPacketStream.this.inserted.add(data);
            }
        }

        @Override
        public String toString() {
            return "Out";
        }
    }

    private final Queue<PacketData> inserted = new LinkedList<>();
    private final List<StreamElement> filters = new ArrayList<>();

    private StreamElement firstElement;

    @Override
    public void insert(PacketData packet) {
        inserted.add(packet);
    }

    @Override
    public void insert(long time, Packet packet) {
        inserted.add(new PacketData(time, packet));
    }

    private void buildPipe() {
        Iterator<StreamElement> iter = filters.iterator();
        StreamElement l = null;
        while (iter.hasNext()) {
            StreamElement e = iter.next();
            if (l == null) {
                firstElement = e;
            } else {
                l.next = e;
            }
            l = e;
        }
        if (l == null) {
            firstElement = new StreamElementEnd();
        } else {
            l.next = new StreamElementEnd();
        }
    }

    @Override
    public void addFilter(StreamFilter filter) {
        addFilter(filter, -1, -1);
    }

    @Override
    public void addFilter(StreamFilter filter, long from, long to) {
        filters.add(new StreamElement(new FilterInfo(filter, from, to)));
        buildPipe();
    }

    @Override
    public void removeFilter(StreamFilter filter) {
        Iterator<StreamElement> iter = filters.iterator();
        while (iter.hasNext()) {
            if (filter == iter.next().filter.getFilter()) {
                iter.remove();
            }
        }
        buildPipe();
    }

    protected abstract PacketData nextInput();

    @Override
    public PacketData next() throws IOException {
        while (inserted.isEmpty()) {
            PacketData next = nextInput();
            if (next == null) {
                break;
            }
            firstElement.process(next);
        }
        return inserted.poll();
    }

    @Override
    public Collection<FilterInfo> getFilters() {
        return Collections.unmodifiableList(Lists.transform(filters, (e) -> e.filter));
    }

    @Override
    public List<PacketData> end() throws IOException {
        firstElement.process(null);
        List<PacketData> result = new LinkedList<>(inserted);
        inserted.clear();
        return result;
    }

    /**
     * Clean up this packet stream (e.g. close input streams, etc.)
     */
    protected abstract void cleanup();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PacketStream[");
        StreamElement e = firstElement;
        while (e != null) {
            sb.append(e);
            if (e.next != null) {
                sb.append(" -> ");
            }
            e = e.next;
        }
        sb.append("]");
        return sb.toString();
    }
}
