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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PacketTeam {
    public enum Action {
        CREATE,
        REMOVE,
        UPDATE,
        ADD_PLAYER,
        REMOVE_PLAYER,
    }

    public static String getName(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            return in.readString();
        }
    }

    public static Action getAction(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readString(); // name
            return Action.values()[in.readByte()];
        }
    }

    public static List<String> getPlayers(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readString(); // name
            Action action = Action.values()[in.readByte()];
            if (action != Action.CREATE && action != Action.ADD_PLAYER && action != Action.REMOVE_PLAYER) {
                return Collections.emptyList();
            }

            if (action == Action.CREATE) {
                in.readString(); // display name
                if (!packet.atLeast(ProtocolVersion.v1_13)) {
                    in.readString(); // prefix
                    in.readString(); // suffix
                }
                in.readByte(); // flags
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    in.readString(); // name tag visibility
                    if (packet.atLeast(ProtocolVersion.v1_9)) {
                        in.readString(); // collision rule
                    }
                    if (packet.atLeast(ProtocolVersion.v1_13)) {
                        in.readVarInt(); // color
                        in.readString(); // prefix
                        in.readString(); // suffix
                    } else {
                        in.readByte(); // color
                    }
                }
            }

            int count;
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                count = in.readVarInt();
            } else {
                count = in.readShort();
            }
            List<String> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(in.readString());
            }
            return result;
        }
    }

    public static Packet addPlayers(PacketTypeRegistry registry, String name, Collection<String> players) throws IOException {
        return addOrRemovePlayers(registry, name, Action.ADD_PLAYER, players);
    }

    public static Packet removePlayers(PacketTypeRegistry registry, String name, Collection<String> players) throws IOException {
        return addOrRemovePlayers(registry, name, Action.REMOVE_PLAYER, players);
    }

    private static Packet addOrRemovePlayers(PacketTypeRegistry registry, String name, Action action, Collection<String> players) throws IOException {
        Packet packet = new Packet(registry, PacketType.Team);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeString(name);
            out.writeByte(action.ordinal());
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeVarInt(players.size());
            } else {
                out.writeShort(players.size());
            }
            for (String player : players) {
                out.writeString(player);
            }
        }
        return packet;
    }
}
