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
package com.replaymod.replaystudio.stream;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.protocol.Packet;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a stream of packets.
 */
public interface PacketStream {

    /**
     * Information on the time frame for which a filter should apply and the filter itself.
     */
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

        public FilterInfo(StreamFilter filter, long from, long to) {
            this.filter = filter;
            this.from = from;
            this.to = to;
        }

        /**
         * Returns whether this filter should be applied at the specified timestamp.
         * @param time The timestamp (milliseconds)
         * @return {@code true} if this filter should apply, {@code false} otherwise
         */
        public boolean applies(long time) {
            return (from == -1 || from <= time) && (to == -1 || to >= time);
        }

        public StreamFilter getFilter() {
            return this.filter;
        }

        public long getFrom() {
            return this.from;
        }

        public long getTo() {
            return this.to;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof FilterInfo)) return false;
            final FilterInfo other = (FilterInfo) o;
            if (!other.canEqual(this)) return false;
            if (!Objects.equals(this.filter, other.filter)) return false;
            if (this.from != other.from) return false;
            if (this.to != other.to) return false;
            return true;
        }

        public int hashCode() {
            int result = 1;
            result = result * 59 + (filter == null ? 0 : filter.hashCode());
            result = result * 59 + (int) (from >>> 32 ^ from);
            result = result * 59 + (int) (to >>> 32 ^ to);
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof FilterInfo;
        }

        public String toString() {
            return "FilterInfo(filter=" + this.filter + ", from=" + this.from + ", to=" + this.to + ")";
        }
    }

    /**
     * Inserts a new packet into this stream.
     * If called from {@link StreamFilter#onPacket(PacketStream, PacketData)}, this inserts the packet after the packet
     * that is being processed. This behavior can be changed by canceling the packet that is being processed and
     * inserting it manually using this method.
     * @param packet The packet
     * @see #insert(long, Packet)
     */
    void insert(PacketData packet);

    /**
     * Inserts a new packet into this stream at the specified time.
     * If called from {@link StreamFilter#onPacket(PacketStream, PacketData)}, this inserts the packet after the packet
     * that is being processed. This behavior can be changed by canceling the packet that is being processed and
     * inserting it manually using this method.
     * @param time The timestamp
     * @param packet The packet
     * @see #insert(PacketData)
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
    PacketData next() throws IOException;

    /**
     * Starts this packet stream (e.g. opening input streams, etc.).
     */
    void start();

    /**
     * Ends this packet stream by calling the {@link StreamFilter#onEnd(PacketStream, long)} method for every filter
     * still active and then performing cleanup (e.g. closing input streams, etc).
     * @return Excess packets generated during this call.
     */
    List<PacketData> end() throws IOException;

}
