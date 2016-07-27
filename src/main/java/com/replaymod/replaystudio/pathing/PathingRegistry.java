package com.replaymod.replaystudio.pathing;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Timeline;

import java.io.IOException;

/**
 * Contains mappings required for serialization.
 */
public interface PathingRegistry {
    Timeline createTimeline();
    void serializeInterpolator(JsonWriter writer, Interpolator interpolator) throws IOException;
    Interpolator deserializeInterpolator(JsonReader reader) throws IOException;
}
