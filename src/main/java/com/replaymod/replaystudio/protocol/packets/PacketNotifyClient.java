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

import java.io.IOException;

public class PacketNotifyClient {
    public enum Action {
        INVALID_BED,
        START_RAIN,
        STOP_RAIN,
        CHANGE_GAMEMODE,
        ENTER_CREDITS,
        DEMO_MESSAGE,
        ARROW_HIT_PLAYER,
        RAIN_STRENGTH,
        THUNDER_STRENGTH,
        AFFECTED_BY_PUFFERFISH,
        AFFECTED_BY_ELDER_GUARDIAN,
        IMMEDIATE_RESPAWN,
        ;
    }

    public static Action getAction(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            return Action.values()[in.readUnsignedByte()];
        }
    }

    public static float getValue(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readUnsignedByte(); // action
            return in.readFloat();
        }
    }

    public static Packet write(PacketTypeRegistry registry, Action action, float value) throws IOException {
        Packet packet = new Packet(registry, PacketType.NotifyClient);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeByte(action.ordinal());
            out.writeFloat(value);
        }
        return packet;
    }
}
