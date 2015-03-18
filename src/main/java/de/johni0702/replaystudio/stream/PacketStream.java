package de.johni0702.replaystudio.stream;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.StreamFilter;
import lombok.Data;
import org.spacehq.packetlib.packet.Packet;

import java.util.Collection;
import java.util.List;

/**
 * Represents a stream of packets.
 */
public interface PacketStream {

    /**
     * Information on the time frame for which a filter should apply and the filter itself.
     */
    @Data
    public static class FilterInfo {
        /**
         * The filter.
         */
        private final StreamFilter filter;

        /**
         * Timestamp (milliseconds) from which this filter should apply (inclusive).
         * A value of -1 means that it should apply right from the start.
         */
        private final long from;

        /**
         * Timestamp (milliseconds) up to which this filter should apply (inclusive).
         * A value of -1 means that it should apply up until the end.
         */
        private final long to;

        /**
         * Returns whether this filter should be applied at the specified timestamp.
         * @param time The timestamp (milliseconds)
         * @return {@code true} if this filter should apply, {@code false} otherwise
         */
        public boolean applies(long time) {
            return (from == -1 || from <= time) && (to == -1 || to >= time);
        }
    }

    /**
     * Inserts a new packet into this stream.
     * If called from {@link StreamFilter#onPacket(PacketStream, de.johni0702.replaystudio.PacketData)}, this inserts the packet after the packet
     * that is being processed. This behavior can be changed by canceling the packet that is being processed and
     * inserting it manually using this method.
     * @param packet The packet
     * @see #insert(long, org.spacehq.packetlib.packet.Packet)
     */
    void insert(PacketData packet);

    /**
     * Inserts a new packet into this stream at the specified time.
     * If called from {@link StreamFilter#onPacket(PacketStream, de.johni0702.replaystudio.PacketData)}, this inserts the packet after the packet
     * that is being processed. This behavior can be changed by canceling the packet that is being processed and
     * inserting it manually using this method.
     * @param time The timestamp
     * @param packet The packet
     * @see #insert(de.johni0702.replaystudio.PacketData)
     */
    void insert(long time, Packet packet);

    /**
     * Adds a new filter to this packet stream.
     * @param filter The filter
     */
    void addFilter(StreamFilter filter);

    /**
     * Adds a new filter to this packet stream.
     * Only applies the filter within the specified bounds (inclusive).
     * A timestamp of -1 does not limit the duration.
     * @param filter The filter
     * @param from Timestamp from which to apply the filter
     * @param to Timestamp to which to apply the filter
     */
    void addFilter(StreamFilter filter, long from, long to);

    /**
     * Removes a filter from this packet stream.
     * @param filter The filter
     */
    void removeFilter(StreamFilter filter);

    /**
     * Returns all filters in the order they were added.
     * @return Unmodifiable list of filters
     */
    Collection<FilterInfo> getFilters();

    /**
     * Retrieves the next element in this stream applying all filters.
     * @return The next packet or {@code null} if the end of the stream has been reached
     */
    PacketData next();

    /**
     * Starts this packet stream (e.g. opening input streams, etc.).
     */
    void start();

    /**
     * Ends this packet stream by calling the {@link StreamFilter#onEnd(PacketStream, long)} method for every filter
     * still active and then performing cleanup (e.g. closing input streams, etc).
     * @return Excess packets generated during this call.
     */
    List<PacketData> end();

}
