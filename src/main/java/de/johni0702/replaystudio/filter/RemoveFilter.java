package de.johni0702.replaystudio.filter;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.api.packet.PacketStream;
import de.johni0702.replaystudio.io.WrappedPacket;

public class RemoveFilter extends MultiFilter {

    private Predicate<PacketData> filter = Predicates.alwaysTrue();

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        if (config.has("type")) {
            String name = config.get("type").getAsString();
            System.out.println("ServerChatPacket".equals(name));
            filter = (d) -> WrappedPacket.getWrapped(d.getPacket()).getSimpleName().equals(name);
        }
    }

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        return !filter.apply(data);
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {

    }
}
