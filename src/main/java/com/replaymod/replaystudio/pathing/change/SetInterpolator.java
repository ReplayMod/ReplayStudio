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
import com.google.common.collect.Iterables;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Sets the interpolator of a path segment.
 */
public final class SetInterpolator implements Change {

    @NonNull
    public static SetInterpolator create(PathSegment segment, Interpolator interpolator) {
        Path path = segment.getPath();
        return new SetInterpolator(path.getTimeline().getPaths().indexOf(path),
                Iterables.indexOf(path.getSegments(), segment::equals),
                interpolator);
    }

    SetInterpolator(int path, int index, Interpolator interpolator) {
        this.path = path;
        this.index = index;
        this.interpolator = interpolator;
    }

    private final int path;
    private final int index;

    private final Interpolator interpolator;

    private Interpolator oldInterpolator;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        Path path = timeline.getPaths().get(this.path);
        PathSegment segment = Iterables.get(path.getSegments(), index);

        oldInterpolator = segment.getInterpolator();
        segment.setInterpolator(interpolator);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        Path path = timeline.getPaths().get(this.path);
        PathSegment segment = Iterables.get(path.getSegments(), index);

        segment.setInterpolator(oldInterpolator);

        applied = false;
    }
}
