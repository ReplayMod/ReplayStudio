package de.johni0702.replaystudio.io;

import com.google.gson.Gson;
import de.johni0702.replaystudio.api.Replay;
import de.johni0702.replaystudio.api.ReplayMetaData;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.packet.PacketData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.EncoderException;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import org.spacehq.packetlib.packet.Packet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static de.johni0702.replaystudio.io.Utils.writeInt;


public class ReplayOutputStream extends OutputStream {

    private static final Gson GSON = new Gson();
    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;

    private final ReplayMetaData metaData;
    private final OutputStream out;
    private final ZipOutputStream zipOut;

    private final StudioSession session;
    private final StudioCodec codec;
    private StudioCompression compression = null;
    private int duration;

    public ReplayOutputStream(Studio studio, OutputStream out) {
        this.session = new StudioSession(studio, false);
        this.codec = new StudioCodec(session);
        this.out = out;
        this.zipOut = null;
        this.metaData = null;
    }

    public ReplayOutputStream(Studio studio, OutputStream out, ReplayMetaData metaData) throws IOException {
        this.session = new StudioSession(studio, false);
        this.codec = new StudioCodec(session);
        if (metaData == null) {
            metaData = new ReplayMetaData();
            metaData.setSingleplayer(false);
            metaData.setServerName(studio.getName() + " v" + studio.getVersion());
            metaData.setDate(System.currentTimeMillis());
        }
        metaData.setFileFormat("MCPR");
        metaData.setFileFormatVersion(1);
        metaData.setGenerator("ReplayStudio v" + studio.getVersion());
        this.metaData = metaData;

        this.out = zipOut = new ZipOutputStream(out);

        zipOut.putNextEntry(new ZipEntry("recording.tmcpr"));

    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(PacketData data) throws IOException {
        write(data.getTime(), data.getPacket());
    }

    public void write(long time, Packet packet) throws IOException {
        if (duration < time) {
            duration = (int) time;
        }

        ByteBuf encoded = ALLOC.buffer();
        try {
            codec.encode(null, packet, encoded);
        } catch (Exception e) {
            throw new EncoderException(ToStringBuilder.reflectionToString(packet), e);
        }

        ByteBuf compressed;
        if (compression == null) {
            compressed = encoded;
        } else {
            compressed = ALLOC.buffer();
            try {
                compression.encode(null, encoded, compressed);
            } catch (Exception e) {
                throw new EncoderException(ToStringBuilder.reflectionToString(packet), e);
            }
            encoded.release();
        }

        int length = compressed.readableBytes();
        writeInt(out, (int) time);
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

    @Override
    public void close() throws IOException {
        if (zipOut != null) {
            zipOut.closeEntry();

            metaData.setDuration(duration);
            zipOut.putNextEntry(new ZipEntry("metaData.json"));
            zipOut.write(GSON.toJson(metaData).getBytes());
            zipOut.closeEntry();
        }
        out.close();
    }

    public static void writeReplay(Studio studio, OutputStream output, Replay replay) throws IOException {
        ReplayOutputStream out = new ReplayOutputStream(studio, output, replay.getMetaData());
        for (PacketData data : replay) {
            out.write(data);
        }
        out.close();
    }

    public static void writePackets(Studio studio, OutputStream out, Iterable<PacketData> packets) throws IOException {
        ReplayOutputStream replayOut = new ReplayOutputStream(studio, out);
        for (PacketData data : packets) {
            replayOut.write(data);
        }
    }

}
