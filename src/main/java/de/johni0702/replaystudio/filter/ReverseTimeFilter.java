package de.johni0702.replaystudio.filter;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.util.Location;
import de.johni0702.replaystudio.util.Motion;
import de.johni0702.replaystudio.util.PacketUtils;
import de.johni0702.replaystudio.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.spacehq.mc.protocol.data.game.Chunk;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.attribute.Attribute;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntry;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntryAction;
import org.spacehq.mc.protocol.data.game.values.entity.*;
import org.spacehq.mc.protocol.data.game.values.entity.player.Animation;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.scoreboard.*;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.mc.protocol.data.game.values.statistic.Statistic;
import org.spacehq.mc.protocol.data.game.values.window.WindowType;
import org.spacehq.mc.protocol.data.game.values.world.WorldType;
import org.spacehq.mc.protocol.data.game.values.world.block.BlockChangeRecord;
import org.spacehq.mc.protocol.data.game.values.world.block.ExplodedBlockRecord;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.server.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.*;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.*;
import org.spacehq.mc.protocol.packet.ingame.server.world.*;
import org.spacehq.packetlib.packet.Packet;

import java.util.*;

import static de.johni0702.replaystudio.io.WrappedPacket.instanceOf;
import static de.johni0702.replaystudio.util.Java8.Map8.putIfAbsent;

public class ReverseTimeFilter extends StreamFilterBase {

    public boolean outputWarnings = true;

    private Location playerLocation;
    private final Map<Class<? extends Packet>, Packet> lastPacket = new HashMap<>();
    private final Map<Statistic, Integer> statistics = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<String, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, PlayerListEntry> playerList = new LinkedHashMap<>();
    private final Map<Integer, Entity> entities = new HashMap<>();
    private final Map<Long, ChunkData> chunks = new HashMap<>();
    private final Map<Integer, Inventory> inventories = new HashMap<>();
    private final Map<Integer, ServerMapDataPacket> maps = new HashMap<>();
    private final Map<Position, ServerUpdateTileEntityPacket> tileEntities = new HashMap<>();
    private final TitleInfo title = new TitleInfo();

    private long lastTimeUpdate;
    private long age;
    private long time;

    private int camera = -1;
    private int dimension;
    private Difficulty difficulty;
    private GameMode gamemode;
    private WorldType worldType;

    @Override
    public void init(Studio studio, JsonObject config) {
        studio.setParsing(ServerPluginMessagePacket.class, true);
        studio.setParsing(ServerStatisticsPacket.class, true);
        studio.setParsing(ServerTeamPacket.class, true);
        studio.setParsing(ServerPlayerPositionRotationPacket.class, true);
        studio.setParsing(ServerPlayerListEntryPacket.class, true);
        studio.setParsing(ServerSpawnPlayerPacket.class, true);
        studio.setParsing(ServerSpawnMobPacket.class, true);
        studio.setParsing(ServerDestroyEntitiesPacket.class, true);
        studio.setParsing(ServerScoreboardObjectivePacket.class, true);
        studio.setParsing(ServerDisplayScoreboardPacket.class, true);
        studio.setParsing(ServerUpdateScorePacket.class, true);
        studio.setParsing(ServerChunkDataPacket.class, true);
        studio.setParsing(ServerMultiChunkDataPacket.class, true);
        studio.setParsing(ServerUpdateTimePacket.class, true);
        studio.setParsing(ServerWindowItemsPacket.class, true);
        studio.setParsing(ServerSetSlotPacket.class, true);
        studio.setParsing(ServerEntityTeleportPacket.class, true);
        studio.setParsing(ServerEntityHeadLookPacket.class, true);
        studio.setParsing(ServerEntityVelocityPacket.class, true);
        studio.setParsing(ServerEntityPositionPacket.class, true); // ServerEntityMovementPacket
        studio.setParsing(ServerEntityRotationPacket.class, true); // ServerEntityMovementPacket
        studio.setParsing(ServerEntityPositionRotationPacket.class, true); // ServerEntityMovementPacket
        studio.setParsing(ServerUpdateTileEntityPacket.class, true);
        studio.setParsing(ServerEntityMetadataPacket.class, true);
        studio.setParsing(ServerEntityPropertiesPacket.class, true);
        studio.setParsing(ServerBlockChangePacket.class, true);
        studio.setParsing(ServerEntityEquipmentPacket.class, true);
        studio.setParsing(ServerDestroyEntitiesPacket.class, true);
        studio.setParsing(ServerOpenWindowPacket.class, true);
        studio.setParsing(ServerCloseWindowPacket.class, true);
        studio.setParsing(ServerSpawnObjectPacket.class, true);
        studio.setParsing(ServerSpawnExpOrbPacket.class, true);
        studio.setParsing(ServerSpawnPaintingPacket.class, true);
        studio.setParsing(ServerAnimationPacket.class, true);
        studio.setParsing(ServerPlayerUseBedPacket.class, true);
        studio.setParsing(ServerRespawnPacket.class, true);
        studio.setParsing(ServerJoinGamePacket.class, true);
        studio.setParsing(ServerEntityRemoveEffectPacket.class, true);
        studio.setParsing(ServerEntityEffectPacket.class, true);
        studio.setParsing(ServerEntityStatusPacket.class, true);
        studio.setParsing(ServerNotifyClientPacket.class, true);
        studio.setParsing(ServerMultiBlockChangePacket.class, true);
        studio.setParsing(ServerExplosionPacket.class, true);
        studio.setParsing(ServerCollectItemPacket.class, true);
        studio.setParsing(ServerEntityAttachPacket.class, true);
        studio.setParsing(ServerDifficultyPacket.class, true);
        studio.setParsing(ServerSwitchCameraPacket.class, true);
        studio.setParsing(ServerTitlePacket.class, true);
        studio.setParsing(ServerWorldBorderPacket.class, true);
        studio.setParsing(ServerMapDataPacket.class, true);
    }

