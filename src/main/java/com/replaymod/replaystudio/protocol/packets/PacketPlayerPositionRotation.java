/*
 * Copyright (c) 2024
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

import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;

import java.io.IOException;

public class PacketPlayerPositionRotation {
    public double x;
    public double y;
    public double z;
    public double dx; // 1.21.2+
    public double dy; // 1.21.2+
    public double dz; // 1.21.2+
    public float yaw;
    public float pitch;
    public int flags;
    public int teleportId; // 1.9+
    public boolean dismount; // 1.17 - 1.19.3

    public static PacketPlayerPositionRotation read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            PacketPlayerPositionRotation result = new PacketPlayerPositionRotation();
            result.read(packet, in);
            return result;
        }
    }

    private void read(Packet packet, Packet.Reader in) throws IOException {
        if (packet.atLeast(ProtocolVersion.v1_21_2)) {
            teleportId = in.readVarInt();
        }
        x = in.readDouble();
        y = in.readDouble();
        z = in.readDouble();
        if (packet.atLeast(ProtocolVersion.v1_21_2)) {
            dx = in.readDouble();
            dy = in.readDouble();
            dz = in.readDouble();
        }
        yaw = in.readFloat();
        pitch = in.readFloat();
        if (packet.atLeast(ProtocolVersion.v1_21_2)) {
            flags = in.readInt();
        } else {
            flags = in.readByte();
        }
        if (packet.atLeast(ProtocolVersion.v1_9) && packet.olderThan(ProtocolVersion.v1_21_2)) {
            teleportId = in.readVarInt();
        }
        if (packet.atLeast(ProtocolVersion.v1_17) && packet.atMost(ProtocolVersion.v1_19_3)) {
            dismount = in.readBoolean();
        }
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.PlayerPositionRotation);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_21_2)) {
                out.writeVarInt(teleportId);
            }
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            if (packet.atLeast(ProtocolVersion.v1_21_2)) {
                out.writeDouble(dx);
                out.writeDouble(dy);
                out.writeDouble(dz);
            }
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            if (packet.atLeast(ProtocolVersion.v1_21_2)) {
                out.writeInt(flags);
            } else {
                out.writeByte(flags);
            }
            if (packet.atLeast(ProtocolVersion.v1_9) && packet.olderThan(ProtocolVersion.v1_21_2)) {
                out.writeVarInt(teleportId);
            }
            if (packet.atLeast(ProtocolVersion.v1_17) && packet.atMost(ProtocolVersion.v1_19_3)) {
                out.writeBoolean(dismount);
            }
        }
        return packet;
    }
}
