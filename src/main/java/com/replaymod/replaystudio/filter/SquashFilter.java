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
package com.replaymod.replaystudio.filter;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.*;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData.Chunk;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData.Column;
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.protocol.registry.Registries;
import com.replaymod.replaystudio.protocol.registry.RegistriesBuilder;
import com.replaymod.replaystudio.stream.IteratorStream;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.util.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.replaymod.replaystudio.util.Utils.within;

public class SquashFilter implements StreamFilter {

    private static final long POS_MIN = Byte.MIN_VALUE;
    private static final long POS_MAX = Byte.MAX_VALUE;

    private static class Team {
        private final String name;
        private Packet create;
        private Packet update;
        private Packet remove;
        private final Set<String> added = new HashSet<>();
        private final Set<String> removed = new HashSet<>();

        private Team(String name) {
            this.name = name;
        }

        public Team copy() {
            Team copy = new Team(this.name);
            copy.create = this.create != null ? this.create.copy() : null;
            copy.update = this.update != null ? this.update.copy() : null;
            copy.remove = this.remove != null ? this.remove.copy() : null;
            copy.added.addAll(this.added);
            copy.removed.addAll(this.removed);
            return copy;
        }

        void release() {
            if (create != null) {
                create.release();
                create = null;
            }
            if (update != null) {
                update.release();
                update = null;
            }
            if (remove != null) {
                remove.release();
                remove = null;
            }
        }
    }

    private static class Entity {
        private boolean complete;
        private boolean despawned;
        private List<PacketData> packets = new ArrayList<>();
        private long lastTimestamp = 0;
        private Packet teleport;
        private long dx = 0;
        private long dy = 0;
        private long dz = 0;
        private Float yaw = null;
        private Float pitch = null;
        private boolean onGround = false; // 1.8+

        Entity copy() {
            Entity copy = new Entity();
            copy.complete = this.complete;
            copy.despawned = this.despawned;
            this.packets.forEach(it -> copy.packets.add(it.copy()));
            copy.lastTimestamp = this.lastTimestamp;
            copy.teleport = this.teleport != null ? this.teleport.copy() : null;
            copy.dx = this.dx;
            copy.dy = this.dy;
            copy.dz = this.dz;
            copy.yaw = this.yaw;
            copy.pitch = this.pitch;
            copy.onGround = this.onGround;
            return copy;
        }

        void release() {
            if (teleport != null) {
                teleport.release();
                teleport = null;
            }
            packets.forEach(PacketData::release);
            packets.clear();
        }
    }

    private PacketTypeRegistry registry;

    /**
     * Forge handshake takes place after login phase (i.e. after LoginSuccess) but before JoinGame.
     * And we'll want to keep those before JoinGame.
     */
    private boolean forgeHandshake;
    private final List<PacketData> emitFirst = new ArrayList<>();
    private final List<PacketData> loginPhase = new ArrayList<>();
    private final List<PacketData> configurationPhase = new ArrayList<>();
    private final List<PacketData> preJoinGame = new ArrayList<>();
    private final List<PacketData> unhandled = new ArrayList<>();
    private final Map<Integer, Entity> entities = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<Integer, PacketData> mainInventoryChanges = new HashMap<>();
    private final Map<Integer, Packet> maps = new HashMap<>();

    // Packets affecting state which is cleared only when switching to configuration phase
    private final List<PacketData> currentPlayPhase = new ArrayList<>();
    // Packets affecting state which is cleared on JoinGame (and above)
    private final List<PacketData> currentJoinGame = new ArrayList<>();
    // Packets affecting state of the world which is cleared by Respawn (provided it's a different world) (and above)
    private final List<PacketData> currentWorld = new ArrayList<>();
    // Packets affecting state of the currently opened window
    private final List<PacketData> currentWindow = new ArrayList<>();
    private final List<PacketData> closeWindows = new ArrayList<>();
    private final Map<PacketType, PacketData> latestOnly = new HashMap<>();

    private final Map<Long, ChunkData> chunks = new HashMap<>();
    private final Map<Long, Long> unloadedChunks = new HashMap<>();

    private Registries registries;
    private final RegistriesBuilder registriesBuilder = new RegistriesBuilder();

    /**
     * The behavior of the Respawn packet depends on the current world. While vanilla seems to never
     * make any use of that fact, custom server and proxies do, so we need to take it into consideration.
     */
    private String dimension;

    /**
     * Starting with 1.18, we need to know the height of our dimension to be able to parse a chunk packet.
     */
    private DimensionType dimensionType;

    /**
     * If the squashed time includes the initial camera position packet, we'll re-write that packet to the actual entity
     * position after all the movement which will be cut out.
     * Otherwise, the camera may confusingly end up in the void.
     */
    private PacketPlayerPositionRotation cameraPositionRotation;

