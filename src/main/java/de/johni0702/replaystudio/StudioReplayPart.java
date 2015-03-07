package de.johni0702.replaystudio;

import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.ReplayPartView;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.api.packet.PacketList;
import de.johni0702.replaystudio.api.packet.PacketListIterator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.spacehq.packetlib.packet.Packet;

import java.util.*;

@RequiredArgsConstructor
public class StudioReplayPart implements ReplayPart {

    private final PacketList packets;

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
