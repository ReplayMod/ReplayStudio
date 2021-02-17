/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.io;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.ByteBufAllocator;
import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketLoginSuccess;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.studio.StudioPacketStream;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;
import com.replaymod.replaystudio.viaversion.ViaVersionPacketConverter;

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

    private PacketTypeRegistry registry;

    /**
     * The actual input stream.
     */
    private final InputStream in;

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
     * Creates a new replay input stream for reading raw packet data.
     * @param registry The registry used for the first packet produced.
     *                 Further packets may be using a registry for the same version but PLAY state instead.
     *                 Should generally start in LOGIN state, even if the file doesn't not include the LOGIN phase,
     *                 the ReplayInputStream will handle it.
     * @param in The actual input stream.
     * @param fileFormatVersion The file format version of the replay packet data
     * @param fileProtocol The MC protocol version of the replay packet data
     */
    public ReplayInputStream(PacketTypeRegistry registry, InputStream in, int fileFormatVersion, int fileProtocol) throws IOException {
        boolean includeLoginPhase = fileFormatVersion >= 14;
        this.registry = registry;
        this.loginPhase = includeLoginPhase;
        this.outputLoginPhase = registry.getState() == State.LOGIN;
        if (!includeLoginPhase && outputLoginPhase) {
            // For Replays older than version 14, immediately end the Login phase to enter Play phase where the replay starts
            buffer.offer(new PacketData(0, new PacketLoginSuccess(UUID.nameUUIDFromBytes(new byte[0]), "Player").write(registry)));
            this.registry = PacketTypeRegistry.get(registry.getVersion(), State.PLAY);
        } else if (includeLoginPhase && !outputLoginPhase) {
            this.registry = PacketTypeRegistry.get(registry.getVersion(), State.LOGIN);
        }
        this.in = in;
        this.viaVersionConverter = ViaVersionPacketConverter.createForFileVersion(fileFormatVersion, fileProtocol, registry.getVersion().getId());
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    public PacketTypeRegistry getRegistry() {
        return registry;
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

            List<Packet> decoded = new LinkedList<>();
            try {
                for (ByteBuf packet : viaVersionConverter.convertPacket(buf, loginPhase ? State.LOGIN : State.PLAY)) {
                    int packetId = new ByteBufNetInput(packet).readVarInt();
                    decoded.add(new Packet(registry, packetId, registry.getType(packetId), packet));
                }
            } catch (Exception e) {
                throw e instanceof IOException ? (IOException) e : new IOException("decoding", e);
            }
            buf.release();

            for (Packet packet : decoded) {
                PacketType type = packet.getType();
                if (type == PacketType.KeepAlive) {
                    packet.release();
                    continue; // They aren't needed in a replay
                }

                if (type == PacketType.LoginSuccess) {
                    loginPhase = false;
                    registry = PacketTypeRegistry.get(registry.getVersion(), State.PLAY);
                }
                if ((loginPhase || type == PacketType.LoginSuccess) && !outputLoginPhase) {
                    packet.release();
                    continue;
                }
                buffer.offer(new PacketData(next, packet));
            }
        }
    }

    /**
     * Wraps this {@link ReplayInputStream} into a {@link PacketStream}.
     * Closing the replay input stream will close the packet stream and vice versa.
     */
    public PacketStream asPacketStream() {
        return new StudioPacketStream(this);
    }
}
