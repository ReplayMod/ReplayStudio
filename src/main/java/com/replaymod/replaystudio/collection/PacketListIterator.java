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
package com.replaymod.replaystudio.collection;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.collection.PacketList.ListEntry;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Iterator for a PacketList.
 * This iterator is not thread-safe.
 * Modifications to the underlying list through any other methods
 * than this iterator might result in undefined behavior.
 *
 * @deprecated Use some standard collection type instead.
 */
@Deprecated
public class PacketListIterator implements ListIterator<PacketData>, Cloneable {

    /**
     * The list of this iterator.
     */
    private PacketList list;

    /**
     * Previous entry. Returned by {@link #previous()}.
     */
    private ListEntry previous;

    /**
     * Latest entry returned. When remove or set is called, this is the entry on which the operation is performed.
     */
    private ListEntry latest;

    /**
     * Next entry. Returned by {@link #next()}.
     */
    private ListEntry next;

    /**
     * Creates a new iterator for the specified list starting at index 0.
     * @param list The list
     */
    public PacketListIterator(PacketList list) {
        this(list, list.first);
    }

    /**
     * Creates a new iterator for the specified list starting at the specified index.
     * This either iterates through the list from the beginning or the end depending
     * on what is closest.
     * @param list The list
     * @param index The index
     */
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

    /**
     * Creates a copy of the specified iterator.
     * @param iterator The iterator to copy
     */
    public PacketListIterator(PacketListIterator iterator) {
        this.list = iterator.list;
        this.previous = iterator.previous;
        this.latest = iterator.latest;
        this.next = iterator.next;
    }

    /**
     * Creates a new iterator for the specified list starting with the specified entry.
     * @param list The list
     * @param next The next entry
     */
    private PacketListIterator(PacketList list, ListEntry next) {
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
        return latest.data;
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
        return latest.data;
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

        if (previous == latest) { // Last call was to next()
            previous = previous.previous;
        }

        if (next == latest) { // Last call was to previous()
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
        if (latest.next != null && latest.next.data.getTime() < packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Next is " + latest.next);
        }
        if (latest.previous != null && latest.previous.data.getTime() > packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Previous is " + latest.previous);
        }

        ListEntry entry = new ListEntry(packetData);

        if (latest.previous == null) { // Beginning of list
            list.first = entry;
        } else {
            latest.previous.next = entry;
        }
        if (latest.next == null) { // End of list
            list.last = entry;
        } else {
            latest.next.previous = entry;
        }

        entry.next = latest.next;
        entry.previous = latest.previous;

        if (next == latest) { // Last call was to previous()
            next = entry;
        }
        if (previous == latest) { // Last call was to next()
            previous = entry;
        }
        latest.previous = latest.next = null;
        latest = entry;
    }

    @Override
    public void add(PacketData packetData) {
        System.out.println("Next -> " + next);
        System.out.println("Previous -> " + previous);
        if (next != null && next.data.getTime() < packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Next is " + next);
        }
        if (previous != null && previous.data.getTime() > packetData.getTime()) {
            throw new IllegalStateException("Cannot insert " + packetData + " at this position. Previous is " + previous);
        }

        ListEntry entry = new ListEntry(packetData);
        list.size++;
        if (next == null) { // End of list
            list.last = entry;
        } else {
            next.previous = entry;
        }

        if (previous == null) { // Start of list
            list.first = entry;
        } else {
            previous.next = entry;
        }

        entry.previous = previous;
        entry.next = next;
        previous = entry;
        latest = null;
    }

    /**
     * Skips to the specified timestamp. A subsequent call to {@link #next()} will return the first element
     * in the list with the specified timestamp or if there is none the first one after the timestamp.
     * @param time Timestamp in milliseconds
     * @return {@code this} for chaining
     */
    public PacketListIterator skipTo(long time) {
        while (hasNext() && next.data.getTime() < time) {
            next();
        }
        while (hasPrevious() && previous.data.getTime() >= time) {
            previous();
        }
        return this;
    }

    @Override
    public PacketListIterator clone() throws CloneNotSupportedException {
        return (PacketListIterator) super.clone();
    }

}
