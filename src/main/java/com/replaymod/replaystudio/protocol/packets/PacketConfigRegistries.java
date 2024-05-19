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

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.registry.Registries;

import java.io.IOException;

public class PacketConfigRegistries {
    public static Registries read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            return new Registries(in.readNBT());
        }
    }

    public static Packet write(PacketTypeRegistry registry, Registries registries) throws IOException {
        Packet packet = new Packet(registry, PacketType.ConfigRegistries);
        try (Packet.Writer writer = packet.overwrite()) {
            writer.writeNBT(registries.registriesTag);
        }
        return packet;
    }
}
