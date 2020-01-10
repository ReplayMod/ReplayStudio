/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
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
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;

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
            default:
                return null;
        }
    }
}
