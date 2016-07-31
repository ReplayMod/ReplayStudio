package com.replaymod.replaystudio.pathing.change;

import com.replaymod.replaystudio.pathing.path.Timeline;

/**
 * A change to any part of a timeline.
 * If {@link #undo(Timeline)} is not called in the reverse order of {@link #apply(Timeline)}, the behavior is unspecified.
 */
public interface Change {

    /**
     * Apply this change.
     *
     * @param timeline The timeline
     * @throws IllegalStateException If already applied.
     */
    void apply(Timeline timeline);

    /**
     * Undo this change.
     *
     * @param timeline The timeline
     * @throws IllegalStateException If not yet applied.
     */
    void undo(Timeline timeline);
}
