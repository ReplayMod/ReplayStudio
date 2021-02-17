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
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;

import java.io.IOException;

public class PacketSpawnPlayer extends SpawnEntity {
    /**
     * @see PacketPlayerListEntry#getId()
     */
    public static String getPlayerListEntryId(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readVarInt(); // id
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                return in.readUUID().toString(); // uuid
            } else {
                in.readString(); // uuid
                return in.readString(); // name
            }
        }
    }
}
