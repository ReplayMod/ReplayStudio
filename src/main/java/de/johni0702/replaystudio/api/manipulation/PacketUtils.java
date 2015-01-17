package de.johni0702.replaystudio.api.manipulation;

import com.google.common.primitives.Ints;
import org.spacehq.mc.protocol.packet.ingame.server.ServerCombatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSwitchCameraPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerUseBedPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.*;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import org.spacehq.packetlib.packet.Packet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contains utilities for working with packets.
 */
public class PacketUtils {

    /**
     * Returns the entity id in the specified packet.
     * If no entity is associated with the packet this returns {@code null}.
     * If multiple entities are associated with the packet this returns {@code -1},
     * use {@link #getEntityIds(org.spacehq.packetlib.packet.Packet)} in that case.
     * @return Entity id or {@code null}
     */
    public static Integer getEntityId(Packet packet) {
        if (packet instanceof ServerPlayerUseBedPacket) {
            return ((ServerPlayerUseBedPacket) packet).getEntityId();
        }
        if (packet instanceof ServerSpawnExpOrbPacket) {
            return ((ServerSpawnExpOrbPacket) packet).getEntityId();
        }
        if (packet instanceof ServerSpawnGlobalEntityPacket) {
            return ((ServerSpawnGlobalEntityPacket) packet).getEntityId();
        }
        if (packet instanceof ServerSpawnMobPacket) {
            return ((ServerSpawnMobPacket) packet).getEntityId();
        }
        if (packet instanceof ServerSpawnObjectPacket) {
            return ((ServerSpawnObjectPacket) packet).getEntityId();
        }
        if (packet instanceof ServerSpawnPaintingPacket) {
            return ((ServerSpawnPaintingPacket) packet).getEntityId();
        }
        if (packet instanceof ServerSpawnPlayerPacket) {
            return ((ServerSpawnPlayerPacket) packet).getEntityId();
        }
        if (packet instanceof ServerAnimationPacket) {
            return ((ServerAnimationPacket) packet).getEntityId();
        }
        if (packet instanceof ServerCollectItemPacket) {
            return -1;
        }
        if (packet instanceof ServerDestroyEntitiesPacket) {
            return -1;
        }
        if (packet instanceof ServerEntityAttachPacket) {
            return -1;
        }
        if (packet instanceof ServerEntityEffectPacket) {
            return ((ServerEntityEffectPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityEquipmentPacket) {
            return ((ServerEntityEquipmentPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityHeadLookPacket) {
            return ((ServerEntityHeadLookPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityMetadataPacket) {
            return ((ServerEntityMetadataPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityNBTUpdatePacket) {
            return ((ServerEntityNBTUpdatePacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityPropertiesPacket) {
            return ((ServerEntityPropertiesPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityRemoveEffectPacket) {
            return ((ServerEntityRemoveEffectPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityStatusPacket) {
            return ((ServerEntityStatusPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityTeleportPacket) {
            return ((ServerEntityTeleportPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityVelocityPacket) {
            return ((ServerEntityVelocityPacket) packet).getEntityId();
        }
        if (packet instanceof ServerBlockBreakAnimPacket) {
            return ((ServerBlockBreakAnimPacket) packet).getBreakerEntityId();
        }
        if (packet instanceof ServerCombatPacket) {
            return -1;
        }
        if (packet instanceof ServerSwitchCameraPacket) {
            return ((ServerSwitchCameraPacket) packet).getCameraEntityId();
        }
        return null;
    }

    /**
     * Returns entity ids in the specified packet.
     * If no entity is associated with the packet this returns an empty list.
     * @return List of entity ids
     */
    public static List<Integer> getEntityIds(Packet packet) {
        if (packet instanceof ServerCollectItemPacket) {
            ServerCollectItemPacket p = (ServerCollectItemPacket) packet;
            return Arrays.asList(p.getCollectedEntityId(), p.getCollectorEntityId());
        }
        if (packet instanceof ServerDestroyEntitiesPacket) {
            return Ints.asList(((ServerDestroyEntitiesPacket) packet).getEntityIds());
        }
        if (packet instanceof ServerEntityAttachPacket) {
            ServerEntityAttachPacket p = (ServerEntityAttachPacket) packet;
            return Arrays.asList(p.getEntityId(), p.getAttachedToId());
        }
        if (packet instanceof ServerCombatPacket) {
            ServerCombatPacket p = (ServerCombatPacket) packet;
            return Arrays.asList(p.getEntityId(), p.getPlayerId());
        }
        Integer id = getEntityId(packet);
        if (id == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(id);
        }
    }

}
