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
package com.replaymod.replaystudio.filter;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockValuePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerExplosionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerOpenTileEntityEditorPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.replaystudio.util.PacketUtils;
import com.replaymod.replaystudio.util.Utils;
import org.apache.commons.lang3.tuple.MutablePair;

//#if MC>=11300
import com.github.steveice10.mc.protocol.data.message.Message;
//#endif

//#if MC>=10904
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.scoreboard.CollisionRule;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
//#else
//$$ import com.github.steveice10.mc.protocol.data.game.Chunk;
//#if MC>=10800
//$$ import com.github.steveice10.mc.protocol.data.game.Position;
//#endif
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerSetExperiencePacket;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiChunkDataPacket;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateSignPacket;
//$$ import com.replaymod.replaystudio.util.Utils;
//#endif

//#if MC>=10800
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerWorldBorderPacket;
//#else
//$$ import com.github.steveice10.mc.protocol.data.game.BlockChangeRecord;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket.Difficulty;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket.GameMode;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket.WorldType;
//#endif

import java.util.*;

import static com.replaymod.replaystudio.io.WrappedPacket.instanceOf;
import static com.replaymod.replaystudio.util.Utils.within;

public class SquashFilter extends StreamFilterBase {

    private static final long POS_MIN = Byte.MIN_VALUE;
    private static final long POS_MAX = Byte.MAX_VALUE;

    private static class Team {
        private enum Status {
            CREATED, UPDATED, REMOVED
        }

        private final Status status;

        private String name;
        //#if MC>=11300
        private Message displayName;
        private Message prefix;
        private Message suffix;
        //#else
        //$$ private String displayName;
        //$$ private String prefix;
        //$$ private String suffix;
        //#endif
        //#if MC>=10800
        private boolean friendlyFire;
        private boolean seeingFriendlyInvisibles;
        private NameTagVisibility nameTagVisibility;
        //#if MC>=10904
        private CollisionRule collisionRule;
        //#endif
        private TeamColor color;
        //#else
        //$$ private ServerTeamPacket.FriendlyFireMode friendlyFire;
        //#endif
        private final Set<String> added = new HashSet<>();
        private final Set<String> removed = new HashSet<>();

        public Team(Status status) {
            this.status = status;
        }
    }

    private static class Entity {
        private boolean complete;
        private boolean despawned;
        private List<PacketData> packets = new ArrayList<>();
        private long lastTimestamp = 0;
        private Location loc = null;
        private long dx = 0;
        private long dy = 0;
        private long dz = 0;
        private Float yaw = null;
        private Float pitch = null;
        //#if MC>=10800
        private boolean onGround = false;
        //#endif
    }

    private final List<PacketData> unhandled = new ArrayList<>();
    private final Map<Integer, Entity> entities = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<Integer, PacketData> mainInventoryChanges = new HashMap<>();
    private final Map<Integer, ServerMapDataPacket> maps = new HashMap<>();

    private final List<PacketData> currentWorld = new ArrayList<>();
    private final List<PacketData> currentWindow = new ArrayList<>();
    private final List<PacketData> closeWindows = new ArrayList<>();

    private final Map<Long, ChunkData> chunks = new HashMap<>();
    private final Map<Long, Long> unloadedChunks = new HashMap<>();

    private long lastTimestamp;

    private GameMode gameMode = null;
    private Integer dimension = null;
    private Difficulty difficulty = null;
    private WorldType worldType = null;
    //#if MC>=10800
    private Boolean reducedDebugInfo = null;
    //#endif
    private PacketData joinGame;
    private PacketData respawn;
    private PacketData mainInventory;
    //#if MC>=10904
    private ServerPlayerSetExperiencePacket experience = null;
    //#else
    //$$ private ServerSetExperiencePacket experience = null;
    //#endif
    private ServerPlayerAbilitiesPacket abilities = null;

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        Packet packet = data.getPacket();
        lastTimestamp = data.getTime();

        if (instanceOf(packet, ServerSpawnParticlePacket.class)) {
            return false;
        }

