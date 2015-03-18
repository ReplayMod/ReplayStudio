package de.johni0702.replaystudio;

import lombok.Data;
import lombok.NonNull;
import org.spacehq.packetlib.packet.Packet;

/**
 * Contains packet data. That is the packet itself and its timestamp.
 */
@Data
public final class PacketData implements Cloneable {

    /**
     * Timestamp in milliseconds.
     */
    private final long time;

    /**
     * The packet.
     */
    @NonNull
    private final Packet packet;

}
