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
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EntityId {
    public static List<Integer> getEntityIds(Packet packet) throws IOException {
        switch (packet.getType()) {
            case EntityCollectItem: try (Packet.Reader in = packet.reader()) {
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    return Arrays.asList(in.readVarInt(), in.readVarInt());
                } else {
                    return Arrays.asList(in.readInt(), in.readInt());
                }
            }
            case DestroyEntities:
                return PacketDestroyEntities.getEntityIds(packet);
            case SetPassengers: try (Packet.Reader in = packet.reader()) {
                int entityId = in.readVarInt();
                int len = in.readVarInt();
                List<Integer> result = new ArrayList<>(len + 1);
                result.add(entityId);
                for (int i = 0; i < len; i++) {
                    result.add(in.readVarInt());
                }
                return result;
            }
            case EntityAttach: try (Packet.Reader in = packet.reader()) {
                return Arrays.asList(in.readInt(), in.readInt());
            }
            case Combat: try (Packet.Reader in = packet.reader()) {
                int event = in.readVarInt();
                if (event == 1) { // end combat
                    in.readVarInt(); // duration
                    return Collections.singletonList(in.readInt());
                } else if (event == 2) { // entity dead
                    return Arrays.asList(in.readVarInt(), in.readInt());
                }
                return Collections.emptyList();
            }
            case CombatEntityDead: try (Packet.Reader in = packet.reader()) {
                if (packet.atLeast(ProtocolVersion.v1_20)) {
                    return Collections.singletonList(in.readVarInt());
                } else {
                    return Arrays.asList(in.readVarInt(), in.readInt());
                }
            }
            default:
                Integer entityId = getEntityId(packet);
                if (entityId != null) {
                    return Collections.singletonList(entityId);
                } else {
                    return Collections.emptyList();
                }
        }
    }

    public static Integer getEntityId(Packet packet) throws IOException {
        switch (packet.getType()) {
            case OpenHorseWindow: try (Packet.Reader in = packet.reader()) {
                in.readByte();
                in.readVarInt();
                return in.readInt();
            }
            case EntitySoundEffect: try (Packet.Reader in = packet.reader()) {
                in.readVarInt();
                in.readVarInt();
                return in.readVarInt();
            }
            case EntityEffect:
            case EntityRemoveEffect:
            case EntityEquipment:
            case EntityHeadLook:
            case EntityMetadata:
            case EntityMovement:
            case EntityPosition:
            case EntityRotation:
            case EntityPositionRotation:
            case EntityAnimation:
            case EntityNBTUpdate:
            case EntityProperties:
            case EntityTeleport:
            case EntityVelocity:
            case SwitchCamera:
            case PlayerUseBed: try (Packet.Reader in = packet.reader()) {
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    return in.readVarInt();
                } else {
                    return in.readInt();
                }
            }
            case BlockBreakAnim:
            case DestroyEntity:
            case SpawnPlayer:
            case SpawnObject:
            case SpawnPainting:
            case SpawnMob:
            case SpawnGlobalEntity:
            case SpawnExpOrb: try (Packet.Reader in = packet.reader()) {
                return in.readVarInt();
            }
            case EntityStatus: try (Packet.Reader in = packet.reader()) {
                return in.readInt();
            }
            case CombatEnd: try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // duration
                if (packet.atLeast(ProtocolVersion.v1_20)) {
                    return null;
                } else {
                    return in.readInt();
                }
            }
            default:
                return null;
        }
    }
}
