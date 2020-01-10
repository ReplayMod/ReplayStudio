/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.util;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.google.common.base.Optional;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * An EntityPositionTracker knows every entity's position at any timestamp for a single Replay.
 * To do so, it once reads the whole Replay and stores all packets that set or change an entity's position into memory.<br>
 *     <br>
 * While this significantly increases the Replay loading time, it's the only way to:<br>
 *     1) Properly preview the Camera Path when an Entity is spectated<br>
 *     2) Calculate a smooth Path from an Entity's Shoulder Cam perspective<br>
 * Instances of this class should therefore only be initialized when needed.
 * Results are also cached in the Replay file.<br>
 * <br>
 * This class is thread-safe. As such, it will synchronize on the ReplayFile object when using it.
 */
public class EntityPositionTracker {
    private static final String CACHE_ENTRY = "entity_positions.bin";
    private static final String OLD_CACHE_ENTRY = "entity_positions.json";

    private final ReplayFile replayFile;

    private volatile Map<Integer, NavigableMap<Long, Location>> entityPositions;

    public EntityPositionTracker(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    /**
     * Load the entity positions either from cache or from the packet data.
     * @param progressMonitor Called with the current progress [0, 1] or not at all
     * @throws IOException if an i/o error occurs
     */
    public void load(Consumer<Double> progressMonitor) throws IOException {
        Optional<InputStream> cached;
        synchronized (replayFile) {
            Optional<InputStream> oldCache = replayFile.get(OLD_CACHE_ENTRY);
            if (oldCache.isPresent()) {
                oldCache.get().close();
                replayFile.remove(OLD_CACHE_ENTRY);
            }
            cached = replayFile.getCache(CACHE_ENTRY);
        }
        if (cached.isPresent()) {
            try (InputStream in = cached.get()) {
                loadFromCache(in);
            } catch (EOFException e) {
                // Cache contains insufficient data, probably due to a previous crash / full disk
                loadFromPacketData(progressMonitor);
                synchronized (replayFile) {
                    replayFile.removeCache(CACHE_ENTRY);
                }
                saveToCache();
            }
        } else {
            loadFromPacketData(progressMonitor);
            saveToCache();
        }
    }

    private void loadFromCache(InputStream rawIn) throws IOException {
        NetInput in = new StreamNetInput(rawIn);
        entityPositions = new TreeMap<>();
        for (int i = in.readVarInt(); i > 0; i--) {
            int entityId = in.readVarInt();
            TreeMap<Long, Location> locationMap = new TreeMap<>();
            long time = 0;
            for (int j = in.readVarInt(); j > 0; j--) {
                time += in.readVarLong();
                locationMap.put(time, new Location(
                        in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat()
                ));
            }
            entityPositions.put(entityId, locationMap);
        }
    }

    private void saveToCache() throws IOException {
        synchronized (replayFile) {
            Optional<InputStream> cached = replayFile.getCache(CACHE_ENTRY);
            if (cached.isPresent()) {
                // Someone was faster than we were
                cached.get().close();
                return;
            }

            try (OutputStream rawOut = replayFile.writeCache(CACHE_ENTRY)) {
                NetOutput out = new StreamNetOutput(rawOut);
                out.writeVarInt(entityPositions.size());
                for (Map.Entry<Integer, NavigableMap<Long, Location>> entry : entityPositions.entrySet()) {
                    out.writeVarInt(entry.getKey());
                    out.writeVarInt(entry.getValue().size());
                    long time = 0;
                    for (Map.Entry<Long, Location> locEntry : entry.getValue().entrySet()) {
                        out.writeVarLong(locEntry.getKey() - time);
                        time = locEntry.getKey();
                        Location loc = locEntry.getValue();
                        out.writeDouble(loc.getX());
                        out.writeDouble(loc.getY());
                        out.writeDouble(loc.getZ());
                        out.writeFloat(loc.getYaw());
                        out.writeFloat(loc.getPitch());
                    }
                }
            }
        }
    }

    private void loadFromPacketData(Consumer<Double> progressMonitor) throws IOException {
        // Get the packet data input stream
        int replayLength;
        ReplayInputStream origIn;
        synchronized (replayFile) {
            ReplayMetaData metaData = replayFile.getMetaData();
            replayLength = Math.max(1, metaData.getDuration());
            origIn = replayFile.getPacketData(PacketTypeRegistry.get(ProtocolVersion.getProtocol(metaData.getProtocolVersion()), State.LOGIN));
        }

        Map<Integer, NavigableMap<Long, Location>> entityPositions = new HashMap<>();
        try (ReplayInputStream in = origIn) {
            PacketData packetData;
            while ((packetData = in.readPacket()) != null) {
                Packet packet = packetData.getPacket();

                Integer entityID = PacketUtils.getEntityId(packet);
                if (entityID == null) continue;

                NavigableMap<Long, Location> positions = entityPositions.get(entityID);
                if (positions == null) {
                    entityPositions.put(entityID, positions = new TreeMap<>());
                }

                Location oldPosition = positions.isEmpty() ? null : positions.lastEntry().getValue();
                Location newPosition = PacketUtils.updateLocation(oldPosition, packet);

                if (newPosition != null) {
                    positions.put(packetData.getTime(), newPosition);

                    double progress = (double) packetData.getTime() / replayLength;
                    progressMonitor.accept(Math.min(1, Math.max(0, progress)));
                }
            }
        }

        this.entityPositions = entityPositions;
    }

    /**
     * @param entityID The ID of the entity
     * @param timestamp The timestamp
     * @return The position of the specified entity at the given timestamp
     *          or {@code null} if the entity hasn't yet been spawned at that timestamp
     * @throws IllegalStateException if {@link #load(Consumer)} hasn't been called or hasn't finished yet.
     */
    public Location getEntityPositionAtTimestamp(int entityID, long timestamp) {
        if (entityPositions == null) {
            throw new IllegalStateException("Not yet initialized.");
        }

        NavigableMap<Long, Location> positions = entityPositions.get(entityID);
        if (positions == null) {
            return null;
        }
        Map.Entry<Long, Location> lower = positions.floorEntry(timestamp);
        Map.Entry<Long, Location> higher = positions.higherEntry(timestamp);
        if (lower == null || higher == null) {
            return null;
        }
        double r = (higher.getKey() - timestamp) / (higher.getKey() - lower.getKey());
        Location l = lower.getValue();
        Location h = higher.getValue();
        return new Location(
                l.getX() + (h.getX() - l.getX()) * r,
                l.getY() + (h.getY() - l.getY()) * r,
                l.getZ() + (h.getZ() - l.getZ()) * r,
                l.getYaw() + (h.getYaw() - l.getYaw()) * (float) r,
                l.getPitch() + (h.getPitch() - l.getPitch()) * (float) r
        );
    }
}