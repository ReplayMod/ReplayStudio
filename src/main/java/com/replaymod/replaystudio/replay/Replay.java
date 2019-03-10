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
package com.replaymod.replaystudio.replay;

import com.google.common.base.Optional;
import com.replaymod.replaystudio.collection.ReplayPart;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A replay which consist out of multiple packets with their timestamps and some meta data.
 *
 * @deprecated Use {@link ReplayFile} with your standard collection types instead.
 */
@Deprecated
public interface Replay extends ReplayPart {

    /**
     * Returns the meta data for this replay.
     * @return The meta data
     */
    ReplayMetaData getMetaData();

    /**
     * Returns the replay file containing all metadata for this replay.
     * @return The replay file
     */
    Optional<ReplayFile> getReplayFile();

    /**
     * Sets the meta data of this replay.
     * @param metaData The new meta data
     */
    void setMetaData(ReplayMetaData metaData);

    /**
     * Saves this replay to the specified file.
     * @param file The file
     */
    void save(File file) throws IOException;

    /**
     * Saves this replay to the specified output stream.
     * @param output The output stream
     * @param raw If {@code true} only saves the packet recording itself, otherwise also saves metadata
     */
    void save(OutputStream output, boolean raw) throws IOException;

}
