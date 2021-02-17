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
        // The interpolator can only be saved if there are at least two keyframes / one segment
        if (!path.getSegments().isEmpty()) {
            // By default we keep the interpolator of the left-hand side and and store the right-hand side for undoing
            // however if this is the last keyframe, we have to store the left-hand side as it will otherwise be lost
            if (index == path.getSegments().size()) {
                // This is the last keyframe, save the previous interpolator
                removedInterpolator = Iterables.get(path.getSegments(), index - 1).getInterpolator();
            } else {
                // Save the next interpolator
                removedInterpolator = Iterables.get(path.getSegments(), index).getInterpolator();
            }
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
