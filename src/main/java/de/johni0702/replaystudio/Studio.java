package de.johni0702.replaystudio;

import de.johni0702.replaystudio.collection.ReplayPart;
import de.johni0702.replaystudio.filter.Filter;
import de.johni0702.replaystudio.filter.StreamFilter;
import de.johni0702.replaystudio.replay.Replay;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import de.johni0702.replaystudio.stream.PacketStream;
import org.spacehq.packetlib.packet.Packet;

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
     */
    Replay createReplay(InputStream in) throws IOException;

    /**
     * Creates a new replay from the specified replay file.
     * @param file The replay file to read from
     * @return The created replay
     */
    Replay createReplay(ReplayFile file) throws IOException;

    /**
     * Creates a new replay from the specified input stream.
     * @param in The InputStream to read from
     * @param raw True if {@code in} does not contain meta data but only the packet data itself
     * @return The created replay
     */
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
     */
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

}
