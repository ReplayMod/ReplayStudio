package de.johni0702.replaystudio.studio;

import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.collection.ReplayPart;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.replay.Replay;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.io.*;

@AllArgsConstructor
@RequiredArgsConstructor
public class StudioDelegatingReplay implements Replay {

    @Getter
    @Setter
    private ReplayMetaData metaData;

    private final Studio studio;

    @Delegate
    private final ReplayPart delegate;

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
