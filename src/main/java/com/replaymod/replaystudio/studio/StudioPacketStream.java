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

import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.PacketList;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.stream.AbstractPacketStream;

import java.io.IOException;
import java.io.InputStream;

public class StudioPacketStream extends AbstractPacketStream {

    private final ReplayInputStream in;

    public StudioPacketStream(Studio studio, InputStream in, int fileformatversion) {
        this.in = new ReplayInputStream(studio, in, fileformatversion);
    }

    @Override
    protected PacketList nextInput() {
        try {
            return in.readPacket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {

    }

    @Override
    protected void cleanup() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
