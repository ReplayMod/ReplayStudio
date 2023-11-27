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
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.containers.PacketStateTree;
import com.replaymod.replaystudio.rar.containers.WorldStateTree;

import java.io.IOException;

public class Replay implements RandomAccessState {
    private final PacketTypeRegistry registry;
    public final PacketStateTree features;
    public final PacketStateTree tags;
    private final WorldStateTree world;

    public Replay(PacketTypeRegistry registry, NetInput in) throws IOException {
        this.registry = registry;
        features = new PacketStateTree(registry, in.readVarInt());
        tags = new PacketStateTree(registry, in.readVarInt());
        world = new WorldStateTree(registry, this, in.readVarInt());
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        features.load(sink, cache);
        tags.load(sink, cache);
        world.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        world.unload(sink, cache);
        tags.unload(sink, cache);
        features.unload(sink, cache);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        // As of 1.20.2, features are exclusive to the config phase and will instead be sent on world change
        if (registry.olderThan(ProtocolVersion.v1_20_2)) {
            features.play(sink, currentTimeStamp, targetTime);
        }
        tags.play(sink, currentTimeStamp, targetTime);
        world.play(sink, currentTimeStamp, targetTime);
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        // As of 1.20.2, features are exclusive to the config phase and will instead be sent on world change
        if (registry.olderThan(ProtocolVersion.v1_20_2)) {
            features.rewind(sink, currentTimeStamp, targetTime);
        }
        tags.rewind(sink, currentTimeStamp, targetTime);
        world.rewind(sink, currentTimeStamp, targetTime);
    }

    public static class Builder {
        private final WriteableCache cache;
        public final PacketStateTree.Builder features = new PacketStateTree.Builder();
        public final PacketStateTree.Builder tags = new PacketStateTree.Builder();
        private final WorldStateTree.Builder worlds;
        public World.Builder world;

        public Builder(PacketTypeRegistry registry, WriteableCache cache) throws IOException {
            this.cache = cache;
            this.worlds = new WorldStateTree.Builder(registry, cache);
        }

        public World.Builder newWorld(int time, World.Info info) throws IOException {
            return world = worlds.newWorld(time, info);
        }

        public void build(NetOutput out, int time) throws IOException {
            out.writeVarInt(features.build(cache));
            out.writeVarInt(tags.build(cache));
            out.writeVarInt(worlds.build(time));
        }
    }
}
