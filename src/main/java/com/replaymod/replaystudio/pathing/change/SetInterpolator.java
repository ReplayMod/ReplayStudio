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
