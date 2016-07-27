package com.replaymod.replaystudio.studio;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.stream.AbstractPacketStream;

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
