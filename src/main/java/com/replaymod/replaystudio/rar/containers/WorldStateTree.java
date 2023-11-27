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

package com.replaymod.replaystudio.rar.containers;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.Unpooled;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketConfigRegistries;
import com.replaymod.replaystudio.protocol.packets.PacketJoinGame;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.state.Replay;
import com.replaymod.replaystudio.rar.state.World;
import com.replaymod.replaystudio.util.IPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WorldStateTree extends StateTree<World> {
    private final PacketTypeRegistry registry;
    private final Replay replay;
    private ReadableCache cache;
    private World activeWorld;

    public WorldStateTree(PacketTypeRegistry registry, Replay replay, int index) {
        super(index);
        this.registry = registry;
        this.replay = replay;
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        this.cache = cache;
        super.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        if (activeWorld != null) {
            activeWorld.unload(sink, cache);
            activeWorld = null;
        }
        this.cache = null;
        super.unload(sink, cache);
    }

    @Override
    protected World read(NetInput in) throws IOException {
        return new World(registry, in);
    }

    @Override
    protected void discard(World value) {
    }

    private void ensureActiveWorld(World world) throws IOException {
        if (world == activeWorld) {
            return; // our cache is up-to-date, nothing to do
        }

        // Slow path, this should almost never be required for normal operation
        // unless the client state is completely reset.

        if (activeWorld != null) {
            activeWorld.unload(Packet::release, cache);
            activeWorld = null;
        }

        if (world != null) {
            world.load(Packet::release, cache);
            activeWorld = world;
        }
    }

    // If the world does not change between current and target time, simply returns the world.
    // Otherwise unloads the old one and switches to the new one (already playing it to the target time).
    private World getWorldOrSwitch(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        Map.Entry<Integer, World> previousEntry = map.floorEntry(currentTimeStamp);
        Map.Entry<Integer, World> targetEntry = map.floorEntry(targetTime);
        World previousWorld = previousEntry != null ? previousEntry.getValue() : null;
        World targetWorld = targetEntry != null ? targetEntry.getValue() : null;
        if (previousWorld == null && targetWorld == null) {
            return null;
        }

        ensureActiveWorld(previousWorld);

        if (previousWorld == targetWorld) {
            return targetWorld;
        } else {
            if (previousWorld != null) {
                previousWorld.unload(sink, cache);
            }

            activeWorld = targetWorld;

            if (targetWorld != null) {
                List<Packet> featuresChanged;
                if (registry.atLeast(ProtocolVersion.v1_20_2)) {
                    // Need to check if any features need changing between the previous world and the current one.
                    // If so, we definitely need to go through the config phase.
                    featuresChanged = new ArrayList<>();
                    replay.features.playOrRewind(featuresChanged::add, previousEntry != null ? previousEntry.getKey() : -1, targetEntry.getKey());
                } else {
                    // Features do not yet need to be correlated with worlds.
                    // They are sent from [Replay] instead.
                    featuresChanged = Collections.emptyList();
                }

                if (previousWorld == null || !previousWorld.info.equals(targetWorld.info) || !featuresChanged.isEmpty()) {
                    boolean isRespawnSufficient = previousWorld != null && previousWorld.info.isRespawnSufficient(targetWorld.info);

                    if (registry.atLeast(ProtocolVersion.v1_20_2)) {
                        // On first join or on certain changes that require it, we need to re-enter configuration phase
                        boolean registriesChanged = previousWorld == null || !previousWorld.info.registries.equals(targetWorld.info.registries);
                        if (registriesChanged || !featuresChanged.isEmpty()) {
                            // Enter config phase
                            PacketTypeRegistry configRegistry = registry.withState(State.CONFIGURATION);
                            sink.accept(new Packet(registry, PacketType.Reconfigure));

                            // Update game registries
                            if (registriesChanged) {
                                sink.accept(PacketConfigRegistries.write(configRegistry, targetWorld.info.registries));

                                // Sending game registries resets any previously sent tags, so we need to re-send those.
                                replay.tags.play(packet -> {
                                    // We store tags as play-phase packets though, so we need to convert them if we want
                                    // to send them in the config phase. Their encoding is identical.
                                    assert packet.getType() == PacketType.Tags;
                                    sink.accept(new Packet(configRegistry, PacketType.ConfigTags, packet.getBuf()));
                                }, -1, targetTime);
                            }

                            // Update feature flags
                            if (!featuresChanged.isEmpty()) {
                                featuresChanged.forEach(sink);
                            }

                            // Leave config phase
                            sink.accept(new Packet(configRegistry, PacketType.ConfigFinish));
                            isRespawnSufficient = false;
                        }
                    }

                    // On first join or on significant changes, we need to send another full JoinGame packet
                    // which is more complicated than just a respawn packet.
                    if (!isRespawnSufficient) {
                        // We need to send a JoinGame packet to update the client. Followed by a player pos look packet
                        // to get rid of the Loading Terrain screen.
                        PacketJoinGame joinGame = targetWorld.info.toPacketJoinGame();
                        if (registry.olderThan(ProtocolVersion.v1_16)) {
                            // In older versions, the Respawn packet only sets `positionLookSetup` if the dimension
                            // actually changes, so we send them to the wrong one initially. For newer versions we don't
                            // cause not having to change twice is faster.
                            joinGame.dimension = joinGame.dimension.equals("0") ? "-1" : "0";
                        }
                        sink.accept(joinGame.write(registry));

                        // JoinGame resets various interdimensional game state, so we need to restore that
                        if (registry.olderThan(ProtocolVersion.v1_20_2)) {
                            replay.tags.play(sink, -1, targetTime);
                        }

                        // But on versions prior to 1.18.2, we also require `ClientPlayNetworkHandler.positionLookSetup`
                        // to be set to false before that arrives (only the first pos look packet closes the screen),
                        // which only happens when we send a Respawn packet. So we must send one of those as well.
                        if (registry.olderThan(ProtocolVersion.v1_18_2)) {
                            sink.accept(targetWorld.info.toRespawnPacket().write(registry));
                        }
                    } else {
                        sink.accept(targetWorld.info.toRespawnPacket().write(registry));
                    }

                    // This packet is required to close the "Loading Terrain" screen
                    Packet packet;
                    if (registry.atLeast(ProtocolVersion.v1_18_2)) {
                        packet = new Packet(registry, PacketType.SpawnPosition);
                        try (Packet.Writer out = packet.overwrite()) {
                            out.writePosition(new IPosition(0, 0, 0));
                            out.writeFloat(0);
                        }
                    } else {
                        packet = new Packet(registry, PacketType.PlayerPositionRotation);
                        try (Packet.Writer out = packet.overwrite()) {
                            out.writeDouble(0); // x
                            out.writeDouble(0); // y
                            out.writeDouble(0); // z
                            out.writeFloat(0); // yaw
                            out.writeFloat(0); // pitch
                            out.writeByte(0); // flags
                            if (packet.atLeast(ProtocolVersion.v1_9)) {
                                out.writeVarInt(0); // teleport id
                            }
                            if (packet.atLeast(ProtocolVersion.v1_17) && packet.atMost(ProtocolVersion.v1_19_3)) {
                                out.writeBoolean(false); // dismount
                            }
                        }
                    }
                    sink.accept(packet);
                }
                targetWorld.load(sink, cache);
                targetWorld.play(sink, -1, targetTime);
            }
            return null;
        }
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        World world = getWorldOrSwitch(sink, currentTimeStamp, targetTime);
        if (world != null) {
            world.play(sink, currentTimeStamp, targetTime);
        }
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        World world = getWorldOrSwitch(sink, currentTimeStamp, targetTime);
        if (world != null) {
            world.rewind(sink, currentTimeStamp, targetTime);
        }
    }

    public static class Builder {
        private final PacketTypeRegistry registry;
        private final WriteableCache cache;
        private final TreeBuilder builder = new TreeBuilder();
        public World.Builder world;

        public Builder(PacketTypeRegistry registry, WriteableCache cache) throws IOException {
            this.registry = registry;
            this.cache = cache;
        }

        public World.Builder newWorld(int time, World.Info info) throws IOException {
            World.Builder builder = new World.Builder(registry, cache, info);
            this.builder.put(time, world = builder);
            return builder;
        }

        public int build(int time) throws IOException {
            builder.endTime = time;
            return builder.build(cache);
        }

        private static class TreeBuilder extends StateTree.Builder<World.Builder> {
            private int endTime;

            @Override
            protected void write(NetOutput out, World.Builder world, int time) throws IOException {
                Integer nextTime = map.higherKey(time);
                world.build(out, nextTime != null ? nextTime : endTime);
            }

            @Override
            protected void discard(World.Builder world) {
                ByteBuf buf = Unpooled.buffer();
                try {
                    world.build(new ByteBufNetOutput(buf), 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                buf.release();
            }
        }
    }
}
