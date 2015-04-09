package de.johni0702.replaystudio.studio;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.stream.AbstractPacketStream;

import java.io.IOException;
import java.io.InputStream;

public class StudioPacketStream extends AbstractPacketStream {

    private final ReplayInputStream in;

    public StudioPacketStream(Studio studio, InputStream in) {
        this.in = new ReplayInputStream(studio, in);
    }

    @Override
    protected PacketData nextInput() {
        try {
            return in.readPacket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
