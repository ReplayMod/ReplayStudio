package com.replaymod.replaystudio.util;

import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;

/**
 * Motion data for entities.
 */
public class Motion {

    public static final Motion NULL = new Motion(0, 0, 0);

    private final double x, y, z;

    public Motion(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Motion from(ServerEntityVelocityPacket p) {
        return new Motion(p.getMotionX(), p.getMotionY(), p.getMotionZ());
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Motion)) return false;
        final Motion other = (Motion) o;
        if (!other.canEqual(this)) return false;
        if (Double.compare(this.x, other.x) != 0) return false;
        if (Double.compare(this.y, other.y) != 0) return false;
        if (Double.compare(this.z, other.z) != 0) return false;
        return true;
    }

    public int hashCode() {
        int result = 1;
        final long x = Double.doubleToLongBits(this.x);
        result = result * 59 + (int) (x >>> 32 ^ x);
        final long y = Double.doubleToLongBits(this.y);
        result = result * 59 + (int) (y >>> 32 ^ y);
        final long z = Double.doubleToLongBits(this.z);
        result = result * 59 + (int) (z >>> 32 ^ z);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Motion;
    }

    public String toString() {
        return "Motion(x=" + this.x + ", y=" + this.y + ", z=" + this.z + ")";
    }
}
