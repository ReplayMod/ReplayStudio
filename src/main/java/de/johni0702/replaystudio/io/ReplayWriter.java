package de.johni0702.replaystudio.io;

import com.google.gson.Gson;
import de.johni0702.replaystudio.api.Replay;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import org.spacehq.packetlib.packet.Packet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static de.johni0702.replaystudio.io.Utils.writeInt;


public class ReplayWriter {

    private static final Gson GSON = new Gson();

    public static void writeReplay(OutputStream output, Replay replay, boolean raw) throws IOException {
        if (raw) {
            ReplayWriter.writePackets(output, replay);
        } else {
            ZipOutputStream out = new ZipOutputStream(output);
            out.putNextEntry(new ZipEntry("metaData.json"));
            out.write(GSON.toJson(replay.getMetaData()).getBytes());
            out.closeEntry();

            out.putNextEntry(new ZipEntry("recording.tmcpr"));
            ReplayWriter.writePackets(out, replay);
            out.closeEntry();
            out.close();
        }
    }

    @SneakyThrows
    public static void writePackets(OutputStream out, Iterable<Pair<Long, Packet>> packets) throws IOException {
        ByteBufAllocator alloc = new PooledByteBufAllocator();
        StudioSession session = new StudioSession(false);
        StudioCodec codec = new StudioCodec(session);
        StudioCompression compression = null;

        for (Pair<Long, Packet> packetPair : packets) {
            Packet packet = packetPair.getValue();
            ByteBuf encoded = alloc.buffer();
            codec.encode(null, packet, encoded);

            ByteBuf compressed;
            if (compression == null) {
                compressed = encoded;
            } else {
                compressed = alloc.buffer();
                compression.encode(null, encoded, compressed);
                encoded.release();
            }

            int length = compressed.readableBytes();
            writeInt(out, (int) (long) packetPair.getKey());
            writeInt(out, length);
            compressed.readBytes(out, length);

            compressed.release();

            if (packet instanceof ServerSetCompressionPacket) {
                int threshold = ((ServerSetCompressionPacket) packet).getThreshold();
                if (threshold == -1) {
                    compression = null;
                } else {
                    compression = new StudioCompression(session);
                    session.setCompressionThreshold(threshold);
                }
            }
        }
    }

}
