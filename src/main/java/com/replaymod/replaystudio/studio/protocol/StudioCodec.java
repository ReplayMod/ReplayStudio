package com.replaymod.replaystudio.studio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.tcp.TcpPacketCodec;

import java.util.List;

public class StudioCodec extends TcpPacketCodec {

    public StudioCodec(Session session) {
        super(session);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        super.decode(ctx, buf, out);
    }

}
