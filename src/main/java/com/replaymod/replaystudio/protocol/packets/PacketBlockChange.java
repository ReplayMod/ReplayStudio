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
import java.util.Collections;
import java.util.List;

public class PacketBlockChange {
    private IPosition pos;
    private int id;

    private PacketBlockChange() {}
    public PacketBlockChange(IPosition pos, int id) {
        this.pos = pos;
        this.id = id;
    }

    public static PacketBlockChange read(Packet packet) throws IOException {
        PacketBlockChange p = new PacketBlockChange();
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                p.pos = in.readPosition();
                p.id = in.readVarInt();
            } else {
                int x = in.readInt();
                int y = in.readUnsignedByte();
                int z = in.readInt();
                p.pos = new IPosition(x, y, z);
                p.id = (in.readVarInt() << 4) | (in.readUnsignedByte() & 0xf);
            }
        }
        return p;
    }

    public static Packet write(PacketTypeRegistry registry, IPosition pos, int id) throws IOException {
        return new PacketBlockChange(pos, id).write(registry);
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.BlockChange);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writePosition(pos);
                out.writeVarInt(id);
            } else {
                out.writeInt(pos.getX());
                out.writeByte(pos.getY());
                out.writeInt(pos.getZ());
                out.writeVarInt(id >> 4);
                out.writeByte(id & 0xf);
            }
        }
        return packet;
    }

    public static List<PacketBlockChange> readBulk(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int chunkX = in.readInt();
            int chunkZ = in.readInt();
            PacketBlockChange[] result;
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                result = new PacketBlockChange[in.readVarInt()];
            } else {
                result = new PacketBlockChange[in.readShort()];
                in.readInt(); // Unneeded size variable
            }
            for(int index = 0; index < result.length; index++) {
                PacketBlockChange p = new PacketBlockChange();

                short coords = in.readShort();
                int x = (chunkX << 4) + (coords >> 12 & 15);
                int y = coords & 255;
                int z = (chunkZ << 4) + (coords >> 8 & 15);
                p.pos = new IPosition(x, y, z);

                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    p.id = in.readVarInt();
                } else {
                    p.id = in.readShort();
                }
                result[index] = p;
            }
            return Arrays.asList(result);
        }
    }

    public static List<PacketBlockChange> readSingleOrBulk(Packet packet) throws IOException {
        if (packet.getType() == PacketType.BlockChange) {
            return Collections.singletonList(read(packet));
        } else {
            return readBulk(packet);
        }
    }

    public IPosition getPosition() {
        return pos;
    }

    public int getId() {
        return id;
    }
}
