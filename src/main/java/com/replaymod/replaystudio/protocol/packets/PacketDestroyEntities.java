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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PacketDestroyEntities {
    public static List<Integer> getEntityIds(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.getType() == PacketType.DestroyEntity) {
                return Collections.singletonList(in.readVarInt());
            }
            int len = packet.atLeast(ProtocolVersion.v1_8) ? in.readVarInt() : in.readByte();
            List<Integer> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(packet.atLeast(ProtocolVersion.v1_8) ? in.readVarInt() : in.readInt());
            }
            return result;
        }
    }

    public static Collection<Packet> write(PacketTypeRegistry registry, int...entityIds) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_17) && registry.olderThan(ProtocolVersion.v1_17_1)) {
            List<Packet> packets = new ArrayList<>(entityIds.length);
            for (int entityId : entityIds) {
                packets.add(write(registry, entityId));
            }
            return packets;
        } else {
            return Collections.singletonList(writeDestroyEntities(registry, entityIds));
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_17) && registry.olderThan(ProtocolVersion.v1_17_1)) {
            Packet packet = new Packet(registry, PacketType.DestroyEntity);
            try (Packet.Writer out = packet.overwrite()) {
                out.writeVarInt(entityId);
            }
            return packet;
        } else {
            return writeDestroyEntities(registry, entityId);
        }
    }

    // All versions except 1.17.0
    private static Packet writeDestroyEntities(PacketTypeRegistry registry, int...entityIds) throws IOException {
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
