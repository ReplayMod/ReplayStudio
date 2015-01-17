package de.johni0702.replaystudio;

import com.google.common.collect.Iterables;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.ReplayPartView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.packetlib.packet.Packet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
        return packets().size();
    }

    @Override
    public ReplayPart copy() {
        return copyOf(from);
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
        return new StudioReplayPartView(this, from, to);
    }

    @Override
    public List<Packet> packets() {
        return packets(0);
    }

    @Override
    public List<Packet> packets(long from) {
        return packets(from, length());
    }

    @Override
    public List<Packet> packets(long from, long to) {
        return viewed.packets(this.from + from, this.from + to);
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
    public void add(Iterable<Pair<Long, Packet>> packets) {
        viewed.addAt(this.from, packets);
    }

    @Override
    public void addAt(long offset, Iterable<Pair<Long, Packet>> packets) {
        viewed.addAt(this.from + offset, packets);
    }

    @Override
    public ReplayPart append(ReplayPart part) {
        ReplayPart combined = copy();
        combined.addAt(length(), part);
        return combined;
    }

    @Override
    public Collection<Pair<Long, Packet>> remove(long from, long to) {
        return viewed.remove(this.from + from, this.from + to);
    }

    @Override
    public Iterator<Pair<Long, Packet>> iterator() {
        return Iterables.transform(
                Iterables.filter(viewed, (p) -> p.getKey() >= from && p.getKey() <= to),
                (p) -> Pair.of(p.getKey() - from, p.getValue())).iterator();
    }

}
