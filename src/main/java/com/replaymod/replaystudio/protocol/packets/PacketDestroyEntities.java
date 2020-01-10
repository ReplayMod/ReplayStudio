/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
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
import java.util.List;

public class PacketDestroyEntities {
    public static List<Integer> getEntityIds(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int len = packet.atLeast(ProtocolVersion.v1_8) ? in.readVarInt() : in.readByte();
            List<Integer> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(packet.atLeast(ProtocolVersion.v1_8) ? in.readVarInt() : in.readInt());
            }
            return result;
        }
    }

    public static Packet write(PacketTypeRegistry registry, int...entityIds) throws IOException {
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
