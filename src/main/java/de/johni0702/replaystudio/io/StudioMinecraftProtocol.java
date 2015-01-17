package de.johni0702.replaystudio.io;

import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.ProtocolMode;
import org.spacehq.packetlib.Session;

public class StudioMinecraftProtocol extends MinecraftProtocol {

    public StudioMinecraftProtocol(StudioSession session, boolean client) {
        super(ProtocolMode.LOGIN);
        setMode(ProtocolMode.GAME, client, session);
    }

    @Override
    public void setMode(ProtocolMode mode, boolean client, Session session) {
        super.setMode(mode, client, session);
    }

}
