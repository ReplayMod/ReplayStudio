package de.johni0702.replaystudio.filter;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.stream.PacketStream;

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
