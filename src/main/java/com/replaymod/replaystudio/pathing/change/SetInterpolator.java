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
package com.replaymod.replaystudio.pathing.change;

import com.google.common.base.Preconditions;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Sets the interpolator of a path segment.
 */
public final class SetInterpolator implements Change {

    @NonNull
    public static SetInterpolator create(PathSegment segment, Interpolator interpolator) {
        return new SetInterpolator(segment, interpolator);
    }

    SetInterpolator(PathSegment segment, Interpolator interpolator) {
        this.segment = segment;
        this.interpolator = interpolator;
    }

    private final PathSegment segment;

    private final Interpolator interpolator;

    private Interpolator oldInterpolator;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        oldInterpolator = segment.getInterpolator();
        segment.setInterpolator(interpolator);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        segment.setInterpolator(oldInterpolator);

        applied = false;
    }
}
