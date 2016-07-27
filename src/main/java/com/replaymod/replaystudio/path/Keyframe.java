package com.replaymod.replaystudio.path;

/**
 * A keyframe at a specific point in time in a path.
 */
public abstract class Keyframe {

    /**
     * Timestamp of this keyframe in milliseconds.
     */
    private final long time;

    public Keyframe(long time) {
        this.time = time;
    }

    /**
     * Returns the time at which this keyframe is positioned.
     * @return Time in milliseconds
     */
    public long getTime() {
        return time;
    }
}
