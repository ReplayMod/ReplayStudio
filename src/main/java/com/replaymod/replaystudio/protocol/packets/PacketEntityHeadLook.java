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
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.util.Location;

import java.io.IOException;

public class PacketEntityHeadLook {
    public static float getYaw(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                in.readVarInt(); // entity id
            } else {
                in.readInt(); // entity id
            }
            return in.readByte() / 256f * 360;
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId, float yaw) throws IOException {
        Packet packet = new Packet(registry, PacketType.EntityHeadLook);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeVarInt(entityId);
            } else {
                out.writeInt(entityId);
            }
            out.writeByte((int) (yaw / 360 * 256));
        }
        return packet;
    }
}
