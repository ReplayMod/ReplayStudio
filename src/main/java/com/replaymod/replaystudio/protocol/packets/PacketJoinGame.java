/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
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
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;

import java.io.IOException;

public class PacketJoinGame {
    // 1.14+
    public static int getViewDistance(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readInt(); // entity id
            if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                in.readBoolean(); // hardcore
            }
            in.readByte(); // gamemode (and hardcore flag for pre-1.16.2)
            if (packet.atLeast(ProtocolVersion.v1_16)) {
                in.readByte(); // prev gamemode
                int count = in.readVarInt(); // dimension registry
                for (int i = 0; i < count; i++) {
                    in.readString(); // dimension
                }
                in.readNBT(); // dimension tracker
                if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                    in.readNBT(); // unknown
                } else {
                    in.readString(); // unknown
                }
                in.readString(); // dimension
            } else {
                in.readInt(); // dimension
            }
            if (packet.atLeast(ProtocolVersion.v1_15)) {
                in.readLong(); // seed
            }
            if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                in.readVarInt(); // max players
            } else {
                in.readByte(); // max players
            }
            if (!packet.atLeast(ProtocolVersion.v1_16)) {
                in.readString(); // geneator type
            }
            return in.readVarInt();
        }
    }
}
