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
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Removes a property.
 */
public final class RemoveKeyframe implements Change {
    @NonNull
    public static RemoveKeyframe create(@NonNull Path path, @NonNull Keyframe keyframe) {
        return new RemoveKeyframe(path.getTimeline().getPaths().indexOf(path),
                Iterables.indexOf(path.getKeyframes(), Predicates.equalTo(keyframe)));
    }

    RemoveKeyframe(int path, int index) {
        this.path = path;
        this.index = index;
    }

    /**
     * Path index
     */
    private final int path;

    /**
     * Index of the property to be removed.
     */
    private final int index;

    private volatile Keyframe removedKeyframe;
    private volatile Interpolator removedInterpolator;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        Path path = timeline.getPaths().get(this.path);
        // By default we keep the interpolator of the left-hand side and and store the right-hand side for undoing
        // however if this is the last keyframe, we have to store the left-hand side as it will otherwise be lost
        if (index == path.getSegments().size()) {
            // This is the last keyframe, save the previous interpolator
            removedInterpolator = Iterables.get(path.getSegments(), index - 1).getInterpolator();
        } else {
            // Save the next interpolator
            removedInterpolator = Iterables.get(path.getSegments(), index).getInterpolator();
        }
        path.remove(removedKeyframe = Iterables.get(path.getKeyframes(), index), true);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        Path path = timeline.getPaths().get(this.path);
        path.insert(removedKeyframe);
        if (removedInterpolator != null) {
            if (index == path.getSegments().size()) {
                // The keyframe is the last one, restore the previous interpolator
                Iterables.get(path.getSegments(), index - 1).setInterpolator(removedInterpolator);
            } else {
                // Save the next iterpolator
                Iterables.get(path.getSegments(), index).setInterpolator(removedInterpolator);
            }
        }

        applied = false;
    }
}
