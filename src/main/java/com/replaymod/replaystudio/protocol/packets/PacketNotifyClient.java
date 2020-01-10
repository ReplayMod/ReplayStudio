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
        Packet packet = new Packet(registry, PacketType.PlayerListEntry);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeByte(action.ordinal());
            out.writeFloat(value);
        }
        return packet;
    }
}
