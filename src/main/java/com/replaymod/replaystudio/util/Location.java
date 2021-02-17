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
package com.replaymod.replaystudio.util;


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

    public Location(DPosition position) {
        this(position, 0, 0);
    }

    public Location(DPosition position, float yaw, float pitch) {
        this(position.getX(), position.getY(), position.getZ(), yaw, pitch);
    }

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

    public DPosition getDPosition() {
        return new DPosition(x, y, z);
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
