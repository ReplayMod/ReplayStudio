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

package com.replaymod.replaystudio.util;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.Unpooled;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketBlockChange;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData;
import com.replaymod.replaystudio.protocol.packets.PacketDestroyEntities;
import com.replaymod.replaystudio.protocol.packets.PacketEntityHeadLook;
import com.replaymod.replaystudio.protocol.packets.PacketEntityTeleport;
import com.replaymod.replaystudio.protocol.packets.PacketJoinGame;
import com.replaymod.replaystudio.protocol.packets.PacketNotifyClient;
import com.replaymod.replaystudio.protocol.packets.PacketPlayerListEntry;
import com.replaymod.replaystudio.protocol.packets.PacketRespawn;
import com.replaymod.replaystudio.protocol.packets.PacketSpawnPlayer;
import com.replaymod.replaystudio.protocol.packets.PacketUpdateLight;
import com.replaymod.replaystudio.protocol.packets.PacketUpdateViewDistance;
import com.replaymod.replaystudio.protocol.packets.PacketUpdateViewPosition;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Allows random access (i.e. very quick backwards and forwards seeking) to a replay. This is what powers the Quick Mode
 * in the Replay Mod.
 * Supports 1.9+ only.
 * Does not emit the initial JoinGame packet, usually the replay viewer will need to fake a respawn to switch to quick
 * mode anyway.
 *
 * To do so, it performs an initial analysis of the replay, scanning all of its packets and storing entity positions
 * and chunk states while doing so.
 * This allows it to later jump to any time by doing a diff from the current time (including backwards jumping).
 *
 * Exactly replicating the input replay is not realistically doable (and much less so if you consider doing to for
 * all versions supported), as such only entity positions, chunk state, world time and weather will be replicated.
 * This is by design and any further additions should be carefully considered as it'll probably cause significant
 * maintenance work in the future.
 *
 * @param <T> Type of resulting packets, these should be somewhat immutable in the sense that they can be safely
 *           {@link #dispatch(Object) dispatched} multiple times even if only {@link #decode(ByteBuf) decoded} once.
 * @deprecated use {@link com.replaymod.replaystudio.rar.RandomAccessReplay} instead
 */
@Deprecated
public abstract class RandomAccessReplay<T> {
    private static final String CACHE_ENTRY = "quickModeCache.bin";
    private static final String CACHE_INDEX_ENTRY = "quickModeCacheIndex.bin";
    private static final int CACHE_VERSION = 4;
    private static Logger LOGGER = Logger.getLogger(RandomAccessReplay.class.getName());

    private final ReplayFile replayFile;
    private final PacketTypeRegistry registry;

    private int currentTimeStamp;

    private ByteBuf buf;
    private NetInput bufInput;
    private TreeMap<Integer, Collection<BakedTrackedThing>> thingSpawnsT = new TreeMap<>();
    private ListMultimap<Integer, BakedTrackedThing> thingSpawns = Multimaps.newListMultimap(thingSpawnsT, ArrayList::new);
    private TreeMap<Integer, Collection<BakedTrackedThing>> thingDespawnsT = new TreeMap<>();
    private ListMultimap<Integer, BakedTrackedThing> thingDespawns = Multimaps.newListMultimap(thingDespawnsT, ArrayList::new);
    private List<BakedTrackedThing> activeThings = new LinkedList<>();
    private TreeMap<Integer, T> viewDistance = new TreeMap<>(); // 1.14+
    private TreeMap<Integer, T> viewPosition = new TreeMap<>(); // 1.14+
    private TreeMap<Integer, T> worldTimes = new TreeMap<>();
    private TreeMap<Integer, T> thunderStrengths = new TreeMap<>(); // For some reason, this isn't tied to Weather

    public RandomAccessReplay(ReplayFile replayFile, PacketTypeRegistry registry) {
        this.replayFile = replayFile;
        this.registry = registry;
    }

    /**
     * Decode the packet in the given buffer into the native format for immediate or delayed {@link #dispatch(Object)}.
     * The provided ByteBuf is only valid for the duration of the call and must not be released by the callee.
     * It does not contain the packet length and starts with the packet id encoded as varint.
     */
    protected abstract T decode(ByteBuf buf) throws IOException;

    protected abstract void dispatch(T packet);

    public void load(Consumer<Double> progress) throws IOException {
        if (!tryLoadFromCache(progress)) {
            double progressSplit = 0.9; // 90% of progress time for analysing, 10% for loading
            analyseReplay(d -> progress.accept(d * progressSplit));
            tryLoadFromCache(d -> progress.accept(d * (1 - progressSplit) + progressSplit));
        }
    }

    private boolean tryLoadFromCache(Consumer<Double> progress) throws IOException {
        boolean success = false;

        Optional<InputStream> cacheIndexOpt = replayFile.getCache(CACHE_INDEX_ENTRY);
        if (!cacheIndexOpt.isPresent()) return false;
        try (InputStream indexIn = cacheIndexOpt.get()) {
            Optional<InputStream> cacheOpt = replayFile.getCache(CACHE_ENTRY);
            if (!cacheOpt.isPresent()) return false;
            try (InputStream cacheIn = cacheOpt.get()) {
                success = loadFromCache(cacheIn, indexIn, progress);
            }
        } catch (EOFException e) {
            LOGGER.log(Level.WARNING, "Re-analysing replay due to premature EOF while loading the cache:", e);
        } finally {
            if (!success) {
                buf = null;
                bufInput = null;
                thingSpawnsT.clear();
                thingDespawnsT.clear();
                viewPosition.clear();
                viewDistance.clear();
                worldTimes.clear();
                thunderStrengths.clear();
            }
        }

        return success;
    }

    private boolean loadFromCache(InputStream rawCacheIn, InputStream rawIndexIn, Consumer<Double> progress) throws IOException {
        long sysTimeStart = System.currentTimeMillis();

        NetInput cacheIn = new StreamNetInput(rawCacheIn);
        NetInput in = new StreamNetInput(rawIndexIn);
        if (in.readVarInt() != CACHE_VERSION) return false; // Incompatible cache version
        if (cacheIn.readVarInt() != CACHE_VERSION) return false; // Incompatible cache version
        if (in.readVarInt() != registry.getVersion().getVersion()) return false; // Cache of incompatible protocol version
        if (cacheIn.readVarInt() != registry.getVersion().getVersion()) return false; // Cache of incompatible protocol version

        things: while (true) {
            BakedTrackedThing trackedThing;
            switch (in.readVarInt()) {
                case 0: break things;
                case 1: trackedThing = new BakedEntity(in); break;
                case 2: trackedThing = new BakedChunk(in); break;
                case 3: trackedThing = new BakedWeather(in); break;
                default: return false;
            }
            thingSpawns.put(trackedThing.spawnTime, trackedThing);
            thingDespawns.put(trackedThing.despawnTime, trackedThing);
        }

        readFromCache(in, viewPosition);
        readFromCache(in, viewDistance);
        readFromCache(in, worldTimes);
        readFromCache(in, thunderStrengths);
        int size = in.readVarInt();

        LOGGER.info("Creating quick mode buffer of size: " + size / 1024 + "KB");
        buf = Unpooled.buffer(size);
        int read = 0;
        while (true) {
            int len = buf.writeBytes(rawCacheIn, Math.min(size - read, 4096));
            if (len <= 0) break;
            read += len;
            progress.accept((double) read / size);
        }
        bufInput = new ByteBufNetInput(buf);

        LOGGER.info("Loaded quick replay from cache in " + (System.currentTimeMillis() - sysTimeStart) + "ms");
        return true;
    }

    private void analyseReplay(Consumer<Double> progress) throws IOException {
        int currentViewChunkX = 0;
        int currentViewChunkZ = 0;
        int currentViewDistance = 0;
        TreeMap<Integer, Packet> viewPosition = new TreeMap<>();
        TreeMap<Integer, Packet> viewDistance = new TreeMap<>();
        TreeMap<Integer, Packet> worldTimes = new TreeMap<>();
        TreeMap<Integer, Packet> thunderStrengths = new TreeMap<>();
        Map<String, PacketPlayerListEntry> playerListEntries = new HashMap<>();
        Map<Integer, Entity> activeEntities = new HashMap<>();
        Map<Long, Chunk> activeChunks = new HashMap<>();
        String activeDimension = null;
        Packet lastLightUpdate = null;
        Weather activeWeather = null;

        double sysTimeStart = System.currentTimeMillis();
        double duration;
        try (ReplayInputStream in = replayFile.getPacketData(registry);
             OutputStream cacheOut = replayFile.writeCache(CACHE_ENTRY);
             OutputStream cacheIndexOut = replayFile.writeCache(CACHE_INDEX_ENTRY)) {
            NetOutput out = new StreamNetOutput(cacheOut);
            out.writeVarInt(CACHE_VERSION);
            out.writeVarInt(registry.getVersion().getVersion());
            NetOutput indexOut = new StreamNetOutput(cacheIndexOut);
            indexOut.writeVarInt(CACHE_VERSION);
            indexOut.writeVarInt(registry.getVersion().getVersion());

            int index = 0;
            int time = 0;
            duration = replayFile.getMetaData().getDuration();
            PacketData packetData;
            while ((packetData = in.readPacket()) != null) {
                com.replaymod.replaystudio.protocol.Packet packet = packetData.getPacket();
                time = (int) packetData.getTime();
                progress.accept(time / duration);
                Integer entityId = PacketUtils.getEntityId(packet);
                switch (packet.getType()) {
                    case SpawnMob:
                    case SpawnObject:
                    case SpawnPainting: {
                        Entity entity = new Entity(entityId, Collections.singletonList(packet.retain()));
                        entity.spawnTime = time;
                        Entity prev = activeEntities.put(entityId, entity);
                        if (prev != null) {
                            index = prev.writeToCache(indexOut, out, time, index);
                        }
                        break;
                    }
                    case SpawnPlayer: {
                        PacketPlayerListEntry playerListEntry =
                                playerListEntries.get(PacketSpawnPlayer.getPlayerListEntryId(packet));
                        List<Packet> spawnPackets = new ArrayList<>();
                        if (playerListEntry != null) {
                            spawnPackets.addAll(PacketPlayerListEntry.write(
                                    registry,
                                    PacketPlayerListEntry.Action.ADD,
                                    Collections.singletonList(playerListEntry)
                            ));
                        }
                        spawnPackets.add(packet.retain());
                        Entity entity = new Entity(entityId, spawnPackets);
                        entity.spawnTime = time;
                        Entity prev = activeEntities.put(entityId, entity);
                        if (prev != null) {
                            index = prev.writeToCache(indexOut, out, time, index);
                        }
                        break;
                    }
                    case DestroyEntity:
                    case DestroyEntities: {
                        for (int id : PacketDestroyEntities.getEntityIds(packet)) {
                            Entity entity = activeEntities.remove(id);
                            if (entity != null) {
                                index = entity.writeToCache(indexOut, out, time, index);
                            }
                        }
                        break;
                    }
                    case ChunkData: {
                        PacketChunkData chunkData = PacketChunkData.read(packet);
                        PacketChunkData.Column column = chunkData.getColumn();
                        if (column.isFull()) {
                            Packet initialLight = null;
                            if (lastLightUpdate != null) {
                                PacketUpdateLight updateLight = PacketUpdateLight.read(lastLightUpdate);
                                if (column.x == updateLight.getX() && column.z == updateLight.getZ()) {
                                    initialLight = lastLightUpdate;
                                    lastLightUpdate = null;
                                }
                            }
                            Chunk chunk = new Chunk(column, initialLight);
                            chunk.spawnTime = time;
                            Chunk prev = activeChunks.put(coordToLong(column.x, column.z), chunk);
                            if (prev != null) {
                                index = prev.writeToCache(indexOut, out, time, index);
                            }
                        } else {
                            Chunk chunk = activeChunks.get(coordToLong(column.x, column.z));
                            if (chunk != null) {
                                int sectionY = 0;
                                for (PacketChunkData.Chunk section : column.chunks) {
                                    if (section == null) {
                                        sectionY++;
                                        continue;
                                    }
                                    PacketChunkData.BlockStorage toBlocks = section.blocks;
                                    PacketChunkData.BlockStorage fromBlocks = chunk.currentBlockState[sectionY];
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            for (int x = 0; x < 16; x++) {
                                                int fromState = fromBlocks.get(x, y, z);
                                                int toState = toBlocks.get(x, y, z);
                                                if (fromState != toState) {
                                                    IPosition pos = new IPosition(column.x << 4 | x, sectionY << 4 | y, column.z << 4 | z);
                                                    chunk.blocks.put(time, new BlockChange(pos, fromState, toState));
                                                }
                                            }
                                        }
                                    }
                                    chunk.currentBlockState[sectionY] = toBlocks;
                                    sectionY++;
                                }
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
                        Chunk chunk = activeChunks.get(coordToLong(updateLight.getX(), updateLight.getZ()));
                        if (chunk != null && chunk.spawnPackets.size() == 1) {
                            // We we already know about the chunk and this is the first light update we receive for it,
                            // then add the packet to the chunks spawn packets.
                            List<Packet> spawnPackets = new ArrayList<>();
                            spawnPackets.add(packet.retain());
                            spawnPackets.addAll(chunk.spawnPackets);
                            chunk.spawnPackets = spawnPackets;
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
                        Chunk prev = activeChunks.remove(coordToLong(chunkData.getUnloadX(), chunkData.getUnloadZ()));
                        if (prev != null) {
                            index = prev.writeToCache(indexOut, out, time, index);
                        }
                        break;
                    }
                    case BlockChange:
                    case MultiBlockChange: {
                        for (PacketBlockChange record : PacketBlockChange.readSingleOrBulk(packet)) {
                            IPosition pos = record.getPosition();
                            Chunk chunk = activeChunks.get(coordToLong(pos.getX() >> 4, pos.getZ() >> 4));
                            if (chunk != null) {
                                PacketChunkData.BlockStorage blockStorage = chunk.currentBlockState[pos.getY() >> 4];
                                int x = pos.getX() & 15, y = pos.getY() & 15, z = pos.getZ() & 15;
                                int prevState = blockStorage.get(x, y, z);
                                int newState = record.getId();
                                blockStorage.set(x, y, z, newState);
                                chunk.blocks.put(time, new BlockChange(pos, prevState, newState));
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
                        String newDimension = PacketRespawn.read(packet).dimension;
                        if (!newDimension.equals(activeDimension)) {
                            for (Entity entity : activeEntities.values()) {
                                index = entity.writeToCache(indexOut, out, time, index);
                            }
                            activeEntities.clear();
                            for (Chunk chunk : activeChunks.values()) {
                                index = chunk.writeToCache(indexOut, out, time, index);
                            }
                            activeChunks.clear();
                            if (activeWeather != null) {
                                index = activeWeather.writeToCache(indexOut, out, time, index);
                            }
                            activeWeather = null;
                        }
                        activeDimension = newDimension;
                        break;
                    }
                    case JoinGame: {
                        PacketJoinGame joinGame = PacketJoinGame.read(packet);
                        activeDimension = joinGame.dimension;

                        for (Entity entity : activeEntities.values()) {
                            index = entity.writeToCache(indexOut, out, time, index);
                        }
                        activeEntities.clear();
                        for (Chunk chunk : activeChunks.values()) {
                            index = chunk.writeToCache(indexOut, out, time, index);
                        }
                        activeChunks.clear();
                        if (activeWeather != null) {
                            index = activeWeather.writeToCache(indexOut, out, time, index);
                        }
                        activeWeather = null;

                        if (registry.atLeast(ProtocolVersion.v1_14)) {
                            Packet prev;

                            currentViewChunkX = currentViewChunkZ = 0;
                            prev = viewPosition.put(time, PacketUpdateViewPosition.write(registry, 0, 0));
                            if (prev != null) {
                                prev.release();
                            }

                            currentViewDistance = joinGame.viewDistance;
                            prev = viewDistance.put(time, PacketUpdateViewDistance.write(registry, currentViewDistance));
                            if (prev != null) {
                                prev.release();
                            }
                        }
                        break;
                    }
                    case UpdateViewPosition: {
                        currentViewChunkX = PacketUpdateViewPosition.getChunkX(packet);
                        currentViewChunkZ = PacketUpdateViewPosition.getChunkZ(packet);
                        index = invalidateOutOfBoundsChunks(indexOut, index, out, time, activeChunks, currentViewChunkX, currentViewChunkZ, currentViewDistance);

                        Packet prev = viewPosition.put(time, packet.retain());
                        if (prev != null) {
                            prev.release();
                        }
                        break;
                    }
                    case UpdateViewDistance: {
                        currentViewDistance = PacketUpdateViewDistance.getDistance(packet);
                        index = invalidateOutOfBoundsChunks(indexOut, index, out, time, activeChunks, currentViewChunkX, currentViewChunkZ, currentViewDistance);

                        Packet prev = viewDistance.put(time, packet.retain());
                        if (prev != null) {
                            prev.release();
                        }
                        break;
                    }
                    case UpdateTime: {
                        Packet prev = worldTimes.put(time, packet.retain());
                        if (prev != null) {
                            prev.release();
                        }
                        break;
                    }
                    case NotifyClient: {
                        switch (PacketNotifyClient.getAction(packet)) {
                            case START_RAIN:
                                if (activeWeather != null) {
                                    index = activeWeather.writeToCache(indexOut, out, time, index);
                                }
                                activeWeather = new Weather();
                                activeWeather.spawnTime = time;
                                break;
                            case STOP_RAIN:
                                if (activeWeather != null) {
                                    index = activeWeather.writeToCache(indexOut, out, time, index);
                                    activeWeather = null;
                                }
                                break;
                            case RAIN_STRENGTH:
                                if (activeWeather != null) {
                                    Packet prev = activeWeather.rainStrengths.put(time, packet.retain());
                                    if (prev != null) {
                                        prev.release();
                                    }
                                }
                                break;
                            case THUNDER_STRENGTH:
                                Packet prev = thunderStrengths.put(time, packet.retain());
                                if (prev != null) {
                                    prev.release();
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                }
                if (entityId != null) {
                    Entity entity = activeEntities.get(entityId);
                    if (entity != null) {
                        Location current = entity.locations.isEmpty() ? null : entity.locations.lastEntry().getValue();
                        Location updated = PacketUtils.updateLocation(current, packet);
                        if (updated != null) {
                            entity.locations.put(time, updated);
                        }
                    }
                }
                packet.release();
            }

            for (Entity entity : activeEntities.values()) {
                index = entity.writeToCache(indexOut, out, time, index);
            }
            for (Chunk chunk : activeChunks.values()) {
                index = chunk.writeToCache(indexOut, out, time, index);
            }
            if (activeWeather != null) {
                index = activeWeather.writeToCache(indexOut, out, time, index);
            }

            indexOut.writeByte(0);
            writeToCache(indexOut, viewPosition);
            writeToCache(indexOut, viewDistance);
            writeToCache(indexOut, worldTimes);
            writeToCache(indexOut, thunderStrengths);

            viewPosition.values().forEach(Packet::release);
            viewDistance.values().forEach(Packet::release);
            worldTimes.values().forEach(Packet::release);
            thunderStrengths.values().forEach(Packet::release);

            if (lastLightUpdate != null) {
                lastLightUpdate.release();
            }

            indexOut.writeVarInt(index);
        }
        LOGGER.info("Analysed replay in " + (System.currentTimeMillis() - sysTimeStart) + "ms");
    }

    private int invalidateOutOfBoundsChunks(NetOutput indexOut, int index, NetOutput out, int time, Map<Long, Chunk> activeChunks, int centerX, int centerZ, int distance) throws IOException {
        Iterator<Map.Entry<Long, Chunk>> iterator = activeChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Chunk> entry = iterator.next();
            int x = longToX(entry.getKey());
            int z = longToZ(entry.getKey());
            if (Math.abs(x - centerX) <= distance && Math.abs(z - centerZ) <= distance) {
                continue;
            }
            index = entry.getValue().writeToCache(indexOut, out, time, index);
            iterator.remove();
        }
        return index;
    }

    public void reset() {
        activeThings.clear();
        currentTimeStamp = -1;
    }

    public void seek(int replayTime) throws IOException {
        if (replayTime > currentTimeStamp) {
            activeThings.removeIf(thing -> {
                if (thing.despawnTime <= replayTime) {
                    try {
                        thing.despawn();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else {
                    return false;
                }
            });
            playMap(viewPosition, currentTimeStamp, replayTime, this::dispatch);
            playMap(viewDistance, currentTimeStamp, replayTime, this::dispatch);
            for (Collection<BakedTrackedThing> things : thingSpawnsT.subMap(currentTimeStamp, false, replayTime, true).values()) {
                for (BakedTrackedThing thing : things) {
                    if (thing.despawnTime > replayTime) {
                        thing.spawn();
                        activeThings.add(thing);
                    }
                }
            }
            for (BakedTrackedThing thing : activeThings) {
                thing.play(currentTimeStamp, replayTime);
            }
            playMap(worldTimes, currentTimeStamp, replayTime, this::dispatch);
            playMap(thunderStrengths, currentTimeStamp, replayTime, this::dispatch);
        } else {
            activeThings.removeIf(thing -> {
                if (thing.spawnTime > replayTime) {
                    try {
                        thing.despawn();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else {
                    return false;
                }
            });
            rewindMap(viewPosition, currentTimeStamp, replayTime, this::dispatch);
            rewindMap(viewDistance, currentTimeStamp, replayTime, this::dispatch);
            for (Collection<BakedTrackedThing> things : thingDespawnsT.subMap(replayTime, false, currentTimeStamp, true).values()) {
                for (BakedTrackedThing thing : things) {
                    if (thing.spawnTime <= replayTime) {
                        thing.spawn();
                        activeThings.add(thing);
                    }
                }
            }
            for (BakedTrackedThing thing : activeThings) {
                thing.rewind(currentTimeStamp, replayTime);
            }
            rewindMap(worldTimes, currentTimeStamp, replayTime, this::dispatch);
            rewindMap(thunderStrengths, currentTimeStamp, replayTime, this::dispatch);
        }
        currentTimeStamp = replayTime;
    }

    private final ByteBuf byteBuf = Unpooled.buffer();
    private final ByteBufNetOutput byteBufNetOutput = new ByteBufNetOutput(byteBuf);
    private final Inflater inflater = new Inflater();
    private final Deflater deflater = new Deflater();

    private T toMC(Packet packet) {
        // We need to re-encode MCProtocolLib packets, so we can then decode them as NMS packets
        // The main reason we aren't reading them as NMS packets is that we want ReplayStudio to be able
        // to apply ViaVersion (and potentially other magic) to it.
        int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
        int writerIndex = byteBuf.writerIndex();
        try {
            byteBufNetOutput.writeVarInt(packet.getId()); // Re-encode packet, data will end up in byteBuf
            byteBuf.writeBytes(packet.getBuf());

            return decode(byteBuf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
            byteBuf.writerIndex(writerIndex);
        }
    }

    private T readPacketFromCache(NetInput in) throws IOException {
        int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
        int writerIndex = byteBuf.writerIndex();
        try {
            int prefix = in.readVarInt();
            int len = prefix >> 1;
            if ((prefix & 1) == 1) {
                int fullLen = in.readVarInt();
                byteBuf.writeBytes(in.readBytes(len));
                byteBuf.ensureWritable(fullLen);

                inflater.setInput(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(), len);
                inflater.inflate(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.writerIndex(), fullLen);

                byteBuf.readerIndex(byteBuf.readerIndex() + len);
                byteBuf.writerIndex(byteBuf.writerIndex() + fullLen);
            } else {
                byteBuf.writeBytes(in.readBytes(len));
            }

            return decode(byteBuf);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
            byteBuf.writerIndex(writerIndex);
            inflater.reset();
        }
    }

    private List<T> readPacketsFromCache(NetInput in) throws IOException {
        int size = in.readVarInt();
        List<T> packets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            packets.add(readPacketFromCache(in));
        }
        return packets;
    }

    private void readFromCache(NetInput in, SortedMap<Integer, T> packets) throws IOException {
        int time = 0;
        for (int i = in.readVarInt(); i > 0; i--) {
            time += in.readVarInt();
            packets.put(time, readPacketFromCache(in));
        }
    }

    private int writeToCache(NetOutput out, Packet packet) throws IOException {
        int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
        int writerIndex = byteBuf.writerIndex();
        try {
            byteBufNetOutput.writeVarInt(packet.getId()); // Re-encode packet, data will end up in byteBuf
            byteBuf.writeBytes(packet.getBuf());

            int rawIndex = byteBuf.readerIndex();
            int size = byteBuf.readableBytes();

            byteBuf.ensureWritable(size);
            deflater.setInput(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(), size);
            deflater.finish();
            int compressedSize = 0;
            while (!deflater.finished() && compressedSize < size) {
                compressedSize += deflater.deflate(
                        byteBuf.array(),
                        byteBuf.arrayOffset() + byteBuf.writerIndex() + compressedSize,
                        size - compressedSize
                );
            }

            int len = 0;
            if (compressedSize < size) {
                byteBuf.readerIndex(rawIndex + size);
                byteBuf.writerIndex(rawIndex + size + compressedSize);
                len += writeVarInt(out, compressedSize << 1 | 1);
                len += writeVarInt(out, size);
            } else {
                byteBuf.readerIndex(rawIndex);
                byteBuf.writerIndex(rawIndex + size);
                len += writeVarInt(out, size << 1);
            }
            while (byteBuf.isReadable()) {
                out.writeByte(byteBuf.readByte());
                len += 1;
            }
            return len;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
            byteBuf.writerIndex(writerIndex);
            deflater.reset();
        }
    }

    private int writeToCache(NetOutput out, Collection<Packet> packets) throws IOException {
        int len = writeVarInt(out, packets.size());
        for (Packet packet : packets) {
            len += writeToCache(out, packet);
        }
        return len;
    }

    private int writeToCache(NetOutput out, SortedMap<Integer, Packet> packets) throws IOException {
        int len = 0;
        len += writeVarInt(out, packets.size());
        int lastTime = 0;
        for (Map.Entry<Integer, Packet> entry : packets.entrySet()) {
            int time = entry.getKey();
            len += writeVarInt(out, time - lastTime);
            lastTime = time;

            len += writeToCache(out, entry.getValue());
        }
        return len;
    }

    private static int writeVarInt(NetOutput out, int i) throws IOException {
        int len = 1;
        while ((i & -128) != 0) {
            out.writeByte(i & 127 | 128);
            i >>>= 7;
            len++;
        }
        out.writeByte(i);
        return len;
    }

    private static long coordToLong(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }

    private static int longToX(long v) {
        return (int) (v >> 32);
    }

    private static int longToZ(long v) {
        return (int) (v & 0xFFFFFFFFL);
    }

    private static <V> void playMap(NavigableMap<Integer, V> updates, int currentTimeStamp, int replayTime, IOConsumer<V> update) throws IOException {
        Map.Entry<Integer, V> lastUpdate = updates.floorEntry(replayTime);
        if (lastUpdate != null && lastUpdate.getKey() > currentTimeStamp) {
            update.accept(lastUpdate.getValue());
        }
    }

    private static <V> void rewindMap(NavigableMap<Integer, V> updates, int currentTimeStamp, int replayTime, IOConsumer<V> update) throws IOException {
        Map.Entry<Integer, V> lastUpdate = updates.floorEntry(replayTime);
        if (lastUpdate != null && !lastUpdate.getKey().equals(updates.floorKey(currentTimeStamp))) {
            update.accept(lastUpdate.getValue());
        }
    }

    private abstract class TrackedThing {
        List<Packet> spawnPackets;
        List<Packet> despawnPackets;
        int spawnTime;

        private TrackedThing(List<Packet> spawnPackets,
                             List<Packet> despawnPackets) {
            this.spawnPackets = spawnPackets;
            this.despawnPackets = despawnPackets;
        }

        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeVarInt(spawnTime);
            indexOut.writeVarInt(despawnTime);
            indexOut.writeVarInt(index);
            index += RandomAccessReplay.this.writeToCache(cacheOut, spawnPackets);
            indexOut.writeVarInt(index);
            index += RandomAccessReplay.this.writeToCache(cacheOut, despawnPackets);

            spawnPackets.forEach(Packet::release);
            despawnPackets.forEach(Packet::release);

            return index;
        }
    }

    // For memory efficiency we store raw packets (we might even want to consider compression or memory mapped files).
    // During analysis we use TrackedThing which we then serialize (we'd have to do that anyway for caching)
    // and afterwards deserialize in BakedTrackedThing as MC packets on demand for replaying.
    private abstract class BakedTrackedThing {
        int indexSpawnPackets;
        int indexDespawnPackets;
        int spawnTime;
        int despawnTime;

        private BakedTrackedThing(NetInput in) throws IOException {
            spawnTime = in.readVarInt();
            despawnTime = in.readVarInt();
            indexSpawnPackets = in.readVarInt();
            indexDespawnPackets = in.readVarInt();
        }

        void dispatch(T packet) {
            RandomAccessReplay.this.dispatch(packet);
        }

        void spawn() throws IOException {
            buf.readerIndex(indexSpawnPackets);
            readPacketsFromCache(bufInput).forEach(this::dispatch);
        }

        void despawn() throws IOException {
            buf.readerIndex(indexDespawnPackets);
            readPacketsFromCache(bufInput).forEach(this::dispatch);
        }

        abstract void play(int currentTimeStamp, int replayTime) throws IOException;
        abstract void rewind(int currentTimeStamp, int replayTime) throws IOException;
    }

    private class Entity extends TrackedThing {
        private int id;
        private NavigableMap<Integer, Location> locations = new TreeMap<>();

        private Entity(int entityId, List<Packet> spawnPackets) throws IOException {
            super(spawnPackets, Collections.singletonList(PacketDestroyEntities.write(registry, entityId)));
            this.id = entityId;
        }

        @Override
        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeByte(1);
            index = super.writeToCache(indexOut, cacheOut, despawnTime, index);

            indexOut.writeVarInt(id);
            indexOut.writeVarInt(index);
            index += writeVarInt(cacheOut, locations.size());
            int lastTime = 0;
            for (Map.Entry<Integer, Location> entry : locations.entrySet()) {
                int time = entry.getKey();
                Location loc = entry.getValue();
                index += writeVarInt(cacheOut, time - lastTime);
                lastTime = time;
                cacheOut.writeDouble(loc.getX());
                cacheOut.writeDouble(loc.getY());
                cacheOut.writeDouble(loc.getZ());
                cacheOut.writeFloat(loc.getYaw());
                cacheOut.writeFloat(loc.getPitch());
                index += 32;
            }

            return index;
        }
    }

    private class BakedEntity extends BakedTrackedThing {
        private int id;
        private int index;
        private NavigableMap<Integer, Location> locations;

        private BakedEntity(NetInput in) throws IOException {
            super(in);

            id = in.readVarInt();
            index = in.readVarInt();
        }

        @Override
        public void spawn() throws IOException {
            super.spawn();

            buf.readerIndex(index);
            NetInput in = bufInput;

            locations = new TreeMap<>();

            int time = 0;
            for (int i = in.readVarInt(); i > 0; i--) {
                time += in.readVarInt();
                locations.put(time, new Location(in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat()));
            }
        }

        @Override
        public void despawn() throws IOException {
            super.despawn();
            locations = null;
        }

        @Override
        public void play(int currentTimeStamp, int replayTime) throws IOException {
            playMap(locations, currentTimeStamp, replayTime, l -> {
                dispatch(toMC(PacketEntityTeleport.write(registry, id, l, false)));
                dispatch(toMC(PacketEntityHeadLook.write(registry, id, l.getYaw())));
            });
        }

        @Override
        public void rewind(int currentTimeStamp, int replayTime) throws IOException {
            rewindMap(locations, currentTimeStamp, replayTime, l -> {
                dispatch(toMC(PacketEntityTeleport.write(registry, id, l, false)));
                dispatch(toMC(PacketEntityHeadLook.write(registry, id, l.getYaw())));
            });
        }
    }

    private class Chunk extends TrackedThing {
        private TreeMap<Integer, Collection<BlockChange>> blocksT = new TreeMap<>();
        private ListMultimap<Integer, BlockChange> blocks = Multimaps.newListMultimap(blocksT, LinkedList::new); // LinkedList to allow .descendingIterator
        private PacketChunkData.BlockStorage[] currentBlockState = new PacketChunkData.BlockStorage[16];

        private Chunk(PacketChunkData.Column column, Packet initialLight) throws IOException {
            super(initialLight == null
                            ? Collections.singletonList(PacketChunkData.load(column).write(registry))
                            : Arrays.asList(initialLight, PacketChunkData.load(column).write(registry)),
                    Collections.singletonList(PacketChunkData.unload(column.x, column.z).write(registry)));
            PacketChunkData.Chunk[] chunks = column.chunks;
            for (int i = 0; i < currentBlockState.length; i++) {
                currentBlockState[i] = chunks[i] == null ? new PacketChunkData.BlockStorage(registry) : chunks[i].blocks.copy();
            }
        }

        @Override
        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeByte(2);
            index = super.writeToCache(indexOut, cacheOut, despawnTime, index);

            indexOut.writeVarInt(index);
            index += writeVarInt(cacheOut, blocksT.size());
            int lastTime = 0;
            for (Map.Entry<Integer, Collection<BlockChange>> entry : blocksT.entrySet()) {
                int time = entry.getKey();
                index += writeVarInt(cacheOut, time - lastTime);
                lastTime = time;

                Collection<BlockChange> blockChanges = entry.getValue();
                index += writeVarInt(cacheOut, blockChanges.size());
                for (BlockChange blockChange : blockChanges) {
                    Packet.Writer.writePosition(registry, cacheOut, blockChange.pos);
                    index += 8;
                    index += writeVarInt(cacheOut, blockChange.from);
                    index += writeVarInt(cacheOut, blockChange.to);
                }
            }

            return index;
        }
    }

    private class BakedChunk extends BakedTrackedThing {
        private int index;
        private TreeMap<Integer, Collection<BlockChange>> blocksT;

        private BakedChunk(NetInput in) throws IOException {
            super(in);

            index = in.readVarInt();
        }

        @Override
        public void spawn() throws IOException {
            super.spawn();

            buf.readerIndex(index);
            NetInput in = bufInput;

            blocksT = new TreeMap<>();
            ListMultimap<Integer, BlockChange> blocks = Multimaps.newListMultimap(blocksT, LinkedList::new); // LinkedList to allow .descendingIterator

            int time = 0;
            for (int i = in.readVarInt(); i > 0; i--) {
                time += in.readVarInt();

                for (int j = in.readVarInt(); j > 0; j--) {
                    blocks.put(time, new BlockChange(
                            Packet.Reader.readPosition(registry, in),
                            in.readVarInt(),
                            in.readVarInt()
                    ));
                }
            }
        }

        @Override
        public void despawn() throws IOException {
            super.despawn();

            blocksT = null;
        }

        @Override
        public void play(int currentTimeStamp, int replayTime) throws IOException {
            for (Collection<BlockChange> updates : blocksT.subMap(currentTimeStamp, false, replayTime, true).values()) {
                for (BlockChange update : updates) {
                    dispatch(toMC(new PacketBlockChange(update.pos, update.to).write(registry)));
                }
            }
        }

        @Override
        public void rewind(int currentTimeStamp, int replayTime) throws IOException {
            if (currentTimeStamp >= despawnTime) {
                play(spawnTime - 1, replayTime);
                return;
            }
            for (Collection<BlockChange> updates : blocksT.subMap(replayTime, false, currentTimeStamp, true).descendingMap().values()) {
                for (Iterator<BlockChange> it = ((LinkedList<BlockChange>) updates).descendingIterator(); it.hasNext(); ) {
                    BlockChange update = it.next();
                    dispatch(toMC(PacketBlockChange.write(registry, update.pos, update.from)));
                }
            }
        }
    }

    private static class BlockChange {
        private IPosition pos;
        private int from;
        private int to;

        private BlockChange(IPosition pos, int from, int to) {
            this.pos = pos;
            this.from = from;
            this.to = to;
        }
    }

    private class Weather extends TrackedThing {
        private TreeMap<Integer, Packet> rainStrengths = new TreeMap<>();

        private Weather() throws IOException {
            super(Collections.singletonList(PacketNotifyClient.write(registry, PacketNotifyClient.Action.START_RAIN, 0)),
                    Collections.singletonList(PacketNotifyClient.write(registry, PacketNotifyClient.Action.STOP_RAIN, 0)));
        }

        @Override
        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeByte(3);
            index = super.writeToCache(indexOut, cacheOut, despawnTime, index);

            indexOut.writeVarInt(index);
            index += RandomAccessReplay.this.writeToCache(cacheOut, rainStrengths);

            rainStrengths.values().forEach(Packet::release);

            return index;
        }
    }

    private class BakedWeather extends BakedTrackedThing {
        private int index;
        private TreeMap<Integer, T> rainStrengths;

        private BakedWeather(NetInput in) throws IOException {
            super(in);

            index = in.readVarInt();
        }

        @Override
        public void spawn() throws IOException {
            super.spawn();

            buf.readerIndex(index);

            rainStrengths = new TreeMap<>();

            readFromCache(bufInput, rainStrengths);
        }

        @Override
        public void despawn() throws IOException {
            super.despawn();

            rainStrengths = null;
        }

        @Override
        public void play(int currentTimeStamp, int replayTime) throws IOException {
            playMap(rainStrengths, currentTimeStamp, replayTime, this::dispatch);
        }

        @Override
        public void rewind(int currentTimeStamp, int replayTime) throws IOException {
            rewindMap(rainStrengths, currentTimeStamp, replayTime, this::dispatch);
        }
    }

    interface IOConsumer<T> {
        void accept(T it) throws IOException;
    }
}
