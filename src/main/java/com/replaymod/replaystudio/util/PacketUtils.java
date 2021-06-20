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
package com.replaymod.replaystudio.util;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.packets.EntityId;
import com.replaymod.replaystudio.protocol.packets.PacketEntityMovement;
import com.replaymod.replaystudio.protocol.packets.PacketEntityTeleport;
import com.replaymod.replaystudio.protocol.packets.SpawnEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.List;

/**
 * Contains utilities for working with packets.
 */
public class PacketUtils {

    public static boolean isSpawnEntityPacket(Packet packet) {
        switch (packet.getType()) {
            case SpawnPlayer:
            case SpawnMob:
            case SpawnObject:
            case SpawnExpOrb:
            case SpawnPainting:
            case SpawnGlobalEntity:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the entity id in the specified packet.
     * If no entity is associated with the packet this returns {@code null}.
     * For packets which have multiple entities associated with them use {@link #getEntityIds(Packet)})} instead.
     * @return Entity id or {@code null}
     */
    public static Integer getEntityId(Packet packet) throws IOException {
        return EntityId.getEntityId(packet);
    }

    /**
     * Returns entity ids in the specified packet.
     * If no entity is associated with the packet this returns an empty list.
     * @return List of entity ids
     */
    public static List<Integer> getEntityIds(Packet packet) throws IOException {
        return EntityId.getEntityIds(packet);
    }

    /**
     * Update (or initialize) a location with the movement (or spawn) data in the specified packet.
     * @param loc The location (may be {@code null} in case of spawn or absolute movement packets)
     * @param packet The packet
     * @return The new location or {@code null} when the packet could not be handled
     */
    public static Location updateLocation(Location loc, Packet packet) throws IOException {
        Location spawnLocation = SpawnEntity.getLocation(packet);
        if (spawnLocation != null) {
            return spawnLocation;
        }

        switch (packet.getType()) {
            case EntityMovement:
            case EntityPosition:
            case EntityRotation:
            case EntityPositionRotation:
                if (loc == null) {
                    loc = Location.NULL;
                }
                Triple<DPosition, Pair<Float, Float>, Boolean> movement = PacketEntityMovement.getMovement(packet);
                DPosition deltaPos = movement.getLeft();
                Pair<Float, Float> yawPitch = movement.getMiddle();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();
                if (deltaPos != null) {
                    x += deltaPos.getX();
                    y += deltaPos.getY();
                    z += deltaPos.getZ();
                }
                float yaw = yawPitch != null ? yawPitch.getKey() : loc.getYaw();
                float pitch = yawPitch != null ? yawPitch.getValue() : loc.getPitch();

                return new Location(x, y, z, yaw, pitch);
            case EntityTeleport:
                return PacketEntityTeleport.getLocation(packet);
            default:
                return null;
        }
    }
}