    @Override
    public void onStart(PacketStream stream) {
        inventories.put(0, new Inventory(0, WindowType.GENERIC_INVENTORY, "", 45, 0));
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        long time = data.getTime();
        Packet packet = data.getPacket();

        if (instanceOf(packet, ServerKeepAlivePacket.class)
                || instanceOf(packet, ServerChatPacket.class)
                || instanceOf(packet, ServerPlayEffectPacket.class)
                || instanceOf(packet, ServerPlaySoundPacket.class)
                || instanceOf(packet, ServerEntityStatusPacket.class)
                || instanceOf(packet, ServerBlockValuePacket.class)
                || instanceOf(packet, ServerTabCompletePacket.class)
                || instanceOf(packet, ServerCombatPacket.class) // Really don't known what this actually does?!
                || instanceOf(packet, ServerEntityNBTUpdatePacket.class) // Dunno know how to probably handle this one
                || instanceOf(packet, ServerSpawnParticlePacket.class)
                || instanceOf(packet, ServerBlockBreakAnimPacket.class) // While theoretically possible, we don't bother
                || instanceOf(packet, ServerConfirmTransactionPacket.class)
                || instanceOf(packet, ServerResourcePackSendPacket.class)
                || instanceOf(packet, ServerOpenTileEntityEditorPacket.class)
                || instanceOf(packet, ServerWindowPropertyPacket.class)
                || instanceOf(packet, ServerDisconnectPacket.class)
                || instanceOf(packet, ServerSetCompressionPacket.class)) { // Those packets cannot be reversed
            return false;
        } else if (packet instanceof ServerPluginMessagePacket) { // Do not yet know what to do
            switch (((ServerPluginMessagePacket) packet).getChannel()) {
                case "MC|Brand":
                    updateLatest(stream, data);
                    break;
                default:
//                    System.out.println(((ServerPluginMessagePacket) packet).getChannel());
            }
        } else if (instanceOf(packet, ServerSpawnPositionPacket.class)  // For these just resend the previous packet
                || instanceOf(packet, ServerPlayerAbilitiesPacket.class)
                || instanceOf(packet, ServerChangeHeldItemPacket.class)
                || instanceOf(packet, ServerUpdateSignPacket.class)
                || instanceOf(packet, ServerUpdateHealthPacket.class)
                || instanceOf(packet, ServerSetExperiencePacket.class)
                || instanceOf(packet, ServerPlayerListDataPacket.class)) {
            updateLatest(stream, data);
        } else if (packet instanceof ServerStatisticsPacket) {
            ServerStatisticsPacket p = (ServerStatisticsPacket) packet;
            Map<Statistic, Integer> reversed = new HashMap<>();
            for (Map.Entry<Statistic, Integer> e : p.getStatistics().entrySet()) {
                Statistic stat = e.getKey();
                Integer oldValue = statistics.put(stat, e.getValue());
                if (oldValue != null) {
                    reversed.put(stat, oldValue);
                }
            }
            if (!reversed.isEmpty()) {
                stream.insert(new PacketData(time, new ServerStatisticsPacket(reversed)));
            }
        } else if (packet instanceof ServerTeamPacket) {
            updateTeam(stream, time, (ServerTeamPacket) packet);
        } else if (packet instanceof ServerPlayerPositionRotationPacket) {
            if (playerLocation != null) {
                stream.insert(new PacketData(time, PacketUtils.toServerPlayerPositionRotationPacket(playerLocation)));
            }
            playerLocation = PacketUtils.updateLocation(playerLocation, (ServerPlayerPositionRotationPacket) packet);
        } else if (packet instanceof ServerPlayerListEntryPacket) {
            updatePlayerList(stream, time, (ServerPlayerListEntryPacket) packet);
        } else if (packet instanceof ServerSpawnPlayerPacket) {
            ServerSpawnPlayerPacket p = (ServerSpawnPlayerPacket) packet;
            int entityId = p.getEntityId();
            putIfAbsent(entities, entityId, new PlayerEntity(p));
            stream.insert(time, new ServerDestroyEntitiesPacket(entityId));
        } else if (packet instanceof ServerSpawnMobPacket) {
            ServerSpawnMobPacket p = (ServerSpawnMobPacket) packet;
            int entityId = p.getEntityId();
            putIfAbsent(entities, entityId, new MobEntity(p));
            stream.insert(time, new ServerDestroyEntitiesPacket(entityId));
        } else if (packet instanceof ServerSpawnObjectPacket) {
            ServerSpawnObjectPacket p = (ServerSpawnObjectPacket) packet;
            int entityId = p.getEntityId();
            putIfAbsent(entities, entityId, new ObjectEntity(p));
            stream.insert(time, new ServerDestroyEntitiesPacket(entityId));
        } else if (packet instanceof ServerSpawnPaintingPacket) {
            ServerSpawnPaintingPacket p = (ServerSpawnPaintingPacket) packet;
            int entityId = p.getEntityId();
            putIfAbsent(entities, entityId, new PaintingEntity(p));
            stream.insert(time, new ServerDestroyEntitiesPacket(entityId));
        } else if (packet instanceof ServerSpawnExpOrbPacket) {
            ServerSpawnExpOrbPacket p = (ServerSpawnExpOrbPacket) packet;
            int entityId = p.getEntityId();
            putIfAbsent(entities, entityId, new ExpOrbEntity(p));
            stream.insert(time, new ServerDestroyEntitiesPacket(entityId));
        } else if (packet instanceof ServerSpawnGlobalEntityPacket) {
            ServerSpawnGlobalEntityPacket p = (ServerSpawnGlobalEntityPacket) packet;
            int entityId = p.getEntityId();
            putIfAbsent(entities, entityId, new GlobalEntity(p));
            stream.insert(time, new ServerDestroyEntitiesPacket(entityId));
        } else if (packet instanceof ServerScoreboardObjectivePacket) {
            updateScoreboard(stream, time, (ServerScoreboardObjectivePacket) packet);
        } else if (packet instanceof ServerDisplayScoreboardPacket) {
            ServerDisplayScoreboardPacket p = (ServerDisplayScoreboardPacket) packet;
            Scoreboard scoreboard = scoreboards.get(p.getScoreboardName());
            if (scoreboard != null) {
                if (scoreboard.position != null) {
                    stream.insert(time, new ServerDisplayScoreboardPacket(scoreboard.position, scoreboard.name));
                }
                scoreboard.position = p.getPosition();
            }
        } else if (packet instanceof ServerUpdateScorePacket) {
            ServerUpdateScorePacket p = (ServerUpdateScorePacket) packet;
            Scoreboard scoreboard = scoreboards.get(p.getObjective());
            if (scoreboard != null) {
                if (p.getAction() == ScoreboardAction.ADD_OR_UPDATE) {
                    Integer oldValue = scoreboard.values.put(p.getEntry(), p.getValue());
                    if (oldValue == null) { // ADD
                        stream.insert(time, new ServerUpdateScorePacket(p.getEntry(), scoreboard.name));
                    } else { // UPDATE
                        stream.insert(time, new ServerUpdateScorePacket(p.getEntry(), scoreboard.name, oldValue));
                    }
                } else { // REMOVE
                    Integer value = scoreboard.values.remove(p.getEntry());
                    if (value != null) {
                        stream.insert(time, new ServerUpdateScorePacket(p.getEntry(), scoreboard.name, value));
                    }
                }
            }
        } else if (packet instanceof ServerChunkDataPacket) {
            ServerChunkDataPacket p = (ServerChunkDataPacket) packet;
            updateChunk(stream, time, p.getX(), p.getZ(), p.getChunks(), p.getBiomeData());
        } else if (packet instanceof ServerMultiChunkDataPacket) {
            ServerMultiChunkDataPacket p = (ServerMultiChunkDataPacket) packet;
            for (int i = 0; i < p.getColumns(); i++) {
                updateChunk(stream, time, p.getX(i), p.getZ(i), p.getChunks(i), p.getBiomeData(i));
            }
        } else if (packet instanceof ServerUpdateTimePacket) {
            ServerUpdateTimePacket p = (ServerUpdateTimePacket) packet;

            long millisPassed = time - lastTimeUpdate;
            long ticksPassed = millisPassed / 50;
            this.time += ticksPassed;
            this.age += ticksPassed;
            this.lastTimeUpdate = time;

            stream.insert(time, new ServerUpdateTimePacket(this.age, this.time));

            this.time = p.getTime();
            this.age = p.getWorldAge();
        } else if (packet instanceof ServerWindowItemsPacket) {
            ServerWindowItemsPacket p = (ServerWindowItemsPacket) packet;
            Inventory inv = inventories.get(p.getWindowId());
            if (inv != null) {
                Inventory mainInv = inventories.get(0);
                stream.insert(time, inv.items(mainInv));
                inv.setItems(mainInv, p.getItems());
            } else {
                warn("ServerWindowItemsPacket for unknown window: " + ReflectionToStringBuilder.toString(p));
            }
        } else if (packet instanceof ServerSetSlotPacket) {
            ServerSetSlotPacket p = (ServerSetSlotPacket) packet;
            Inventory inv = inventories.get(p.getWindowId());
            if (inv != null) {
                Inventory mainInv = inventories.get(0);
                stream.insert(time, inv.slot(mainInv, p.getSlot(), p.getItem()));
            } else if (p.getWindowId() != 255) {
                warn("ServerSetSlotPacket for unknown window: " + ReflectionToStringBuilder.toString(p));
            }
        } else if (packet instanceof ServerEntityTeleportPacket) {
            ServerEntityTeleportPacket p = (ServerEntityTeleportPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                stream.insert(time, entity.teleport());
                entity.location = Location.from(p);
                entity.onGround = p.isPriority();
            }
        } else if (packet instanceof ServerEntityHeadLookPacket) {
            ServerEntityHeadLookPacket p = (ServerEntityHeadLookPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                stream.insert(time, new ServerEntityHeadLookPacket(entity.id, entity.headYaw));
                entity.headYaw = p.getHeadYaw();
            }
        } else if (packet instanceof ServerEntityVelocityPacket) {
            ServerEntityVelocityPacket p = (ServerEntityVelocityPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                stream.insert(time, entity.velocity());
                entity.motion = Motion.from(p);
            }
        } else if (packet instanceof ServerEntityMovementPacket) {
            ServerEntityMovementPacket p = (ServerEntityMovementPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                double mx = p.getMovementX();
                double my = p.getMovementY();
                double mz = p.getMovementZ();
                float yaw = entity.location.getYaw();
                float pitch = entity.location.getPitch();

                ServerEntityMovementPacket n;
                if (p instanceof ServerEntityPositionPacket) {
                    n = new ServerEntityPositionPacket(entity.id, -mx, -my, -mz, entity.onGround);
                } else if (p instanceof ServerEntityRotationPacket) {
                    n = new ServerEntityRotationPacket(entity.id, yaw, pitch, entity.onGround);
                } else if (p instanceof ServerEntityPositionRotationPacket) {
                    n = new ServerEntityPositionRotationPacket(entity.id, -mx, -my, -mz, yaw, pitch, entity.onGround);
                } else {
                    throw new UnsupportedOperationException("Unknown movement packet: " + p);
                }
                stream.insert(time, n);
                entity.location = PacketUtils.updateLocation(entity.location, p);
            }
        } else  if (packet instanceof ServerUpdateTileEntityPacket) {
            ServerUpdateTileEntityPacket p = (ServerUpdateTileEntityPacket) packet;
            ServerUpdateTileEntityPacket old = tileEntities.put(p.getPosition(), p);
            if (old != null) {
                stream.insert(time, old);
            }
        } else if (packet instanceof ServerEntityMetadataPacket) {
            ServerEntityMetadataPacket p = (ServerEntityMetadataPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                List<EntityMetadata> newMeta = new ArrayList<>();
                for (EntityMetadata metadata : p.getMetadata()) {
                    metadata = entity.metadataMap.put(metadata.getId(), metadata);
                    if (metadata != null) {
                        newMeta.add(metadata);
                    }
                }
                stream.insert(time, new ServerEntityMetadataPacket(entity.id, newMeta.toArray(new EntityMetadata[newMeta.size()])));
            }
        } else if (packet instanceof ServerEntityPropertiesPacket) {
            ServerEntityPropertiesPacket p = (ServerEntityPropertiesPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                List<Attribute> newAttributes = new ArrayList<>();
                for (Attribute attribute : p.getAttributes()) {
                    attribute = entity.attributeMap.put(attribute.getType(), attribute);
                    if (attribute != null) {
                        newAttributes.add(attribute);
                    }
                }
                stream.insert(time, new ServerEntityPropertiesPacket(entity.id, newAttributes));
            }
        } else if (packet instanceof ServerBlockChangePacket) {
            ServerBlockChangePacket p = (ServerBlockChangePacket) packet;
            BlockChangeRecord record = p.getRecord();
            record = updateBlock(record);
            if (record != null) {
                stream.insert(time, new ServerBlockChangePacket(record));
            }
        } else if (packet instanceof ServerMultiBlockChangePacket) {
            ServerMultiBlockChangePacket p = (ServerMultiBlockChangePacket) packet;
            List<BlockChangeRecord> records = new ArrayList<>(p.getRecords().length);
            for (BlockChangeRecord record : p.getRecords()) {
                record = updateBlock(record);
                if (record != null) {
                    records.add(record);
                }
            }
            if (!records.isEmpty()) {
                stream.insert(time, new ServerMultiBlockChangePacket(records.toArray(new BlockChangeRecord[records.size()])));
            }
        } else if (packet instanceof ServerExplosionPacket) {
            ServerExplosionPacket p = (ServerExplosionPacket) packet;
            List<BlockChangeRecord> records = new ArrayList<>(p.getExploded().size());
            for (ExplodedBlockRecord e : p.getExploded()) {
                Position pos = new Position(e.getX(), e.getY(), e.getZ());
                BlockChangeRecord record = updateBlock(new BlockChangeRecord(pos, 0));
                if (record != null) {
                    records.add(record);
                }
            }
            if (!records.isEmpty()) {
                stream.insert(time, new ServerMultiBlockChangePacket(records.toArray(new BlockChangeRecord[records.size()])));
            }
        } else if (packet instanceof ServerEntityEquipmentPacket) {
            ServerEntityEquipmentPacket p = (ServerEntityEquipmentPacket) packet;
            int slot = p.getSlot();
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                stream.insert(time, new ServerEntityEquipmentPacket(entity.id, slot, entity.equipment[slot]));
                entity.equipment[slot] = p.getItem();
            }
        } else if (packet instanceof ServerDestroyEntitiesPacket) {
            for (int entityId : ((ServerDestroyEntitiesPacket) packet).getEntityIds()) {
                Entity entity = entities.remove(entityId);
                if (entity != null) {
                    List<Packet> packets = entity.create();
                    // Iterate backwards as packet output is reversed
                    for (ListIterator<Packet> iter = packets.listIterator(packets.size()); iter.hasPrevious(); ) {
                        stream.insert(time, iter.previous());
                    }
                }
            }
        } else if (packet instanceof ServerCollectItemPacket) {
            Entity entity = entities.remove(((ServerCollectItemPacket) packet).getCollectedEntityId());
            if (entity != null) {
                List<Packet> packets = entity.create();
                // Iterate backwards as packet output is reversed
                for (ListIterator<Packet> iter = packets.listIterator(packets.size()); iter.hasPrevious(); ) {
                    stream.insert(time, iter.previous());
                }
            }
        } else if (packet instanceof ServerOpenWindowPacket) {
            ServerOpenWindowPacket p = (ServerOpenWindowPacket) packet;
            Inventory inv = new Inventory(p);
            inventories.put(p.getWindowId(), inv);
            stream.insert(time, inv.close());
        } else if (packet instanceof ServerCloseWindowPacket) {
            ServerCloseWindowPacket p = (ServerCloseWindowPacket) packet;
            if (p.getWindowId() != 0) {
                Inventory inv = inventories.remove(p.getWindowId());
                stream.insert(time, inv.items(inventories.get(0)));
                stream.insert(time, inv.open());
            }
        } else if (packet instanceof ServerAnimationPacket) {
            ServerAnimationPacket p = (ServerAnimationPacket) packet;
            if (p.getAnimation() == Animation.LEAVE_BED) {
                PlayerEntity player = (PlayerEntity) entities.get(p.getEntityId());
                if (player != null && player.bed != null) {
                    stream.insert(time, new ServerPlayerUseBedPacket(player.id, player.bed));
                    player.bed = null;
                }
            }
        } else if (packet instanceof ServerPlayerUseBedPacket) {
            ServerPlayerUseBedPacket p = (ServerPlayerUseBedPacket) packet;
            PlayerEntity player = (PlayerEntity) entities.get(p.getEntityId());
            if (player != null && player.bed == null) {
                stream.insert(time, new ServerAnimationPacket(player.id, Animation.LEAVE_BED));
                player.bed = p.getPosition();
            }
        } else if (packet instanceof ServerRespawnPacket) {
            ServerRespawnPacket p = (ServerRespawnPacket) packet;
            if (playerLocation == null) {
                return false; // Respawn without any previous player position? just ignore it
            }
            stream.insert(time, PacketUtils.toServerPlayerPositionRotationPacket(playerLocation));
            for (Entity entity : entities.values()) {
                List<Packet> packets = entity.create();
                // Iterate backwards as packet output is reversed
                for (ListIterator<Packet> iter = packets.listIterator(packets.size()); iter.hasPrevious(); ) {
                    stream.insert(time, iter.previous());
                }
            }
            for (ChunkData chunk : chunks.values()) {
                stream.insert(time, new ServerChunkDataPacket(chunk.x, chunk.z, chunk.chunks, chunk.biomeData));
            }
            stream.insert(time, new ServerRespawnPacket(dimension, difficulty, gamemode, worldType));

            chunks.clear();
            entities.clear();
            dimension = p.getDimension();
            difficulty = p.getDifficulty();
            gamemode = p.getGameMode();
            worldType = p.getWorldType();
        } else if (packet instanceof ServerJoinGamePacket) {
            ServerJoinGamePacket p = (ServerJoinGamePacket) packet;
            dimension = p.getDimension();
            difficulty = p.getDifficulty();
            gamemode = p.getGameMode();
            worldType = p.getWorldType();
            // TODO: More
        } else if (packet instanceof ServerEntityRemoveEffectPacket) {
            ServerEntityRemoveEffectPacket p = (ServerEntityRemoveEffectPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                EffectData effectData = entity.effects.remove(p.getEffect());
                if (effectData != null) {
                    long duration = effectData.duration - (time - effectData.time) / 1000;
                    stream.insert(time, new ServerEntityEffectPacket(entity.id, p.getEffect(), effectData.amplifier,
                            (int) duration, effectData.hideParticles));
                }
            }
        } else if (packet instanceof ServerEntityEffectPacket) {
            ServerEntityEffectPacket p = (ServerEntityEffectPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                EffectData effectData = new EffectData(time, p.getAmplifier(), p.getDuration(), p.getHideParticles());
                effectData = entity.effects.put(p.getEffect(), effectData);
                if (effectData == null) {
                    stream.insert(time, new ServerEntityRemoveEffectPacket(entity.id, p.getEffect()));
                } else {
                    long duration = effectData.duration - (time - effectData.time) / 1000;
                    if (duration <= 0) {
                        stream.insert(time, new ServerEntityRemoveEffectPacket(entity.id, p.getEffect()));
                    } else {
                        stream.insert(time, new ServerEntityEffectPacket(entity.id, p.getEffect(), effectData.amplifier,
                                (int) duration, effectData.hideParticles));
                    }
                }
            }
        } else if (packet instanceof ServerNotifyClientPacket) {
            ServerNotifyClientPacket p = (ServerNotifyClientPacket) packet;
            switch (p.getNotification()) { // TODO
                case INVALID_BED:
                    break;
                case START_RAIN:
                    break;
                case STOP_RAIN:
                    break;
                case CHANGE_GAMEMODE:
                    break;
                case ENTER_CREDITS:
                    break;
                case DEMO_MESSAGE:
                    break;
                case ARROW_HIT_PLAYER:
                    break;
                case RAIN_STRENGTH:
                    break;
                case THUNDER_STRENGTH:
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown client notification: " + p.getNotification());
            }
        } else if (packet instanceof ServerEntityAttachPacket) {
            ServerEntityAttachPacket p = (ServerEntityAttachPacket) packet;
            Entity entity = entities.get(p.getEntityId());
            if (entity != null) {
                if (entity.attachedTo != -1) {
                    stream.insert(time, new ServerEntityAttachPacket(entity.id, entity.attachedTo, entity.leashed));
                }
                entity.attachedTo = p.getAttachedToId();
                entity.leashed = p.getLeash();
            }
        } else if (packet instanceof ServerDifficultyPacket) {
            stream.insert(time, new ServerDifficultyPacket(difficulty));
            this.difficulty = ((ServerDifficultyPacket) packet).getDifficulty();
        } else if (packet instanceof ServerSwitchCameraPacket) {
            stream.insert(time, new ServerSwitchCameraPacket(camera));
            this.camera = ((ServerSwitchCameraPacket) packet).getCameraEntityId();
        } else if (packet instanceof ServerTitlePacket) {
            ServerTitlePacket p = (ServerTitlePacket) packet;
            int passed = (int) ((time - title.time) / 50);
            switch (p.getAction()) {
                case TITLE:
                    stream.insert(time, new ServerTitlePacket(title.title, false));
                    title.title = p.getTitle();
                    break;
                case SUBTITLE:
                    stream.insert(time, new ServerTitlePacket(title.subTitle, true));
                    title.subTitle = p.getSubtitle();
                    break;
                case TIMES:
                    if (title.fadeIn + title.stay < passed) {
                        stream.insert(time, new ServerTitlePacket(0, title.stay + title.fadeIn - passed, title.fadeOut));
                    } else if (title.fadeIn + title.stay + title.fadeOut < passed) {
                        stream.insert(time, new ServerTitlePacket(0, 0, title.stay + title.fadeIn + title.fadeOut - passed));
                    }
                    title.time = time;
                    title.fadeIn = p.getFadeIn();
                    title.stay = p.getStay();
                    title.fadeOut = p.getFadeOut();
                    break;
                case CLEAR:
                    if (title.fadeIn + title.stay < passed) {
                        stream.insert(time, new ServerTitlePacket(0, title.stay + title.fadeIn - passed, title.fadeOut));
                    } else if (title.fadeIn + title.stay + title.fadeOut < passed) {
                        stream.insert(time, new ServerTitlePacket(0, 0, title.stay + title.fadeIn + title.fadeOut - passed));
                    }
                    title.fadeIn = 0;
                    title.stay = 0;
                    title.fadeOut = 0;
                    break;
                case RESET:
                    stream.insert(time, new ServerTitlePacket(title.title, false));
                    stream.insert(time, new ServerTitlePacket(title.subTitle, true));
                    if (title.fadeIn + title.stay < passed) {
                        stream.insert(time, new ServerTitlePacket(0, title.stay + title.fadeIn - passed, title.fadeOut));
                    } else if (title.fadeIn + title.stay + title.fadeOut < passed) {
                        stream.insert(time, new ServerTitlePacket(0, 0, title.stay + title.fadeIn + title.fadeOut - passed));
                    }
                    title.fadeIn = 0;
                    title.stay = 0;
                    title.fadeOut = 0;
                    title.title = null;
                    title.subTitle = null;
                    break;
            }
        } else if (packet instanceof ServerWorldBorderPacket) {
            // TODO
        } else if (packet instanceof ServerMapDataPacket) {
            ServerMapDataPacket p = (ServerMapDataPacket) packet;
            p = maps.put(p.getMapId(), p);
            if (p != null) {
                stream.insert(time, p);
            }
        } else {
            throw new UnsupportedOperationException("Cannot reverse " + packet);
        }
        return false;
    }

    private void warn(String str) {
        if (outputWarnings) {
            System.out.println("[Warning] " + str);
        }
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {

    }

    @Override
    public String getName() {
        return "reverse";
    }

    private int bTc(int blockCoord) {
        return Math.floorMod(blockCoord, 16);
    }

    private void updateLatest(PacketStream stream, PacketData data) {
        Packet packet = data.getPacket();
        Class<? extends Packet> cls = packet.getClass();
        Packet previous = lastPacket.get(cls);
        if (previous != null) {
            stream.insert(new PacketData(data.getTime(), previous));
        }
        lastPacket.put(cls, packet);
    }

    private BlockChangeRecord updateBlock(BlockChangeRecord record) {
        Position pos = record.getPosition();
        ChunkData chunkData = chunks.get(ChunkData.coordToLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (chunkData != null) {
            Chunk chunk = chunkData.chunks[pos.getY() >> 4];
            if (chunk != null) {
                int oldBlock = chunk.getBlocks().get(bTc(pos.getX()), bTc(pos.getY()), bTc(pos.getZ()));
                chunk.getBlocks().set(bTc(pos.getX()), bTc(pos.getY()), bTc(pos.getZ()), record.getBlock());
                return new BlockChangeRecord(pos, oldBlock);
            }
        }
        return null;
    }

    private void updateTeam(PacketStream stream, long time, ServerTeamPacket packet) {
        String name = packet.getTeamName();
        if (packet.getAction() != TeamAction.CREATE && !teams.containsKey(name)) {
            return;
        }
        Team team;
        List<String> players;
        switch (packet.getAction()) {
            case CREATE:
                teams.put(name, team = new Team(packet));
                stream.insert(time, team.remove());
                break;
            case REMOVE:
                team = teams.remove(name);
                stream.insert(time, team.create());
                break;
            case UPDATE:
                team = teams.get(name);
                stream.insert(time, team.update());
                team.update(packet);
                break;
            case ADD_PLAYER:
                team = teams.get(name);
                players = new ArrayList<>();
                for (String player : packet.getPlayers()) {
                    if (team.players.add(player)) {
                        players.add(player);
                    }
                }
                packet = new ServerTeamPacket(name, TeamAction.REMOVE_PLAYER, players.toArray(new String[players.size()]));
                stream.insert(time, packet);
                break;
            case REMOVE_PLAYER:
                team = teams.get(name);
                players = new ArrayList<>();
                for (String player : packet.getPlayers()) {
                    if (team.players.remove(player)) {
                        players.add(player);
                    }
                }
                packet = new ServerTeamPacket(name, TeamAction.ADD_PLAYER, players.toArray(new String[players.size()]));
                stream.insert(time, packet);
                break;
        }
    }

    private void updatePlayerList(PacketStream stream, long time, ServerPlayerListEntryPacket packet) {
        PlayerListEntryAction reversedAction;
        List<PlayerListEntry> reversed = new ArrayList<>();
        if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
            reversedAction = PlayerListEntryAction.REMOVE_PLAYER;
            for (PlayerListEntry entry : packet.getEntries()) {
                UUID uuid = entry.getProfile().getId();
                if (!playerList.containsKey(uuid)) {
                    playerList.put(uuid, entry);
                    reversed.add(entry);
                }
            }
        } else if (packet.getAction() == PlayerListEntryAction.REMOVE_PLAYER) {
            List<PlayerListEntry> removed = new ArrayList<>();
            int untouched = 0;
            for (PlayerListEntry entry : packet.getEntries()) {
                int pos = 0;
                for (PlayerListEntry e : playerList.values()) {
                    if (e.getProfile().getId().equals(entry.getProfile().getId())) {
                        removed.add(e);
                        break;
                    }
                    pos++;
                }
                if (pos < untouched) {
                    untouched = pos;
                }
            }
            int i = 0;
            for (Iterator<Map.Entry<UUID, PlayerListEntry>> iter = playerList.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<UUID, PlayerListEntry> e = iter.next();
                if (i++ >= untouched) {
                    reversed.add(e.getValue());
                    iter.remove();
                }
            }
            // Remove all
            PlayerListEntry[] remove = reversed.toArray(new PlayerListEntry[reversed.size()]);
            // Re-add all except the removed ones
            reversed.removeAll(removed);
            PlayerListEntry[] add = reversed.toArray(new PlayerListEntry[reversed.size()]);

            // Packet output is reversed therefore we have to reverse added players as well
            ArrayUtils.reverse(add);

            // First send add then remove as whole packet output is reversed
            stream.insert(new PacketData(time, new ServerPlayerListEntryPacket(PlayerListEntryAction.ADD_PLAYER, add)));
            stream.insert(new PacketData(time, new ServerPlayerListEntryPacket(PlayerListEntryAction.REMOVE_PLAYER, remove)));
            return;
        } else {
            reversedAction = packet.getAction();
            for (PlayerListEntry entry : packet.getEntries()) {
                UUID uuid = entry.getProfile().getId();
                PlayerListEntry oldEntry = playerList.get(uuid);
                if (oldEntry != null) {
                    reversed.add(oldEntry);
                    GameMode gameMode = oldEntry.getGameMode();
                    int ping = oldEntry.getPing();
                    Message displayName = oldEntry.getDisplayName();
                    if (packet.getAction() == PlayerListEntryAction.UPDATE_GAMEMODE) {
                        gameMode = entry.getGameMode();
                    } else if (packet.getAction() == PlayerListEntryAction.UPDATE_LATENCY) {
                        ping = entry.getPing();
                    } else if (packet.getAction() == PlayerListEntryAction.UPDATE_DISPLAY_NAME) {
                        displayName = entry.getDisplayName();
                    }
                    playerList.put(uuid, new PlayerListEntry(oldEntry.getProfile(), gameMode, ping, displayName));
                }
            }
        }
        stream.insert(new PacketData(time, new ServerPlayerListEntryPacket(reversedAction,
                reversed.toArray(new PlayerListEntry[reversed.size()]))));
    }

    private void updateScoreboard(PacketStream stream, long time, ServerScoreboardObjectivePacket packet) {
        String name = packet.getName();
        Scoreboard scoreboard;
        switch (packet.getAction()) {
            case ADD:
                scoreboards.put(name, scoreboard = new Scoreboard(packet));
                stream.insert(time, scoreboard.remove());
                break;
            case REMOVE:
                scoreboard = scoreboards.remove(name);
                if (scoreboard == null) {
                    return;
                }
                for (Packet p : scoreboard.add()) {
                    stream.insert(time, p);
                }
                break;
            case UPDATE:
                scoreboard = scoreboards.get(name);
                if (scoreboard == null) {
                    return;
                }
                stream.insert(time, scoreboard.update());
                scoreboard.update(packet);
                break;
        }
    }

    private void updateChunk(PacketStream stream, long time, int x, int z, Chunk[] chunkArray, byte[] biomeData) {
        ChunkData chunk;
        if (Utils.containsOnlyNull(chunkArray)) { // UNLOAD
            chunk = chunks.remove(ChunkData.coordToLong(x, z));
            if (chunk != null) {
                stream.insert(time, new ServerChunkDataPacket(x, z, chunk.chunks, chunk.biomeData));
            }
        } else { // LOAD
            chunk = chunks.get(ChunkData.coordToLong(x, z));
            if (chunk == null) { // FIRST LOAD
                chunks.put(ChunkData.coordToLong(x, z), chunk = new ChunkData(x, z));
                stream.insert(time, chunk.unload());
                chunk.chunks = chunkArray;
                chunk.biomeData = biomeData;
            } else { // UPDATE
                stream.insert(time, chunk.update(chunkArray, biomeData));
            }
        }
    }

    private static final class Team {
        private final String name;
        private final List<String> players;
        private String displayName;
        private String prefix;
        private String suffix;
        private FriendlyFire friendlyFire;
        private NameTagVisibility nameTagVisibility;
        private TeamColor color;

        public Team(ServerTeamPacket packet) {
            this.name = packet.getTeamName();
            this.players = Lists.newArrayList(packet.getPlayers());
            update(packet);
        }

        public void update(ServerTeamPacket packet) {
            this.displayName = packet.getDisplayName();
            this.prefix = packet.getPrefix();
            this.suffix = packet.getSuffix();
            this.friendlyFire = packet.getFriendlyFire();
            this.nameTagVisibility = packet.getNameTagVisibility();
            this.color = packet.getColor();
        }

        public ServerTeamPacket create() {
            return new ServerTeamPacket(name, displayName, prefix, suffix, friendlyFire, nameTagVisibility, color,
                    players.toArray(new String[players.size()]));
        }

        public ServerTeamPacket update() {
            return new ServerTeamPacket(name, displayName, prefix, suffix, friendlyFire, nameTagVisibility, color);
        }

        public ServerTeamPacket remove() {
            return new ServerTeamPacket(name);
        }
    }

    private static final class Scoreboard {
        private final String name;
        private String displayName;
        private ScoreType type;
        private ScoreboardPosition position;
        private final Map<String, Integer> values = new HashMap<>();

        public Scoreboard(ServerScoreboardObjectivePacket packet) {
            this.name = packet.getName();
            update(packet);
        }

        public void update(ServerScoreboardObjectivePacket packet) {
            this.displayName = packet.getDisplayName();
            this.type = packet.getType();
        }

        private Packet[] add() {
            Packet add = new ServerScoreboardObjectivePacket(name, ObjectiveAction.ADD, displayName, type);
            if (position != null) {
                return new Packet[]{add, new ServerDisplayScoreboardPacket(position, name)};
            }
            return new Packet[]{add};
        }

        private ServerScoreboardObjectivePacket update() {
            return new ServerScoreboardObjectivePacket(name, ObjectiveAction.UPDATE, displayName, type);
        }

        private ServerScoreboardObjectivePacket remove() {
            return new ServerScoreboardObjectivePacket(name);
        }
    }

    @RequiredArgsConstructor
    private static class ChunkData {
        private final int x;
        private final int z;
        private Chunk[] chunks;
        private byte[] biomeData;

        public static long coordToLong(int x, int z) {
            return (long) x << 32 | z & 0xFFFFFFFFL;
        }

        public ServerChunkDataPacket load() {
            return new ServerChunkDataPacket(x, z, chunks, biomeData);
        }

        public ServerChunkDataPacket unload() {
            return new ServerChunkDataPacket(x, z);
        }

        public Packet update(Chunk[] newChunks, byte[] newBiomeData) {
            Chunk[] updatedChunk = new Chunk[16];
            for (int i = 0; i < updatedChunk.length; i++) {
                if (newChunks[i] != null) {
                    updatedChunk[i] = chunks[i];
                    chunks[i] = newChunks[i];
                }
            }

            byte[] biomeData;
            if (newBiomeData != null) {
                biomeData = this.biomeData;
                this.biomeData = newBiomeData;
            } else {
                biomeData = null;
            }

            return new ServerChunkDataPacket(x, z, updatedChunk, biomeData);
        }
    }

    private static class Inventory {
        private final int id;
        private final WindowType type;
        private final String name;
        private final ItemStack[] items;
        private final int ownerEntityId;

        public Inventory(int id, WindowType type, String name, int size, int ownerEntityId) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.items = new ItemStack[size];
            this.ownerEntityId = ownerEntityId;
        }

        public Inventory(ServerOpenWindowPacket p) {
            this(p.getWindowId(), p.getType(), p.getName(), p.getSlots(), p.getOwnerEntityId());
        }

        public ServerOpenWindowPacket open() {
            return new ServerOpenWindowPacket(id, type, name, items.length, ownerEntityId);
        }

        public ServerCloseWindowPacket close() {
            return new ServerCloseWindowPacket(id);
        }

        public void setItems(Inventory mainInventory, ItemStack[] items) {
            System.arraycopy(items, 0, this.items, 0, this.items.length);
            if (items.length > this.items.length) {
                int remaining = Math.min(mainInventory.items.length, items.length - this.items.length);
                System.arraycopy(items, this.items.length, mainInventory.items, 0, remaining);
            }
        }

        public ServerWindowItemsPacket items(Inventory mainInventory) {
            ItemStack[] result = new ItemStack[mainInventory.items.length + items.length];
            System.arraycopy(items, 0, result, 0, items.length);
            System.arraycopy(mainInventory.items, 0, result, items.length, mainInventory.items.length);
            return new ServerWindowItemsPacket(id, result);
        }

        public ServerSetSlotPacket slot(Inventory mainInventory, int slot, ItemStack newItem) {
            ItemStack oldItem;
            if (slot < items.length) {
                oldItem = items[slot];
                items[slot] = newItem;
            } else {
                oldItem = mainInventory.items[slot - items.length];
                mainInventory.items[slot - items.length] = newItem;
            }
            return new ServerSetSlotPacket(id, slot, oldItem);
        }
    }

    @Data
    private static final class EffectData {
        private final long time;
        private final int amplifier;
        private final int duration;
        private final boolean hideParticles;
    }

    @Getter
    private static abstract class Entity {
        protected final int id;
        protected Location location;
        protected float headYaw;
        protected Motion motion;
        protected boolean onGround;
        protected final Map<Integer, EntityMetadata> metadataMap = new HashMap<>();
        protected final Map<AttributeType, Attribute> attributeMap = new HashMap<>();
        protected final Map<Effect, EffectData> effects = new HashMap<>();
        protected final ItemStack[] equipment = new ItemStack[5];
        private int attachedTo = -1;
        private boolean leashed;

        public Entity(int id, Location location, Motion motion) {
            this(id, location, motion, new EntityMetadata[0]);
        }

        public Entity(int id, Location location, Motion motion, EntityMetadata...metadata) {
            this(id, location, 0f, motion, metadata);
        }

        public Entity(int id, Location location, float headYaw, Motion motion, EntityMetadata...metadata) {
            this.id = id;
            this.location = location;
            this.headYaw = headYaw;
            this.motion = motion;
            for (EntityMetadata meta : metadata) {
                getMetadataMap().put(meta.getId(), meta);
            }
        }

        public ServerEntityTeleportPacket teleport() {
            return new ServerEntityTeleportPacket(id, location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch(), onGround);
        }

        public ServerEntityVelocityPacket velocity() {
            return new ServerEntityVelocityPacket(id, motion.getX(), motion.getY(), motion.getZ());
        }

        public List<Packet> create() {
            ArrayList<Packet> result = new ArrayList<>(2);
            result.add(spawnPacket());
            result.add(new ServerEntityPropertiesPacket(id, new ArrayList<>(attributeMap.values())));
            for (int i = 0; i < equipment.length; i++) {
                if (equipment[i] != null) {
                    result.add(new ServerEntityEquipmentPacket(id, i, equipment[i]));
                }
            }
            return result;
        }


        public abstract Packet spawnPacket();
    }

    private static class PlayerEntity extends Entity {
        private final UUID uuid;
        private int currentItem;
        private Position bed;

        public PlayerEntity(ServerSpawnPlayerPacket packet) {
            super(packet.getEntityId(),
                    new Location(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch()),
                    Motion.NULL, packet.getMetadata());
            this.uuid = packet.getUUID();
            this.currentItem = packet.getCurrentItem();
        }

        @Override
        public Packet spawnPacket() {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            EntityMetadata[] metadataArray = metadataMap.values().toArray(new EntityMetadata[metadataMap.size()]);
            return new ServerSpawnPlayerPacket(currentItem, uuid, x, y, z, yaw, pitch, currentItem, metadataArray);
        }
    }

    private static class MobEntity extends Entity {
        private final MobType type;

        public MobEntity(ServerSpawnMobPacket packet) {
            super(packet.getEntityId(),
                    new Location(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch()),
                    packet.getHeadYaw(), new Motion(packet.getX(), packet.getY(), packet.getZ()), packet.getMetadata());
            this.type = packet.getType();
        }

        @Override
        public Packet spawnPacket() {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            double motX = motion.getX();
            double motY = motion.getY();
            double motZ = motion.getZ();
            EntityMetadata[] metadataArray = metadataMap.values().toArray(new EntityMetadata[metadataMap.size()]);
            return new ServerSpawnMobPacket(id, type, x, y, z, yaw, pitch, headYaw, motX, motY, motZ, metadataArray);
        }
    }

    private static class ObjectEntity extends Entity {
        private final ObjectType type;
        private final ObjectData data;

        public ObjectEntity(ServerSpawnObjectPacket packet) {
            super(packet.getEntityId(),
                    new Location(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch()),
                    new Motion(packet.getX(), packet.getY(), packet.getZ()));
            this.type = packet.getType();
            this.data = packet.getData();
        }

        @Override
        public Packet spawnPacket() {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            double motX = motion.getX();
            double motY = motion.getY();
            double motZ = motion.getZ();
            return new ServerSpawnObjectPacket(id, type, data, x, y, z, yaw, pitch, motX, motY, motZ);
        }
    }

    private static class PaintingEntity extends Entity {
        private final Art art;
        private final HangingDirection direction;

        public PaintingEntity(ServerSpawnPaintingPacket packet) {
            super(packet.getEntityId(), new Location(packet.getPosition()), Motion.NULL);
            this.art = packet.getArt();
            this.direction = packet.getDirection();
        }

        @Override
        public Packet spawnPacket() {
            return new ServerSpawnPaintingPacket(id, art, location.getPosition(), direction);
        }
    }

    private static class ExpOrbEntity extends Entity {
        private final int exp;

        public ExpOrbEntity(ServerSpawnExpOrbPacket packet) {
            super(packet.getEntityId(), new Location(packet.getX(), packet.getY(), packet.getZ()), Motion.NULL);
            this.exp = packet.getExp();
        }

        @Override
        public Packet spawnPacket() {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            return new ServerSpawnExpOrbPacket(id, x, y, z, exp);
        }
    }

    private static class GlobalEntity extends Entity {
        private final GlobalEntityType type;

        public GlobalEntity(ServerSpawnGlobalEntityPacket packet) {
            super(packet.getEntityId(), new Location(packet.getX(), packet.getY(), packet.getZ()), Motion.NULL);
            this.type = packet.getType();
        }

        @Override
        public Packet spawnPacket() {
            int x = (int) location.getX();
            int y = (int) location.getY();
            int z = (int) location.getZ();
            return new ServerSpawnGlobalEntityPacket(id, type, x, y, z);
        }
    }

    private static class TitleInfo {
        private Message title;
        private Message subTitle;
        private long time;
        private int fadeIn;
        private int stay;
        private int fadeOut;
    }
}
