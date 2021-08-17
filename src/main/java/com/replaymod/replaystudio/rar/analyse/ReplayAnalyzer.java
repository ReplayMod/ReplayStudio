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

package com.replaymod.replaystudio.rar.analyse;

import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketBlockChange;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData;
import com.replaymod.replaystudio.protocol.packets.PacketDestroyEntities;
import com.replaymod.replaystudio.protocol.packets.PacketJoinGame;
import com.replaymod.replaystudio.protocol.packets.PacketNotifyClient;
import com.replaymod.replaystudio.protocol.packets.PacketPlayerListEntry;
import com.replaymod.replaystudio.protocol.packets.PacketRespawn;
import com.replaymod.replaystudio.protocol.packets.PacketSpawnPlayer;
import com.replaymod.replaystudio.protocol.packets.PacketUpdateLight;
import com.replaymod.replaystudio.protocol.packets.PacketUpdateViewDistance;
import com.replaymod.replaystudio.protocol.packets.PacketUpdateViewPosition;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.state.Chunk;
import com.replaymod.replaystudio.rar.state.Entity;
import com.replaymod.replaystudio.rar.state.Replay;
import com.replaymod.replaystudio.rar.state.Weather;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.rar.state.World;
import com.replaymod.replaystudio.util.IPosition;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.replaystudio.util.PacketUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import static com.replaymod.replaystudio.protocol.packets.PacketChunkData.Column.longToX;
import static com.replaymod.replaystudio.protocol.packets.PacketChunkData.Column.longToZ;

public class ReplayAnalyzer {
    private final PacketTypeRegistry registry;
    private final NetOutput out;
    private final Replay.Builder replay;

    private int currentViewChunkX = 0;
    private int currentViewChunkZ = 0;
    private int currentViewDistance = 0;

    private final Map<String, PacketPlayerListEntry> playerListEntries = new HashMap<>();
    private Packet lastLightUpdate = null;

    public ReplayAnalyzer(PacketTypeRegistry registry, NetOutput out, WriteableCache cache) throws IOException {
        this.registry = registry;
        this.out = out;
        this.replay = new Replay.Builder(registry, cache);
    }

