/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.io;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.ByteBufAllocator;
import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.netty.handler.codec.EncoderException;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.gson.Gson;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.studio.protocol.StudioCodec;
import com.replaymod.replaystudio.studio.protocol.StudioSession;
import org.apache.commons.lang3.builder.ToStringBuilder;

//#if MC>=10800
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import com.replaymod.replaystudio.studio.protocol.StudioCompression;
//#endif

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.replaymod.replaystudio.util.Utils.writeInt;

/**
 * Output stream capable of writing {@link Packet}s and (optionally)
 * {@link ReplayMetaData}.
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

    //#if MC>=10800
    /**
     * The studio compression. May be null if no compression is applied at the moment.
     */
    private StudioCompression compression = null;
    //#endif

    /**
     * Duration of the replay written. This gets updated with each packet and is afterwards used to set the
     * duration in the replay meta data.
     */
    private int duration;

    /**
     * Creates a new replay output stream which will not compress packets written to it nor write any meta data.
     * The resulting output can be read directly by a {@link ReplayInputStream}.
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
        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
        metaData.setProtocolVersion(ReplayMetaData.CURRENT_PROTOCOL_VERSION);
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
     * @see #write(long, Packet)
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
     * @see #write(PacketData)
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
        //#if MC>=10800
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
        //#else
        //$$ compressed = encoded;
        //#endif

        int length = compressed.readableBytes();
        writeInt(out, (int) time);
        writeInt(out, length);
        compressed.readBytes(out, length);

        compressed.release();

        //#if MC>=10800
        if (packet instanceof ServerSetCompressionPacket) {
            int threshold = ((ServerSetCompressionPacket) packet).getThreshold();
            if (threshold == -1) {
                compression = null;
            } else {
                compression = new StudioCompression(session);
                session.setCompressionThreshold(threshold);
            }
        }
        //#endif
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
