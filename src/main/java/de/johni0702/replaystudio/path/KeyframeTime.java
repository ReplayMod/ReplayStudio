package de.johni0702.replaystudio.path;

/**
 * A keyframe for a specific replay time.
 */
public class KeyframeTime extends Keyframe {
    private final long replayTime;

    public KeyframeTime(long time, long replayTime) {
        super(time);
        this.replayTime = replayTime;
    }

    /**
     * Returns the time in the replay for this keyframe.
     * @return Time in milliseconds
     */
    public long getReplayTime() {
        return replayTime;
    }
}
