package de.johni0702.replaystudio.collection;

import de.johni0702.replaystudio.PacketData;
import org.spacehq.packetlib.packet.Packet;

import java.util.Collection;
import java.util.ListIterator;

/**
 * A part of a replay. Containing packets with their timestamps in their chronological order.
 */
public interface ReplayPart extends Iterable<PacketData> {

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
    void add(Iterable<PacketData> packets);

    /**
     * Adds all packets into this part.
     * Adding packets at a timestamp beyond this part increases the length of this part which might also increase the
     * view range if this part is a view.
     * @param offset Offset added to each timestamp
     * @param packets Iterable of all packets with their timestamp in milliseconds
     */
    void addAt(long offset, Iterable<PacketData> packets);

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
    Collection<PacketData> remove(long from, long to);

    @Override
    ListIterator<PacketData> iterator();
}
