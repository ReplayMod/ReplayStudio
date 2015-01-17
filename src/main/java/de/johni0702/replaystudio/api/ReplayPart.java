package de.johni0702.replaystudio.api;

import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.packetlib.packet.Packet;

import java.util.Collection;
import java.util.List;

/**
 * A part of a replay. Containing packets with their timestamps in their chronological order.
 */
public interface ReplayPart extends Iterable<Pair<Long, Packet>> {

    /**
     * Returns the length of this part.
     * @return Length in milliseconds
     */
    long length();

    /**
     * Return the size of this part.
     * @return Amount of packets
     */
    int size();

    /**
     * Creates a copy of this part.
     * @return Copy of the whole part
     */
    ReplayPart copy();

    /**
     * Creates a copy of this part.
     * @param from timestamp in milliseconds
     * @return Copy of this part starting at {@code from} (inclusive) milliseconds
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0}
     */
    ReplayPart copyOf(long from);

    /**
     * Creates a copy of this part.
     * @param from timestamp in milliseconds
     * @param to timestamp in milliseconds
     * @return Copy of this part starting at {@code from} (inclusive) milliseconds
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0} or {@code to > length}
     */
    ReplayPart copyOf(long from, long to);

    /**
     * Creates a view of this part. The view starts at {@code from} (inclusive).
     * Changes to the returned part will be reflected in this part and vice versa.
     * @param from timestamp in milliseconds
     * @return Partial view of this part
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0}
     */
    ReplayPartView viewOf(long from);

    /**
     * Creates a view of this part. The view starts at {@code from} (inclusive) and extends to {@code to} (inclusive).
     * Changes to the returned part will be reflected in this part and vice versa.
     * @param from timestamp in milliseconds
     * @param to timestamp in milliseconds
     * @return Partial view of this part
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0} or {@code to > length}
     */
    ReplayPartView viewOf(long from, long to);

    /**
     * Returns a list of packets in this part.
     * Inserting a new packet into the returned list inserts it at the same time as the previous packet.
     * @return List of packets
     */
    List<Packet> packets();

    /**
     * Returns a list of packets in this part starting from {@code from} (inclusive).
     * Inserting a new packet into the returned list inserts it at the same time as the previous packet.
     * @param from timestamp in milliseconds
     * @return List of packets
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0}
     */
    List<Packet> packets(long from);

    /**
     * Returns a list of packets in this part starting from {@code from} (inclusive) till {@code to} (inclusive).
     * Inserting a new packet into the returned list inserts it at the same time as the previous packet.
     * @param from timestamp in milliseconds
     * @param to timestamp in milliseconds
     * @return List of packets
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0} or {@code to > length}
     */
    List<Packet> packets(long from, long to);

    /**
     * Adds a new packet into this part.
     * Adding packets at a timestamp beyond this part increases the length of this part which might also increase the
     * view range if this part is a view.
     * @param at The time in milliseconds at which to place the packet
     * @param packet The packet
     */
    void add(long at, Packet packet);

    /**
     * Adds all packets into this part.
     * Adding packets at a timestamp beyond this part increases the length of this part which might also increase the
     * view range if this part is a view.
     * @param at The time in milliseconds at which to place all packets
     * @param packets Collection of all packets
     */
    void add(long at, Iterable<Packet> packets);

    /**
     * Adds all packets into this part.
     * Adding packets at a timestamp beyond this part increases the length of this part which might also increase the
     * view range if this part is a view.
     * @param packets Collection of all packets with their timestamp in milliseconds
     */
    void add(Iterable<Pair<Long, Packet>> packets);

    /**
     * Adds all packets into this part.
     * Adding packets at a timestamp beyond this part increases the length of this part which might also increase the
     * view range if this part is a view.
     * @param offset Offset added to each timestamp
     * @param packets Iterable of all packets with their timestamp in milliseconds
     */
    void addAt(long offset, Iterable<Pair<Long, Packet>> packets);

    /**
     * Appends the specified part to this part and returns the resulting combination.
     * Increases the length of this part which might also increase the
     * view range if this part is a view.
     * @param part The part which is to be added to this part.
     * @return New part containing both parts
     */
    ReplayPart append(ReplayPart part);

    /**
     * Removes every packet within the specified time frame (both timestamps are inclusive).
     * @param from timestamp in milliseconds
     * @param to timestamp in milliseconds
     * @return Collection of removed packets and their timestamps
     * @throws java.lang.IndexOutOfBoundsException if {@code from < 0} or {@code to > length}
     */
    Collection<Pair<Long, Packet>> remove(long from, long to);

}
