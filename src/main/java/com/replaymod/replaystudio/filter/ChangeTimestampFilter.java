package com.replaymod.replaystudio.filter;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.stream.PacketStream;

public class ChangeTimestampFilter extends StreamFilterBase {

    private long offset;

    @Override
    public String getName() {
        return "timestamp";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        offset = config.get("offset").getAsLong();
    }

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        stream.insert(new PacketData(data.getTime() + offset, data.getPacket()));
        return false;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {

    }
}
