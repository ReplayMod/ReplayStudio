package de.johni0702.replaystudio;

import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.manipulation.AbstractPacketStream;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.io.ReplayInputStream;
import lombok.SneakyThrows;

import java.io.InputStream;

public class StudioPacketStream extends AbstractPacketStream {

    private final ReplayInputStream in;

    public StudioPacketStream(Studio studio, InputStream in) {
        this.in = new ReplayInputStream(studio, in);
    }

    @Override
    @SneakyThrows
    protected PacketData nextInput() {
        return in.readPacket();
    }

}
