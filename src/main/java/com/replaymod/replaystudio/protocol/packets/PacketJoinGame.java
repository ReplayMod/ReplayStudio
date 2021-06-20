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
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;

import java.io.IOException;

public class PacketJoinGame {
    public static String getDimension(Packet packet) throws IOException {
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
                return in.readString();
            } else if (packet.atLeast(ProtocolVersion.v1_9_1)) {
                return String.valueOf(in.readInt());
            } else {
                return String.valueOf(in.readByte());
            }
        }
    }

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
