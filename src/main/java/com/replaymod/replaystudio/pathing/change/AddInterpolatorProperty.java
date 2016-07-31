package com.replaymod.replaystudio.pathing.change;

import com.google.common.base.Preconditions;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import lombok.NonNull;

/**
 * Adds a new property.
 */
public final class AddInterpolatorProperty implements Change {
    @NonNull
    public static AddInterpolatorProperty create(Interpolator interpolator, Property property) {
        return new AddInterpolatorProperty(interpolator, property);
    }

    AddInterpolatorProperty(Interpolator interpolator, Property property) {
        this.interpolator = interpolator;
        this.property = property;
    }

    private final Interpolator interpolator;

    private final Property property;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        interpolator.registerProperty(property);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        interpolator.unregisterProperty(property);

        applied = false;
    }
}
