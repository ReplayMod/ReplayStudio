package de.johni0702.replaystudio.filter;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.api.packet.PacketStream;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerChangeHeldItemPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import org.spacehq.packetlib.packet.Packet;

import java.util.HashMap;
import java.util.Map;

public class ReverseTimeFilter extends MultiFilter {

    private Position spawnPosition;
    private final Map<Class<? extends Packet>, Packet> lastPacket = new HashMap<>();

    @Override
    public void init(Studio studio, JsonObject config) {
        studio.setWrappingEnabled(false);
    }

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        long time = data.getTime();
        Packet packet = data.getPacket();

        if (packet instanceof ServerJoinGamePacket) { // Those packets cannot be reversed
            return false;
        } else if (packet instanceof ServerPluginMessagePacket) { // Do not yet know what to do
            System.out.println(((ServerPluginMessagePacket) packet).getChannel());
        } else if (packet instanceof ServerSpawnPositionPacket  // For these just resend the previous packet
                || packet instanceof ServerPlayerAbilitiesPacket
                || packet instanceof ServerChangeHeldItemPacket) {
            Class<? extends Packet> cls = packet.getClass();
            Packet previous = lastPacket.get(cls);
            if (previous != null) {
                stream.insert(new PacketData(time, previous));
            }
            lastPacket.put(cls, packet);
        } else {
            throw new UnsupportedOperationException("Cannot reverse " + packet);
        }
        return false;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {

    }

    @Override
    public String getName() {
        return "reverse_time";
    }
}
