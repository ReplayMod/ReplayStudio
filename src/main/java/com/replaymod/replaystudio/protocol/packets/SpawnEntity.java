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
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
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
