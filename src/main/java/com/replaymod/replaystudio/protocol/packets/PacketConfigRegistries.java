/*
 * Copyright (c) 2023
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

import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.registry.Registries;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class PacketConfigRegistries {
    public static void read(Packet packet, Registries registries) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_20_5)) {
                String registryName = in.readString();
                List<Pair<String, Tag>> registryEntries = in.readList(() -> {
                    String key = in.readString();
                    Tag value = in.readBoolean() ? in.readNBT() : null;
                    return Pair.of(key, value);
                });
                if (registries.registriesMap == null) {
                    registries.registriesMap = new HashMap<>();
                }
                registries.registriesMap.computeIfAbsent(registryName, it -> new ArrayList<>()).addAll(registryEntries);
            } else {
                registries.registriesTag = in.readNBT();
            }
        }
    }

    public static List<Packet> write(PacketTypeRegistry registry, Registries registries) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_20_5)) {
            if (registries.registriesMap == null) {
                return Collections.emptyList();
            }
            List<Packet> packets = new ArrayList<>(registries.registriesMap.size());
            for (Map.Entry<String, List<Pair<String, Tag>>> registryEntry : registries.registriesMap.entrySet()) {
                Packet packet = new Packet(registry, PacketType.ConfigRegistries);
                try (Packet.Writer writer = packet.overwrite()) {
                    writer.writeString(registryEntry.getKey());
                    writer.writeList(registryEntry.getValue(), entry -> {
                        writer.writeString(entry.getKey());
                        Tag value = entry.getValue();
                        if (value != null) {
                            writer.writeBoolean(true);
                            writer.writeNBT(value);
                        } else {
                            writer.writeBoolean(false);
                        }
                    });
                }
                packets.add(packet);
            }
            return packets;
        } else {
            Packet packet = new Packet(registry, PacketType.ConfigRegistries);
            try (Packet.Writer writer = packet.overwrite()) {
                writer.writeNBT(registries.registriesTag);
            }
            return Collections.singletonList(packet);
        }
    }
}
