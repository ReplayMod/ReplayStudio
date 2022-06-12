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
import com.replaymod.replaystudio.util.Property;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PacketLoginSuccess {
    private final UUID id;
    private final String name;
    private final List<Property> properties; // 1.19+

    public PacketLoginSuccess(UUID id, String name, List<Property> properties) {
        this.id = id;
        this.name = name;
        this.properties = properties;
    }

    public static PacketLoginSuccess read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            UUID id;
            if (packet.atLeast(ProtocolVersion.v1_16)) {
                id = in.readUUID();
            } else {
                id = UUID.fromString(in.readString());
            }
            String name = in.readString();
            List<Property> properties = null;
            if (packet.atLeast(ProtocolVersion.v1_19)) {
                properties = in.readList(() -> Property.read(in));
            }
            return new PacketLoginSuccess(id, name, properties);
        }
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.LoginSuccess);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_16)) {
                out.writeUUID(id);
            } else {
                out.writeString(id.toString());
            }
            out.writeString(name);
            if (packet.atLeast(ProtocolVersion.v1_19)) {
                out.writeList(properties, it -> it.write(out));
            }
        }
        return packet;
    }
}
