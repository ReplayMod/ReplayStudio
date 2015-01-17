package de.johni0702.replaystudio.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.mc.protocol.ProtocolMode;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSuccessPacket;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.tcp.io.ByteBufNetInput;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static de.johni0702.replaystudio.io.Utils.readInt;

public class ReplayReader {

    public static List<Pair<Long, Packet>> readPackets(InputStream in) {
        List<Pair<Long, Packet>> packets = new LinkedList<>();
        ByteBufAllocator alloc = new PooledByteBufAllocator();
        StudioSession session = new StudioSession(true);
        StudioCodec codec = new StudioCodec(session);
        StudioCompression compression = null;

        try {
            for (int next = readInt(in); next != -1; next = readInt(in)) {
                int length = readInt(in);
                if (length == -1) {
                    break;
                }
                ByteBuf buf = alloc.buffer(length);
                while (length > 0) {
                    length -= buf.writeBytes(in, length);
                }

                ByteBuf decompressed;
                if (compression != null) {
                    List<Object> out = new LinkedList<>();
                    compression.decode(null, buf, out);
                    buf.release();
                    decompressed = (ByteBuf) out.get(0);
                } else {
                    decompressed = buf;
                }

                decompressed.markReaderIndex();
                if (session.getPacketProtocol().getMode() == ProtocolMode.LOGIN
                        && session.getPacketProtocol().getPacketHeader().readPacketId(new ByteBufNetInput(decompressed)) == 1) {
                    continue; // Skip encryption packets as they fail to decode and throw an exception
                }
                decompressed.resetReaderIndex();


                List<Object> decoded = new LinkedList<>();
                codec.decode(null, decompressed, decoded);
                decompressed.release();

                for (Object o : decoded) {
//                    System.out.println(o);
                    packets.add(Pair.of((long) next, (Packet) o));
                    if (o instanceof LoginSetCompressionPacket) {
                        int threshold = ((LoginSetCompressionPacket) o).getThreshold();
                        if (threshold == -1) {
                            compression = null;
                        } else {
                            session.setCompressionThreshold(threshold);
                            compression = new StudioCompression(session);
                        }
                    }
                    if (o instanceof ServerSetCompressionPacket) {
                        int threshold = ((ServerSetCompressionPacket) o).getThreshold();
                        if (threshold == -1) {
                            compression = null;
                        } else {
                            session.setCompressionThreshold(threshold);
                            compression = new StudioCompression(session);
                        }
                    }
                    if (o instanceof LoginSuccessPacket) {
                        session.getPacketProtocol().setMode(ProtocolMode.GAME, true, session);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return packets;
    }

}
