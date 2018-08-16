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

import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.IWrappedPacket;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class EntityPositionTracker { //TODO rename ReplayPreprocessor
    private static final String ENTITY_TRACKER_CACHE_ENTRY = "entity_positions.json";
    private static final String TIMESTAMP_CACHE_ENTRY = "client_timestamps.json";

    private static final String[] ENTRIES = {ENTITY_TRACKER_CACHE_ENTRY, TIMESTAMP_CACHE_ENTRY};

    private final ReplayFile replayFile;

    private volatile Map<Integer, NavigableMap<Long, Location>> entityPositions;
    private volatile List<Long> clientTickTimestamps;

    public EntityPositionTracker(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    /**
     * Load preprocessing entries either from cache or from the packet data.
     * @param progressMonitor Called with the current progress [0, 1] or not at all
     * @throws IOException if an i/o error occurs
     */
    public void load(Consumer<Double> progressMonitor) throws IOException {
        // BAH disable entityTracker cache for automated rendering
        Optional<InputStream> cached;
        Boolean allCached = true;
        for (String entry : ENTRIES) {
            synchronized (replayFile) {
                cached = replayFile.get(entry);
            }
            if (cached.isPresent()) {
                try (InputStream in = cached.get()) {
                    loadFromCache(in, entry);
                } catch (JsonSyntaxException e) {
                    // Cache contains invalid json, probably due to a previous crash / full disk
                    synchronized (replayFile) {
                        replayFile.remove(entry);
                    }
                }
            } else {
                allCached = false;
            }
        }

        // Load from packet data any missing entries
        if (!allCached) {
            loadFromPacketData(progressMonitor);
            saveToCache();
        }
        


    }

    private void loadFromCache(InputStream in, String entry) throws IOException {
        if (entry == ENTITY_TRACKER_CACHE_ENTRY){
            entityPositions = new Gson().fromJson(new InputStreamReader(in),
                    new TypeToken<TreeMap<Integer, TreeMap<Long, Location>>>(){}.getType());
        } 
        else if (entry == TIMESTAMP_CACHE_ENTRY) {
            clientTickTimestamps = new Gson().fromJson(new InputStreamReader(in), 
                    new TypeToken<ArrayList<Long>>(){}.getType());
        }
    }

    private void saveToCache() throws IOException {
        synchronized (replayFile) {
            Optional<InputStream> cached = replayFile.get(ENTITY_TRACKER_CACHE_ENTRY);
            if (cached.isPresent()) {
                // Someone was faster than we were
                cached.get().close();
                return;
            }

            try (OutputStream out = replayFile.write(ENTITY_TRACKER_CACHE_ENTRY);
                 OutputStreamWriter writer = new OutputStreamWriter(out, Charsets.UTF_8)) {
                new Gson().toJson(entityPositions, writer);
            }

            cached = replayFile.get(TIMESTAMP_CACHE_ENTRY);
            if (cached.isPresent()) {
                // Someone was faster than we were
                cached.get().close();
                return;
            }

            try (OutputStream out = replayFile.write(TIMESTAMP_CACHE_ENTRY);
                 OutputStreamWriter writer = new OutputStreamWriter(out, Charsets.UTF_8)) {
                new Gson().toJson(clientTickTimestamps, writer);
            }
        }
    }

    private void loadFromPacketData(Consumer<Double> progressMonitor) throws IOException {
        // We use a different studio than the default one as we're only interested in some specific packets.
        ReplayStudio studio = new ReplayStudio();
        PacketUtils.registerAllMovementRelated(studio);
        // Get the packet data input stream
        int replayLength;
        ReplayInputStream origIn;
        synchronized (replayFile) {
            replayLength = Math.max(1, replayFile.getMetaData().getDuration());
            origIn = replayFile.getPacketData(studio);
        }

        Map<Integer, NavigableMap<Long, Location>> entityPositions = new HashMap<>();
        List<Long> clientTickTimestamps = new ArrayList<Long>();
        try (ReplayInputStream in = origIn) {
            PacketData packetData;
            while ((packetData = in.readPacket()) != null) {
                Packet packet = packetData.getPacket();

                // Filter packets that are not of interest
                if (packet instanceof IWrappedPacket) continue;

                // Process Entity ID Packets
                Integer entityID = PacketUtils.getEntityId(packet);
                if (entityID != null) {
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
                };

                // Process client tick timestamps
                if (PacketUtils.isClientTick(packet)) {
                    clientTickTimestamps.add(packetData.getTime());
                }
            }
        }

        this.entityPositions = entityPositions;
        this.clientTickTimestamps = clientTickTimestamps;
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

    public List<Long> getClientTickTimestamps() {
        if (clientTickTimestamps == null) {
            throw new IllegalStateException("Not yet initialized.");
        }
        return clientTickTimestamps;
    }
}