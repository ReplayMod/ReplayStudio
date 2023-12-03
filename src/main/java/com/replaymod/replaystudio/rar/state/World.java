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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketJoinGame;
import com.replaymod.replaystudio.protocol.packets.PacketRespawn;
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.containers.PacketStateTree;
import com.replaymod.replaystudio.rar.containers.TransientThings;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class World implements RandomAccessState {
    public final Info info;
    private final TransientThings transientThings;
    private final PacketStateTree viewPosition; // 1.14+
    private final PacketStateTree viewDistance; // 1.14+
    private final PacketStateTree simulationDistance; // 1.18+
    private final PacketStateTree worldTimes;
    // These may look like they should be tied to weather, but they are actually independent.
    // MC just sends a weather start/stop packet at 0.2 rain strength. That toggle packet also overwrites the rain
    // strength on the client, but it's immediately reset by a followup strength packet... smells like legacy code.
    // And now we have deal with that, but luckily it should be as simple as making sure we send strength packets
    // after rain start/stop packets.
    private final PacketStateTree rainStrengths;
    private final PacketStateTree thunderStrengths;

    public World(PacketTypeRegistry registry, NetInput in) throws IOException {
        this.info = new Info(registry, in);
        this.transientThings = new TransientThings(registry, in.readVarInt());
        this.viewPosition = new PacketStateTree(registry, in.readVarInt());
        this.viewDistance = new PacketStateTree(registry, in.readVarInt());
        this.simulationDistance = new PacketStateTree(registry, in.readVarInt());
        this.worldTimes = new PacketStateTree(registry, in.readVarInt());
        this.rainStrengths = new PacketStateTree(registry, in.readVarInt());
        this.thunderStrengths = new PacketStateTree(registry, in.readVarInt());
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        viewPosition.load(sink, cache);
        viewDistance.load(sink, cache);
        simulationDistance.load(sink, cache);
        transientThings.load(sink, cache);
        worldTimes.load(sink, cache);
        rainStrengths.load(sink, cache);
        thunderStrengths.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        viewPosition.unload(sink, cache);
        viewDistance.unload(sink, cache);
        simulationDistance.unload(sink, cache);
        transientThings.unload(sink, cache);
        worldTimes.unload(sink, cache);
        rainStrengths.unload(sink, cache);
        thunderStrengths.unload(sink, cache);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        viewPosition.play(sink, currentTimeStamp, targetTime);
        viewDistance.play(sink, currentTimeStamp, targetTime);
        simulationDistance.play(sink, currentTimeStamp, targetTime);
        transientThings.play(sink, currentTimeStamp, targetTime);
        worldTimes.play(sink, currentTimeStamp, targetTime);
        rainStrengths.play(sink, currentTimeStamp, targetTime);
        thunderStrengths.play(sink, currentTimeStamp, targetTime);
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        viewPosition.rewind(sink, currentTimeStamp, targetTime);
        viewDistance.rewind(sink, currentTimeStamp, targetTime);
        simulationDistance.rewind(sink, currentTimeStamp, targetTime);
        transientThings.rewind(sink, currentTimeStamp, targetTime);
        worldTimes.rewind(sink, currentTimeStamp, targetTime);
        rainStrengths.rewind(sink, currentTimeStamp, targetTime);
        thunderStrengths.rewind(sink, currentTimeStamp, targetTime);
    }

    public static class Builder {
        private final WriteableCache cache;
        private final PacketTypeRegistry registry;

        public final Info info;
        public final TransientThings.Builder transientThings;
        public final PacketStateTree.Builder viewPosition = new PacketStateTree.Builder();
        public final PacketStateTree.Builder viewDistance = new PacketStateTree.Builder();
        public final PacketStateTree.Builder simulationDistance = new PacketStateTree.Builder();
        public final PacketStateTree.Builder worldTimes = new PacketStateTree.Builder();
        public final PacketStateTree.Builder rainStrengths = new PacketStateTree.Builder();
        public final PacketStateTree.Builder thunderStrengths = new PacketStateTree.Builder();

        public Builder(PacketTypeRegistry registry, WriteableCache cache, Info info) throws IOException {
            this.registry = registry;
            this.cache = cache;
            this.info = info;
            transientThings = new TransientThings.Builder(registry, cache, info.dimensionType);
        }

        public void build(NetOutput out, int time) throws IOException {
            info.write(registry, out);
            out.writeVarInt(transientThings.build(time));
            out.writeVarInt(viewPosition.build(cache));
            out.writeVarInt(viewDistance.build(cache));
            out.writeVarInt(simulationDistance.build(cache));
            out.writeVarInt(worldTimes.build(cache));
            out.writeVarInt(rainStrengths.build(cache));
            out.writeVarInt(thunderStrengths.build(cache));
        }
    }

    public static final class Info {
        public final List<String> dimensions; // 1.16+
        public final CompoundTag registries; // 1.16+
        public final String dimension;
        public final DimensionType dimensionType;
        public final long seed; // 1.15+
        public final int difficulty; // pre 1.14
        public final boolean debugWorld; // 1.16+
        public final boolean flatWorld; // 1.16+

        public Info(List<String> dimensions, CompoundTag registries, String dimension, DimensionType dimensionType, long seed, int difficulty, boolean debugWorld, boolean flatWorld) {
            this.dimensions = dimensions;
            this.registries = registries;
            this.dimension = dimension;
            this.dimensionType = dimensionType;
            this.seed = seed;
            this.difficulty = difficulty;
            this.debugWorld = debugWorld;
            this.flatWorld = flatWorld;
        }

        public Info(PacketJoinGame packet, CompoundTag registries) {
            this(packet.dimensions, registries, packet.dimension, packet.dimensionType, packet.seed, packet.difficulty, packet.debugWorld, packet.flatWorld);
        }

        public Info(List<String> dimensions, CompoundTag registries, PacketRespawn packet) {
            this(dimensions, registries, packet.dimension, packet.dimensionType, packet.seed, packet.difficulty, packet.debugWorld, packet.flatWorld);
        }

        public Info(Info info, PacketRespawn packet) {
            this(info.dimensions, info.registries, packet);
        }

        public Info(PacketTypeRegistry registry, NetInput in) throws IOException {
            this(
                    registry.atLeast(ProtocolVersion.v1_16) ? Packet.Reader.readList(registry, in, in::readString) : null,
                    registry.atLeast(ProtocolVersion.v1_16) ? Packet.Reader.readNBT(registry, in) : null,
                    in.readString(),
                    new DimensionType(requireNonNull(Packet.Reader.readNBT(registry, in)), in.readString()),
                    in.readLong(),
                    in.readByte(),
                    in.readBoolean(),
                    in.readBoolean()
            );
        }

        public void write(PacketTypeRegistry registry, NetOutput out) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_16)) {
                Packet.Writer.writeList(registry, out, dimensions, out::writeString);
                Packet.Writer.writeNBT(registry, out, this.registries);
            }
            out.writeString(dimension);
            Packet.Writer.writeNBT(registry, out, dimensionType.getTag());
            out.writeString(dimensionType.getName());
            out.writeLong(seed);
            out.writeByte(difficulty);
            out.writeBoolean(debugWorld);
            out.writeBoolean(flatWorld);
        }

        public boolean isRespawnSufficient(Info other) {
            // We can get away with skipping the JoinGame packet if none of the relevant info changed
            return Objects.equals(this.dimensions, other.dimensions)
                    && Objects.equals(this.registries, other.registries)
                    // but only if the dimension did change, otherwise a simple respawn is insufficient
                    && !this.dimension.equals(other.dimension);
        }

        public PacketJoinGame toPacketJoinGame() {
            PacketJoinGame joinGame = new PacketJoinGame();
            joinGame.entityId = -1789435; // arbitrary negative value
            joinGame.gameMode = 3; // Spectator
            joinGame.prevGameMode = 3; // Spectator
            joinGame.dimensions = dimensions;
            joinGame.registries = registries;
            joinGame.dimensionType = dimensionType;
            joinGame.dimension = dimension;
            joinGame.seed = seed;
            joinGame.difficulty = difficulty;
            joinGame.debugWorld = debugWorld;
            joinGame.flatWorld = flatWorld;
            return joinGame;
        }

        public PacketRespawn toRespawnPacket() {
            PacketRespawn respawn = new PacketRespawn();
            respawn.gameMode = 3; // Spectator
            respawn.prevGameMode = 3; // Spectator
            respawn.dimensionType = dimensionType;
            respawn.dimension = dimension;
            respawn.seed = seed;
            respawn.difficulty = difficulty;
            respawn.debugWorld = debugWorld;
            respawn.flatWorld = flatWorld;
            return respawn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Info info = (Info) o;
            return seed == info.seed
                    && difficulty == info.difficulty
                    && debugWorld == info.debugWorld
                    && flatWorld == info.flatWorld
                    && Objects.equals(dimensions, info.dimensions)
                    && Objects.equals(registries, info.registries)
                    && dimension.equals(info.dimension)
                    && dimensionType.equals(info.dimensionType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimensions, registries, dimension, dimensionType, seed, difficulty, debugWorld, flatWorld);
        }
    }
}
