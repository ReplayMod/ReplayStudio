package de.johni0702.replaystudio.api.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.spacehq.packetlib.packet.Packet;

/**
 * Contains packet data. That is the packet itself, its timestamp and previous/next packets.
 */
@RequiredArgsConstructor
@AllArgsConstructor
public final class PacketData implements Cloneable {

    @Getter
    private final long time;
    @Getter
    @NonNull
    private final Packet packet;

    protected PacketData previous;
    protected PacketData next;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PacketData) {
            PacketData other = (PacketData) obj;
            return time == other.time && packet.equals(other.packet);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 37 * result + (int)(time ^ (time >>> 32));
        result = 37 * result + packet.hashCode();
        return result;
    }

}
