package de.johni0702.replaystudio.studio;

import com.google.gson.Gson;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.collection.PacketList;
import de.johni0702.replaystudio.collection.ReplayPart;
import de.johni0702.replaystudio.filter.Filter;
import de.johni0702.replaystudio.filter.SquashFilter;
import de.johni0702.replaystudio.filter.StreamFilter;
import de.johni0702.replaystudio.replay.Replay;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import de.johni0702.replaystudio.stream.PacketStream;
import lombok.Getter;
import lombok.Setter;
import org.spacehq.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
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

    /**
     * Whether packets should be wrapped instead of parsed unless they're set to be parsed explicitly.
     */
    @Getter
    @Setter
    private boolean wrappingEnabled = true;

    private final ServiceLoader<Filter> filterServiceLoader = ServiceLoader.load(Filter.class);
    private final ServiceLoader<StreamFilter> streamFilterServiceLoader = ServiceLoader.load(StreamFilter.class);

    public ReplayStudio() {
        // Required for importing / exporting
        setParsing(LoginSetCompressionPacket.class, true);
        setParsing(ServerSetCompressionPacket.class, true);
        setParsing(ServerKeepAlivePacket.class, true);
    }

    @Override
    public String getName() {
        return "ReplayStudio";
    }

    @Override
    public int getVersion() {
        return 1;
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
        part = part.copy();
        new SquashFilter().apply(part);
        return part;
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