        // Entities
        Integer entityId = PacketUtils.getEntityId(packet);
        if (entityId != null) { // Some entity is associated with this packet
            if (entityId == -1) { // Multiple entities in fact
                for (int id : PacketUtils.getEntityIds(packet)) {
                    //#if MC>=10904
                    if (packet instanceof ServerEntityDestroyPacket) {
                    //#else
                    //$$ if (packet instanceof ServerDestroyEntitiesPacket) {
                    //#endif
                        Entity entity = entities.computeIfAbsent(id, i -> new Entity());
                        entity.packets.clear();
                        entity.despawned = true;
                        if (entity.complete) {
                            entities.remove(id);
                        }
                    } else {
                        entities.compute(id, (i, e) -> e == null || e.despawned ? new Entity() : e).packets.add(data);
                    }
                }
            } else { // Only one entity
                Entity entity = entities.compute(entityId, (i, e) -> e == null || e.despawned ? new Entity() : e);
                if (packet instanceof ServerEntityMovementPacket) {
                    ServerEntityMovementPacket p = (ServerEntityMovementPacket) packet;
                    double mx = p.getMovementX();
                    double my = p.getMovementY();
                    double mz = p.getMovementZ();
                    //#if MC>=10800
                    entity.onGround = p.isOnGround();
                    //#endif

                    if (p instanceof ServerEntityPositionPacket || p instanceof ServerEntityPositionRotationPacket) {
                        entity.dx += mx * 32;
                        entity.dy += my * 32;
                        entity.dz += mz * 32;
                    }
                    if (p instanceof ServerEntityRotationPacket || p instanceof ServerEntityPositionRotationPacket) {
                        entity.yaw = p.getYaw();
                        entity.pitch = p.getPitch();
                    }
                } else if (packet instanceof ServerEntityTeleportPacket) {
                    ServerEntityTeleportPacket p = (ServerEntityTeleportPacket) packet;
                    entity.loc = Location.from(p);
                    entity.dx = entity.dy = entity.dz = 0;
                    entity.yaw = entity.pitch = null;
                    //#if MC>=10800
                    entity.onGround = p.isOnGround();
                    //#endif
                } else {
                    if (PacketUtils.isSpawnEntityPacket(packet)) {
                        entity.complete = true;
                    }
                    entity.packets.add(data);
                }
                entity.lastTimestamp = lastTimestamp;
            }
            return false;
        }

        // World
        if (packet instanceof ServerNotifyClientPacket) {
            ServerNotifyClientPacket p = (ServerNotifyClientPacket) packet;
            //#if MC>=10800
            if (p.getNotification() == ClientNotification.CHANGE_GAMEMODE) {
                gameMode = (GameMode) p.getValue();
            //#else
            //$$ if (p.getNotification() == ServerNotifyClientPacket.Notification.CHANGE_GAMEMODE) {
            //$$     gameMode = GameMode.valueOf(p.getValue().toString());
            //#endif
                return false;
            }
        }

        //#if MC>=10904
        if (packet instanceof ServerPlayerSetExperiencePacket) {
            experience = (ServerPlayerSetExperiencePacket) packet;
        //#else
        //$$ if (packet instanceof ServerSetExperiencePacket) {
        //$$     experience = (ServerSetExperiencePacket) packet;
        //#endif
            return false;
        }

        if (packet instanceof ServerPlayerAbilitiesPacket) {
            abilities = (ServerPlayerAbilitiesPacket) packet;
            return false;
        }

        //#if MC>=10800
        if (packet instanceof ServerDifficultyPacket) {
            difficulty = ((ServerDifficultyPacket) packet).getDifficulty();
            return false;
        }
        //#endif

        if (packet instanceof ServerJoinGamePacket) {
            ServerJoinGamePacket p = (ServerJoinGamePacket) packet;
            gameMode = p.getGameMode();
            dimension = p.getDimension();
            difficulty = p.getDifficulty();
            worldType = p.getWorldType();
            //#if MC>=10800
            reducedDebugInfo = p.getReducedDebugInfo();
            //#endif
            joinGame = data;
            return false;
        }

        if (packet instanceof ServerRespawnPacket) {
            ServerRespawnPacket p = (ServerRespawnPacket) packet;
            dimension = p.getDimension();
            //#if MC>=10800
            difficulty = p.getDifficulty();
            worldType = p.getWorldType();
            gameMode = p.getGameMode();
            //#else
            //$$ difficulty = Difficulty.valueOf(p.getDifficulty().toString());
            //$$ worldType = WorldType.valueOf(p.getWorldType().toString());
            //$$ gameMode = GameMode.valueOf(p.getGameMode().toString());
            //#endif
            currentWorld.clear();
            chunks.clear();
            unloadedChunks.clear();
            currentWindow.clear();
            entities.clear();
            respawn = data;
            return false;
        }

