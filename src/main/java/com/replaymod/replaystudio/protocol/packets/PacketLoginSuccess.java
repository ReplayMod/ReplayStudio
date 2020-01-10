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
import com.replaymod.replaystudio.util.IPosition;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PacketLoginSuccess {
    private final String id;
    private final String name;

    public PacketLoginSuccess(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static PacketLoginSuccess read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            return new PacketLoginSuccess(in.readString(), in.readString());
        }
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.LoginSuccess);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeString(id);
            out.writeString(name);
        }
        return packet;
    }
}
