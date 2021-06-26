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
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;

import java.io.IOException;

public class Replay implements RandomAccessState {
    private final World world;

    public Replay(PacketTypeRegistry registry, NetInput in) throws IOException {
        this.world = new World(registry, in);
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        world.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        world.unload(sink, cache);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        world.play(sink, currentTimeStamp, targetTime);
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        world.rewind(sink, currentTimeStamp, targetTime);
    }

    public static class Builder {
        private final PacketTypeRegistry registry;
        private final WriteableCache cache;
        public World.Builder world;

        public Builder(PacketTypeRegistry registry, WriteableCache cache) throws IOException {
            this.registry = registry;
            this.cache = cache;
        }

        public void newWorld(DimensionType dimensionType) throws IOException {
            if (this.world != null) {
                throw new IllegalStateException("Multiple worlds are not yet supported."); // TODO
            }
            this.world = new World.Builder(registry, cache, dimensionType);
        }

        public void build(NetOutput out, int time) throws IOException {
            world.build(out, time);
        }
    }
}
