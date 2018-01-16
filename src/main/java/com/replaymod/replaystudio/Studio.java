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

import com.github.steveice10.packetlib.packet.Packet;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.filter.Filter;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.PacketStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

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
     * Squash the supplied replay part into a zero second long replay part.
     * Removes all redundant packets (such as spawning an entity which gets removed later)
     * @param part The original part
     * @return A new squashed copy
     */
    ReplayPart squash(ReplayPart part);

    /**
     * Creates a new replay part.
     * @return The created replay part
     */
    ReplayPart createReplayPart();

    /**
     * Creates a new replay part containing the specified packets.
     * @param packets Collection of packets with their timestamp in milliseconds
     * @return The created replay part
     */
    ReplayPart createReplayPart(Collection<PacketData> packets);

    /**
     * Creates a new replay from the specified input stream.
     * @param in The InputStream to read from
     * @return The created replay
     * @deprecated Use {@link ZipReplayFile} instead
     */
    @Deprecated
    Replay createReplay(InputStream in) throws IOException;

    /**
     * Creates a new replay from the specified replay file.
     * @param file The replay file to read from
     * @return The created replay
     */
    Replay createReplay(ReplayFile file) throws IOException;

    /**
     * Creates a new replay from the specified raw input stream.
     * @param in The InputStream to read from
     * @param fileFormatVersion The FileFormatVersion
     * @return The created replay
     */
    Replay createReplay(InputStream in, int fileFormatVersion) throws IOException;

    /**
     * Creates a new replay from the specified input stream.
     * @param in The InputStream to read from
     * @param raw True if {@code in} does not contain meta data but only the packet data itself
     * @return The created replay
     * @deprecated Use {@link #createReplay(InputStream, int)} or {@link #createReplay(InputStream)} instead
     */
    @Deprecated
    Replay createReplay(InputStream in, boolean raw) throws IOException;

    /**
     * Creates a new replay from the specified replay part.
     * @param part The part from which to create the replay
     * @return The created replay
     */
    Replay createReplay(ReplayPart part);

    /**
     * Reads the replay meta data from the specified input.
     * @param in The InputStream to read from
     * @return The replay meta data
     */
    ReplayMetaData readReplayMetaData(InputStream in) throws IOException;

    /**
     * Creates a new packet stream from the specified input stream.
     * @param in The InputStream to read from
     * @param raw True if {@code in} does not contain meta data but only the packet data itself
     * @return The packet stream
     * @deprecated Use {@link ReplayInputStream#asPacketStream()} instead.<br>
     *             If raw, use {@link ReplayInputStream#ReplayInputStream(Studio, InputStream, int)},
     *             otherwise use {@link ZipReplayFile#getPacketData()}
     */
    @Deprecated
    PacketStream createReplayStream(InputStream in, boolean raw) throws IOException;

    /**
     * For increased performance every packet not registered with {@link #setParsing(Class, boolean)} will be
     * wrapped in a packet wrapper instead of being parsed.
     * Disabling this feature can help during development but should rarely be done in production.
     * @param enabled {@code true} to enable wrapping, {@code false} disables packet wrapping
     */
    void setWrappingEnabled(boolean enabled);

    /**
     * Returns whether wrapping is enabled.
     * @return {@code true} if wrapping is enabled, {@code false} otherwise
     * @see #setWrappingEnabled(boolean)
     */
    boolean isWrappingEnabled();

    /**
     * Set whether the specified packet should be parsed.
     * Only parse what's needed in order to maintain optimal performance.
     * @param packetClass Class of the packet
     * @param parse If {@code true} the packets will be parsed
     */
    void setParsing(Class<? extends Packet> packetClass, boolean parse);

    /**
     * Returns whether the specified packet class should be parsed.
     * @param packetClass Class of the packet
     * @return {@code true} if packet of that type will be parsed
     */
    boolean willBeParsed(Class<? extends Packet> packetClass);

    /**
     * Loads a new instance of the specified filter.
     * @param name Name of the filter
     * @return New instance of the filter
     */
    Filter loadFilter(String name);

    /**
     * Loads a new instance of the specified stream filter.
     * @param name Name of the stream filter
     * @return New instance of the stream filter
     */
    StreamFilter loadStreamFilter(String name);


    /**
     * Return whether the specified replay file version can be read (and if necessary be converted to the
     * current version) by this Studio implementation.
     * @param fileVersion The file version
     * @return {@code true} if the specified version is supported, {@code false} otherwise
     */
    boolean isCompatible(int fileVersion);

    /**
     * Returns the file format version of replay files written with this Studio implementation.
     * @return The current file format version
     */
    int getCurrentFileFormatVersion();

}
