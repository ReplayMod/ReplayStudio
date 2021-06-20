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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketDestroyEntities {
    public static List<Integer> getEntityIds(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int len = packet.atLeast(ProtocolVersion.v1_8) ? in.readVarInt() : in.readByte();
            List<Integer> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(packet.atLeast(ProtocolVersion.v1_8) ? in.readVarInt() : in.readInt());
            }
            return result;
        }
    }

    public static Packet write(PacketTypeRegistry registry, int...entityIds) throws IOException {
        Packet packet = new Packet(registry, PacketType.DestroyEntities);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeVarInt(entityIds.length);
            } else {
                out.writeByte(entityIds.length);
            }
            for (int entityId : entityIds) {
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    out.writeVarInt(entityId);
                } else {
                    out.writeInt(entityId);
                }
            }
        }
        return packet;
    }
}
