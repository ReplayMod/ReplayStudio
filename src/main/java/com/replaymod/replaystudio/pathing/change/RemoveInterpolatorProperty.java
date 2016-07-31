package com.replaymod.replaystudio.pathing.change;

import com.google.common.base.Preconditions;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import lombok.NonNull;

/**
 * Removes a property.
 */
public final class RemoveInterpolatorProperty implements Change {
    @NonNull
    public static RemoveInterpolatorProperty create(Interpolator interpolator, Property property) {
        return new RemoveInterpolatorProperty(interpolator, property);
    }

    RemoveInterpolatorProperty(Interpolator interpolator, Property property) {
        this.interpolator = interpolator;
        this.property = property;
    }

    private final Interpolator interpolator;

    private final Property property;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        interpolator.unregisterProperty(property);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        interpolator.registerProperty(property);

        applied = false;
    }
}
