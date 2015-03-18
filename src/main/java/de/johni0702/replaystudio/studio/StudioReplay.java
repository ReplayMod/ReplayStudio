package de.johni0702.replaystudio.studio;

import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.collection.PacketList;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.replay.Replay;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import lombok.Getter;
import lombok.Setter;

import java.io.*;

public class StudioReplay extends StudioReplayPart implements Replay {

    @Getter
    @Setter
    private ReplayMetaData metaData;

    private final Studio studio;

    public StudioReplay(Studio studio, PacketList packets) {
        super(packets);
        this.studio = studio;
    }

    public StudioReplay(Studio studio, InputStream in) {
        super(ReplayInputStream.readPackets(studio, in));
        this.studio = studio;
    }

    @Override
    public void save(File file) throws IOException {
        save(new BufferedOutputStream(new FileOutputStream(file)), false);
    }

    @Override
    public void save(OutputStream output, boolean raw) throws IOException {
        if (raw) {
            ReplayOutputStream.writePackets(studio, output, this);
        } else {
            ReplayOutputStream.writeReplay(studio, output, this);
        }
    }

}
