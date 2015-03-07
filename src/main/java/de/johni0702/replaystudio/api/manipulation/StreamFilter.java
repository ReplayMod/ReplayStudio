package de.johni0702.replaystudio.api.manipulation;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.api.packet.PacketStream;

/**
 * A manipulation that applies some effect onto the supplied packet stream on the fly.
 */
public interface StreamFilter {

    /**
     * Returns a unique but simple name for this filter. This name is used when referring to the filter
     * in configs and in {@link de.johni0702.replaystudio.api.Studio#loadStreamFilter(String)}.
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