        if (packet instanceof ServerChunkDataPacket) {
            ServerChunkDataPacket p = (ServerChunkDataPacket) packet;
            //#if MC>=10904
            updateChunk(data.getTime(), p.getColumn());
            //#else
            //$$ updateChunk(data.getTime(), p.getX(), p.getZ(), p.getChunks(), p.getBiomeData());
            //#endif
            return false;
        }

        //#if MC>=10904
        if (packet instanceof ServerUnloadChunkPacket) {
            ServerUnloadChunkPacket p = (ServerUnloadChunkPacket) packet;
            unloadChunk(data.getTime(), p.getX(), p.getZ());
            return false;
        }
        //#else
        //$$ if (packet instanceof ServerMultiChunkDataPacket) {
        //$$     ServerMultiChunkDataPacket p = (ServerMultiChunkDataPacket) packet;
        //$$     for (int i = 0; i < p.getColumns(); i++) {
        //$$         updateChunk(data.getTime(), p.getX(i), p.getZ(i), p.getChunks(i), p.getBiomeData(i));
        //$$     }
        //$$     return false;
        //$$ }
        //#endif

        if (packet instanceof ServerBlockChangePacket) {
            updateBlock(data.getTime(), ((ServerBlockChangePacket) packet).getRecord());
            return false;
        }

        if (packet instanceof ServerMultiBlockChangePacket) {
            for (BlockChangeRecord record : ((ServerMultiBlockChangePacket) packet).getRecords()) {
                updateBlock(data.getTime(), record);
            }
            return false;
        }

        if (instanceOf(packet, ServerPlayerPositionRotationPacket.class)
                || instanceOf(packet, ServerRespawnPacket.class)
                || instanceOf(packet, ServerBlockBreakAnimPacket.class)
                || instanceOf(packet, ServerBlockChangePacket.class)
                || instanceOf(packet, ServerBlockValuePacket.class)
                || instanceOf(packet, ServerExplosionPacket.class)
                || instanceOf(packet, ServerMultiBlockChangePacket.class)
                || instanceOf(packet, ServerOpenTileEntityEditorPacket.class)
                || instanceOf(packet, ServerPlayEffectPacket.class)
                || instanceOf(packet, ServerPlaySoundPacket.class)
                || instanceOf(packet, ServerSpawnParticlePacket.class)
                || instanceOf(packet, ServerSpawnPositionPacket.class)
                //#if MC<10904
                //$$ || instanceOf(packet, ServerUpdateSignPacket.class)
                //#endif
                || instanceOf(packet, ServerUpdateTileEntityPacket.class)
                || instanceOf(packet, ServerUpdateTimePacket.class)
                //#if MC>=10800
                || instanceOf(packet, ServerWorldBorderPacket.class)
                //#endif
                ) {
            currentWorld.add(data);
            return false;
        }

        // Windows
        if (packet instanceof ServerCloseWindowPacket) {
            currentWindow.clear();
            closeWindows.add(data);
            return false;
        }

        if (instanceOf(packet, ServerConfirmTransactionPacket.class)) {
            return false; // This packet isn't of any use in replays
        }

        if (instanceOf(packet, ServerOpenWindowPacket.class)
                || instanceOf(packet, ServerWindowPropertyPacket.class)) {
            currentWindow.add(data);
            return false;
        }

        if (packet instanceof ServerWindowItemsPacket) {
            ServerWindowItemsPacket p = (ServerWindowItemsPacket) packet;
            if (p.getWindowId() == 0) {
                mainInventory = data;
            } else {
                currentWindow.add(data);
            }
            return false;
        }

        if (packet instanceof ServerSetSlotPacket) {
            ServerSetSlotPacket p = (ServerSetSlotPacket) packet;
            if (p.getWindowId() == 0) {
                mainInventoryChanges.put(p.getSlot(), data);
            } else {
                currentWindow.add(data);
            }
            return false;
        }

