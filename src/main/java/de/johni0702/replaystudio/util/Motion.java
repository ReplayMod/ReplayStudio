package de.johni0702.replaystudio.util;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;

/**
 * Motion data for entities.
 */
@Data
@RequiredArgsConstructor
public class Motion {

    public static final Motion NULL = new Motion(0, 0, 0);

    private final double x, y, z;

    public static Motion from(ServerEntityVelocityPacket p) {
        return new Motion(p.getMotionX(), p.getMotionY(), p.getMotionZ());
    }
}
