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
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.google.gson.Gson;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketLoginSuccess;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
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
     * Duration of the replay written. This gets updated with each packet and is afterwards used to set the
     * duration in the replay meta data.
     */
    private int duration;

    private boolean loginPhase = true;

    /**
     * Creates a new replay output stream which will not compress packets written to it nor write any meta data.
     * The resulting output can be read directly by a {@link ReplayInputStream}.
     * @param out The actual output stream
     */
    public ReplayOutputStream(OutputStream out) {
        this.out = out;
        this.zipOut = null;
        this.metaData = null;
    }

    /**
     * Creates a new replay output stream which will write its packets and the specified meta data
     * in a zip output stream according to the MCPR format.
     *
     * @param out The actual output stream
     * @param metaData The meta data written to the output
     * @throws IOException If an exception occurred while writing the first entry to the zip output stream
     */
    public ReplayOutputStream(ProtocolVersion version, OutputStream out, ReplayMetaData metaData) throws IOException {
        Studio studio = new ReplayStudio();
        if (metaData == null) {
            metaData = new ReplayMetaData();
            metaData.setSingleplayer(false);
            metaData.setServerName(studio.getName() + " v" + studio.getVersion());
            metaData.setDate(System.currentTimeMillis());
        }
        metaData.setFileFormat("MCPR");
        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
        metaData.setProtocolVersion(version.getId());
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
        if (packet.getRegistry().getState() != State.LOGIN && loginPhase) {
            PacketTypeRegistry registry = PacketTypeRegistry.get(packet.getProtocolVersion(), State.LOGIN);
            doWrite(0, new PacketLoginSuccess(UUID.nameUUIDFromBytes(new byte[0]), "Player").write(registry));
        }
        doWrite(time, packet);
    }

    private void doWrite(long time, Packet packet) throws IOException {
        if (duration < time) {
            duration = (int) time;
        }

        ByteBuf packetIdBuf = ALLOC.buffer();
        try {
            new ByteBufNetOutput(packetIdBuf).writeVarInt(packet.getId());

            int packetIdLen = packetIdBuf.readableBytes();
            int packetBufLen = packet.getBuf().readableBytes();
            writeInt(out, (int) time);
            writeInt(out, packetIdLen + packetBufLen);
            packetIdBuf.readBytes(out, packetIdLen);
            packet.getBuf().getBytes(packet.getBuf().readerIndex(), out, packetBufLen);
        } finally {
            packetIdBuf.release();
            packet.getBuf().release();
        }

        if (packet.getType() == PacketType.LoginSuccess) {
            loginPhase = false;
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
}
