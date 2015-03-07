package de.johni0702.replaystudio.filter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.manipulation.Filter;
import de.johni0702.replaystudio.api.manipulation.PacketUtils;
import de.johni0702.replaystudio.api.packet.PacketData;
import org.spacehq.mc.protocol.data.game.values.entity.MobType;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
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
            if (packet instanceof ServerDestroyEntitiesPacket) {
                ServerDestroyEntitiesPacket p = (ServerDestroyEntitiesPacket) packet;
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
