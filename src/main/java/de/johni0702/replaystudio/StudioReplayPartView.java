package de.johni0702.replaystudio;

import com.google.common.collect.Iterators;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.ReplayPartView;
import de.johni0702.replaystudio.api.packet.PacketData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.Validate;
import org.spacehq.packetlib.packet.Packet;

import java.util.Collection;
import java.util.ListIterator;

@RequiredArgsConstructor
public class StudioReplayPartView implements ReplayPartView {

    @Getter
    private final ReplayPart viewed;

    @Getter
    private final long from;

    @Getter
    private final long to;

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

}
