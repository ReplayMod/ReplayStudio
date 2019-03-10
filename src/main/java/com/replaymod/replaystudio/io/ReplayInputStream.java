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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.ByteBufAllocator;
import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.packetlib.packet.Packet;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.PacketList;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.studio.StudioPacketStream;
import com.replaymod.replaystudio.studio.StudioReplay;
import com.replaymod.replaystudio.studio.protocol.StudioCodec;
import com.replaymod.replaystudio.studio.protocol.StudioSession;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;
import com.replaymod.replaystudio.viaversion.ViaVersionPacketConverter;

//#if MC>=10800
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import com.replaymod.replaystudio.studio.protocol.StudioCompression;
//#else
//$$ import com.github.steveice10.mc.protocol.ProtocolMode;
//#endif

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static com.replaymod.replaystudio.util.Utils.readInt;

/**
 * Input stream for reading packet data.
 */
public class ReplayInputStream extends InputStream {

    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;

    private final Studio studio;

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

    //#if MC>=10800
    /**
     * The studio compression. May be null if no compression is applied at the moment.
     */
    private StudioCompression compression = null;
    //#endif

    /**
     * The instance of the ViaVersion packet converter in use.
     */
    private ViaVersionPacketConverter viaVersionConverter;

    /**
     * Whether the packet stream (at the head of the input stream, not any already buffered packets) is currently
     * in the login phase.
     */
    private boolean loginPhase;

    /**
     * Whether login phase packets are returned from the stream (otherwise they'll be silently dropped for backwards compatibility).
     */
    private boolean outputLoginPhase;

    /**
     * Packets which have already been read from the input but have not yet been requested via {@link #readPacket()}.
     */
    private Queue<PacketData> buffer = new ArrayDeque<>();

    /**
     * @deprecated Use {@link #ReplayInputStream(Studio, InputStream, int, int)} instead
     */
    @Deprecated
    public ReplayInputStream(Studio studio, InputStream in) {
        this(studio, in, studio.getCurrentFileFormatVersion());
    }

    /**
     * @deprecated Use {@link #ReplayInputStream(Studio, InputStream, int, int)} instead
     */
    @Deprecated
    public ReplayInputStream(Studio studio, InputStream in, int fileFormatVersion) {
        this(studio, in, fileFormatVersion, 0);
    }

    @Deprecated
    public ReplayInputStream(Studio studio, InputStream in, int fileFormatVersion, int fileProtocol) {
        this(studio, in, fileFormatVersion, fileProtocol, true);
    }

