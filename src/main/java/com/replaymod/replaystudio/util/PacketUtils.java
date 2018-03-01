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

import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerCombatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerSwitchCameraPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAttachPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerUseBedPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnGlobalEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.replaymod.replaystudio.Studio;

//#if MC>=10904
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAnimationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityCollectItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
//#else
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerAnimationPacket;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerCollectItemPacket;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityNBTUpdatePacket;
//#endif

import java.util.*;
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
        //#if MC>=10904
        studio.setParsing(ServerEntityAnimationPacket.class, true);
        studio.setParsing(ServerEntityCollectItemPacket.class, true);
        studio.setParsing(ServerEntityDestroyPacket.class, true);
        //#else
        //$$ studio.setParsing(ServerAnimationPacket.class, true);
        //$$ studio.setParsing(ServerCollectItemPacket.class, true);
        //$$ studio.setParsing(ServerDestroyEntitiesPacket.class, true);
        //#endif
        studio.setParsing(ServerEntityAttachPacket.class, true);
        studio.setParsing(ServerEntityEffectPacket.class, true);
        studio.setParsing(ServerEntityEquipmentPacket.class, true);
        studio.setParsing(ServerEntityHeadLookPacket.class, true);
        studio.setParsing(ServerEntityMetadataPacket.class, true);
        studio.setParsing(ServerEntityMovementPacket.class, true);
        studio.setParsing(ServerEntityPositionRotationPacket.class, true);
        studio.setParsing(ServerEntityPositionPacket.class, true);
        studio.setParsing(ServerEntityRotationPacket.class, true);
        //#if MC>=10904
        studio.setParsing(ServerEntityPropertiesPacket.class, true);
        studio.setParsing(ServerEntityRemoveEffectPacket.class, true);
        studio.setParsing(ServerEntitySetPassengersPacket.class, true);
        //#else
        //$$ studio.setParsing(ServerEntityNBTUpdatePacket.class, true);
        //$$ studio.setParsing(ServerEntityPropertiesPacket.class, true);
        //$$ studio.setParsing(ServerEntityRemoveEffectPacket.class, true);
        //#endif
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
     * use {@link #getEntityIds(Packet)})} in that case.
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
        //#if MC>=10904
        if (packet instanceof ServerEntityAnimationPacket) {
            return ((ServerEntityAnimationPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityCollectItemPacket) {
            return -1;
        }
        if (packet instanceof ServerEntityDestroyPacket) {
            return -1;
        }
        //#else
        //$$ if (packet instanceof ServerAnimationPacket) {
        //$$     return ((ServerAnimationPacket) packet).getEntityId();
        //$$ }
        //$$ if (packet instanceof ServerCollectItemPacket) {
        //$$     return -1;
        //$$ }
        //$$ if (packet instanceof ServerDestroyEntitiesPacket) {
        //$$     return -1;
        //$$ }
        //#endif
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
        //#if MC<10904
        //$$ if (packet instanceof ServerEntityNBTUpdatePacket) {
        //$$     return ((ServerEntityNBTUpdatePacket) packet).getEntityId();
        //$$ }
        //#endif
        if (packet instanceof ServerEntityPropertiesPacket) {
            return ((ServerEntityPropertiesPacket) packet).getEntityId();
        }
        if (packet instanceof ServerEntityRemoveEffectPacket) {
            return ((ServerEntityRemoveEffectPacket) packet).getEntityId();
        }
        //#if MC>=10904
        if (packet instanceof ServerEntitySetPassengersPacket) {
            return -1;
        }
        //#endif
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
        //#if MC>=10904
        if (packet instanceof ServerEntityCollectItemPacket) {
            ServerEntityCollectItemPacket p = (ServerEntityCollectItemPacket) packet;
            return Arrays.asList(p.getCollectedEntityId(), p.getCollectorEntityId());
        }
        if (packet instanceof ServerEntityDestroyPacket) {
            return Ints.asList(((ServerEntityDestroyPacket) packet).getEntityIds());
        }
        //#else
        //$$ if (packet instanceof ServerCollectItemPacket) {
        //$$     ServerCollectItemPacket p = (ServerCollectItemPacket) packet;
        //$$     return Arrays.asList(p.getCollectedEntityId(), p.getCollectorEntityId());
        //$$ }
        //$$ if (packet instanceof ServerDestroyEntitiesPacket) {
        //$$     return Ints.asList(((ServerDestroyEntitiesPacket) packet).getEntityIds());
        //$$ }
        //#endif
        if (packet instanceof ServerEntityAttachPacket) {
            ServerEntityAttachPacket p = (ServerEntityAttachPacket) packet;
            return Arrays.asList(p.getEntityId(), p.getAttachedToId());
        }
        //#if MC>=10904
        if (packet instanceof ServerEntitySetPassengersPacket) {
            ServerEntitySetPassengersPacket p = (ServerEntitySetPassengersPacket) packet;
            List<Integer> list = new ArrayList<>(Ints.asList(p.getPassengerIds()));
            list.add(p.getEntityId());
            return list;
        }
        //#endif
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
    //#if MC>=10904
    public static ServerPlayerPositionRotationPacket toServerPlayerPositionRotationPacket(Location loc, int teleportId) {
        return new ServerPlayerPositionRotationPacket(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), teleportId);
    }
    //#else
    //$$ public static ServerPlayerPositionRotationPacket toServerPlayerPositionRotationPacket(Location loc) {
    //$$     return new ServerPlayerPositionRotationPacket(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    //$$ }
    //#endif
}
