package de.johni0702.replaystudio.io;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.collection.PacketList;
import de.johni0702.replaystudio.studio.protocol.StudioCodec;
import de.johni0702.replaystudio.studio.protocol.StudioCompression;
import de.johni0702.replaystudio.studio.protocol.StudioSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.SneakyThrows;
import org.spacehq.mc.protocol.ProtocolMode;
import org.spacehq.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSuccessPacket;
import org.spacehq.packetlib.packet.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static de.johni0702.replaystudio.util.Utils.readInt;

/**
 * Input stream for reading packet data.
 */
public class ReplayInputStream extends InputStream {

    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;

    /**
     * The actual input stream.
     */
    private final InputStream in;

    /**
     * The studio session.
     */
    private final StudioSession session;

    /**
     * The studio codec.
     */
    private final StudioCodec codec;

    /**
     * The studio compression. May be null if no compression is applied at the moment.
     */
    private StudioCompression compression = null;

    /**
     * Creates a new replay input stream for reading raw packet data.
     * @param studio The studio
     * @param in The actual input stream.
     */
    public ReplayInputStream(Studio studio, InputStream in) {
        this.session = new StudioSession(studio, true);
        this.codec = new StudioCodec(session);
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Read the next packet from this input stream.
     * @return The packet
     * @throws IOException if an I/O error occurs.
     */
    public PacketData readPacket() throws IOException {
        while (true) {
            int next = readInt(in);
            int length = readInt(in);
            if (next == -1 || length == -1) {
                break; // reached end of stream
            }
            if (length == 0) {
                continue; // skip empty segments
            }

            ByteBuf buf = ALLOC.buffer(length);
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

            List<Object> decoded = new LinkedList<>();
            codec.decode(null, decompressed, decoded);
            decompressed.release();

            for (Object o : decoded) {
                if (o instanceof ServerKeepAlivePacket) {
                    continue; // They aren't needed in a replay
                }

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
                return new PacketData(next, (Packet) o);
            }
        }
        return null;
    }

    /**
     * Reads all packets from the specified input stream into a new packet list.
     * The input stream is closed if no more packets can be read.
     * @param studio The studio
     * @param in The input stream to read from
     * @return The packet list
     */
    @SneakyThrows
    public static PacketList readPackets(Studio studio, InputStream in) {
        ReplayInputStream replayIn = new ReplayInputStream(studio, in);
        List<PacketData> packets = new LinkedList<>();

        PacketData data;
        while ((data = replayIn.readPacket()) != null) {
            packets.add(data);
        }

        in.close();

        return new PacketList(packets);
    }

}
