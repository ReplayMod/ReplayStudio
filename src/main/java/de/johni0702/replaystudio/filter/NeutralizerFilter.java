package de.johni0702.replaystudio.filter;

import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.collection.ReplayPart;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.util.PacketUtils;
import lombok.NonNull;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ObjectiveAction;
import org.spacehq.mc.protocol.data.game.values.scoreboard.TeamAction;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.*;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import org.spacehq.packetlib.packet.Packet;

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
        studio.setParsing(ServerDestroyEntitiesPacket.class, true);
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

        if (packet instanceof ServerDestroyEntitiesPacket) {
            entities.removeAll(Ints.asList(((ServerDestroyEntitiesPacket) packet).getEntityIds()));
        }

        if (packet instanceof ServerTeamPacket) {
            ServerTeamPacket p = (ServerTeamPacket) packet;
            if (p.getAction() == TeamAction.REMOVE) {
                teams.remove(p.getTeamName());
            } else {
                teams.add(p.getTeamName());
            }
        }

        if (packet instanceof ServerScoreboardObjectivePacket) {
            ServerScoreboardObjectivePacket p = (ServerScoreboardObjectivePacket) packet;
            if (p.getAction() == ObjectiveAction.REMOVE) {
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
            stream.insert(new PacketData(timestamp, new ServerDestroyEntitiesPacket(Ints.toArray(entities))));
        }

        for (String team : teams) {
            stream.insert(new PacketData(timestamp, new ServerTeamPacket(team)));
        }

        for (String scoreboard : scoreboards) {
            stream.insert(new PacketData(timestamp, new ServerScoreboardObjectivePacket(scoreboard)));
        }
    }

    /**
     * Applies this filter to a replay part.
     * @param part The replay part
     * @return The modified replay part. (No copying is performed)
     */
    public static ReplayPart neutralize(@NonNull ReplayPart part) {
        return new NeutralizerFilter().apply(part);
    }

}
