package de.johni0702.replaystudio.api.packet;

import de.johni0702.replaystudio.api.manipulation.StreamFilter;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * A stream wrapper for list iterators.
 */
@RequiredArgsConstructor
public class IteratorStream implements PacketStream {

    private final ListIterator<PacketData> iterator;
    private final List<PacketData> added = new ArrayList<>();
    private final Map<FilterInfo, Boolean> filters = new LinkedHashMap<>();
    private boolean processing;
    private long lastTimestamp = -1;

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
    public void addFilter(StreamFilter filter) {
        addFilter(filter, -1, -1);
    }

    @Override
    public void addFilter(StreamFilter filter, long from, long to) {
        filters.put(new FilterInfo(filter, from, to), false);
    }

    @Override
    public void removeFilter(StreamFilter filter) {
        Iterator<FilterInfo> iter = filters.keySet().iterator();
        while (iter.hasNext()) {
            if (filter == iter.next().getFilter()) {
                iter.remove();
            }
        }
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public PacketData next() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Collection<FilterInfo> getFilters() {
        return Collections.unmodifiableSet(filters.keySet());
    }

    public void processNext() {
        processing = true;

        PacketData next = iterator.next();
        boolean keep = true;
        for (Map.Entry<FilterInfo, Boolean> e : filters.entrySet()) {
            FilterInfo filter = e.getKey();
            if ((filter.getFrom() == -1 || filter.getFrom() <= next.getTime())
                    && (filter.getTo() == -1 || filter.getFrom() >= next.getTime())) {
                if (!e.getValue()) {
                    filter.getFilter().onStart(this);
                    e.setValue(true);
                }
                keep &= filter.getFilter().onPacket(this, next);
            } else if (e.getValue()) {
                filter.getFilter().onEnd(this, lastTimestamp);
                e.setValue(false);
            }
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
        start();

        while (hasNext()) {
            processNext();
        }

        end();
    }

    @Override
    public void start() {
        for (Map.Entry<FilterInfo, Boolean> e : filters.entrySet()) {
            e.getKey().getFilter().onStart(this);
            e.setValue(true);
        }
    }

    @Override
    public List<PacketData> end() {
        for (Map.Entry<FilterInfo, Boolean> e : filters.entrySet()) {
            e.getKey().getFilter().onEnd(this, lastTimestamp);
            e.setValue(false);
        }
        return Collections.emptyList();
    }
}
