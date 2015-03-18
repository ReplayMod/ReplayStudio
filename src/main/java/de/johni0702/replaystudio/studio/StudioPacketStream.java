package de.johni0702.replaystudio.studio;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.stream.AbstractPacketStream;
import lombok.SneakyThrows;

import java.io.IOException;
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

    @Override
    public void start() {

    }

    @Override
    protected void cleanup() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
