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
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.util.IPosition;
import com.replaymod.replaystudio.util.Location;

import java.io.IOException;

public class SpawnEntity {
    public static Location getLocation(Packet packet) throws IOException {
        PacketType type = packet.getType();
        switch (type) {
            case SpawnExpOrb: try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    return new Location(in.readDouble(), in.readDouble(), in.readDouble(), 0, 0);
                } else {
                    return new Location(in.readInt() / 32.0, in.readInt() / 32.0, in.readInt() / 32.0, 0, 0);
                }
            }
            case SpawnObject:
            case SpawnMob: try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    in.readUUID(); // uuid
                }
                if (packet.atLeast(ProtocolVersion.v1_11)) {
                    in.readVarInt(); // type
                } else {
                    in.readUnsignedByte(); // type
                }
                return readXYZYaPi(packet, in);
            }
            case SpawnPlayer: try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    in.readUUID(); // uuid
                } else {
                    in.readString(); // uuid
                    in.readString(); // name
                    int properties = in.readVarInt();
                    for (int i = 0; i < properties; i++) {
                        in.readString(); // name
                        in.readString(); // value
                        in.readString(); // signature
                    }
                }
                return readXYZYaPi(packet, in);
            }
            case SpawnPainting: try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    in.readUUID(); // uuid
                }
                if (packet.atLeast(ProtocolVersion.v1_13)) {
                    in.readVarInt(); // type
                } else {
                    in.readString(); // type
                }
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    IPosition pos = in.readPosition();
                    return new Location(pos.getX(), pos.getY(), pos.getZ(), 0 , 0);
                } else {
                    return new Location(in.readInt(), in.readInt(), in.readInt(), 0, 0);
                }
            }
            default:
                return null;
        }
    }

    static Location readXYZYaPi(Packet packet, Packet.Reader in) throws IOException {
        if (packet.atLeast(ProtocolVersion.v1_9)) {
            return new Location(in.readDouble(), in.readDouble(), in.readDouble(),
                    in.readByte() / 256f * 360, in.readByte() / 256f * 360);
        } else {
            return new Location(in.readInt() / 32.0, in.readInt() / 32.0, in.readInt() / 32.0,
                    in.readByte() / 256f * 360, in.readByte() / 256f * 360);
        }
    }

    static void writeXYZYaPi(Packet packet, Packet.Writer out, Location loc) throws IOException {
        if (packet.atLeast(ProtocolVersion.v1_9)) {
            out.writeDouble(loc.getX());
            out.writeDouble(loc.getY());
            out.writeDouble(loc.getZ());
        } else {
            out.writeInt((int) (loc.getX() * 32));
            out.writeInt((int) (loc.getY() * 32));
            out.writeInt((int) (loc.getZ() * 32));
        }
        out.writeByte((int) (loc.getYaw() / 360 * 256));
        out.writeByte((int) (loc.getPitch() / 360 * 256));
    }
}
