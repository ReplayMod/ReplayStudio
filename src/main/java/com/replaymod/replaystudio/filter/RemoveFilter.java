package com.replaymod.replaystudio.filter;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.io.WrappedPacket;
import com.replaymod.replaystudio.stream.PacketStream;

public class RemoveFilter extends StreamFilterBase {

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
