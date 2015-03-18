package de.johni0702.replaystudio.io;

import com.google.gson.Gson;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.replay.Replay;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import de.johni0702.replaystudio.studio.protocol.StudioCodec;
import de.johni0702.replaystudio.studio.protocol.StudioCompression;
import de.johni0702.replaystudio.studio.protocol.StudioSession;
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

import static de.johni0702.replaystudio.util.Utils.writeInt;

/**
 * Output stream capable of writing {@link org.spacehq.packetlib.packet.Packet}s and (optionally)
 * {@link de.johni0702.replaystudio.replay.ReplayMetaData}.
 */
public class ReplayOutputStream extends OutputStream {

    private static final Gson GSON = new Gson();
    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;

    /**
     * Meta data for the current replay. Gets written after all packets are written.
     */
    private final ReplayMetaData metaData;

    /**
     * The actual output stream.
     * If we write to a ZIP output stream, this is the same as {@link #zipOut}.
     */
    private final OutputStream out;

    /**
     * If we write to a ZIP output stream instead of just raw data, this holds a reference to that output stream.
     */
    private final ZipOutputStream zipOut;

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
     * Duration of the replay written. This gets updated with each packet and is afterwards used to set the
     * duration in the replay meta data.
     */
    private int duration;

    /**
     * Creates a new replay output stream which will not compress packets written to it nor write any meta data.
     * The resulting output can be read directly by a {@link de.johni0702.replaystudio.io.ReplayInputStream}.
     * @param studio The studio
     * @param out The actual output stream
     */
    public ReplayOutputStream(Studio studio, OutputStream out) {
        this.session = new StudioSession(studio, false);
        this.codec = new StudioCodec(session);
        this.out = out;
        this.zipOut = null;
        this.metaData = null;
    }

    /**
     * Creates a new replay output stream which will write its packets and the specified meta data
     * in a zip output stream according to the MCPR format.
     *
     * @param studio The studio
     * @param out The actual output stream
     * @param metaData The meta data written to the output
     * @throws IOException If an exception occurred while writing the first entry to the zip output stream
     */
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

    /**
     * Writes the specified packet data to the underlying output stream.
     * @param data The packet data
     * @throws IOException - if an I/O error occurs.
     *      In particular, an IOException may be thrown if the output stream has been closed.
     * @see #write(long, org.spacehq.packetlib.packet.Packet)
     */
    public void write(PacketData data) throws IOException {
        write(data.getTime(), data.getPacket());
    }

    /**
     * Writes the specified packet data to the underlying output stream.
     * @param time The timestamp
     * @param packet The packet
     * @throws IOException - if an I/O error occurs.
     *      In particular, an IOException may be thrown if the output stream has been closed.
     * @see #write(de.johni0702.replaystudio.PacketData)
     */
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

    /**
     * Starts a new entry in this replay zip file.
     * The previous entry is therefore closed.
     * @param name Name of the new entry
     */
    public void nextEntry(String name) throws IOException {
        if (zipOut != null) {
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry(name));
        } else {
            throw new UnsupportedOperationException("Cannot start new entry when writing raw replay output.");
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

    /**
     * Writes the specified replay file to the output stream.
     * The output stream is closed when writing is done.
     * @param studio The studio
     * @param output The output stream
     * @param replay The replay
     * @throws IOException - if an I/O error occurs.
     */
    public static void writeReplay(Studio studio, OutputStream output, Replay replay) throws IOException {
        ReplayOutputStream out = new ReplayOutputStream(studio, output, replay.getMetaData());
        for (PacketData data : replay) {
            out.write(data);
        }
        out.close();
    }

    /**
     * Writes the specified packets to the output stream in the order of their occurrence.
     * The output stream is not closed when done allowing for further writing.
     * @param studio The studio
     * @param output The output stream
     * @param packets Iterable of packet data
     * @throws IOException - if an I/O error occurs.
     */
    public static void writePackets(Studio studio, OutputStream output, Iterable<PacketData> packets) throws IOException {
        ReplayOutputStream out = new ReplayOutputStream(studio, output);
        for (PacketData data : packets) {
            out.write(data);
        }
    }

}
