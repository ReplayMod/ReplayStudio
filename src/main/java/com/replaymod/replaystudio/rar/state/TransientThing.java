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

package com.replaymod.replaystudio.rar.state;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.LazyPacketList;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;

import java.io.IOException;

public abstract class TransientThing implements RandomAccessState {
    protected final PacketTypeRegistry registry;
    public final int spawnTime;
    public final int despawnTime;
    private final LazyPacketList spawnPackets;
    private final LazyPacketList despawnPackets;

    public TransientThing(PacketTypeRegistry registry, NetInput in) throws IOException {
        this.registry = registry;

        spawnTime = in.readVarInt();
        despawnTime = in.readVarInt();
        spawnPackets = new LazyPacketList(registry, in.readVarInt());
        despawnPackets = new LazyPacketList(registry, in.readVarInt());
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        spawnPackets.read(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        despawnPackets.read(sink, cache);
    }

    public static class Builder {
        private int spawnTime;
        private int despawnTime;
        public final LazyPacketList.Builder spawnPackets = new LazyPacketList.Builder();
        public final LazyPacketList.Builder despawnPackets = new LazyPacketList.Builder();

        public void build(NetOutput out, WriteableCache cache) throws IOException {
            out.writeVarInt(spawnTime);
            out.writeVarInt(despawnTime);
            out.writeVarInt(spawnPackets.build(cache));
            out.writeVarInt(despawnPackets.build(cache));
        }

        public void setSpawnTime(int spawnTime) {
            this.spawnTime = spawnTime;
        }

        public void setDespawnTime(int despawnTime) {
            this.despawnTime = despawnTime;
        }

        public void addSpawnPacket(Packet packet) {
            spawnPackets.add(packet);
        }

        public void addDespawnPacket(Packet packet) {
            despawnPackets.add(packet);
        }
    }
}
