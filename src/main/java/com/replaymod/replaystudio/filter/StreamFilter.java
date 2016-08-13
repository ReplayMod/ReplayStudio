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
package com.replaymod.replaystudio.filter;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.stream.PacketStream;

/**
 * A manipulation that applies some effect onto the supplied packet stream on the fly.
 */
public interface StreamFilter {

    /**
     * Returns a unique but simple name for this filter. This name is used when referring to the filter
     * in configs and in {@link Studio#loadStreamFilter(String)}.
     * It may not contain whitespace or special characters except underscores.
     * It should be all lowercase, however this is not a requirement.
     * @return Name of this filter
     */
    String getName();

    /**
     * Initializes this filter.
     * Read the configuration of this filter from the supplied json.
     * This can be called multiple times.
     */
    void init(Studio studio, JsonObject config);

    /**
     * Called at the beginning of a new stream.
     * @param stream The stream of packets
     */
    void onStart(PacketStream stream);

    /**
     * Called for each packet traversing the stream.
     * @param stream The stream
     * @param data The packet
     * @return {@code true} if the packet should remain in the stream, {@code false} if it should be removed
     */
    boolean onPacket(PacketStream stream, PacketData data);

    /**
     * Called at the end of a stream.
     * @param stream The stream of packets
     * @param timestamp The current time int this stream in milliseconds
     */
    void onEnd(PacketStream stream, long timestamp);

}
