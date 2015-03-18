package de.johni0702.replaystudio.filter;

import com.google.common.collect.Ordering;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.io.WrappedPacket;
import de.johni0702.replaystudio.stream.PacketStream;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.HashMap;
import java.util.Map;

public class PacketCountFilter extends StreamFilterBase {

    private final Map<Class<?>, MutableInt> count = new HashMap<>();

    @Override
    public String getName() {
        return "packet_count";
    }

    @Override
    public void init(Studio studio, JsonObject config) {

    }

    @Override
    public void onStart(PacketStream stream) {
        count.clear();
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        Class<?> cls = WrappedPacket.getWrapped(data.getPacket());

        MutableInt counter = count.get(cls);
        if (counter == null) {
            counter = new MutableInt();
            count.put(cls, counter);
        }

        counter.increment();
        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
        System.out.println();
        System.out.println();

        Ordering<Map.Entry<Class<?>, MutableInt>> entryOrdering = Ordering.natural().reverse().onResultOf(Map.Entry::getValue);
        for (Map.Entry<Class<?>, MutableInt> e : entryOrdering.immutableSortedCopy(count.entrySet())) {
            System.out.println(String.format("[%dx] %s", e.getValue().intValue(), e.getKey().getSimpleName()));
        }

        System.out.println();

    }
}
