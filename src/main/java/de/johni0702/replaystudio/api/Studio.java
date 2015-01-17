package de.johni0702.replaystudio.api;

import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.packetlib.packet.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface Studio {

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
     * @param packets Collection of pairs of packets with their timestamp in milliseconds
     * @return The created replay part
     */
    ReplayPart createReplayPart(Collection<Pair<Long, Packet>> packets);

    /**
     * Creates a new replay from the specified input stream.
     * @param in The InputStream to read from
     * @return The created replay
     */
    Replay createReplay(InputStream in) throws IOException;

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

}
