/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 * It is partially derived from MCProtocolLib <https://github.com/ReplayMod/MCProtocolLib>, under the same license.
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
 * Copyright (c) 2013-2019 Steveice10
 * Copyright (c) MCProtocolLib contributors (see git at 34352c1, linked above)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;

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
