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
package com.replaymod.replaystudio.viaversion;

import com.replaymod.replaystudio.us.myles.ViaVersion.api.PacketWrapper;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.data.UserConnection;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.Protocol;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolPipeline;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import com.replaymod.replaystudio.us.myles.ViaVersion.exception.CancelException;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.Direction;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;
import com.replaymod.replaystudio.us.myles.ViaVersion.protocols.base.ProtocolInfo;
import org.spacehq.netty.buffer.ByteBuf;
import org.spacehq.netty.channel.ChannelFuture;
import org.spacehq.packetlib.tcp.io.ByteBufNetInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts a sequence of packets from one version to another using ViaVersion.
 * This class is stateful and packets must be processed in order.
 */
public class ViaVersionPacketConverter {
    private static final Map<Integer, Integer> PROTOCOL_FOR_FILE_FORMAT = new HashMap<>();
    static {
        PROTOCOL_FOR_FILE_FORMAT.put(0, 47);
        PROTOCOL_FOR_FILE_FORMAT.put(1, 47);
        PROTOCOL_FOR_FILE_FORMAT.put(2, 110);
        PROTOCOL_FOR_FILE_FORMAT.put(3, 210);
        PROTOCOL_FOR_FILE_FORMAT.put(4, 315);
        PROTOCOL_FOR_FILE_FORMAT.put(5, 316);
        PROTOCOL_FOR_FILE_FORMAT.put(6, 335);
        PROTOCOL_FOR_FILE_FORMAT.put(7, 338);
    }

    public static ViaVersionPacketConverter createForFileVersion(int input, int output) {
        if (!PROTOCOL_FOR_FILE_FORMAT.containsKey(input)) throw new IllegalArgumentException("Unknown input version");
        if (!PROTOCOL_FOR_FILE_FORMAT.containsKey(output)) throw new IllegalArgumentException("Unknown output version");
        return createForProtocolVersion(PROTOCOL_FOR_FILE_FORMAT.get(input), PROTOCOL_FOR_FILE_FORMAT.get(output));
    }

    public static ViaVersionPacketConverter createForProtocolVersion(int input, int output) {
        return new ViaVersionPacketConverter(input, output);
    }

    public static boolean isFileVersionSupported(int input, int output) {
        return PROTOCOL_FOR_FILE_FORMAT.containsKey(input) &&
                PROTOCOL_FOR_FILE_FORMAT.containsKey(output) &&
                isProtocolVersionSupported(PROTOCOL_FOR_FILE_FORMAT.get(input), PROTOCOL_FOR_FILE_FORMAT.get(output));
    }

    public static boolean isProtocolVersionSupported(int input, int output) {
        if (input == output) return true;
        CustomViaManager.initialize();
        return ProtocolRegistry.getProtocolPath(output, input) != null;
    }

    private final UserConnection user;
    private final CustomViaAPI viaAPI;
    private final ProtocolPipeline pipeline;
    private List<ByteBuf> out = new ArrayList<>();

    private ViaVersionPacketConverter(int inputProtocol, int outputProtocol) {
        CustomViaManager.initialize();

        List<Pair<Integer, Protocol>> path = ProtocolRegistry.getProtocolPath(outputProtocol, inputProtocol);
        if (path != null) {
            user = new DummyUserConnection();
            viaAPI = new CustomViaAPI(inputProtocol, user);
            pipeline = new ProtocolPipeline(user);
            ProtocolInfo protocolInfo = user.get(ProtocolInfo.class);
            protocolInfo.setState(State.PLAY);
            protocolInfo.setUsername("$Camera$");
            protocolInfo.setUuid(UUID.randomUUID());
            path.stream().map(Pair::getValue).forEachOrdered(pipeline::add);
        } else {
            user = null;
            viaAPI = null;
            pipeline = null;
        }
    }

    /**
     * Converts the provided packet to the output protocol.
     * @param buf The ByteBuf containing the packet, may be modified
     * @return List of ByteBuf, one for each output packet, may be empty
     */
    public List<ByteBuf> convertPacket(ByteBuf buf) throws IOException {
        if (user == null) {
            buf.retain();
            return Collections.singletonList(buf);
        }
        CustomViaAPI.INSTANCE.set(viaAPI);
        try {
            int packetId = new ByteBufNetInput(buf).readVarInt();
            PacketWrapper packetWrapper = new PacketWrapper(packetId, buf, user);

            try {
                pipeline.transform(Direction.OUTGOING, State.PLAY, packetWrapper);
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
    private final class DummyUserConnection extends UserConnection {
        DummyUserConnection() {
            super(null);
        }

        @Override
        public void sendRawPacket(ByteBuf packet, boolean currentThread) {
            out.add(packet);
        }

        @Override
        public ChannelFuture sendRawPacketFuture(ByteBuf packet) {
            throw new UnsupportedOperationException();
        }
    }
}