    public void analyse(ReplayInputStream in, IntConsumer progress) throws IOException {
        int time = 0;
        PacketData packetData;
        while ((packetData = in.readPacket()) != null) {
            Packet packet = packetData.getPacket();
            time = (int) packetData.getTime();
            progress.accept(time);
            Integer entityId = PacketUtils.getEntityId(packet);
            switch (packet.getType()) {
                case SpawnMob:
                case SpawnObject:
                case SpawnPainting: {
                    Entity.Builder entity = replay.world.transientThings.newEntity(time, entityId);
                    entity.addSpawnPacket(packet.retain());
                    break;
                }
                case SpawnPlayer: {
                    Entity.Builder entity = replay.world.transientThings.newEntity(time, entityId);

                    PacketPlayerListEntry entry = playerListEntries.get(PacketSpawnPlayer.getPlayerListEntryId(packet));
                    if (entry != null) {
                        entity.addSpawnPacket(PacketPlayerListEntry.write(registry, PacketPlayerListEntry.Action.ADD, entry));
                    }

                    entity.addSpawnPacket(packet.retain());
                    break;
                }
                case DestroyEntity:
                case DestroyEntities: {
                    for (int id : PacketDestroyEntities.getEntityIds(packet)) {
                        replay.world.transientThings.removeEntity(time, id);
                    }
                    break;
                }
                case ChunkData: {
                    PacketChunkData chunkData = PacketChunkData.read(packet);
                    PacketChunkData.Column column = chunkData.getColumn();
                    if (column.isFull()) {
                        Chunk.Builder chunk = replay.world.transientThings.newChunk(time, column);
                        if (lastLightUpdate != null) {
                            PacketUpdateLight updateLight = PacketUpdateLight.read(lastLightUpdate);
                            if (column.x == updateLight.getX() && column.z == updateLight.getZ()) {
                                chunk.spawnPackets.list.add(0, lastLightUpdate);
                                lastLightUpdate = null;
                            }
                        }
                    } else {
                        Chunk.Builder chunk = replay.world.transientThings.getChunk(column.x, column.z);
                        if (chunk != null) {
                            chunk.blocks.update(time, column);
                        }
                    }
                    break;
                }
                case UpdateLight: {
                    // A light update packet may be sent either before or after the corresponding chunk packet.
                    // The vanilla server appears to always send it immediately before the chunk packet.
                    // Third-party servers (e.g. Hypixel) may sent it after the corresponding chunk packet, hence
                    // why we must support both options here.
                    PacketUpdateLight updateLight = PacketUpdateLight.read(packet);
                    Chunk.Builder chunk = replay.world.transientThings.getChunk(updateLight.getX(), updateLight.getZ());
                    if (chunk != null && chunk.spawnPackets.list.size() == 1) {
                        // We we already know about the chunk and this is the first light update we receive for it,
                        // then add the packet to the chunks spawn packets.
                        chunk.spawnPackets.list.add(0, packet.retain());
                    } else {
                        // If we don't yet know about the chunk, then store the packet for when the chunk arrives.
                        if (lastLightUpdate != null) {
                            lastLightUpdate.release();
                        }
                        lastLightUpdate = packet.retain();
                    }
                    break;
                }
                case UnloadChunk: {
                    PacketChunkData chunkData = PacketChunkData.read(packet);
                    replay.world.transientThings.removeChunk(time, chunkData.getUnloadX(), chunkData.getUnloadZ());
                    break;
                }
                case BlockChange:
                case MultiBlockChange: {
                    for (PacketBlockChange record : PacketBlockChange.readSingleOrBulk(packet)) {
                        IPosition pos = record.getPosition();
                        Chunk.Builder chunk = replay.world.transientThings.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                        if (chunk != null) {
                            chunk.blocks.update(time, record);
                        }
                    }
                    break;
                }
                case PlayerListEntry: {
                    PacketPlayerListEntry.Action action = PacketPlayerListEntry.getAction(packet);
                    for (PacketPlayerListEntry entry : PacketPlayerListEntry.read(packet)) {
                        switch (action) {
                            case ADD:
                                playerListEntries.put(entry.getId(), entry);
                                break;
                            case GAMEMODE:
                                playerListEntries.computeIfPresent(entry.getId(), (key, it) ->
                                        PacketPlayerListEntry.updateGamemode(it, entry.getGamemode()));
                                break;
                            case LATENCY:
                                playerListEntries.computeIfPresent(entry.getId(), (key, it) ->
                                        PacketPlayerListEntry.updateLatency(it, entry.getLatency()));
                                break;
                            case DISPLAY_NAME:
                                playerListEntries.computeIfPresent(entry.getId(), (key, it) ->
                                        PacketPlayerListEntry.updateDisplayName(it, entry.getDisplayName()));
                                break;
                            case REMOVE:
                                playerListEntries.remove(entry.getId());
                        }
                    }
                    break;
                }
                case Respawn: {
                    PacketRespawn respawn = PacketRespawn.read(packet);
                    String newDimension = respawn.dimension;
                    if (!newDimension.equals(replay.world.info.dimension)) {
                        World.Builder world = replay.newWorld(time, new World.Info(replay.world.info, respawn));
                        if (registry.atLeast(ProtocolVersion.v1_14)) {
                            currentViewChunkX = currentViewChunkZ = 0;
                            world.viewPosition.put(time, PacketUpdateViewPosition.write(registry, 0, 0));
                            world.viewDistance.put(time, PacketUpdateViewDistance.write(registry, currentViewDistance));
                        }
                    }
                    break;
                }
                case JoinGame: {
                    PacketJoinGame joinGame = PacketJoinGame.read(packet);
                    replay.newWorld(time, new World.Info(joinGame));
                    if (registry.atLeast(ProtocolVersion.v1_14)) {
                        currentViewChunkX = currentViewChunkZ = 0;
                        replay.world.viewPosition.put(time, PacketUpdateViewPosition.write(registry, 0, 0));

                        currentViewDistance = joinGame.viewDistance;
                        replay.world.viewDistance.put(time, PacketUpdateViewDistance.write(registry, currentViewDistance));
                    }
                    break;
                }
                case Tags: {
                    replay.tags.put(time, packet.retain());
                    break;
                }
                case UpdateViewPosition: {
                    currentViewChunkX = PacketUpdateViewPosition.getChunkX(packet);
                    currentViewChunkZ = PacketUpdateViewPosition.getChunkZ(packet);
                    invalidateOutOfBoundsChunks(time, currentViewChunkX, currentViewChunkZ, currentViewDistance);

                    replay.world.viewPosition.put(time, packet.retain());
                    break;
                }
                case UpdateViewDistance: {
                    currentViewDistance = PacketUpdateViewDistance.getDistance(packet);
                    invalidateOutOfBoundsChunks(time, currentViewChunkX, currentViewChunkZ, currentViewDistance);

                    replay.world.viewDistance.put(time, packet.retain());
                    break;
                }
                case UpdateTime: {
                    replay.world.worldTimes.put(time, packet.retain());
                    break;
                }
                case NotifyClient: {
                    switch (PacketNotifyClient.getAction(packet)) {
                        case START_RAIN:
                            replay.world.transientThings.newWeather(time);
                            break;
                        case STOP_RAIN:
                            replay.world.transientThings.removeWeather(time);
                            break;
                        case RAIN_STRENGTH:
                            Weather.Builder weather = replay.world.transientThings.getWeather();
                            if (weather != null) {
                                weather.updateRainStrength(time, packet.retain());
                            }
                            break;
                        case THUNDER_STRENGTH:
                            replay.world.thunderStrengths.put(time, packet.retain());
                            break;
                        default:
                            break;
                    }
                    break;
                }
            }
            if (entityId != null) {
                Entity.Builder entity = replay.world.transientThings.getEntity(entityId);
                if (entity != null) {
                    Location current = entity.getLocation();
                    Location updated = PacketUtils.updateLocation(current, packet);
                    if (updated != null) {
                        entity.updateLocation(time, updated);
                    }
                }
            }
            packet.release();
        }

        if (lastLightUpdate != null) {
            lastLightUpdate.release();
        }

        replay.build(out, time);
    }

    private void invalidateOutOfBoundsChunks(int time, int centerX, int centerZ, int viewDistance) throws IOException {
        // For some reason MC does not transmit the actual value, instead we have to compute it ourselves.
        int distance = Math.max(2, viewDistance) + 3;

        LongSet toBeRemoved = new LongOpenHashSet();

        for (Long2ObjectMap.Entry<Chunk.Builder> entry : replay.world.transientThings.getChunks().long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            int x = longToX(key);
            int z = longToZ(key);
            if (Math.abs(x - centerX) > distance || Math.abs(z - centerZ) > distance) {
                toBeRemoved.add(key);
            }
        }

        for (long key : toBeRemoved) {
            replay.world.transientThings.removeChunk(time, key);
        }
    }
}