    /**
     * The entity id of the player which recorded the replay.
     * Used for {@link #cameraPositionRotation}.
     */
    private Integer recordingPlayerEntityId;

    /**
     * We rely on the timestamps to keep relative packet order. However, by default, two packets may have the same
     * timestamp, in which case their relative order may be affected by the place in which we store the packet, and that
     * change may break things (e.g. set block needs to be sent before the corresponding block entity data packet, but
     * we process them the other way round).
     * We therefore need to keep track and manually increment the timestamp if it didn't increment by itself. This will
     * mess with the actual time value but that is not much of an issue because everything will get squashed into a
     * single moment at the end anyway.
     */
    private long prevTimestamp;

    public SquashFilter(Registries registries, String dimension, DimensionType dimensionType) {
        this.registries = registries != null ? registries : new Registries();
        this.dimension = dimension;
        this.dimensionType = dimensionType;
    }

    public SquashFilter(DimensionTracker dimensionTracker) {
        this(dimensionTracker.registries.copy(), dimensionTracker.dimension, dimensionTracker.dimensionType);
    }

    public SquashFilter copy() {
        SquashFilter copy = new SquashFilter(this.registries.copy(), this.dimension, this.dimensionType);
        copy.registry = this.registry;
        copy.forgeHandshake = this.forgeHandshake;
        this.teams.forEach((key, value) -> copy.teams.put(key, value.copy()));
        this.entities.forEach((key, value) -> copy.entities.put(key, value.copy()));
        this.emitFirst.forEach(it -> copy.emitFirst.add(it.copy()));
        this.loginPhase.forEach(it -> copy.loginPhase.add(it.copy()));
        this.configurationPhase.forEach(it -> copy.configurationPhase.add(it.copy()));
        this.preJoinGame.forEach(it -> copy.preJoinGame.add(it.copy()));
        this.unhandled.forEach(it -> copy.unhandled.add(it.copy()));
        this.mainInventoryChanges.forEach((key, value) -> copy.mainInventoryChanges.put(key, value.copy()));
        this.maps.forEach((key, value) -> copy.maps.put(key, value.copy()));
        this.currentPlayPhase.forEach(it -> copy.currentPlayPhase.add(it.copy()));
        this.currentJoinGame.forEach(it -> copy.currentJoinGame.add(it.copy()));
        this.currentWorld.forEach(it -> copy.currentWorld.add(it.copy()));
        this.currentWindow.forEach(it -> copy.currentWindow.add(it.copy()));
        this.closeWindows.forEach(it -> copy.closeWindows.add(it.copy()));
        this.latestOnly.forEach((key, value) -> copy.latestOnly.put(key, value.copy()));
        this.chunks.forEach((key, value) -> copy.chunks.put(key, value.copy()));
        copy.unloadedChunks.putAll(this.unloadedChunks);
        copy.registriesBuilder.copyFrom(this.registriesBuilder);
        copy.cameraPositionRotation = this.cameraPositionRotation;
        copy.recordingPlayerEntityId = this.recordingPlayerEntityId;
        copy.prevTimestamp = this.prevTimestamp;
        return copy;
    }

    /**
     * Flushes all state via {@link #onEnd(PacketStream, long)} and returns the filter to a mostly empty
     * state such that one can continue to use it for subsequent packets.
     */
    private void flush() throws IOException {
        // Emit all packets
        List<PacketData> flushedPackets = new ArrayList<>();
        onEnd(new IteratorStream(flushedPackets.listIterator(), (PacketStream.FilterInfo) null), 0);

        // Store the flushed packets in the emit-first list for later
        emitFirst.addAll(flushedPackets);
    }

