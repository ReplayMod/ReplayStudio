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
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.util.Location;

import java.io.IOException;

public class PacketEntityTeleport {
    public static Location getLocation(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                in.readVarInt(); // entity id
            } else {
                in.readInt(); // entity id
            }
            if (packet.atLeast(ProtocolVersion.v1_21_2)) {
                double x, y, z;
                if (packet.atLeast(ProtocolVersion.v26_3)) {
                    int pathType = in.readVarInt();
                    if (pathType < 0 || pathType > 1) pathType = 0;
                    switch (pathType) {
                        case 1: // Stepped
                            x = 0; y = 0; z = 0;
                            for (int len = in.readVarInt(), i = 0; i < len; i++) {
                                x = in.readDouble();
                                y = in.readDouble();
                                z = in.readDouble();
                                in.readVarInt(); // tickOffset
                            }
                            break;
                        case 0: // Linear
                        default:
                            x = in.readDouble();
                            y = in.readDouble();
                            z = in.readDouble();
                            break;
                    }
                } else {
                    x = in.readDouble();
                    y = in.readDouble();
                    z = in.readDouble();
                    in.readDouble(); // unused
                    in.readDouble(); // unused
                    in.readDouble(); // unused
                }
                float yaw = in.readFloat();
                float pitch = in.readFloat();
                return new Location(x, y, z, yaw, pitch);
            } else {
                return SpawnEntity.readXYZYaPi(packet, in, false);
            }
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId, Location location, boolean onGround) throws IOException {
        Packet packet = new Packet(registry, PacketType.EntityTeleport);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeVarInt(entityId);
            } else {
                out.writeInt(entityId);
            }
            if (packet.atLeast(ProtocolVersion.v1_21_2)) {
                if (packet.atLeast(ProtocolVersion.v26_3)) {
                    out.writeVarInt(0); // pathType: Linear
                }
                out.writeDouble(location.getX());
                out.writeDouble(location.getY());
                out.writeDouble(location.getZ());
                if (packet.olderThan(ProtocolVersion.v26_3)) {
                    out.writeDouble(0); // unused
                    out.writeDouble(0); // unused
                    out.writeDouble(0); // unused
                }
                out.writeFloat(location.getYaw());
                out.writeFloat(location.getPitch());
            } else {
                SpawnEntity.writeXYZYaPi(packet, out, location);
            }
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeBoolean(onGround);
            }
        }
        return packet;
    }
}
