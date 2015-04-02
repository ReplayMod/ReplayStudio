package de.johni0702.replaystudio.util;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;

/**
 * Position and rotation (pitch and yaw, no roll) in three dimensional space.
 */
@Data
@RequiredArgsConstructor
public class Location {

    /**
     * Location at 0/0/0 with 0 yaw and 0 pitch.
     */
    public static final Location NULL = new Location(0, 0, 0);

    private final double x, y, z;
    private final float yaw, pitch;

    public Location(Position position) {
        this(position, 0, 0);
    }

    public Location(Position position, float yaw, float pitch) {
        this(position.getX(), position.getY(), position.getZ(), yaw, pitch);
    }

    public Location(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    public static Location from(ServerEntityTeleportPacket p) {
        return new Location(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }

    public ServerEntityTeleportPacket toServerEntityTeleportPacket(int entityId, boolean onGround) {
        return new ServerEntityTeleportPacket(entityId, x, y, z, yaw, pitch, onGround);
    }

    public Position getPosition() {
        return new Position((int) x, (int) y, (int) z);
    }
}
