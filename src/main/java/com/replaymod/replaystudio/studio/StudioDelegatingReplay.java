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
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.collection.ReplayPartView;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import org.spacehq.packetlib.packet.Packet;

import java.io.*;
import java.util.Collection;
import java.util.ListIterator;

import static com.google.common.base.Preconditions.checkNotNull;

public class StudioDelegatingReplay implements Replay {

    private ReplayMetaData metaData;

    private final Studio studio;

    private final ReplayPart delegate;

    public StudioDelegatingReplay(Studio studio, ReplayPart delegate) {
        this(studio, delegate, null);
    }

    public StudioDelegatingReplay(Studio studio, ReplayPart delegate, ReplayMetaData metaData) {
        this.studio = checkNotNull(studio);
        this.delegate = checkNotNull(delegate);
        this.metaData = metaData;
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

    public ReplayMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public Optional<ReplayFile> getReplayFile() {
        return Optional.absent();
    }

    public void setMetaData(ReplayMetaData metaData) {
        this.metaData = metaData;
    }

    public ReplayPart copy() {
        return this.delegate.copy();
    }

    public ListIterator<PacketData> iterator() {
        return this.delegate.iterator();
    }

    public void add(long at, Packet packet) {
        this.delegate.add(at, packet);
    }

    public int size() {
        return this.delegate.size();
    }

    public Collection<PacketData> remove(long from, long to) {
        return this.delegate.remove(from, to);
    }

    public ReplayPartView viewOf(long from, long to) {
        return this.delegate.viewOf(from, to);
    }

    public void add(Iterable<PacketData> packets) {
        this.delegate.add(packets);
    }

    public ReplayPart copyOf(long from, long to) {
        return this.delegate.copyOf(from, to);
    }

    public ReplayPartView viewOf(long from) {
        return this.delegate.viewOf(from);
    }

    public long length() {
        return this.delegate.length();
    }

    public ReplayPart append(ReplayPart part) {
        return this.delegate.append(part);
    }

    public void addAt(long offset, Iterable<PacketData> packets) {
        this.delegate.addAt(offset, packets);
    }

    public void add(long at, Iterable<Packet> packets) {
        this.delegate.add(at, packets);
    }

    public ReplayPart copyOf(long from) {
        return this.delegate.copyOf(from);
    }
}
