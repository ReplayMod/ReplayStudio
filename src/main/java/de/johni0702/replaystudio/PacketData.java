package de.johni0702.replaystudio;

import org.spacehq.packetlib.packet.Packet;

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