        // Teams
        if (packet instanceof ServerTeamPacket) {
            ServerTeamPacket p = (ServerTeamPacket) packet;
            //#if MC>=10800
            TeamAction CREATE = TeamAction.CREATE;
            TeamAction REMOVE = TeamAction.REMOVE;
            TeamAction UPDATE = TeamAction.UPDATE;
            TeamAction ADD_PLAYER = TeamAction.ADD_PLAYER;
            TeamAction REMOVE_PLAYER = TeamAction.REMOVE_PLAYER;
            TeamAction action = p.getAction();
            //#else
            //$$ ServerTeamPacket.Action CREATE = ServerTeamPacket.Action.CREATE;
            //$$ ServerTeamPacket.Action REMOVE = ServerTeamPacket.Action.REMOVE;
            //$$ ServerTeamPacket.Action UPDATE = ServerTeamPacket.Action.UPDATE;
            //$$ ServerTeamPacket.Action ADD_PLAYER = ServerTeamPacket.Action.ADD_PLAYER;
            //$$ ServerTeamPacket.Action REMOVE_PLAYER = ServerTeamPacket.Action.REMOVE_PLAYER;
            //$$ ServerTeamPacket.Action action = p.getAction();
            //#endif
            Team team = teams.get(p.getTeamName());
            if (team == null) {
                Team.Status status;
                if (action == CREATE) {
                    status = Team.Status.CREATED;
                } else if (action == REMOVE) {
                    status = Team.Status.REMOVED;
                } else {
                    status = Team.Status.UPDATED;
                }
                team = new Team(status);
                team.name = p.getTeamName();
                teams.put(team.name, team);
            }
            if (action == REMOVE && team.status == Team.Status.CREATED) {
                teams.remove(team.name);
            }
            if (action == CREATE || action == UPDATE) {
                team.displayName = p.getDisplayName();
                team.prefix = p.getPrefix();
                team.suffix = p.getSuffix();
                team.friendlyFire = p.getFriendlyFire();
                //#if MC>=10800
                team.seeingFriendlyInvisibles = p.getSeeFriendlyInvisibles();
                team.nameTagVisibility = p.getNameTagVisibility();
                //#if MC>=10904
                team.collisionRule = p.getCollisionRule();
                //#endif
                team.color = p.getColor();
                //#endif
            }
            if (action == ADD_PLAYER || action == CREATE) {
                for (String player : p.getPlayers()) {
                    if (!team.removed.remove(player)) {
                        team.added.add(player);
                    }
                }
            }
            if (action == REMOVE_PLAYER) {
                for (String player : p.getPlayers()) {
                    if (!team.added.remove(player)) {
                        team.removed.add(player);
                    }
                }
            }
            return false;
        }

        // Misc
        if (packet instanceof ServerMapDataPacket) {
            ServerMapDataPacket p = (ServerMapDataPacket) packet;
            maps.put(p.getMapId(), p);
            return false;
        }

        unhandled.add(data);
        return false;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
        List<PacketData> result = new ArrayList<>();

        result.addAll(unhandled);
        result.addAll(currentWorld);
        result.addAll(currentWindow);
        result.addAll(closeWindows);
        result.addAll(mainInventoryChanges.values());

        if (mainInventory != null) {
            result.add(mainInventory);
        }

        if (joinGame != null) {
            ServerJoinGamePacket org = (ServerJoinGamePacket) joinGame.getPacket();
            Packet packet = new ServerJoinGamePacket(org.getEntityId(), org.getHardcore(), gameMode, dimension,
                    difficulty, org.getMaxPlayers(), worldType
                    //#if MC>=10800
                    , reducedDebugInfo
                    //#endif
            );
            result.add(new PacketData(joinGame.getTime(), packet));
        } else if (respawn != null) {
            Packet packet = new ServerRespawnPacket(dimension,
                    //#if MC>=10800
                    difficulty, gameMode, worldType
                    //#else
                    //$$ ServerRespawnPacket.Difficulty.valueOf(difficulty.toString()),
                    //$$ ServerRespawnPacket.GameMode.valueOf(gameMode.toString()),
                    //$$ ServerRespawnPacket.WorldType.valueOf(worldType.toString())
                    //#endif
            );
            result.add(new PacketData(respawn.getTime(), packet));
        } else {
            //#if MC>=10800
            if (difficulty != null) {
                result.add(new PacketData(lastTimestamp, new ServerDifficultyPacket(difficulty)));
            }
            //#endif
            if (gameMode != null) {
                //#if MC>=10800
                Packet packet = new ServerNotifyClientPacket(ClientNotification.CHANGE_GAMEMODE, gameMode);
                //#else
                //$$ Packet packet = new ServerNotifyClientPacket(ServerNotifyClientPacket.Notification.CHANGE_GAMEMODE,
                //$$         ServerNotifyClientPacket.GameModeValue.valueOf(gameMode.toString()));
                //#endif
                result.add(new PacketData(lastTimestamp, packet));
            }
        }

