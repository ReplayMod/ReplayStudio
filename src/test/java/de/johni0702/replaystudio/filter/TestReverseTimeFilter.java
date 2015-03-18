package de.johni0702.replaystudio.filter;

import com.google.common.base.Functions;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.mock.MinecraftProtocolMock;
import de.johni0702.replaystudio.mock.PacketStreamMock;
import de.johni0702.replaystudio.mock.SessionMock;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spacehq.mc.protocol.data.game.Chunk;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntry;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntryAction;
import org.spacehq.mc.protocol.data.game.values.entity.Art;
import org.spacehq.mc.protocol.data.game.values.entity.HangingDirection;
import org.spacehq.mc.protocol.data.game.values.entity.MobType;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreboardPosition;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.mc.protocol.data.game.values.world.WorldType;
import org.spacehq.mc.protocol.data.game.values.world.block.BlockChangeRecord;
import org.spacehq.mc.protocol.data.game.values.world.notify.ClientNotification;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerTitlePacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.*;
import org.spacehq.packetlib.packet.Packet;

import java.lang.reflect.Constructor;
import java.util.*;

@RunWith(JUnitParamsRunner.class)
public class TestReverseTimeFilter {

    @Test
    @Parameters
    public void testAllPackets(Packet packet) throws Exception {
        ReverseTimeFilter filter = new ReverseTimeFilter();
        filter.outputWarnings = false;
        filter.onPacket(new PacketStreamMock(), new PacketData(0, packet));
    }
    @SuppressWarnings("unchecked")
    private Map<Class<? extends Packet>, Packet> nonEmptyPackets = new HashMap() {
        {
            put(new ServerSpawnPlayerPacket(0, UUID.randomUUID(), 0, 0, 0, 0, 0, 0, new EntityMetadata[0]));
            put(new ServerSpawnMobPacket(0, MobType.BAT, 0, 0, 0, 0, 0, 0, 0, 0, 0, new EntityMetadata[0]));
            put(new ServerSpawnPaintingPacket(0, Art.BOMB, new Position(0, 0, 0), HangingDirection.NORTH));
            put(new ServerDestroyEntitiesPacket());
            put(new ServerChunkDataPacket(0, 0, new Chunk[16], new byte[0]));
            put(new ServerMultiChunkDataPacket(new int[0], new int[0], new Chunk[0][0], new byte[0][0]));
            put(new ServerBlockChangePacket(new BlockChangeRecord(new Position(0, 0, 0), 0)));
            put(new ServerMultiBlockChangePacket(new BlockChangeRecord(new Position(0, 0, 0), 0)));
            put(new ServerExplosionPacket(0, 0, 0, 0, Collections.emptyList(), 0, 0, 0));
            put(new ServerNotifyClientPacket(ClientNotification.CHANGE_GAMEMODE, GameMode.CREATIVE));
            put(new ServerPlayerListEntryPacket(PlayerListEntryAction.ADD_PLAYER, new PlayerListEntry[0]));
            put(new ServerScoreboardObjectivePacket(""));
            put(new ServerUpdateScorePacket("", ""));
            put(new ServerDisplayScoreboardPacket(ScoreboardPosition.BELOW_NAME, ""));
            put(new ServerTeamPacket(""));
            put(new ServerPluginMessagePacket("", new byte[0]));
            put(new ServerRespawnPacket(0, Difficulty.EASY, GameMode.SPECTATOR, WorldType.AMPLIFIED));
            put(new ServerPlayerPositionRotationPacket(0, 0, 0, 0, 0));
            put(new ServerTitlePacket(true));

        }

        private void put(Packet packet) {
            put(packet.getClass(), packet);
        }
    };
    protected List<Packet> parametersForTestAllPackets() throws Exception {
        List<Class<? extends Packet>> packetClasses = new ArrayList<>();
        new SessionMock<>((session) -> new MinecraftProtocolMock(session, true, (c) -> {
            packetClasses.add(c);
            return c;
        }, Functions.identity()));
        List<Packet> packets = new ArrayList<>(packetClasses.size());
        for (Class<? extends Packet> cls : packetClasses) {
            Packet packet = nonEmptyPackets.get(cls);
            if (packet == null) {
                Constructor<? extends Packet> constructor = cls.getDeclaredConstructor();
                constructor.setAccessible(true);
                packet = constructor.newInstance();
            }
            packets.add(packet);
        }
        return packets;
    }

}
