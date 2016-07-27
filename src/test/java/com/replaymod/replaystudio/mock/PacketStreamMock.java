package com.replaymod.replaystudio.mock;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.stream.PacketStream;
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