    public void release() {
        teams.values().forEach(Team::release);
        entities.values().forEach(Entity::release);
        emitFirst.forEach(PacketData::release);
        loginPhase.forEach(PacketData::release);
        configurationPhase.forEach(PacketData::release);
        preJoinGame.forEach(PacketData::release);
        unhandled.forEach(PacketData::release);
        mainInventoryChanges.values().forEach(PacketData::release);
        maps.values().forEach(Packet::release);
        currentPlayPhase.forEach(PacketData::release);
        currentJoinGame.forEach(PacketData::release);
        currentWorld.forEach(PacketData::release);
        currentWindow.forEach(PacketData::release);
        closeWindows.forEach(PacketData::release);
        latestOnly.values().forEach(PacketData::release);
    }

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData originalData) throws IOException {
        // Ensure timestamps are strictly increasing; just monotonically increasing is not enough; see prevTimestamp
        PacketData data = new PacketData(Math.max(originalData.getTime(), prevTimestamp + 1), originalData.getPacket());
        prevTimestamp = data.getTime();

        Packet packet = data.getPacket();
        PacketType type = packet.getType();
        registry = packet.getRegistry();
        long lastTimestamp = data.getTime();

        // Entities
        Integer entityId = PacketUtils.getEntityId(packet);
        if (entityId != null) { // Some entity is associated with this packet
            if (entityId == -1) { // Multiple entities in fact
                for (int id : PacketUtils.getEntityIds(packet)) {
                    Entity entity;
                    if (type == PacketType.DestroyEntity || type == PacketType.DestroyEntities) {
                        entity = entities.computeIfAbsent(id, i -> new Entity());
                        entity.release();
                        entity.despawned = true;
                        if (entity.complete) {
                            entities.remove(id);
                        }
                    } else {
                        entity = entities.compute(id, (i, e) -> e == null || e.despawned ? new Entity() : e);
                        entity.packets.add(data.retain());
                    }
                    entity.lastTimestamp = lastTimestamp;
                }
            } else { // Only one entity
                Entity entity = entities.compute(entityId, (i, e) -> e == null || e.despawned ? new Entity() : e);
                if (type == PacketType.EntityMovement
                        || type == PacketType.EntityPosition
                        || type == PacketType.EntityRotation
                        || type == PacketType.EntityPositionRotation) {
                    Triple<DPosition, Pair<Float, Float>, Boolean> movement = PacketEntityMovement.getMovement(packet);
                    DPosition deltaPos = movement.getLeft();
                    Pair<Float, Float> yawPitch = movement.getMiddle();
                    if (deltaPos != null) {
                        entity.dx += deltaPos.getX() * 32;
                        entity.dy += deltaPos.getY() * 32;
                        entity.dz += deltaPos.getZ() * 32;
                    }
                    if (yawPitch != null) {
                        entity.yaw = yawPitch.getKey();
                        entity.pitch = yawPitch.getValue();
                    }
                    entity.onGround = movement.getRight();
                } else if (type == PacketType.EntityTeleport) {
                    if (entity.teleport != null) {
                        entity.teleport.release();
                    }
                    entity.dx = entity.dy = entity.dz = 0;
                    entity.yaw = entity.pitch = null;
                    entity.teleport = packet.retain();
                } else {
                    if (PacketUtils.isSpawnEntityPacket(packet)) {
                        entity.complete = true;
                    }
                    entity.packets.add(data.retain());
                }
                entity.lastTimestamp = lastTimestamp;
            }
            return false;
        }

        switch (type) {
            //
            // World
            //

            // Appears to only be used to reset blocks which have speculatively been changed in the client world
            // and as such should never do anything useful in a a replay.
            case PlayerActionAck:
            case SpawnParticle:
                break;
            case Respawn: {
                PacketRespawn packetRespawn = PacketRespawn.read(packet, registries);
                String newDimension = packetRespawn.dimension;
                if (dimension == null) {
                    // We do not know which dimension we are current in, so we cannot know how to handle this packet.
                    // Instead we flush all state accumulated so far, and then start fresh with the newly
                    // gained knowledge (so this will only happen once).
                    flush();
                } else if (!dimension.equals(newDimension)) {
                    clearStateAtRespawn();
                }
                dimension = newDimension;
                dimensionType = packetRespawn.dimensionType;

                PacketData prev = this.latestOnly.put(type, data.retain());
                if (prev != null) {
                    prev.release();
                }
                break;
            }
            case JoinGame:
                clearStateAtJoinGame();
                PacketJoinGame packetJoinGame = PacketJoinGame.read(packet, registries);
                recordingPlayerEntityId = ~packetJoinGame.entityId; // inverted, see ReplayInputStream
                registries = packetJoinGame.registries;
                dimension = packetJoinGame.dimension;
                dimensionType = packetJoinGame.dimensionType;
                forgeHandshake = false;
            case SetExperience:
            case PlayerAbilities:
            case Difficulty:
            case UpdateViewPosition:
            case UpdateViewDistance: {
                PacketData prev = this.latestOnly.put(type, data.retain());
                if (prev != null) {
                    prev.release();
                }
                break;
            }
            case UpdateLight:
                PacketUpdateLight updateLight = PacketUpdateLight.read(packet);
                chunks.computeIfAbsent(
                        ColumnPos.coordToLong(updateLight.getX(), updateLight.getZ()),
                        idx -> new ChunkData(data.getTime(), updateLight.getX(), updateLight.getZ())
                ).updateLight(updateLight.getData());
                break;
            case ChunkData:
            case UnloadChunk:
                PacketChunkData chunkData = PacketChunkData.read(packet, dimensionType.getSections());
                if (chunkData.isUnload()) {
                    unloadChunk(data.getTime(), chunkData.getUnloadX(), chunkData.getUnloadZ());
                } else {
                    updateChunk(data.getTime(), chunkData.getColumn());
                }
                break;
            case BulkChunkData:
                for (Column column : PacketChunkData.readBulk(packet)) {
                    updateChunk(data.getTime(), column);
                }
                break;
            case BlockChange:
                updateBlock(data.getTime(), PacketBlockChange.read(packet));
                break;
            case MultiBlockChange:
                for (PacketBlockChange change : PacketBlockChange.readBulk(packet)) {
                    updateBlock(data.getTime(), change);
                }
                break;
            case PlayerPositionRotation:
                cameraPositionRotation = PacketPlayerPositionRotation.read(packet);
                break;
            case BlockBreakAnim:
            case BlockValue:
            case Explosion:
            case OpenTileEntityEditor:
            case PlayEffect:
            case PlaySound:
            case SpawnPosition:
            case UpdateSign:
            case UpdateTileEntity:
            case UpdateTime:
            case WorldBorder:
            case NotifyClient:
            case MapData:
                currentWorld.add(data.retain());
                break;

            case ScoreboardObjective:
            case UpdateScore:
            case DisplayScoreboard:
            case ResetScore:
                if (registry.atLeast(ProtocolVersion.v1_20_5)) {
                    // As of 1.20.5 the scoreboard is part of the ClientPlayNetHandler which only gets reset when switching
                    // away from the play phase.
                    currentPlayPhase.add(data.retain());
                } else {
                    // On older versions it used to be part of the world and was manually copied over in Respawn but
                    // not in JoinGame.
                    currentJoinGame.add(data.retain());
                }
                break;

            //
            // Windows
            //

            case CloseWindow:
                currentWindow.forEach(PacketData::release);
                currentWindow.clear();
                closeWindows.add(data.retain());
                break;
            case ConfirmTransaction:
                break; // This packet isn't of any use in replays
            case OpenWindow:
            case TradeList:
            case WindowProperty:
                currentWindow.add(data.retain());
                break;
            case WindowItems:
                if (PacketWindowItems.getWindowId(packet) == 0) {
                    PacketData prev = latestOnly.put(type, data.retain());
                    if (prev != null) {
                        prev.release();
                    }
                } else {
                    currentWindow.add(data.retain());
                }
                break;
            case SetSlot:
                if (PacketSetSlot.getWindowId(packet) == 0) {
                    PacketData prev = mainInventoryChanges.put(PacketSetSlot.getSlot(packet), data.retain());
                    if (prev != null) {
                        prev.release();
                    }
                } else {
                    currentWindow.add(data.retain());
                }
                break;

            //
            // Teams
            //

            case Team:
                Team team = teams.computeIfAbsent(PacketTeam.getName(packet), Team::new);
                switch (PacketTeam.getAction(packet)) {
                    case CREATE:
                        if (team.create != null) {
                            team.create.release();
                        }
                        team.create = packet.retain();
                    case ADD_PLAYER:
                        for (String player : PacketTeam.getPlayers(packet)) {
                            if (!team.removed.remove(player)) {
                                team.added.add(player);
                            }
                            // Adding a player to one team implicitly removes them from all other teams
                            for (Team otherTeam : teams.values()) {
                                if (otherTeam != team) {
                                    otherTeam.added.remove(player);
                                    otherTeam.removed.remove(player);
                                }
                            }
                        }
                        break;
                    case UPDATE:
                        if (team.update != null) {
                            team.update.release();
                        }
                        team.update = packet.retain();
                        break;
                    case REMOVE:
                        if (team.remove != null) {
                            team.remove.release();
                        }
                        team.remove = packet.retain();
                        if (team.create != null) {
                            team.release();
                            teams.remove(team.name);
                        }
                        break;
                    case REMOVE_PLAYER:
                        // Note: We cannot assume that `added` and `removed` cancels out because an add-action is an
                        // implicit remove-from-all-others action, and we cannot know for sure if the latter implicit
                        // one is being relied on here, so we must retain the add-action (and by extension the
                        // remove-action).
                        team.removed.addAll(PacketTeam.getPlayers(packet));
                        break;
                }
                break;

            //
            // Misc
            //

            case ConfigCustomPayload:
                switch (PacketCustomPayload.getId(packet)) {
                    case PacketEnabledPacksData.ID:
                        registriesBuilder.readEnabledPacksDataPacket(packet);
                        break;
                }
                configurationPhase.add(data.retain());
                break;
            case ConfigRegistries:
                registriesBuilder.readRegistriesPacket(packet);

                if (registry.atLeast(ProtocolVersion.v1_20_5)) {
                    // 1.20.5 resets all registries from before the current config phase iff any are sent
                    boolean previousPhase = false;
                    for (int i = configurationPhase.size() - 1; i >= 0; i--) {
                        PacketType oldType = configurationPhase.get(i).getPacket().getType();
                        if (oldType == PacketType.ConfigFinish) {
                            previousPhase = true;
                        } else if (oldType == PacketType.ConfigRegistries && previousPhase) {
                            configurationPhase.remove(i).release();
                        }
                    }
                } else {
                    // Pre-1.20.5 sends all registry entries in a single packet
                    dropConfigPhasePacketsOfType(type);
                }
                configurationPhase.add(data.retain());
                break;
            case ConfigSelectKnownPacks:
                registriesBuilder.readKnownPacksPacket(packet);
                dropConfigPhasePacketsOfType(type);
                configurationPhase.add(data.retain());
                break;
            case ConfigFinish:
                registries = registriesBuilder.finish(registries);
                dropConfigPhasePacketsOfType(type);
                configurationPhase.add(data.retain());
                break;

            case ConfigTags:
            case ConfigFeatures:
                dropConfigPhasePacketsOfType(type);
                configurationPhase.add(data.retain());
                break;

            case Reconfigure:
                clearStateAtPhaseSwitch();
                break;

            default:
                if (type.getState() == State.CONFIGURATION) {
                    configurationPhase.add(data.retain());
                } else if (type.getState() == State.LOGIN) {
                    loginPhase.add(data.retain());
                    forgeHandshake = true;
                } else if (forgeHandshake) {
                    preJoinGame.add(data.retain());
                } else {
                    unhandled.add(data.retain());
                }
        }
        return false;
    }

    private void dropConfigPhasePacketsOfType(PacketType type) {
        configurationPhase.removeIf(old -> {
            if (old.getPacket().getType() == type) {
                old.release();
                return true;
            } else {
                return false;
            }
        });
    }

    private void clearStateAtPhaseSwitch() {
        currentPlayPhase.forEach(PacketData::release);
        currentPlayPhase.clear();

        // As of 1.20.5, the scoreboard and teams are part of the ClientPlayNetHandler which only gets reset
        // when switching away from the play phase.
        if (registry.atLeast(ProtocolVersion.v1_20_5)) {
            teams.values().forEach(Team::release);
            teams.clear();
        }

        clearStateAtJoinGame();
    }

    private void clearStateAtJoinGame() {
        currentJoinGame.forEach(PacketData::release);
        currentJoinGame.clear();

        // Prior to 1.20.5, the scoreboard and teams were part of the world and were manually copied over in Respawn but
        // not in JoinGame.
        if (registry.olderThan(ProtocolVersion.v1_20_5)) {
            teams.values().forEach(Team::release);
            teams.clear();
        }

        clearStateAtRespawn();
    }

    private void clearStateAtRespawn() {
        currentWorld.forEach(PacketData::release);
        currentWorld.clear();
        chunks.clear();
        unloadedChunks.clear();
        currentWindow.forEach(PacketData::release);
        currentWindow.clear();
        entities.values().forEach(Entity::release);
        entities.clear();
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) throws IOException {
        boolean inBundle = false;

        // Always emit "emitFirst" packets first
        for (PacketData data : emitFirst) {
            if (data.getPacket().getType() == PacketType.Bundle) {
                inBundle = !inBundle;
            }
            stream.insert(timestamp, data.getPacket());
        }
        emitFirst.clear();

        // If we have any login-phase packets, those need to be sent before regular play-phase ones
        boolean hadLoginPhase = !loginPhase.isEmpty();
        for (PacketData data : loginPhase) {
            if (data.getPacket().getType() == PacketType.Bundle) {
                inBundle = !inBundle;
            }
            stream.insert(timestamp, data.getPacket());
        }
        loginPhase.clear();

        // If we have a configuration phase, that need to be sent before the regular play phase
        if (!configurationPhase.isEmpty()) {
            // If this is not preceded by a login phase, then it is preceded by a play phase and we need to re-enter
            // the configuration phase before we can send configuration-phase packets
            if (!hadLoginPhase) {
                stream.insert(timestamp, new Packet(registry, PacketType.Reconfigure));
            }

            for (PacketData data : configurationPhase) {
                stream.insert(timestamp, data.getPacket());
            }
            configurationPhase.clear();
        }

        // Pre-joinGame packets must be sent before JoinGame
        for (PacketData data : preJoinGame) {
            if (data.getPacket().getType() == PacketType.Bundle) {
                inBundle = !inBundle;
            }
            stream.insert(timestamp, data.getPacket());
        }
        preJoinGame.clear();

        // Join/respawn packet must be the first packet
        PacketData join = latestOnly.remove(PacketType.JoinGame);
        PacketData respawn = latestOnly.remove(PacketType.Respawn);
        if (join != null) {
            stream.insert(timestamp, join.getPacket());
        }
        if (respawn != null) {
            stream.insert(timestamp, respawn.getPacket());
        }

        // These must always come before any chunk packets because otherwise those may get rejected.
        // Position must come before distance because that one actually unloads chunks.
        PacketData updateViewPosition = latestOnly.remove(PacketType.UpdateViewPosition);
        PacketData updateViewDistance = latestOnly.remove(PacketType.UpdateViewDistance);
        if (updateViewPosition != null) {
            stream.insert(timestamp, updateViewPosition.getPacket());
        }
        if (updateViewDistance != null) {
            stream.insert(timestamp, updateViewDistance.getPacket());
        }

        List<PacketData> result = new ArrayList<>();

        result.addAll(unhandled);
        result.addAll(currentPlayPhase);
        result.addAll(currentJoinGame);
        result.addAll(currentWorld);
        result.addAll(currentWindow);
        result.addAll(closeWindows);
        result.addAll(mainInventoryChanges.values());
        result.addAll(latestOnly.values());
        unhandled.clear();
        currentWorld.clear();
        currentWindow.clear();
        closeWindows.clear();
        mainInventoryChanges.clear();
        latestOnly.clear();

        // Update initial camera position before we flush entity data
        if (cameraPositionRotation != null) {
            Entity entity = entities.get(recordingPlayerEntityId);
            if (entity != null) {
                if (entity.teleport != null) {
                    Location location = PacketEntityTeleport.getLocation(entity.teleport);
                    cameraPositionRotation.x = location.getX();
                    cameraPositionRotation.y = location.getY();
                    cameraPositionRotation.z = location.getZ();
                    cameraPositionRotation.yaw = location.getYaw();
                    cameraPositionRotation.pitch = location.getPitch();
                }
                cameraPositionRotation.x += entity.dx / 32.0;
                cameraPositionRotation.y += entity.dy / 32.0;
                cameraPositionRotation.z += entity.dz / 32.0;
                if (entity.yaw != null && entity.pitch != null) {
                    cameraPositionRotation.yaw = entity.yaw;
                    cameraPositionRotation.pitch = entity.pitch;
                }
            }
        }

        for (Map.Entry<Integer, Entity> e : entities.entrySet()) {
            Entity entity = e.getValue();

            if (entity.despawned) {
                result.add(new PacketData(entity.lastTimestamp, PacketDestroyEntities.write(registry, e.getKey())));
                entity.release();
                continue;
            }

            FOR_PACKETS:
            for (PacketData data : entity.packets) {
                Packet packet = data.getPacket();
                for (int i : PacketUtils.getEntityIds(packet)) {
                    Entity other = entities.get(i);
                    if (other == null || other.despawned) { // Other entity doesn't exist
                        packet.release();
                        continue FOR_PACKETS;
                    }
                }
                result.add(data);
            }

            if (entity.teleport != null) {
                result.add(new PacketData(entity.lastTimestamp, entity.teleport));
            }
            while (entity.dx != 0 || entity.dy != 0 || entity.dz != 0) {
                long mx = within(entity.dx, POS_MIN, POS_MAX);
                long my = within(entity.dy, POS_MIN, POS_MAX);
                long mz = within(entity.dz, POS_MIN, POS_MAX);
                entity.dx -= mx;
                entity.dy -= my;
                entity.dz -= mz;
                DPosition deltaPos = new DPosition(mx / 32.0, my / 32.0, mz / 32.0);
                result.add(new PacketData(entity.lastTimestamp, PacketEntityMovement.write(
                        registry, e.getKey(), deltaPos, null, entity.onGround)));
            }
            if (entity.yaw != null && entity.pitch != null) {
                result.add(new PacketData(entity.lastTimestamp, PacketEntityMovement.write(
                        registry, e.getKey(), null, Pair.of(entity.yaw, entity.pitch), entity.onGround)));
            }
        }
        entities.clear();

        for (Map.Entry<Long, Long> e : unloadedChunks.entrySet()) {
            int x = ColumnPos.longToX(e.getKey());
            int z = ColumnPos.longToZ(e.getKey());
            result.add(new PacketData(e.getValue(), PacketChunkData.unload(x, z).write(registry)));
        }

        for (ChunkData chunk : chunks.values()) {
            PacketUpdateLight.Data lightData = new PacketUpdateLight.Data(
                    Arrays.asList(chunk.skyLight),
                    Arrays.asList(chunk.blockLight)
            );
            Column column = new Column(
                    chunk.x, chunk.z, chunk.changes,
                    chunk.biomeData, chunk.tileEntities, chunk.heightmaps, chunk.biomes, chunk.useExistingLightData,
                    lightData
            );
            if (column.isFull() || !Utils.containsOnlyNull(chunk.changes)) {
                result.add(new PacketData(chunk.firstAppearance, PacketChunkData.load(column).write(registry)));
            }
            for (Map<Short, MutablePair<Long, PacketBlockChange>> e : chunk.blockChanges) {
                if (e != null) {
                    for (MutablePair<Long, PacketBlockChange> pair : e.values()) {
                        result.add(new PacketData(pair.getLeft(), pair.getRight().write(registry)));
                    }
                }
            }
            for (MutablePair<Long, PacketBlockChange> pair : chunk.allBlockChanges.values()) {
                result.add(new PacketData(pair.getLeft(), pair.getRight().write(registry)));
            }
            if (chunk.hasLight() && registry.olderThan(ProtocolVersion.v1_18)) {
                result.add(new PacketData(chunk.firstAppearance,
                        new PacketUpdateLight(chunk.x, chunk.z, lightData).write(registry)));
            }
        }
        chunks.clear();

        result.sort(Comparator.comparingLong(PacketData::getTime));

        PacketData pendingBundle = null;
        for (PacketData data : result) {
            if (data.getPacket().getType() == PacketType.Bundle) {
                if (inBundle) {
                    inBundle = false;
                } else {
                    // If the bundle was just opened and is already being closed without any packets in it, drop it
                    if (pendingBundle != null) {
                        pendingBundle.release();
                        pendingBundle = null;
                        data.release();
                        continue;
                    }
                    pendingBundle = data;
                    continue;
                }
            } else if (pendingBundle != null) {
                add(stream, timestamp, pendingBundle.getPacket());
                pendingBundle = null;
                inBundle = true;
            }

            add(stream, timestamp, data.getPacket());
        }
        if (pendingBundle != null) {
            add(stream, timestamp, pendingBundle.getPacket());
        }

        for (Team team : teams.values()) {
            if (team.create != null) {
                add(stream, timestamp, PacketTeam.createTeam(team.create, team.added));
                team.added.clear();
            }
            if (team.update != null) {
                add(stream, timestamp, team.update);
            }
            if (team.remove != null) {
                add(stream, timestamp, team.remove);
            } else {
                if (!team.added.isEmpty()) {
                    add(stream, timestamp, PacketTeam.addPlayers(registry, team.name, team.added));
                }
                if (!team.removed.isEmpty()) {
                    add(stream, timestamp, PacketTeam.removePlayers(registry, team.name, team.removed));
                }
            }
        }
        teams.clear();

        for (Packet packet : maps.values()) {
            add(stream, timestamp, packet);
        }
        maps.clear();

        if (cameraPositionRotation != null) {
            add(stream, timestamp, cameraPositionRotation.write(registry));
        }
    }

    @Override
    public String getName() {
        return "squash";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
    }

    private void add(PacketStream stream, long timestamp, Packet packet) {
        stream.insert(new PacketData(timestamp, packet));
    }

    private void updateBlock(long time, PacketBlockChange record) {
        IPosition pos = record.getPosition();
        chunks.computeIfAbsent(
                ColumnPos.coordToLong(pos.getX() >> 4, pos.getZ() >> 4),
                idx -> new ChunkData(time, pos.getX() >> 4, pos.getZ() >> 4)
        ).updateBlock(time, record);
    }

    private void unloadChunk(long time, int x, int z) {
        long coord = ColumnPos.coordToLong(x, z);
        chunks.remove(coord);
        unloadedChunks.put(coord, time);
    }

    private void updateChunk(long time, Column column) {
        long coord = ColumnPos.coordToLong(column.x, column.z);
        unloadedChunks.remove(coord);
        ChunkData chunk = chunks.get(coord);
        if (chunk == null) {
            chunks.put(coord, chunk = new ChunkData(time, column.x, column.z));
        }
        chunk.update(
                column.chunks,
                column.biomeData,
                column.tileEntities,
                column.heightmaps,
                column.biomes,
                column.useExistingLightData
        );
        if (column.lightData != null) { // 1.18+
            chunk.updateLight(column.lightData);
        }
    }

    private class ChunkData {
        private final long firstAppearance;
        private final int x;
        private final int z;
        private Chunk[] changes = new Chunk[0];
        private byte[] biomeData; // pre 1.15
        // We store block changes per chunk so we can easily clear them when we see a partial chunk update.
        // This no longer applies to 1.17 cause MC no longer supports partial chunk updates, so instead we just
        // clear everything. This has the added bonus that we do not need to be aware of the world y to index mapping.
        @SuppressWarnings("unchecked")
        private Map<Short, MutablePair<Long, PacketBlockChange>>[] blockChanges = new Map[16]; // pre 1.17
        private final Map<Integer, MutablePair<Long, PacketBlockChange>> allBlockChanges = new HashMap<>(); // 1.17+
        // 1.9+
        private PacketChunkData.TileEntity[] tileEntities;
        // 1.14+
        private PacketChunkData.Heightmaps heightmaps;
        private byte[][] skyLight = new byte[0][];
        private byte[][] blockLight = new byte[0][];
        // 1.15+
        private int[] biomes;
        // 1.16+
        private boolean useExistingLightData = true;

         ChunkData(long firstAppearance, int x, int z) {
            this.firstAppearance = firstAppearance;
            this.x = x;
            this.z = z;
        }

        ChunkData copy() {
            ChunkData copy = new ChunkData(this.firstAppearance, this.x, this.z);
            copy.changes = new Chunk[this.changes.length];
            for (int i = 0; i < this.changes.length; i++) {
                copy.changes[i] = this.changes[i] != null ? this.changes[i].copy() : null;
            }
            copy.biomeData = this.biomeData;
            for (int i = 0; i < this.blockChanges.length; i++) {
                if (this.blockChanges[i] != null) {
                    Map<Short, MutablePair<Long, PacketBlockChange>> copyMap = new HashMap<>();
                    copy.blockChanges[i] = copyMap;
                    this.blockChanges[i].forEach((key, value) -> copyMap.put(key, new MutablePair<>(value.left, value.right)));
                }
            }
            this.allBlockChanges.forEach((key, value) -> copy.allBlockChanges.put(key, new MutablePair<>(value.left, value.right)));
            copy.tileEntities = this.tileEntities;
            copy.heightmaps = this.heightmaps;
            copy.skyLight = this.skyLight.clone();
            copy.blockLight = this.blockLight.clone();
            copy.biomes = this.biomes;
            copy.useExistingLightData = this.useExistingLightData;
            return copy;
        }

        void update(
                Chunk[] newChunks,
                byte[] newBiomeData, // pre 1.15
                PacketChunkData.TileEntity[] newTileEntities, // 1.9+
                PacketChunkData.Heightmaps newHeightmaps, // 1.14+
                int[] newBiomes, // 1.15+
                boolean useExistingLightData // 1.16+
        ) {
            if (changes.length < newChunks.length) {
                changes = Arrays.copyOf(changes, newChunks.length);
            }
            for (int i = 0; i < newChunks.length; i++) {
                if (newChunks[i] != null) {
                    changes[i] = newChunks[i];
                    if (registry.olderThan(ProtocolVersion.v1_17)) {
                        blockChanges[i] = null;
                    }
                }
            }
            allBlockChanges.clear();

            if (newBiomeData != null) { // pre 1.15
                this.biomeData = newBiomeData;
            }
            if (newTileEntities != null) { // 1.9+
                this.tileEntities = newTileEntities;
            }
            if (newHeightmaps != null) { // 1.14+
                this.heightmaps = newHeightmaps;
            }
            if (newBiomes != null) { // 1.15+
                this.biomes = newBiomes;
            }
            if (!useExistingLightData) { // 1.16+
                this.useExistingLightData = false;
            }
        }

        private void updateLight(PacketUpdateLight.Data data) { // 1.14+
            List<byte[]> skyLightUpdates = data.skyLight;
            List<byte[]> blockLightUpdates = data.blockLight;

            if (skyLight.length < skyLightUpdates.size()) {
                skyLight = Arrays.copyOf(skyLight, skyLightUpdates.size());
            }

            if (blockLight.length < blockLightUpdates.size()) {
                blockLight = Arrays.copyOf(blockLight, blockLightUpdates.size());
            }

            int i = 0;
            for (byte[] light : skyLightUpdates) {
                if (light != null) {
                    skyLight[i] = light;
                }
                i++;
            }
            i = 0;
            for (byte[] light : blockLightUpdates) {
                if (light != null) {
                    blockLight[i] = light;
                }
                i++;
            }
        }

        private boolean hasLight() { // 1.14+
            for (byte[] light : skyLight) {
                if (light != null) {
                    return true;
                }
            }
            for (byte[] light : blockLight) {
                if (light != null) {
                    return true;
                }
            }
            return false;
        }

        private MutablePair<Long, PacketBlockChange> blockChanges(IPosition pos) {
            int x = pos.getX();
            int y = pos.getY();
            int chunkY = y / 16;
            int z = pos.getZ();
            if (registry.atLeast(ProtocolVersion.v1_17)) {
                int index = y << 10 | (x & 15) << 5 | (z & 15);
                return allBlockChanges.computeIfAbsent(index, k -> MutablePair.of(0L, null));
            } else {
                if (chunkY < 0 || chunkY >= blockChanges.length) {
                    return null;
                }
                if (blockChanges[chunkY] == null) {
                    blockChanges[chunkY] = new HashMap<>();
                }
                short index = (short) ((x & 15) << 10 | (y & 15) << 5 | (z & 15));
                return blockChanges[chunkY].computeIfAbsent(index, k -> MutablePair.of(0L, null));
            }
        }

        void updateBlock(long time, PacketBlockChange change) {
            MutablePair<Long, PacketBlockChange> pair = blockChanges(change.getPosition());
            if (pair != null && pair.getLeft() <= time) {
                pair.setLeft(time);
                pair.setRight(change);
            }
        }
    }

    private static class ColumnPos {
        private static long coordToLong(int x, int z) {
            return (long) x << 32 | z & 0xFFFFFFFFL;
        }

        private static int longToX(long coord) {
            return (int) (coord >> 32);
        }

        private static int longToZ(long coord) {
            return (int) (coord & 0xFFFFFFFFL);
        }
    }

}
