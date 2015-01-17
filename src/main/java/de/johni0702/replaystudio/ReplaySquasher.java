package de.johni0702.replaystudio;

import com.google.common.collect.Collections2;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.manipulation.PacketUtils;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.mc.protocol.data.game.values.world.WorldType;
import org.spacehq.mc.protocol.data.game.values.world.notify.ClientNotification;
import org.spacehq.mc.protocol.packet.ingame.server.ServerDifficultyPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.*;
import org.spacehq.packetlib.packet.Packet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

/**
 * Static methods for squashing a replay into a single moment removing all redundant packets.
 */
public class ReplaySquasher {

    public static ReplayPart squash(@NonNull Studio studio, @NonNull ReplayPart part) {
        LinkedList<Packet> packets = new LinkedList<>(part.packets());

        // Redundant things
        Set<Integer> redundantEntities = new HashSet<>();
        boolean worldChanged = false;
        GameMode gameMode = null;
        int dimension = 0;
        Difficulty difficulty = null;
        WorldType worldType = null;
        Boolean reducedDebugInfo = null;


        boolean joinGamePacket = false;
        for (Packet packet : packets) {
            if (packet instanceof ServerJoinGamePacket) {
                joinGamePacket = true;
            }
            if (packet instanceof ServerPlayerPositionRotationPacket) {
                break; // By now we should have received the ServerJoinGamePacket
            }
        }

        // Traverse the array backwards in order to remove redundant packets
        PACKETS:
        for (ListIterator<Packet> iter = packets.listIterator(packets.size()); iter.hasNext(); ) {
            Packet packet = iter.next();

            if (packet instanceof ServerNotifyClientPacket) {
                ServerNotifyClientPacket p = (ServerNotifyClientPacket) packet;
                if (p.getNotification() == ClientNotification.CHANGE_GAMEMODE) {
                    if (gameMode != null) { // Drop all previous changes
                        iter.remove();
                        continue PACKETS;
                    }
                    gameMode = (GameMode) p.getValue();
                }
            }

            if (packet instanceof ServerDifficultyPacket) {
                if (difficulty != null) {
                    iter.remove();
                    continue PACKETS;
                }
                difficulty = ((ServerDifficultyPacket) packet).getDifficulty();
            }

            if (worldChanged && packet instanceof ServerJoinGamePacket) {
                ServerJoinGamePacket p = (ServerJoinGamePacket) packet;
                iter.set(new ServerJoinGamePacket(p.getEntityId(), p.getHardcore(), gameMode, dimension, difficulty, p.getMaxPlayers(), worldType, reducedDebugInfo));
            }

            // Drop all previous worlds
            if (worldChanged
                    && (packet instanceof ServerRespawnPacket
                    || packet instanceof ServerBlockBreakAnimPacket
                    || packet instanceof ServerBlockChangePacket
                    || packet instanceof ServerBlockValuePacket
                    || packet instanceof ServerChunkDataPacket
                    || packet instanceof ServerExplosionPacket
                    || packet instanceof ServerMapDataPacket
                    || packet instanceof ServerMultiBlockChangePacket
                    || packet instanceof ServerMultiChunkDataPacket
                    || packet instanceof ServerOpenTileEntityEditorPacket
                    || packet instanceof ServerPlayEffectPacket
                    || packet instanceof ServerPlaySoundPacket
                    || packet instanceof ServerSpawnParticlePacket
                    || packet instanceof ServerSpawnPositionPacket
                    || packet instanceof ServerUpdateSignPacket
                    || packet instanceof ServerUpdateTileEntityPacket
                    || packet instanceof ServerUpdateTimePacket
                    || packet instanceof ServerWorldBorderPacket)) {
                iter.remove();
                continue PACKETS;
            }

            if (packet instanceof ServerRespawnPacket) {
                worldChanged = true;
                ServerRespawnPacket p = (ServerRespawnPacket) packet;
                dimension = p.getDimension();
                difficulty = p.getDifficulty();
                worldType = p.getWorldType();
                if (gameMode == null) {
                    gameMode = p.getGameMode();
                }
                if (joinGamePacket) {
                    // We can skip every world change and just need to keep the first one
                    iter.remove();
                }
                continue PACKETS;
            }

            // Entities
            if (packet instanceof ServerDestroyEntitiesPacket) {
                // Entities destroyed -> we can drop all their previous actions
                for (int id : ((ServerDestroyEntitiesPacket) packet).getEntityIds()) {
                    redundantEntities.add(id);
                }
            }

            Integer entityId = PacketUtils.getEntityId(packet);
            if (entityId != null) {
                if (worldChanged) { // World has changed -> entity packets are redundant
                    iter.remove();
                    continue PACKETS;
                }
                // Some entity is associated with this packet
                if (entityId == -1) { // Multiple entities in fact
                    for (int id : PacketUtils.getEntityIds(packet)) {
                        if (redundantEntities.contains(entityId)) {
                            iter.remove();
                            continue PACKETS;
                        }
                    }
                } else { // Only one entity
                    if (redundantEntities.contains(entityId)) {
                        iter.remove();
                        continue;
                    }
                }
            }
        }

        // Create a new replay part with all packets
        return studio.createReplayPart(Collections2.transform(packets, (p) -> Pair.of(0L, p)));
    }

}
