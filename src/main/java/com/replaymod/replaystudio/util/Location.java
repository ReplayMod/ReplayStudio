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
package com.replaymod.replaystudio.util;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;

//#if MC>=10904
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
//#else
//#if MC>=10800
//$$ import com.github.steveice10.mc.protocol.data.game.Position;
//#endif
//#endif

/**
 * Position and rotation (pitch and yaw, no roll) in three dimensional space.
 */
public class Location {

    /**
     * Location at 0/0/0 with 0 yaw and 0 pitch.
     */
    public static final Location NULL = new Location(0, 0, 0);

    private final double x, y, z;
    private final float yaw, pitch;

    //#if MC>=10800
    public Location(Position position) {
        this(position, 0, 0);
    }

    public Location(Position position, float yaw, float pitch) {
        this(position.getX(), position.getY(), position.getZ(), yaw, pitch);
    }
    //#endif

    public Location(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    public Location(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static Location from(ServerEntityTeleportPacket p) {
        return new Location(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }

    public ServerEntityTeleportPacket toServerEntityTeleportPacket(int entityId
                                                                   //#if MC>=10800
                                                                   , boolean onGround
                                                                   //#endif
    ) {
        return new ServerEntityTeleportPacket(entityId, x, y, z, yaw, pitch
                //#if MC>=10800
                , onGround
                //#endif
        );
    }

    //#if MC>=10800
    public Position getPosition() {
        return new Position((int) x, (int) y, (int) z);
    }
    //#endif

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Location)) return false;
        final Location other = (Location) o;
        if (!other.canEqual(this)) return false;
        if (Double.compare(this.x, other.x) != 0) return false;
        if (Double.compare(this.y, other.y) != 0) return false;
        if (Double.compare(this.z, other.z) != 0) return false;
        if (Float.compare(this.yaw, other.yaw) != 0) return false;
        if (Float.compare(this.pitch, other.pitch) != 0) return false;
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
        result = result * 59 + Float.floatToIntBits(this.yaw);
        result = result * 59 + Float.floatToIntBits(this.pitch);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Location;
    }

    public String toString() {
        return "Location(x=" + this.x + ", y=" + this.y + ", z=" + this.z + ", yaw=" + this.yaw + ", pitch=" + this.pitch + ")";
    }
}