        if (experience != null) {
            result.add(new PacketData(lastTimestamp, experience));
        }
        if (abilities != null) {
            result.add(new PacketData(lastTimestamp, abilities));
        }

        for (Map.Entry<Integer, Entity> e : entities.entrySet()) {
            Entity entity = e.getValue();

            if (entity.despawned) {
                //#if MC>=10904
                result.add(new PacketData(entity.lastTimestamp, new ServerEntityDestroyPacket(e.getKey())));
                //#else
                //$$ result.add(new PacketData(entity.lastTimestamp, new ServerDestroyEntitiesPacket(e.getKey())));
                //#endif
                continue;
            }

            FOR_PACKETS:
            for (PacketData data : entity.packets) {
                Packet packet = data.getPacket();
                Integer id = PacketUtils.getEntityId(packet);
                if (id == -1) { // Multiple entities
                    List<Integer> allIds = PacketUtils.getEntityIds(packet);
                    for (int i : allIds) {
                        Entity other = entities.get(i);
                        if (other == null || other.despawned) { // Other entity doesn't exist
                            continue FOR_PACKETS;
                        }
                    }
                }
                result.add(data);
            }

            if (entity.loc != null) {
                result.add(new PacketData(entity.lastTimestamp, entity.loc.toServerEntityTeleportPacket(e.getKey()
                        //#if MC>=10800
                        , entity.onGround
                        //#endif
                )));
            }
            while (entity.dx != 0 && entity.dy != 0 && entity.dz != 0) {
                long mx = within(entity.dx, POS_MIN, POS_MAX);
                long my = within(entity.dy, POS_MIN, POS_MAX);
                long mz = within(entity.dz, POS_MIN, POS_MAX);
                entity.dx -= mx;
                entity.dy -= my;
                entity.dz -= mz;
                ServerEntityPositionPacket p = new ServerEntityPositionPacket(e.getKey(), mx / 32d, my / 32d, mz / 32d
                        //#if MC>=10800
                        , entity.onGround
                        //#endif
                );
                result.add(new PacketData(entity.lastTimestamp, p));
            }
            if (entity.yaw != null && entity.pitch != null) {
                ServerEntityRotationPacket p = new ServerEntityRotationPacket(e.getKey(), entity.yaw, entity.pitch
                        //#if MC>=10800
                        , entity.onGround
                        //#endif
                );
                result.add(new PacketData(entity.lastTimestamp, p));
            }
        }

        for (Map.Entry<Long, Long> e : unloadedChunks.entrySet()) {
            int x = ChunkData.longToX(e.getKey());
            int z = ChunkData.longToZ(e.getKey());
            //#if MC>=10904
            result.add(new PacketData(e.getValue(), new ServerUnloadChunkPacket(x, z)));
            //#else
            //$$ result.add(new PacketData(e.getValue(), new ServerChunkDataPacket(x, z)));
            //#endif
        }

        for (ChunkData chunk : chunks.values()) {
            if (!Utils.containsOnlyNull(chunk.changes)) {
                //#if MC>=10904
                Packet packet = new ServerChunkDataPacket(new Column(chunk.x, chunk.z, chunk.changes, chunk.biomeData, chunk.tileEntities));
                //#else
                //$$ Packet packet = new ServerChunkDataPacket(chunk.x, chunk.z, chunk.changes, chunk.biomeData);
                //#endif
                result.add(new PacketData(chunk.firstAppearance, packet));
            }
            for (Map<Short, MutablePair<Long, BlockChangeRecord>> e : chunk.blockChanges) {
                if (e != null) {
                    for (MutablePair<Long, BlockChangeRecord> pair : e.values()) {
                        result.add(new PacketData(pair.getLeft(), new ServerBlockChangePacket(pair.getRight())));
                    }
                }
            }
        }

