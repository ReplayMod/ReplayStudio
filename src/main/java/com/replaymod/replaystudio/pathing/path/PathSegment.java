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
package com.replaymod.replaystudio.pathing.path;

import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import lombok.NonNull;

/**
 * Represents a segment of a Path consisting of one start property and one end property.
 * Each segment is interpolated by one Interpolator. Multiple segments may have the same Interpolator and can only
 * be interpolated together (they may or may not influence each other indirectly).
 */
public interface PathSegment {
    @NonNull
    Path getPath();

    /**
     * Return the start property of this segment.
     *
     * @return The first property
     */
    @NonNull
    Keyframe getStartKeyframe();

    /**
     * Return the end property of this segment.
     *
     * @return The second property
     */
    @NonNull
    Keyframe getEndKeyframe();

    /**
     * Return the interpolator responsible for this path segment.
     *
     * @return The interpolator or {@code null} if not yet set
     * @throws IllegalStateException If no interpolator has been set yet
     */
    @NonNull
    Interpolator getInterpolator();

    /**
     * Set the interpolator responsible for this path segment.
     * @param interpolator The interpolator
     */
    void setInterpolator(Interpolator interpolator);
}
