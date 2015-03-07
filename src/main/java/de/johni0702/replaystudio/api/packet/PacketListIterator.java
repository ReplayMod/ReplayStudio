package de.johni0702.replaystudio.api.packet;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class PacketListIterator implements ListIterator<PacketData>, Cloneable {

    /**
     * The list of this iterator.
     */
    private PacketList list;

    /**
     * Previous entry. Returned by {@link #previous()}
     */
    private PacketData previous;

    /**
     * Latest entry returned. When remove or set is called, this is the entry on which the operation is performed.
     */
    private PacketData latest;

    /**
     * Next entry. Returned by {@link #next()}
     */
    private PacketData next;

    public PacketListIterator(PacketList list) {
        this(list, list.first);
    }

    public PacketListIterator(PacketList list, int index) {
        if (index < 0 || index > list.size) throw new IndexOutOfBoundsException();
        this.list = list;
        if (index < list.size / 2) {
            this.previous = null;
            this.next = list.first;
            for (int i = 0; i < index; i++) {
                next();
            }
        } else {
            this.previous = list.last;
            this.next = null;
            for (int i = list.size; i > index; i--) {
                previous();
            }
        }
    }

    public PacketListIterator(PacketListIterator iterator) {
        this.list = iterator.list;
        this.previous = iterator.previous;
        this.latest = iterator.latest;
        this.next = iterator.next;
    }

    private PacketListIterator(PacketList list, PacketData next) {
        this.list = list;
        this.next = next;
        this.previous = next == null ? null : next.previous;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public PacketData next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        previous = latest = next;
        next = next.next;
        return latest;
    }

    @Override
    public boolean hasPrevious() {
        return previous != null;
    }

    @Override
    public PacketData previous() {
        if (previous == null) {
            throw new NoSuchElementException();
        }
        next = latest = previous;
        previous = previous.previous;
        return latest;
    }

    @Override
    public int nextIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int previousIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() {
        if (latest == null) {
            throw new IllegalStateException();
        }
        list.size--;

        if (previous == latest) {
            previous = previous.previous;
        }

        if (next == latest) {
            next = next.next;
        }

        if (previous == null) { // Beginning of list
            list.first = next;
        } else {
            previous.next = next;
        }
        if (next == null) { // End of list
            list.last = previous;
        } else {
            next.previous = previous;
        }

        latest = latest.previous = latest.next = null;
    }

    @Override
    public void set(PacketData packetData) {
        if (latest == null) {
            throw new IllegalStateException();
        }
        if (latest.next != null && latest.next.getTime() < packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Next is " + latest.next);
        }
        if (latest.previous != null && latest.previous.getTime() > packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Previous is " + latest.previous);
        }
        if (latest.previous == null) { // Beginning of list
            list.first = packetData;
        } else {
            latest.previous.next = packetData;
        }
        if (latest.next == null) { // End of list
            list.last = packetData;
        } else {
            latest.next.previous = packetData;
        }

        packetData.next = latest.next;
        packetData.previous = latest.previous;

        if (next == latest) {
            next = packetData;
        }
        if (previous == latest) {
            previous = packetData;
        }
        latest.previous = latest.next = null;
        latest = packetData;
    }

    @Override
    public void add(PacketData packetData) {
        if (next != null && next.getTime() < packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Next is " + next);
        }
        if (previous != null && previous.getTime() > packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Previous is " + previous);
        }

        list.size++;
        if (next == null) { // End of list
            list.last = packetData;
        } else {
            next.previous = packetData;
        }

        if (previous == null) { // Start of list
            list.first = packetData;
        } else {
            previous.next = packetData;
        }

        packetData.previous = previous;
        packetData.next = next;
        previous = packetData;
        latest = null;
    }

    public PacketListIterator skipTo(long time) {
        while (hasNext() && next.getTime() < time) {
            next();
        }
        while (hasPrevious() && previous.getTime() > time) {
            previous();
        }
        return this;
    }

    @Override
    public PacketListIterator clone() throws CloneNotSupportedException {
        return (PacketListIterator) super.clone();
    }

}