    /**
     * Creates a new replay input stream for reading raw packet data.
     * @param studio The studio
     * @param in The actual input stream.
     * @param fileFormatVersion The file format version of the replay packet data
     * @param fileProtocol The MC protocol version
     * @param outputLoginPhase Whether to output packets in the login phase.
     *                         Do not use {@code false} unless you have got a good reason to skip login phase packets.
     */
    public ReplayInputStream(Studio studio, InputStream in, int fileFormatVersion, int fileProtocol, boolean outputLoginPhase) {
        this.studio = studio;
        boolean includeLoginPhase = fileFormatVersion >= 14;
        this.session = new StudioSession(studio, true, includeLoginPhase);
        this.loginPhase = includeLoginPhase;
        this.outputLoginPhase = outputLoginPhase;
        if (!includeLoginPhase && outputLoginPhase) {
            // For Replays older than version 14, immediately end the Login phase to enter Play phase where the replay starts
            buffer.offer(new PacketData(0, new LoginSuccessPacket(new GameProfile(UUID.nameUUIDFromBytes(new byte[0]), "Player"))));
        }
        this.codec = new StudioCodec(session);
        this.in = in;
        this.viaVersionConverter = ViaVersionPacketConverter.createForFileVersion(fileFormatVersion, fileProtocol, ReplayMetaData.CURRENT_PROTOCOL_VERSION);
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    public Studio getStudio() {
        return studio;
    }

    /**
     * Read the next packet from this input stream.
     * @return The packet
     * @throws IOException if an I/O error occurs.
     */
    public PacketData readPacket() throws IOException {
        fillBuffer();
        return buffer.poll();
    }

    private void fillBuffer() throws IOException {
        while (buffer.isEmpty()) {
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
                int read = buf.writeBytes(in, length);
                if (read == -1) {
                    throw new EOFException();
                }
                length -= read;
            }

            ByteBuf decompressed;
            //#if MC>=10800
            if (compression != null) {
                List<Object> out = new LinkedList<>();
                try {
                    compression.decode(null, buf, out);
                } catch (Exception e) {
                    throw e instanceof IOException ? (IOException) e : new IOException("decompressing", e);
                }
                buf.release();
                decompressed = (ByteBuf) out.get(0);
            } else {
                decompressed = buf;
            }
            //#else
            //$$ decompressed = buf;
            //#endif

            List<Object> decoded = new LinkedList<>();
            try {
                for (ByteBuf packet : viaVersionConverter.convertPacket(decompressed, loginPhase ? State.LOGIN : State.PLAY)) {
                    codec.decode(null, packet, decoded);
                    packet.release();
                }
            } catch (Exception e) {
                throw e instanceof IOException ? (IOException) e : new IOException("decoding", e);
            }
            decompressed.release();

            for (Object o : decoded) {
                if (o instanceof ServerKeepAlivePacket) {
                    continue; // They aren't needed in a replay
                }

                //#if MC>=10800
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
                //#endif
                if (o instanceof LoginSuccessPacket) {
                    loginPhase = false;
                    //#if MC>=10800
                    session.getPacketProtocol().setSubProtocol(SubProtocol.GAME, true, session);
                    //#else
                    //$$ session.getPacketProtocol().setMode(ProtocolMode.GAME, true, session);
                    //#endif
                }
                if ((loginPhase || o instanceof LoginSuccessPacket) && !outputLoginPhase) {
                    continue;
                }
                buffer.offer(new PacketData(next, (Packet) o));
            }
        }
    }

    /**
     * Reads all packets from the specified input stream into a new packet list.
     * The input stream is closed if no more packets can be read.
     * @param studio The studio
     * @param in The input stream to read from
     * @return The packet list
     * @deprecated Use {@link #readAllAndClose()} instead
     */
    @Deprecated
    public static PacketList readPackets(Studio studio, InputStream in) throws IOException {
        ReplayInputStream replayIn;
        if (in instanceof ReplayInputStream) {
            replayIn = (ReplayInputStream) in;
        } else {
            replayIn = new ReplayInputStream(studio, in);
        }
        return replayIn.readAllAndClose();
    }

    /**
     * Reads all remaining packets from this input stream into a new packet list.
     * @deprecated {@link PacketList} is deprecated.
     */
    public PacketList readAll() throws IOException {
        List<PacketData> packets = new LinkedList<>();

        PacketData data;
        while ((data = readPacket()) != null) {
            packets.add(data);
        }

        return new PacketList(packets);
    }

    /**
     * Reads all remaining packets from this input stream into a new packet list.
     * Finally, close this input stream.
     * @deprecated {@link PacketList} is deprecated.
     */
    @Deprecated
    public PacketList readAllAndClose() throws IOException {
        try {
            return readAll();
        } finally {
            close();
        }
    }

    /**
     * Wraps this {@link ReplayInputStream} into a {@link PacketStream}.
     * Closing the replay input stream will close the packet stream and vice versa.
     */
    public PacketStream asPacketStream() {
        return new StudioPacketStream(this);
    }

    /**
     * Reads all remaining packets into a {@link Replay} and closes this input stream.
     * @deprecated {@link Replay} is deprecated.
     */
    @Deprecated
    public Replay toReplay() throws IOException {
        return new StudioReplay(studio, readAllAndClose());
    }
}
