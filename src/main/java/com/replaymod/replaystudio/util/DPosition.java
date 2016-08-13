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

/**
 * Position with double precision.
 */
public class DPosition {

    public static final DPosition NULL = new DPosition(0, 0, 0);

    private final double x, y, z;

    public DPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
        if (!(o instanceof DPosition)) return false;
        final DPosition other = (DPosition) o;
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
        return other instanceof DPosition;
    }

    public String toString() {
        return "DPosition(x=" + this.x + ", y=" + this.y + ", z=" + this.z + ")";
    }
}
