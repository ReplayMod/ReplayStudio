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
package com.replaymod.replaystudio;

import com.replaymod.replaystudio.protocol.Packet;

import java.util.Objects;

/**
 * Contains packet data. That is the packet itself and its timestamp.
 */
public final class PacketData implements Cloneable {

    /**
     * Timestamp in milliseconds.
     */
    private final long time;

    /**
     * The packet.
     */
    private final Packet packet;

    public PacketData(long time, Packet packet) {
        this.time = time;
        this.packet = packet;
    }

    public long getTime() {
        return this.time;
    }

    public Packet getPacket() {
        return this.packet;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PacketData)) return false;
        final PacketData other = (PacketData) o;
        if (this.time != other.time) return false;
        if (!Objects.equals(this.packet, other.packet)) return false;
        return true;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + (int) (time >>> 32 ^ time);
        result = result * 59 + (packet == null ? 0 : packet.hashCode());
        return result;
    }

    public String toString() {
        return "PacketData(time=" + this.time + ", packet=" + this.packet + ")";
    }
}
