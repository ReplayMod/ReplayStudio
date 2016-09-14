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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.util.PacketUtils;
import org.spacehq.mc.protocol.data.game.entity.type.MobType;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import org.spacehq.packetlib.packet.Packet;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RemoveMobsFilter implements Filter {

    private Set<MobType> filterTypes;

    @Override
    public String getName() {
        return "remove_mobs";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        filterTypes = EnumSet.noneOf(MobType.class);
        for (JsonElement e : config.getAsJsonArray("Types")) {
            filterTypes.add(MobType.valueOf(e.getAsString()));
        }
    }

    @Override
    public ReplayPart apply(ReplayPart part) {
        Set<Integer> removedEntities = new HashSet<>();
        for (Iterator<PacketData> iter = part.iterator(); iter.hasNext(); ) {
            Packet packet = iter.next().getPacket();
            if (packet instanceof ServerSpawnMobPacket) {
                ServerSpawnMobPacket p = (ServerSpawnMobPacket) packet;
                if (filterTypes.contains(p.getType())) {
                    removedEntities.add(p.getEntityId());
                }
            }
            if (packet instanceof ServerEntityDestroyPacket) {
                ServerEntityDestroyPacket p = (ServerEntityDestroyPacket) packet;
                for (int id : p.getEntityIds()) {
                    removedEntities.remove(id);
                }
            }
            Integer entityId = PacketUtils.getEntityId(packet);
            if (entityId == null) {
                continue;
            }
            if (entityId == -1) {
                for (int id : PacketUtils.getEntityIds(packet)) {
                    if (removedEntities.contains(id)) {
                        iter.remove();
                        break;
                    }
                }
            } else {
                if (removedEntities.contains(entityId)) {
                    iter.remove();
                }
            }
        }
        return part;
    }

}
