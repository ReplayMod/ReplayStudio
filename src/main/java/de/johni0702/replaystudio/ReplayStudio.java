package de.johni0702.replaystudio;

import de.johni0702.replaystudio.api.Replay;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.Studio;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.packetlib.packet.Packet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ReplayStudio implements Studio {

    @Override
    public ReplayPart squash(ReplayPart part) {
        return ReplaySquasher.squash(this, part);
    }

    @Override
    public ReplayPart createReplayPart() {
        return new StudioReplayPart(new LinkedList<>());
    }

    @Override
    public ReplayPart createReplayPart(Collection<Pair<Long, Packet>> packets) {
        return new StudioReplayPart(new LinkedList<>(packets));
    }

    @Override
    public Replay createReplay(InputStream in) throws IOException {
        return createReplay(in, false);
    }

    @Override
    public Replay createReplay(InputStream in, boolean raw) throws IOException {
        in = new BufferedInputStream(in);
        if (raw) {
            return new StudioReplay(in);
        } else {
            ZipInputStream zipIn = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if ("recording.tmcpr".equals(entry.getName())) {
                    return new StudioReplay(zipIn);
                }
            }
            throw new IOException("ZipInputStream did not contain \"recording.tmcpr\"");
        }
    }

    @Override
    public Replay createReplay(ReplayPart part) throws IOException {
        return new StudioDelegatingReplay(null, part);
    }

}