        Collections.sort(result, (e1, e2) -> Long.compare(e1.getTime(), e2.getTime()));
        for (PacketData data : result) {
            add(stream, timestamp, data.getPacket());
        }

        for (Team team : teams.values()) {
            String[] added = team.added.toArray(new String[team.added.size()]);
            String[] removed = team.added.toArray(new String[team.removed.size()]);
            if (team.status == Team.Status.CREATED) {
                add(stream, timestamp, new ServerTeamPacket(team.name, team.displayName, team.prefix, team.suffix,
                        team.friendlyFire,
                        //#if MC>=10800
                        team.seeingFriendlyInvisibles, team.nameTagVisibility,
                        //#endif
                        //#if MC>=10904
                        team.collisionRule,
                        //#endif
                        //#if MC>=10800
                        team.color,
                        //#endif
                        added));
            } else if (team.status == Team.Status.UPDATED) {
                //#if MC>=10800
                if (team.color != null) {
                    add(stream, timestamp, new ServerTeamPacket(team.name, team.displayName, team.prefix, team.suffix,
                            team.friendlyFire, team.seeingFriendlyInvisibles, team.nameTagVisibility,
                            //#if MC>=10904
                            team.collisionRule,
                            //#endif
                            team.color));
                }
                //#endif
                if (added.length > 0) {
                    add(stream, timestamp, new ServerTeamPacket(team.name,
                            //#if MC>=10800
                            TeamAction.ADD_PLAYER,
                            //#else
                            //$$ ServerTeamPacket.Action.ADD_PLAYER,
                            //#endif
                            added));
                }
                if (removed.length > 0) {
                    add(stream, timestamp, new ServerTeamPacket(team.name,
                            //#if MC>=10800
                            TeamAction.REMOVE_PLAYER,
                            //#else
                            //$$ ServerTeamPacket.Action.REMOVE_PLAYER,
                            //#endif
                            removed));
                }
            } else if (team.status == Team.Status.REMOVED) {
                add(stream, timestamp, new ServerTeamPacket(team.name));
            }
        }

