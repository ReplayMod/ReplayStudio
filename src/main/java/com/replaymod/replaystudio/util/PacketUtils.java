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

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.packets.EntityId;
import com.replaymod.replaystudio.protocol.packets.PacketEntityMovement;
import com.replaymod.replaystudio.protocol.packets.PacketEntityTeleport;
import com.replaymod.replaystudio.protocol.packets.SpawnEntity;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Triple;

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
                DPosition deltaPos = movement.getFirst();
                Pair<Float, Float> yawPitch = movement.getSecond();
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
