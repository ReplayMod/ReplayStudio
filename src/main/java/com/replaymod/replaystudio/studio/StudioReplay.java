/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.studio;

import com.google.common.base.Optional;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.PacketList;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;

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

    public StudioReplay(Studio studio, InputStream in, int fileformatversion) throws IOException {
        this(studio, ReplayInputStream.readPackets(studio, in, fileformatversion));
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
