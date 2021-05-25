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

package com.replaymod.replaystudio.rar.cache;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.rar.PacketSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.replaystudio.util.Utils.readCompressedPacket;
import static com.replaymod.replaystudio.util.Utils.writeCompressedPacket;

public class LazyPacketList {
    private final PacketTypeRegistry registry;
    private final int index;

    public LazyPacketList(PacketTypeRegistry registry, int index) {
        this.registry = registry;
        this.index = index;
    }

    public void read(PacketSink sink, ReadableCache cache) throws IOException {
        NetInput in = cache.seek(index);
        for (int i = in.readVarInt(); i > 0; i--) {
            sink.accept(readCompressedPacket(registry, in));
        }
    }

    public static class Builder {
        public final List<Packet> list = new ArrayList<>();

        public void add(Packet packet) {
            list.add(packet);
        }

        public int build(WriteableCache cache) throws IOException {
            int index = cache.index();

            NetOutput out = cache.write();
            out.writeVarInt(list.size());
            for (Packet packet : list) {
                writeCompressedPacket(out, packet);
                packet.release();
            }

            return index;
        }
    }
}
