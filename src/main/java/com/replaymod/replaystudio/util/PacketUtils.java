package com.replaymod.replaystudio.util;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.replaymod.replaystudio.Studio;
import org.spacehq.mc.protocol.data.game.values.entity.player.PositionElement;
import org.spacehq.mc.protocol.packet.ingame.server.ServerCombatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSwitchCameraPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerUseBedPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.*;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import org.spacehq.packetlib.packet.Packet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Contains utilities for working with packets.
 */
public class PacketUtils {

    @SuppressWarnings("unchecked")
    public static final Set<Class<? extends Packet>> MOVEMENT_RELATED = Collections.unmodifiableSet(Sets.newHashSet(
            ServerPlayerPositionRotationPacket.class,
            ServerSpawnPlayerPacket.class,
            ServerSpawnObjectPacket.class,
            ServerSpawnMobPacket.class,
            ServerSpawnPaintingPacket.class,
            ServerSpawnExpOrbPacket.class,
            ServerEntityMovementPacket.class,
            ServerEntityPositionRotationPacket.class,
            ServerEntityPositionPacket.class,
            ServerEntityTeleportPacket.class
    ));

    /**
     * Registers all packets which contain entity ids necessary for the getEntityId(s) methods.
     * @param studio The studio
     */
    public static void registerAllMovementRelated(Studio studio) {
        MOVEMENT_RELATED.forEach(c -> studio.setParsing(c, true));
    }

    /**
     * Registers all packets which contain entity ids necessary for the getEntityId(s) methods.
     * @param studio The studio
     */
    public static void registerAllEntityRelated(Studio studio) {
        studio.setParsing(ServerPlayerUseBedPacket.class, true);
        studio.setParsing(ServerSpawnExpOrbPacket.class, true);
        studio.setParsing(ServerSpawnGlobalEntityPacket.class, true);
        studio.setParsing(ServerSpawnMobPacket.class, true);
        studio.setParsing(ServerSpawnObjectPacket.class, true);
        studio.setParsing(ServerSpawnPaintingPacket.class, true);
        studio.setParsing(ServerSpawnPlayerPacket.class, true);
        studio.setParsing(ServerAnimationPacket.class, true);
        studio.setParsing(ServerCollectItemPacket.class, true);
        studio.setParsing(ServerDestroyEntitiesPacket.class, true);
        studio.setParsing(ServerEntityAttachPacket.class, true);
        studio.setParsing(ServerEntityEffectPacket.class, true);
        studio.setParsing(ServerEntityEquipmentPacket.class, true);
        studio.setParsing(ServerEntityHeadLookPacket.class, true);
        studio.setParsing(ServerEntityMetadataPacket.class, true);
        studio.setParsing(ServerEntityMovementPacket.class, true);
        studio.setParsing(ServerEntityPositionRotationPacket.class, true);
        studio.setParsing(ServerEntityPositionPacket.class, true);
        studio.setParsing(ServerEntityRotationPacket.class, true);
        studio.setParsing(ServerEntityNBTUpdatePacket.class, true);
        studio.setParsing(ServerEntityPropertiesPacket.class, true);
        studio.setParsing(ServerEntityRemoveEffectPacket.class, true);
        studio.setParsing(ServerEntityStatusPacket.class, true);
        studio.setParsing(ServerEntityTeleportPacket.class, true);
        studio.setParsing(ServerEntityVelocityPacket.class, true);
        studio.setParsing(ServerBlockBreakAnimPacket.class, true);
        studio.setParsing(ServerCombatPacket.class, true);
        studio.setParsing(ServerSwitchCameraPacket.class, true);
    }

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
        if (packet instanceof ServerEntityMovementPacket) {
            return ((ServerEntityMovementPacket) packet).getEntityId();
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

    /**
     * Update a location with the movement data in the specified packet.
     * @param loc The location
     * @param packet The packet
     * @return The new location
     */
    public static Location updateLocation(Location loc, ServerEntityMovementPacket packet) {
        if (loc == null) {
            loc = Location.NULL;
        }
        boolean pos = packet instanceof ServerEntityPositionPacket || packet instanceof ServerEntityPositionRotationPacket;
        boolean rot = packet instanceof ServerEntityRotationPacket || packet instanceof ServerEntityPositionRotationPacket;
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        if (pos) {
            x += packet.getMovementX();
            y += packet.getMovementY();
            z += packet.getMovementZ();
        }
        float yaw = rot ? packet.getYaw() : loc.getYaw();
        float pitch = rot ? packet.getPitch() : loc.getPitch();

        return new Location(x, y, z, yaw, pitch);
    }


    /**
     * Update a location with the movement data in the specified packet.
     * @param loc The location
     * @param packet The packet
     * @return The new location
     */
    public static Location updateLocation(Location loc, ServerPlayerPositionRotationPacket packet) {
        if (loc == null) {
            loc = Location.NULL;
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();

        List<PositionElement> relative = packet.getRelativeElements();
        x = (relative.contains(PositionElement.X) ? x : 0) + packet.getX();
        y = (relative.contains(PositionElement.Y) ? y : 0) + packet.getY();
        z = (relative.contains(PositionElement.Z) ? z : 0) + packet.getZ();
        yaw = (relative.contains(PositionElement.YAW) ? yaw : 0) + packet.getYaw();
        pitch = (relative.contains(PositionElement.PITCH) ? pitch : 0) + packet.getPitch();

        return new Location(x, y, z, yaw, pitch);
    }

    /**
     * Update (or initialize) a location with the movement (or spawn) data in the specified packet.
     * @param loc The location (may be {@code null} in case of spawn or absolute movement packets)
     * @param packet The packet
     * @return The new location or {@code null} when the packet could not be handled
     */
    public static Location updateLocation(Location loc, Packet packet) {
        if (packet instanceof ServerPlayerPositionRotationPacket) {
            return updateLocation(loc, (ServerPlayerPositionRotationPacket) packet);
        }

        if (packet instanceof ServerSpawnPlayerPacket) {
            ServerSpawnPlayerPacket p = (ServerSpawnPlayerPacket) packet;
            return new Location(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
        }

        if (packet instanceof ServerSpawnObjectPacket) {
            ServerSpawnObjectPacket p = (ServerSpawnObjectPacket) packet;
            return new Location(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
        }

        if (packet instanceof ServerSpawnExpOrbPacket) {
            ServerSpawnExpOrbPacket p = (ServerSpawnExpOrbPacket) packet;
            return new Location(p.getX(), p.getY(), p.getZ(), 0, 0);
        }

        if (packet instanceof ServerSpawnMobPacket) {
            ServerSpawnMobPacket p = (ServerSpawnMobPacket) packet;
            return new Location(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
        }

        if (packet instanceof ServerSpawnPaintingPacket) {
            ServerSpawnPaintingPacket p = (ServerSpawnPaintingPacket) packet;
            return new Location(p.getPosition().getX(), p.getPosition().getY(), p.getPosition().getZ(), 0, 0);
        }

        if (packet instanceof ServerEntityMovementPacket) {
            return updateLocation(loc, (ServerEntityMovementPacket) packet);
        }

        if (packet instanceof ServerEntityTeleportPacket) {
            ServerEntityTeleportPacket p = (ServerEntityTeleportPacket) packet;
            return new Location(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
        }

        return null;
    }

    /**
     * Creates a new ServerPlayerPositionRotationPacket from the specified location.
     * @param loc The location
     * @return The packet
     */
    public static ServerPlayerPositionRotationPacket toServerPlayerPositionRotationPacket(Location loc) {
        return new ServerPlayerPositionRotationPacket(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }
}
