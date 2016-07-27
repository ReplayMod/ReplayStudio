package com.replaymod.replaystudio.path;

import com.replaymod.replaystudio.util.DPosition;
import org.spacehq.mc.protocol.data.game.Rotation;

/**
 * A keyframe for a specific position.
 */
public class KeyframePosition extends Keyframe {
    private final DPosition position;
    private final Rotation rotation;

    public KeyframePosition(long time, DPosition position, Rotation rotation) {
        super(time);
        this.position = position;
        this.rotation = rotation;
    }

    /**
     * Returns the position for this keyframe.
     * @return The position
     */
    public DPosition getPosition() {
        return position;
    }

    /**
     * Returns the rotation for this keyframe.
     * @return The rotation
     */
    public Rotation getRotation() {
        return rotation;
    }
}
