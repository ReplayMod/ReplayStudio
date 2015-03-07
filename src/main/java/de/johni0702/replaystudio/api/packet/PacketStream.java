package de.johni0702.replaystudio.api.packet;

import de.johni0702.replaystudio.api.manipulation.StreamFilter;
import lombok.Data;

import java.util.Collection;
import java.util.List;

/**
 * Represents a stream of packets.
 */
public interface PacketStream {

    @Data
    public static class FilterInfo {
        private final StreamFilter filter;
        private final long from, to;

        public boolean applies(long time) {
            return (from == -1 || from <= time) && (to == -1 || to >= time);
        }
    }

    /**
     * Inserts a new packet into this stream.
     * If called from {@link StreamFilter#onPacket(PacketStream, PacketData)}, this inserts the packet after the packet
     * that is being processed. This behavior can be changed by canceling the packet that is being processed and
     * inserting it manually using this method.
     * @param packet The packet
     */
    void insert(PacketData packet);

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
     * Calls the {@link StreamFilter#onStart(PacketStream)} method for every filter in this stream.
     * Does not actually start the stream and can be called multiple times if all filters support that.
     */
    void start();

    /**
     * Calls the {@link StreamFilter#onEnd(PacketStream, long)} method for every filter in this stream.
     * Does not actually end the stream and can be called multiple times if all filters support that.
     * @return Excess packets generated during this call.
     */
    List<PacketData> end();

}
