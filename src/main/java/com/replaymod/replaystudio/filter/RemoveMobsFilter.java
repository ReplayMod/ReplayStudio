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

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.util.PacketUtils;

//#if MC>=10904
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
//#else
//#if MC>=10800
//$$ import com.github.steveice10.mc.protocol.data.game.entity.MobType;
//#else
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket.Type;
//#endif
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
//#endif

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class RemoveMobsFilter extends StreamFilterBase {

    //#if MC>=10800
    private Set<MobType> filterTypes;
    //#else
    //$$ private Set<Type> filterTypes;
    //#endif

    private Set<Integer> removedEntities;

    @Override
    public String getName() {
        return "remove_mobs";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        removedEntities = new HashSet<>();

        //#if MC>=10800
        filterTypes = EnumSet.noneOf(MobType.class);
        //#else
        //$$ filterTypes = EnumSet.noneOf(Type.class);
        //#endif
        for (JsonElement e : config.getAsJsonArray("Types")) {
            //#if MC>=10800
            filterTypes.add(MobType.valueOf(e.getAsString()));
            //#else
            //$$ filterTypes.add(Type.valueOf(e.getAsString()));
            //#endif
        }
    }

    @Override
    public void onStart(PacketStream stream) {
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        Packet packet = data.getPacket();
        if (packet instanceof ServerSpawnMobPacket) {
            ServerSpawnMobPacket p = (ServerSpawnMobPacket) packet;
            if (filterTypes.contains(p.getType())) {
                removedEntities.add(p.getEntityId());
            }
        }
        //#if MC>=10904
        if (packet instanceof ServerEntityDestroyPacket) {
            ServerEntityDestroyPacket p = (ServerEntityDestroyPacket) packet;
            //#else
            //$$ if (packet instanceof ServerDestroyEntitiesPacket) {
            //$$     ServerDestroyEntitiesPacket p = (ServerDestroyEntitiesPacket) packet;
            //#endif
            for (int id : p.getEntityIds()) {
                removedEntities.remove(id);
            }
        }
        Integer entityId = PacketUtils.getEntityId(packet);
        if (entityId == null) {
            return true;
        }
        if (entityId == -1) {
            for (int id : PacketUtils.getEntityIds(packet)) {
                if (removedEntities.contains(id)) {
                    return false;
                }
            }
        } else {
            return !removedEntities.contains(entityId);
        }
        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
    }
}
