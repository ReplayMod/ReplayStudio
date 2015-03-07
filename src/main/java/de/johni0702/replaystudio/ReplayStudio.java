package de.johni0702.replaystudio;

import com.google.gson.Gson;
import de.johni0702.replaystudio.api.Replay;
import de.johni0702.replaystudio.api.ReplayMetaData;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.manipulation.Filter;
import de.johni0702.replaystudio.api.manipulation.StreamFilter;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.api.packet.PacketList;
import de.johni0702.replaystudio.api.packet.PacketStream;
import lombok.Getter;
import lombok.Setter;
import org.spacehq.mc.protocol.packet.ingame.server.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerUseBedPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerSetExperiencePacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.*;
import org.spacehq.mc.protocol.packet.ingame.server.world.*;
import org.spacehq.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import org.spacehq.packetlib.packet.Packet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ReplayStudio implements Studio {

    private static final Gson GSON = new Gson();

    private final Set<Class<? extends Packet>> shouldBeParsed = Collections.newSetFromMap(new HashMap<>());

    @Getter
    @Setter
    private boolean wrappingEnabled = true;

    private final ServiceLoader<Filter> filterServiceLoader = ServiceLoader.load(Filter.class);
    private final ServiceLoader<StreamFilter> streamFilterServiceLoader = ServiceLoader.load(StreamFilter.class);

    public ReplayStudio() {
        this(true);
    }

    public ReplayStudio(boolean useSquasher) {
        // Required for importing / exporting
        setParsing(LoginSetCompressionPacket.class, true);
        setParsing(ServerSetCompressionPacket.class, true);
        setParsing(ServerKeepAlivePacket.class, true);

        if (useSquasher) {
            // Packets modified by ReplaySquasher
            setParsing(ServerPlayerPositionRotationPacket.class, true);
            setParsing(ServerPlayerUseBedPacket.class, true);
            setParsing(ServerSetExperiencePacket.class, true);
            setParsing(ServerSpawnExpOrbPacket.class, true);
            setParsing(ServerSpawnGlobalEntityPacket.class, true);
            setParsing(ServerSpawnMobPacket.class, true);
            setParsing(ServerSpawnObjectPacket.class, true);
            setParsing(ServerSpawnPaintingPacket.class, true);
            setParsing(ServerSpawnPlayerPacket.class, true);
            setParsing(ServerAnimationPacket.class, true);
            setParsing(ServerCollectItemPacket.class, true);
            setParsing(ServerDestroyEntitiesPacket.class, true);
            setParsing(ServerEntityAttachPacket.class, true);
            setParsing(ServerEntityEffectPacket.class, true);
            setParsing(ServerEntityEquipmentPacket.class, true);
            setParsing(ServerEntityHeadLookPacket.class, true);
            setParsing(ServerEntityMetadataPacket.class, true);
            setParsing(ServerEntityMovementPacket.class, true);
            setParsing(ServerEntityNBTUpdatePacket.class, true);
            setParsing(ServerEntityPositionPacket.class, true);
            setParsing(ServerEntityPositionRotationPacket.class, true);
            setParsing(ServerEntityPropertiesPacket.class, true);
            setParsing(ServerEntityRemoveEffectPacket.class, true);
            setParsing(ServerEntityRotationPacket.class, true);
            setParsing(ServerEntityStatusPacket.class, true);
            setParsing(ServerEntityTeleportPacket.class, true);
            setParsing(ServerEntityVelocityPacket.class, true);
            setParsing(ServerBlockBreakAnimPacket.class, true);
            setParsing(ServerCombatPacket.class, true);
            setParsing(ServerJoinGamePacket.class, true);
            setParsing(ServerPlayerPositionRotationPacket.class, true);
            setParsing(ServerNotifyClientPacket.class, true);
            setParsing(ServerDifficultyPacket.class, true);
            setParsing(ServerRespawnPacket.class, true);
            setParsing(ServerBlockChangePacket.class, true);
            setParsing(ServerBlockValuePacket.class, true);
            setParsing(ServerChunkDataPacket.class, true);
            setParsing(ServerExplosionPacket.class, true);
            setParsing(ServerMapDataPacket.class, true);
            setParsing(ServerMultiBlockChangePacket.class, true);
//            setParsing(ServerMultiChunkDataPacket.class, true);
            setParsing(ServerOpenTileEntityEditorPacket.class, true);
            setParsing(ServerPlayEffectPacket.class, true);
            setParsing(ServerPlaySoundPacket.class, true);
            setParsing(ServerSpawnParticlePacket.class, true);
            setParsing(ServerSpawnPositionPacket.class, true);
//            setParsing(ServerUpdateSignPacket.class, true);
            setParsing(ServerPlayerUseBedPacket.class, true);
            setParsing(ServerUpdateTileEntityPacket.class, true);
            setParsing(ServerUpdateTimePacket.class, true);
            setParsing(ServerWorldBorderPacket.class, true);
        }
    }

    @Override
    public String getName() {
        return "ReplayStudio";
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public Filter loadFilter(String name) {
        for (Filter filter : filterServiceLoader) {
            if (filter.getName().equalsIgnoreCase(name)) {
                try {
                    // Create a new instance of the filter
                    return filter.getClass().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @Override
    public StreamFilter loadStreamFilter(String name) {
        for (StreamFilter filter : streamFilterServiceLoader) {
            if (filter.getName().equalsIgnoreCase(name)) {
                try {
                    // Create a new instance of the filter
                    return filter.getClass().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @Override
    public ReplayPart squash(ReplayPart part) {
        return ReplaySquasher.squash(this, part);
    }

    @Override
    public ReplayPart createReplayPart() {
        return new StudioReplayPart(new PacketList());
    }

    @Override
    public ReplayPart createReplayPart(Collection<PacketData> packets) {
        return new StudioReplayPart(new PacketList(packets));
    }

    @Override
    public Replay createReplay(InputStream in) throws IOException {
        return createReplay(in, false);
    }

    @Override
    public Replay createReplay(InputStream in, boolean raw) throws IOException {
        if (raw) {
            return new StudioReplay(this, in);
        } else {
            ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
            ZipEntry entry;
            Replay replay = null;
            ReplayMetaData meta = null;
            while ((entry = zipIn.getNextEntry()) != null) {
                if ("metaData.json".equals(entry.getName())) {
                    meta = GSON.fromJson(new InputStreamReader(zipIn), ReplayMetaData.class);
                }
                if ("recording.tmcpr".equals(entry.getName())) {
                    replay = new StudioReplay(this, zipIn);
                }
            }
            if (replay != null) {
                if (meta != null) {
                    replay.setMetaData(meta);
                }
                return replay;
            } else {
                throw new IOException("ZipInputStream did not contain \"recording.tmcpr\"");
            }
        }
    }

    @Override
    public ReplayMetaData readReplayMetaData(InputStream in) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            if ("metaData.json".equals(entry.getName())) {
                return GSON.fromJson(new InputStreamReader(zipIn), ReplayMetaData.class);
            }
        }
        throw new IOException("ZipInputStream did not contain \"metaData.json\"");
    }

    @Override
    public Replay createReplay(ReplayPart part) {
        return new StudioDelegatingReplay(this, part);
    }

    @Override
    public PacketStream createReplayStream(InputStream in, boolean raw) throws IOException {
        if (raw) {
            return new StudioPacketStream(this, in);
        } else {
            ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if ("recording.tmcpr".equals(entry.getName())) {
                    return new StudioPacketStream(this, zipIn);
                }
            }
            throw new IOException("ZipInputStream did not contain \"recording.tmcpr\"");
        }
    }

    @Override
    public void setParsing(Class<? extends Packet> packetClass, boolean parse) {
        if (parse) {
            shouldBeParsed.add(packetClass);
        } else {
            shouldBeParsed.remove(packetClass);
        }
    }

    @Override
    public boolean willBeParsed(Class<? extends Packet> packetClass) {
        return shouldBeParsed.contains(packetClass);
    }

}
