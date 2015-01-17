package de.johni0702.replaystudio;

import de.johni0702.replaystudio.api.Replay;
import de.johni0702.replaystudio.api.ReplayMetaData;
import de.johni0702.replaystudio.io.ReplayReader;
import de.johni0702.replaystudio.io.ReplayWriter;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.packetlib.packet.Packet;

import java.io.*;
import java.util.List;

public class StudioReplay extends StudioReplayPart implements Replay {

    @Getter
    @Setter
    private ReplayMetaData metaData;

    public StudioReplay(List<Pair<Long, Packet>> packets) {
        super(packets);
    }

    public StudioReplay(InputStream in) {
        super(ReplayReader.readPackets(in));
    }

    @Override
    public void save(File file) throws IOException {
        save(new BufferedOutputStream(new FileOutputStream(file)), false);
    }

    @Override
    public void save(OutputStream output, boolean raw) throws IOException {
        ReplayWriter.writeReplay(output, this, raw);
    }

}
