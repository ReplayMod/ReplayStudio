package de.johni0702.replaystudio;

import com.google.common.collect.Lists;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.ReplayPartView;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.packetlib.packet.Packet;

import java.util.*;

@RequiredArgsConstructor
public class StudioReplayPart implements ReplayPart {

    private final List<Pair<Long, Packet>> packets;

    @Override
    public long length() {
        return size() == 0 ? 0 : packets.get(size() - 1).getKey();
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
        return copyOf(0, length());
    }

    @Override
    public ReplayPart copyOf(long from, long to) {
        List<Pair<Long, Packet>> packets = new LinkedList<>();
        for (Pair<Long, Packet> pair : this.packets) {
            if (pair.getKey() >= from && pair.getKey() <= to) {
                packets.add(Pair.of(pair.getKey(), pair.getRight()));
            }
        }
        return new StudioReplayPart(packets);
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
        return Lists.transform(packets, Pair::getValue);
    }

    private ListIterator<Pair<Long, Packet>> findInsertionIndex(ListIterator<Pair<Long, Packet>> iter, long time) {
        while (iter.hasNext()) {
            if (iter.next().getKey() > time) {
                iter.previous(); // Went too far, go one step back
                break;
            }
        }
        return iter;
    }

    @Override
    public void add(long at, Packet packet) {
        findInsertionIndex(packets.listIterator(), at).add(Pair.of(at, packet));
    }

    @Override
    public void add(long at, Iterable<Packet> packets) {
        ListIterator<Pair<Long, Packet>> iter = findInsertionIndex(this.packets.listIterator(), at);
        for (Packet packet : packets) {
            iter.add(Pair.of(at, packet));
        }
    }

    @Override
    public void add(Iterable<Pair<Long, Packet>> packets) {
        addAt(0, packets);
    }

    @Override
    public void addAt(long offset, Iterable<Pair<Long, Packet>> packets) {
        long start = System.currentTimeMillis(), end;
        ListIterator<Pair<Long, Packet>> iter = findInsertionIndex(this.packets.listIterator(), offset);

        end = System.currentTimeMillis();
        System.out.println("Found index after " + (end - start) + ": " + iter.nextIndex() + "/" + this.packets.size());
        start = end;

        int i = 0;
        long l1 = 0, l2 = 0, l3 = 0;
        for (Pair<Long, Packet> pair : packets) {
            i++;
            if (i % 10000 == 0) {
                System.out.println(i);
                System.out.println("L1: " + l1);
                System.out.println("L2: " + l2);
                System.out.println("L3: " + l3);
                l1 = l2 = l3 = 0;
            }
            long at = pair.getKey() + offset;
            long s = System.nanoTime();
            ListIterator<Pair<Long, Packet>> nIter = this.packets.listIterator(iter.nextIndex());
            l1 += System.nanoTime()-s;
            s = System.nanoTime();
            nIter = findInsertionIndex(nIter, at);
            l2 += System.nanoTime()-s;
            s = System.nanoTime();
            nIter.add(pair);
            l3 += System.nanoTime()-s;
        }

        end = System.currentTimeMillis();
        System.out.println("Finished splitting after " + (end - start));
    }

    @Override
    public ReplayPart append(ReplayPart part) {
        ReplayPart combined = copy();
        combined.addAt(length(), part);
        return combined;
    }

    @Override
    public Collection<Pair<Long, Packet>> remove(long from, long to) {
        List<Pair<Long, Packet>> removed = new LinkedList<>();
        Iterator<Pair<Long, Packet>> iter = iterator();
        while (iter.hasNext()) {
            Pair<Long, Packet> pair = iter.next();
            if (pair.getKey() >= from) {
                iter.remove();
                removed.add(pair);
            }
            if (pair.getKey() > to) {
                // Packets are ordered by time therefore we can stop here
                break;
            }
        }
        return removed;
    }

    @Override
    public Iterator<Pair<Long, Packet>> iterator() {
        return packets.iterator();
    }

}
