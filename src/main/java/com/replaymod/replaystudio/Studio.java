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
package com.replaymod.replaystudio;

import com.replaymod.replaystudio.filter.StreamFilter;

public interface Studio {

    /**
     * Returns the name of this implementation.
     * @return The name
     */
    String getName();

    /**
     * Returns the numerical version of this implementation.
     * @return Version number
     */
    int getVersion();

    /**
     * Loads a new instance of the specified stream filter.
     * @param name Name of the stream filter
     * @return New instance of the stream filter
     */
    StreamFilter loadStreamFilter(String name);

    /**
     * Return whether the specified replay and protocol file version can be read (and if necessary be converted to the
     * current version) by this Studio implementation.
     * @param fileVersion The file version
     * @param protocolVersion The MC protocol version
     * @param currentVersion The desired MC protocol version
     * @return {@code true} if the specified version is supported, {@code false} otherwise
     */
    boolean isCompatible(int fileVersion, int protocolVersion, int currentVersion);

    /**
     * Returns the file format version of replay files written with this Studio implementation.
     * @return The current file format version
     */
    int getCurrentFileFormatVersion();

}
