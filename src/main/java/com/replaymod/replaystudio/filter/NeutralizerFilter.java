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

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnGlobalEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.util.PacketUtils;

//#if MC>=10904
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
//#else
//$$ import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
//#endif

//#if MC>=10800
import com.github.steveice10.mc.protocol.data.game.scoreboard.ObjectiveAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamAction;
//#endif

import java.util.HashSet;
import java.util.Set;

/**
 * (Stream) filter which appends packets to the end of the stream/replay in such a way that all entities, teams and
 * objectives get reset.
 */
public class NeutralizerFilter extends StreamFilterBase {

    private Set<Integer> entities;
    private Set<String> teams;
    private Set<String> scoreboards;

    @Override
    public String getName() {
        return "neutralizer";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        studio.setParsing(ServerSpawnObjectPacket.class, true);
        studio.setParsing(ServerSpawnExpOrbPacket.class, true);
        studio.setParsing(ServerSpawnGlobalEntityPacket.class, true);
        studio.setParsing(ServerSpawnMobPacket.class, true);
        //#if MC>=10904
        studio.setParsing(ServerEntityDestroyPacket.class, true);
        //#else
        //$$ studio.setParsing(ServerDestroyEntitiesPacket.class, true);
        //#endif
        studio.setParsing(ServerTeamPacket.class, true);
        studio.setParsing(ServerScoreboardObjectivePacket.class, true);
    }

    @Override
    public void onStart(PacketStream stream) {
        this.entities = new HashSet<>();
        this.teams = new HashSet<>();
        this.scoreboards = new HashSet<>();
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData packetData) {
        Packet packet = packetData.getPacket();

        if (packet instanceof ServerSpawnObjectPacket
                || packet instanceof ServerSpawnExpOrbPacket
                || packet instanceof ServerSpawnGlobalEntityPacket
                || packet instanceof ServerSpawnMobPacket
                || packet instanceof ServerSpawnPlayerPacket) {
            entities.add(PacketUtils.getEntityId(packet));
        }

        //#if MC>=10904
        if (packet instanceof ServerEntityDestroyPacket) {
            entities.removeAll(Ints.asList(((ServerEntityDestroyPacket) packet).getEntityIds()));
        }
        //#else
        //$$ if (packet instanceof ServerDestroyEntitiesPacket) {
        //$$     entities.removeAll(Ints.asList(((ServerDestroyEntitiesPacket) packet).getEntityIds()));
        //$$ }
        //#endif

        if (packet instanceof ServerTeamPacket) {
            ServerTeamPacket p = (ServerTeamPacket) packet;
            //#if MC>=10800
            if (p.getAction() == TeamAction.REMOVE) {
            //#else
            //$$ if (p.getAction() == ServerTeamPacket.Action.REMOVE) {
            //#endif
                teams.remove(p.getTeamName());
            } else {
                teams.add(p.getTeamName());
            }
        }

        if (packet instanceof ServerScoreboardObjectivePacket) {
            ServerScoreboardObjectivePacket p = (ServerScoreboardObjectivePacket) packet;
            //#if MC>=10800
            if (p.getAction() == ObjectiveAction.REMOVE) {
            //#else
            //$$ if (p.getAction() == ServerScoreboardObjectivePacket.Action.REMOVE) {
            //#endif
                scoreboards.remove(p.getName());
            } else {
                scoreboards.add(p.getName());
            }
        }
        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
        if (!entities.isEmpty()) {
            //#if MC>=10904
            stream.insert(new PacketData(timestamp, new ServerEntityDestroyPacket(Ints.toArray(entities))));
            //#else
            //$$ stream.insert(new PacketData(timestamp, new ServerDestroyEntitiesPacket(Ints.toArray(entities))));
            //#endif
        }

        for (String team : teams) {
            stream.insert(new PacketData(timestamp, new ServerTeamPacket(team)));
        }

        for (String scoreboard : scoreboards) {
            stream.insert(new PacketData(timestamp, new ServerScoreboardObjectivePacket(scoreboard
                    //#if MC<=10710
                    //$$ , "", ServerScoreboardObjectivePacket.Action.REMOVE
                    //#endif
            )));
        }
    }

    /**
     * Applies this filter to a replay part.
     * @param part The replay part
     * @return The modified replay part. (No copying is performed)
     * @deprecated {@link ReplayPart} is deprecated.
     */
    @Deprecated
    public static ReplayPart neutralize(ReplayPart part) {
        return new NeutralizerFilter().apply(part);
    }

}
