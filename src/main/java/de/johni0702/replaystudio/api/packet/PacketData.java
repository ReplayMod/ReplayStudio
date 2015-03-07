package de.johni0702.replaystudio.api.packet;

import lombok.*;
import org.spacehq.packetlib.packet.Packet;

/**
 * Contains packet data. That is the packet itself, its timestamp and previous/next packets.
 */
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"previous", "next"})
@ToString(exclude = {"previous", "next"})
public final class PacketData implements Cloneable {

    @Getter
    private final long time;
    @Getter
    @NonNull
    private final Packet packet;

    protected PacketData previous;
    protected PacketData next;

}
