package com.replaymod.replaystudio.path;

public class Path {
    private final String name;
    private final KeyframePosition[] positionKeyframes;
    private final KeyframeTime[] timeKeyframes;

    public Path(String name, KeyframePosition[] positionKeyframes, KeyframeTime[] timeKeyframes) {
        this.name = name;
        this.positionKeyframes = positionKeyframes;
        this.timeKeyframes = timeKeyframes;
    }

    public String getName() {
        return name;
    }

    public KeyframePosition[] getPositionKeyframes() {
        return positionKeyframes;
    }

    public KeyframeTime[] getTimeKeyframes() {
        return timeKeyframes;
    }
}
