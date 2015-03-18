package de.johni0702.replaystudio.mock;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.StreamFilter;
import de.johni0702.replaystudio.stream.PacketStream;
import org.spacehq.packetlib.packet.Packet;

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
