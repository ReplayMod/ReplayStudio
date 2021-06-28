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

import com.github.steveice10.packetlib.io.NetInput;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData;
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.state.Chunk;
import com.replaymod.replaystudio.rar.state.Entity;
import com.replaymod.replaystudio.rar.state.TransientThing;
import com.replaymod.replaystudio.rar.state.Weather;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class TransientThings implements RandomAccessState {

    private final TreeMap<Integer, Collection<TransientThing>> thingSpawns = new TreeMap<>();
    private final TreeMap<Integer, Collection<TransientThing>> thingDespawns = new TreeMap<>();

    // Keep track of currently active things (optimization)
    private int activeThingsTime = -1;
    private final List<TransientThing> activeThings = new LinkedList<>();

    private final PacketTypeRegistry registry;
    private final int index;
    private ReadableCache cache;

    public TransientThings(PacketTypeRegistry registry, int index) {
        this.registry = registry;
        this.index = index;
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        this.cache = cache;

        NetInput in = cache.seek(index);
        ListMultimap<Integer, TransientThing> thingSpawns = Multimaps.newListMultimap(this.thingSpawns, ArrayList::new);
        ListMultimap<Integer, TransientThing> thingDespawns = Multimaps.newListMultimap(this.thingDespawns, ArrayList::new);
        things: while (true) {
            TransientThing trackedThing;
            switch (in.readVarInt()) {
                case 0: break things;
                case 1: trackedThing = new Entity(registry, in); break;
                case 2: trackedThing = new Chunk(registry, in); break;
                case 3: trackedThing = new Weather(registry, in); break;
                default: throw new IOException("Invalid transient thing id.");
            }
            thingSpawns.put(trackedThing.spawnTime, trackedThing);
            thingDespawns.put(trackedThing.despawnTime, trackedThing);
        }
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        for (TransientThing activeThing : activeThings) {
            activeThing.unload(sink, cache);
        }
        activeThings.clear();
        activeThingsTime = -1;

        thingSpawns.clear();
        thingDespawns.clear();
    }

    private void computeActiveThings(int time) throws IOException {
        if (time == activeThingsTime) {
            return; // our cache is up-to-date, nothing to do
        }

        // Slow path, this should almost never be required for normal operation
        // unless the client state is completely reset.

        for (TransientThing activeThing : activeThings) {
            activeThing.unload(Packet::release, cache);
        }
        activeThings.clear();

        for (Collection<TransientThing> things : thingSpawns.subMap(-1, false, time, true).values()) {
            for (TransientThing thing : things) {
                if (thing.despawnTime > time) {
                    thing.load(Packet::release, cache);
                    activeThings.add(thing);
                }
            }
        }

        activeThingsTime = time;
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        computeActiveThings(currentTimeStamp);

        Iterator<TransientThing> activeIter = activeThings.iterator();
        while (activeIter.hasNext()) {
            TransientThing thing = activeIter.next();
            if (thing.despawnTime <= targetTime) {
                thing.unload(sink, cache);
                activeIter.remove();
            }
        }

        for (Collection<TransientThing> things : thingSpawns.subMap(currentTimeStamp, false, targetTime, true).values()) {
            for (TransientThing thing : things) {
                if (thing.despawnTime > targetTime) {
                    thing.load(sink, cache);
                    activeThings.add(thing);
                }
            }
        }

        activeThingsTime = targetTime;

        for (TransientThing thing : activeThings) {
            thing.play(sink, currentTimeStamp, targetTime);
        }
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        computeActiveThings(currentTimeStamp);

        Iterator<TransientThing> activeIter = activeThings.iterator();
        while (activeIter.hasNext()) {
            TransientThing thing = activeIter.next();
            if (thing.spawnTime > targetTime) {
                thing.unload(sink, cache);
                activeIter.remove();
            }
        }

        for (Collection<TransientThing> things : thingDespawns.subMap(targetTime, false, currentTimeStamp, true).values()) {
            for (TransientThing thing : things) {
                if (thing.spawnTime <= targetTime) {
                    thing.load(sink, cache);
                    activeThings.add(thing);
                }
            }
        }

        activeThingsTime = targetTime;

        for (TransientThing thing : activeThings) {
            thing.rewind(sink, currentTimeStamp, targetTime);
        }
    }

    public static class Builder {
        private final PacketTypeRegistry registry;
        private final WriteableCache cache;
        private final WriteableCache.Deferred indexOut;

        private final DimensionType dimensionType;
        private final Long2ObjectMap<Entity.Builder> entities = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectMap<Chunk.Builder> chunks = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectMap<Weather.Builder> weather = new Long2ObjectOpenHashMap<>();

        public Builder(PacketTypeRegistry registry, WriteableCache cache, DimensionType dimensionType) {
            this.registry = registry;
            this.cache = cache;
            this.indexOut = cache.deferred();
            this.dimensionType = dimensionType;
        }

        public Long2ObjectMap<Chunk.Builder> getChunks() {
            return chunks;
        }

        public Entity.Builder newEntity(int time, int entityId) throws IOException {
            return newTransientThing(entities, time, entityId, new Entity.Builder(registry, entityId));
        }

        public Chunk.Builder newChunk(int time, PacketChunkData.Column column) throws IOException {
            return newTransientThing(chunks, time, column.coordToLong(), new Chunk.Builder(registry, dimensionType, column));
        }

        public Weather.Builder newWeather(int time) throws IOException {
            return newTransientThing(weather, time, 0, new Weather.Builder(registry));
        }

        private <T extends TransientThing.Builder> T newTransientThing(Long2ObjectMap<T> map, int time, long key, T thing) throws IOException {
            thing.setSpawnTime(time);

            TransientThing.Builder prev = map.put(key, thing);
            if (prev != null) {
                commitTransientThing(time, prev);
            }

            return thing;
        }

        public Entity.Builder getEntity(int entityId) {
            return entities.get(entityId);
        }

        public Chunk.Builder getChunk(int x, int z) {
            return chunks.get(PacketChunkData.Column.coordToLong(x, z));
        }

        public Weather.Builder getWeather() {
            return weather.get(0);
        }

        public Entity.Builder removeEntity(int time, int entityId) throws IOException {
            Entity.Builder entity = entities.remove(entityId);
            if (entity != null) commitTransientThing(time, entity);
            return entity;
        }

        public Chunk.Builder removeChunk(int time, int x, int z) throws IOException {
            return removeChunk(time, PacketChunkData.Column.coordToLong(x, z));
        }

        public Chunk.Builder removeChunk(int time, long key) throws IOException {
            Chunk.Builder chunk = chunks.remove(key);
            if (chunk != null) commitTransientThing(time, chunk);
            return chunk;
        }

        public Weather.Builder removeWeather(int time) throws IOException {
            Weather.Builder weather = this.weather.remove(0);
            if (weather != null) commitTransientThing(time, weather);
            return weather;
        }

        private void commitTransientThing(int time, TransientThing.Builder thing) throws IOException {
            int id;
            if (thing instanceof Entity.Builder) {
                id = 1;
            } else if (thing instanceof Chunk.Builder) {
                id = 2;
            } else if (thing instanceof Weather.Builder) {
                id = 3;
            } else {
                throw new IllegalArgumentException("Unsupported type of thing: " + thing.getClass());
            }
            indexOut.writeByte(id);
            thing.setDespawnTime(time);
            thing.build(indexOut, cache);
        }

        private void commitTransientThings(int time, Collection<? extends TransientThing.Builder> things) throws IOException {
            for (TransientThing.Builder thing : things) {
                commitTransientThing(time, thing);
            }
        }

        public void flush(int time) throws IOException {
            commitTransientThings(time, chunks.values());
            commitTransientThings(time, entities.values());
            commitTransientThings(time, weather.values());
            chunks.clear();
            entities.clear();
            weather.clear();
        }

        public int build(int time) throws IOException {
            flush(time);
            indexOut.writeByte(0);

            return indexOut.commit();
        }
    }
}
