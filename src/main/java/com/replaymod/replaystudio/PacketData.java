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

    public PacketData retain() {
        this.packet.retain();
        return this;
    }

    public PacketData copy() {
        return new PacketData(time, packet.copy());
    }

    public boolean release() {
        return this.packet.release();
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
