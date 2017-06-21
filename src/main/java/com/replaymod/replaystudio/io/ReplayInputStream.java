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

import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.ByteBufAllocator;
import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.netty.buffer.Unpooled;
import com.github.steveice10.netty.channel.ChannelFuture;
import com.github.steveice10.packetlib.packet.Packet;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.PacketList;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.studio.protocol.StudioCodec;
import com.replaymod.replaystudio.studio.protocol.StudioCompression;
import com.replaymod.replaystudio.studio.protocol.StudioSession;
import com.replaymod.replaystudio.util.Utils;
import com.replaymod.replaystudio.viaversion.ViaVersionManager;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.Environment;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolPipeline;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.storage.ClientChunks;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.replaymod.replaystudio.util.Utils.readInt;

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
     * The fileformatversion of the ReplayFile being read by this ReplayInputStream.
     */
    private int fileformatversion;

    /**
     * The UserConnection for ViaVersion
     */
    private UserConnection user;

    /**
     * Creates a new replay input stream for reading raw packet data.
     * @param studio The studio
     * @param in The actual input stream.
     */
    public ReplayInputStream(Studio studio, InputStream in, int fileformatversion) {
        this.session = new StudioSession(studio, true);
        this.codec = new StudioCodec(session);
        this.in = in;
        this.fileformatversion = fileformatversion;
        if (fileformatversion!= ReplayMetaData.CURRENT_FILE_FORMAT_VERSION) {
            ViaVersionManager.init();
            initViaVersion();
        }
    }

    /**
     * Initiates ViaVersion
      */
    private void initViaVersion() {
        this.user = new UserConnection(null) {
            @Override
            public void sendRawPacket(final ByteBuf packet, boolean currentThread) {}

            @Override
            public ChannelFuture sendRawPacketFuture(ByteBuf packet) {
                return null;
            }
        };
        user.getStoredObjects().put(ClientWorld.class, new ClientWorld(user) {
            @Override
            public Environment getEnvironment() {
                return Environment.NORMAL;
            }
        });
        new ProtocolPipeline(user);

        ProtocolPipeline pipeline = user.get(ProtocolInfo.class).getPipeline();
        List<Pair<Integer, Protocol>> path = ProtocolRegistry.getProtocolPath(ReplayMetaData.FILEFORMAT_PROTOCOL.get(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION), ReplayMetaData.FILEFORMAT_PROTOCOL.get(fileformatversion));
        if (path == null) return;
        for (int i = 0; i < path.size(); i++) {
            pipeline.add(path.get(i).getValue());
        }
        for (Protocol p : pipeline.pipes()) {
            p.init(user);
        }
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
    public PacketList readPacket() throws IOException {
        PacketList packets = new PacketList();
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
                int read = buf.writeBytes(in, length);
                if (read == -1) {
                    throw new EOFException();
                }
                length -= read;
            }

            ByteBuf decompressed;
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

            if (fileformatversion!= ReplayMetaData.CURRENT_FILE_FORMAT_VERSION) {
                try {
                    int packetId = Utils.readVarInt(decompressed);

                    PacketWrapper packetWrapper = new PacketWrapper(packetId, decompressed, this.user);
                    State state = State.PLAY;

                    ProtocolPipeline pipeline = user.get(ProtocolInfo.class).getPipeline();

                    if (packetId == 38 && this.fileformatversion <= 1) {
                        ClientChunks clientChunks = user.get(ClientChunks.class);
                        if (clientChunks == null) user.put(clientChunks = new ClientChunks(user));
                        List chunkpackets = clientChunks.transformMapChunkBulk(packetWrapper);
                        for (Object object : chunkpackets) {
                            PacketWrapper packet = (PacketWrapper) object;

                            ByteBuf oldChunk = Unpooled.buffer();
                            packet.writeToBuffer(oldChunk);
                            Utils.readVarInt(oldChunk);

                            PacketWrapper newpacket = new PacketWrapper(packet.getId(), oldChunk, user);

                            pipeline.transform(Direction.OUTGOING, state, newpacket);

                            ByteBuf buffer = Unpooled.buffer();
                            newpacket.writeToBuffer(buffer);

                            List<Object> decoded = new LinkedList<>();
                            try {
                                codec.decode(null, buffer, decoded);
                            } catch (Exception e) {continue;}

                            buffer.release();
                            for (Object p : decoded) packets.add(new PacketData(next, (Packet) p));
                        }
                        clientChunks.getBulkChunks().clear();
                        clientChunks.getLoadedChunks().clear();
                        decompressed.release();
                        return packets;
                    } else {
                        try {
                            pipeline.transform(Direction.OUTGOING, state, packetWrapper);
                        } catch (Exception e) {
                            decompressed.release();
                            packetWrapper.clearInputBuffer();
                            continue;
                        }
                    }
                    decompressed.release();
                    decompressed = Unpooled.buffer();
                    packetWrapper.writeToBuffer(decompressed);
                    packetWrapper.clearInputBuffer();
                } catch (Exception e) {continue;}
            }


            List<Object> decoded = new LinkedList<>();
            try {
                codec.decode(null, decompressed, decoded);
            } catch (Exception e) {
                throw e instanceof IOException ? (IOException) e : new IOException("decoding", e);
            }
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
                    session.getPacketProtocol().setSubProtocol(SubProtocol.GAME, true, session);
                }
                return new PacketList(Collections.singleton(new PacketData(next, (Packet) o)));
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
    public static PacketList readPackets(Studio studio, ReplayInputStream in) throws IOException {
        return readPackets(studio, in, in.fileformatversion);
    }

    /**
     * Reads all packets from the specified input stream into a new packet list.
     * The input stream is closed if no more packets can be read.
     * @param studio The studio
     * @param in The input stream to read from
     * @return The packet list
     */
    public static PacketList readPackets(Studio studio, InputStream in, int fileformatversion) throws IOException {
        ReplayInputStream replayIn;
        if (in instanceof ReplayInputStream) {
            replayIn = (ReplayInputStream) in;
        } else {
            replayIn = new ReplayInputStream(studio, in, fileformatversion);
        }
        List<PacketData> packets = new LinkedList<>();

        PacketList data;
        while ((data = replayIn.readPacket()) != null) {
            packets.addAll(data);
        }

        in.close();

        return new PacketList(packets);
    }

}
