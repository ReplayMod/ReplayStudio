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
package com.replaymod.replaystudio.viaversion;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.channel.ChannelFuture;
import com.github.steveice10.netty.channel.embedded.EmbeddedChannel;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.replaymod.replaystudio.lib.viaversion.api.Via;
import com.replaymod.replaystudio.lib.viaversion.api.connection.ProtocolInfo;
import com.replaymod.replaystudio.lib.viaversion.api.connection.UserConnection;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolPathEntry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.Direction;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.PacketWrapper;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.lib.viaversion.connection.UserConnectionImpl;
import com.replaymod.replaystudio.lib.viaversion.protocol.ProtocolPipelineImpl;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolPipeline;
import com.replaymod.replaystudio.lib.viaversion.exception.CancelException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.replaymod.replaystudio.replay.ReplayMetaData.PROTOCOL_FOR_FILE_FORMAT;

/**
 * Converts a sequence of packets from one version to another using ViaVersion.
 * This class is stateful and packets must be processed in order.
 */
public class ViaVersionPacketConverter {
    @Deprecated
    public static ViaVersionPacketConverter createForFileVersion(int input, int output) {
        return createForFileVersion(input, 0, PROTOCOL_FOR_FILE_FORMAT.get(output));
    }

    public static ViaVersionPacketConverter createForFileVersion(int fileVersion, int fileProtocol, int outputProtocol) {
        if (!PROTOCOL_FOR_FILE_FORMAT.containsKey(fileVersion) && fileVersion < 10) throw new IllegalArgumentException("Unknown file version");
        return createForProtocolVersion(fileVersion < 10 ? PROTOCOL_FOR_FILE_FORMAT.get(fileVersion) : fileProtocol, outputProtocol);
    }

    public static ViaVersionPacketConverter createForProtocolVersion(int input, int output) {
        return new ViaVersionPacketConverter(input, output);
    }

    @Deprecated
    public static boolean isFileVersionSupported(int input, int output) {
        return PROTOCOL_FOR_FILE_FORMAT.containsKey(input) &&
                PROTOCOL_FOR_FILE_FORMAT.containsKey(output) &&
                isProtocolVersionSupported(PROTOCOL_FOR_FILE_FORMAT.get(input), PROTOCOL_FOR_FILE_FORMAT.get(output));
    }

    public static boolean isFileVersionSupported(int fileVersion, int fileProtocol, int outputProtocol) {
        if (fileVersion < 10) {
            if (!PROTOCOL_FOR_FILE_FORMAT.containsKey(fileVersion)) return false;
            fileProtocol = PROTOCOL_FOR_FILE_FORMAT.get(fileVersion);
        }
        return isProtocolVersionSupported(fileProtocol, outputProtocol);
    }

    public static boolean isProtocolVersionSupported(int input, int output) {
        if (input == output) return true;
        CustomViaManager.initialize();
        return Via.getManager().getProtocolManager().getProtocolPath(output, input) != null;
    }

    private final UserConnection user;
    private final CustomViaAPI viaAPI;
    private final ProtocolPipeline pipeline;
    private List<ByteBuf> out = new ArrayList<>();

    private ViaVersionPacketConverter(int inputProtocol, int outputProtocol) {
        CustomViaManager.initialize();

        List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(outputProtocol, inputProtocol);
        if (path != null) {
            user = new DummyUserConnection();
            viaAPI = new CustomViaAPI(inputProtocol, user);
            pipeline = new ProtocolPipelineImpl(user);
            ProtocolInfo protocolInfo = user.getProtocolInfo();
            protocolInfo.setClientState(outputProtocol >= ProtocolVersion.v1_20_2.getVersion() ? State.CONFIGURATION : State.PLAY);
            protocolInfo.setServerState(inputProtocol >= ProtocolVersion.v1_20_2.getVersion() ? State.CONFIGURATION : State.PLAY);
            protocolInfo.setUsername("$Camera$");
            protocolInfo.setUuid(UUID.randomUUID());
            path.stream().map(ProtocolPathEntry::getProtocol).forEachOrdered(pipeline::add);
        } else {
            user = null;
            viaAPI = null;
            pipeline = null;
        }
    }

    /**
     * @deprecated Use {@link #convertPacket(ByteBuf,State)} instead.
     */
    @Deprecated
    public List<ByteBuf> convertPacket(ByteBuf buf) throws IOException {
        return convertPacket(buf, State.PLAY);
    }

    /**
     * Converts the provided packet to the output protocol.
     * @param buf The ByteBuf containing the packet, may be modified
     * @return List of ByteBuf, one for each output packet, may be empty
     */
    public List<ByteBuf> convertPacket(ByteBuf buf, State state) throws IOException {
        if (user == null) {
            buf.retain();
            return Collections.singletonList(buf);
        }
        CustomViaAPI.INSTANCE.set(viaAPI);
        try {
            int packetId = new ByteBufNetInput(buf).readVarInt();
            PacketWrapper packetWrapper = PacketWrapper.create(packetId, buf, user);

            try {
                pipeline.transform(Direction.CLIENTBOUND, state, packetWrapper);
            } catch (CancelException e) {
                if (!out.isEmpty()) {
                    return popOut();
                } else {
                    return Collections.emptyList();
                }
            }

            ByteBuf result = buf.alloc().buffer();
            packetWrapper.writeToBuffer(result);
            if (!out.isEmpty()) {
                out.add(0, result);
                return popOut();
            } else {
                return Collections.singletonList(result);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Exception during ViaVersion conversion:", e);
        } finally {
            CustomViaAPI.INSTANCE.remove();
        }
    }

    /**
     * Returns {@link #out} and replaces it with a new list
     * @return {@link #out}
     */
    private List<ByteBuf> popOut() {
        try {
            return out;
        } finally {
            out = new ArrayList<>();
        }
    }

    /**
     * User connection that pushes all sent packets into the {@link #out} list.
     */
    private final class DummyUserConnection extends UserConnectionImpl {
        DummyUserConnection() {
            super(new EmbeddedChannel());
        }

        @Override
        public void sendRawPacket(ByteBuf packet) {
            out.add(packet);
        }

        @Override
        public void scheduleSendRawPacket(ByteBuf packet) {
            out.add(packet);
        }

        @Override
        public ChannelFuture sendRawPacketFuture(ByteBuf packet) {
            throw new UnsupportedOperationException();
        }
    }
}
