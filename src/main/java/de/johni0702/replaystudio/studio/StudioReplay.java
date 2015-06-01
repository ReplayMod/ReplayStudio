package de.johni0702.replaystudio.studio;

import com.google.common.base.Optional;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.collection.PacketList;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.replay.Replay;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ReplayMetaData;

import java.io.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class StudioReplay extends StudioReplayPart implements Replay {

    private ReplayMetaData metaData;

    private final Studio studio;
    private final Optional<ReplayFile> replayFile;

    public StudioReplay(Studio studio, PacketList packets) {
        super(packets);
        this.studio = checkNotNull(studio);
        this.replayFile = Optional.absent();
    }

    public StudioReplay(Studio studio, InputStream in) throws IOException {
        this(studio, ReplayInputStream.readPackets(studio, in));
    }

    public StudioReplay(Studio studio, ReplayFile replayFile) throws IOException {
        super(ReplayInputStream.readPackets(studio, replayFile.getPacketData()));
        this.studio = studio;
        this.replayFile = Optional.of(replayFile);
    }

    @Override
    public void save(File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        if (replayFile.isPresent()) {
            save(replayFile.get().writePacketData(), true);
            replayFile.get().saveTo(file);
        } else {
            save(out, false);
        }
    }

    @Override
    public void save(OutputStream output, boolean raw) throws IOException {
        if (raw) {
            ReplayOutputStream.writePackets(studio, output, this);
        } else {
            ReplayOutputStream.writeReplay(studio, output, this);
        }
    }

    public ReplayMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public Optional<ReplayFile> getReplayFile() {
        return replayFile;
    }

    public void setMetaData(ReplayMetaData metaData) {
        this.metaData = metaData;
    }
}