        for (ServerMapDataPacket packet : maps.values()) {
            add(stream, timestamp, packet);
        }
    }

    @Override
    public String getName() {
        return "squash";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        PacketUtils.registerAllEntityRelated(studio);

        studio.setParsing(ServerNotifyClientPacket.class, true);
        //#if MC>=10904
        studio.setParsing(ServerPlayerSetExperiencePacket.class, true);
        //#else
        //$$ studio.setParsing(ServerSetExperiencePacket.class, true);
        //#endif
        studio.setParsing(ServerPlayerAbilitiesPacket.class, true);
        //#if MC>=10800
        studio.setParsing(ServerDifficultyPacket.class, true);
        //#endif
        studio.setParsing(ServerJoinGamePacket.class, true);
        studio.setParsing(ServerRespawnPacket.class, true);
        studio.setParsing(ServerTeamPacket.class, true);
        studio.setParsing(ServerCloseWindowPacket.class, true);
        studio.setParsing(ServerWindowItemsPacket.class, true);
        studio.setParsing(ServerSetSlotPacket.class, true);
        studio.setParsing(ServerChunkDataPacket.class, true);
        //#if MC>=10904
        studio.setParsing(ServerUnloadChunkPacket.class, true);
        //#else
        //$$ studio.setParsing(ServerMultiChunkDataPacket.class, true);
        //#endif
        studio.setParsing(ServerBlockChangePacket.class, true);
        studio.setParsing(ServerMultiBlockChangePacket.class, true);
        studio.setParsing(ServerMapDataPacket.class, true);
    }

    private void add(PacketStream stream, long timestamp, Packet packet) {
        stream.insert(new PacketData(timestamp, packet));
    }

    private void updateBlock(long time, BlockChangeRecord record) {
        //#if MC>=10800
        Position pos = record.getPosition();
        //#else
        //$$ BlockChangeRecord pos = record;
        //#endif
        chunks.computeIfAbsent(
                ChunkData.coordToLong(pos.getX() >> 4, pos.getZ() >> 4),
                idx -> new ChunkData(time, pos.getX() >> 4, pos.getZ() >> 4)
        ).updateBlock(time, record);
    }

    //#if MC>=10904
    private void unloadChunk(long time, int x, int z) {
        long coord = ChunkData.coordToLong(x, z);
        chunks.remove(coord);
        unloadedChunks.put(coord, time);
    }

    private void updateChunk(long time, Column column) {
        long coord = ChunkData.coordToLong(column.getX(), column.getZ());
        unloadedChunks.remove(coord);
        ChunkData chunk = chunks.get(coord);
        if (chunk == null) {
            chunks.put(coord, chunk = new ChunkData(time, column.getX(), column.getZ()));
        }
        chunk.update(column.getChunks(), column.getBiomeData(), column.getTileEntities());
    }
    //#else
    //$$ private void updateChunk(long time, int x, int z, Chunk[] chunkArray, byte[] biomeData) {
    //$$     long coord = ChunkData.coordToLong(x, z);
    //$$     if (Utils.containsOnlyNull(chunkArray)) { // UNLOAD
    //$$         chunks.remove(coord);
    //$$         unloadedChunks.put(coord, time);
    //$$     } else { // LOAD
    //$$         unloadedChunks.remove(coord);
    //$$         ChunkData chunk = chunks.get(coord);
    //$$         if (chunk == null) {
    //$$             chunks.put(coord, chunk = new ChunkData(time, x, z));
    //$$         }
    //$$         chunk.update(chunkArray, biomeData);
    //$$     }
    //$$ }
    //#endif

    private static class ChunkData {
        private final long firstAppearance;
        private final int x;
        private final int z;
        private final Chunk[] changes = new Chunk[16];
        //#if MC>=11300
        private int[] biomeData;
        //#else
        //$$ private byte[] biomeData;
        //#endif
        @SuppressWarnings("unchecked")
        private Map<Short, MutablePair<Long, BlockChangeRecord>>[] blockChanges = new Map[16];
        //#if MC>=10904
        public CompoundTag[] tileEntities;
        //#endif

        public ChunkData(long firstAppearance, int x, int z) {
            this.firstAppearance = firstAppearance;
            this.x = x;
            this.z = z;
        }

        //#if MC>=11300
        public void update(Chunk[] newChunks, int[] newBiomeData, CompoundTag[] newTileEntities) {
        //#else
        //#if MC>=10904
        //$$ public void update(Chunk[] newChunks, byte[] newBiomeData, CompoundTag[] newTileEntities) {
        //#else
        //$$ public void update(Chunk[] newChunks, byte[] newBiomeData) {
        //#endif
        //#endif
            for (int i = 0; i < newChunks.length; i++) {
                if (newChunks[i] != null) {
                    changes[i] = newChunks[i];
                    blockChanges[i] = null;
                }
            }

            if (newBiomeData != null) {
                this.biomeData = newBiomeData;
            }
            //#if MC>=10904
            if (newTileEntities != null) {
                this.tileEntities = newTileEntities;
            }
            //#endif
        }

        //#if MC>=10800
        private MutablePair<Long, BlockChangeRecord> blockChanges(Position pos) {
            int x = pos.getX();
            int y = pos.getY() / 16;
            int z = pos.getZ();
        //#else
        //$$ private MutablePair<Long, BlockChangeRecord> blockChanges(int x, int y, int z) {
        //$$     y = y / 16;
        //#endif
            if (blockChanges[y] == null) {
                blockChanges[y] = new HashMap<>();
            }
            short index = (short) ((x & 15) << 10 | (y & 15) << 5 | (z & 15));
            MutablePair<Long, BlockChangeRecord> pair = blockChanges[y].get(index);
            if (pair == null) {
                blockChanges[y].put(index, pair = MutablePair.of(0l, null));
            }
            return pair;
        }

        public void updateBlock(long time, BlockChangeRecord record) {
            MutablePair<Long, BlockChangeRecord> pair = blockChanges(
                    //#if MC>=10800
                    record.getPosition()
                    //#else
                    //$$ record.getX(), record.getY(), record.getZ()
                    //#endif
            );
            if (pair.getLeft() < time) {
                pair.setLeft(time);
                pair.setRight(record);
            }
        }

        public static long coordToLong(int x, int z) {
            return (long) x << 32 | z & 0xFFFFFFFFL;
        }

        public static int longToX(long coord) {
            return (int) (coord >> 32);
        }

        public static int longToZ(long coord) {
            return (int) (coord & 0xFFFFFFFFL);
        }
    }

}
