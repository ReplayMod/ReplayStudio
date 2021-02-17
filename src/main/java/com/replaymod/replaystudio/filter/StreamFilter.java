/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.filter;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.stream.PacketStream;

import java.io.IOException;

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
    void onStart(PacketStream stream) throws IOException;

    /**
     * Called for each packet traversing the stream.
     * @param stream The stream
     * @param data The packet
     * @return {@code true} if the packet should remain in the stream, {@code false} if it should be removed
     */
    boolean onPacket(PacketStream stream, PacketData data) throws IOException;

    /**
     * Called at the end of a stream.
     * @param stream The stream of packets
     * @param timestamp The current time int this stream in milliseconds
     */
    void onEnd(PacketStream stream, long timestamp) throws IOException;

}
