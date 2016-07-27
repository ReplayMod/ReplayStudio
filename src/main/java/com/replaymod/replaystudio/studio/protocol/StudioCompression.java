package com.replaymod.replaystudio.studio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.tcp.TcpPacketCompression;

import java.util.List;

public class StudioCompression extends TcpPacketCompression {

    public StudioCompression(Session session) {
        super(session);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        super.decode(ctx, buf, out);
    }

}
