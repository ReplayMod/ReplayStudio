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
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.containers.PacketStateTree;
import com.replaymod.replaystudio.rar.containers.TransientThings;

import java.io.IOException;

public class World implements RandomAccessState {
    private final TransientThings transientThings;
    private final PacketStateTree viewPosition; // 1.14+
    private final PacketStateTree viewDistance; // 1.14+
    private final PacketStateTree worldTimes;
    private final PacketStateTree thunderStrengths; // For some reason, this isn't tied to Weather

    public World(PacketTypeRegistry registry, NetInput in) throws IOException {
        this.transientThings = new TransientThings(registry, in);
        this.viewPosition = new PacketStateTree(registry, in.readVarInt());
        this.viewDistance = new PacketStateTree(registry, in.readVarInt());
        this.worldTimes = new PacketStateTree(registry, in.readVarInt());
        this.thunderStrengths = new PacketStateTree(registry, in.readVarInt());
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        viewPosition.load(sink, cache);
        viewDistance.load(sink, cache);
        transientThings.load(sink, cache);
        worldTimes.load(sink, cache);
        thunderStrengths.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        viewPosition.unload(sink, cache);
        viewDistance.unload(sink, cache);
        transientThings.unload(sink, cache);
        worldTimes.unload(sink, cache);
        thunderStrengths.unload(sink, cache);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        viewPosition.play(sink, currentTimeStamp, targetTime);
        viewDistance.play(sink, currentTimeStamp, targetTime);
        transientThings.play(sink, currentTimeStamp, targetTime);
        worldTimes.play(sink, currentTimeStamp, targetTime);
        thunderStrengths.play(sink, currentTimeStamp, targetTime);
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        viewPosition.rewind(sink, currentTimeStamp, targetTime);
        viewDistance.rewind(sink, currentTimeStamp, targetTime);
        transientThings.rewind(sink, currentTimeStamp, targetTime);
        worldTimes.rewind(sink, currentTimeStamp, targetTime);
        thunderStrengths.rewind(sink, currentTimeStamp, targetTime);
    }

    public static class Builder {
        private final WriteableCache cache;

        public final TransientThings.Builder transientThings;
        public final PacketStateTree.Builder viewPosition = new PacketStateTree.Builder();
        public final PacketStateTree.Builder viewDistance = new PacketStateTree.Builder();
        public final PacketStateTree.Builder worldTimes = new PacketStateTree.Builder();
        public final PacketStateTree.Builder thunderStrengths = new PacketStateTree.Builder();

        public Builder(PacketTypeRegistry registry, WriteableCache cache) throws IOException {
            this.cache = cache;
            transientThings = new TransientThings.Builder(registry, cache);
        }

        public void build(NetOutput out, int time) throws IOException {
            transientThings.build(out, time);
            out.writeVarInt(viewPosition.build(cache));
            out.writeVarInt(viewDistance.build(cache));
            out.writeVarInt(worldTimes.build(cache));
            out.writeVarInt(thunderStrengths.build(cache));
        }
    }
}
