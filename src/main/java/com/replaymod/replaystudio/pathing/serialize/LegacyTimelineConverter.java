/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.pathing.serialize;

import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.replay.ReplayFile;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class LegacyTimelineConverter {
    public static Map<String, Timeline> convert(PathingRegistry registry, ReplayFile replayFile) throws IOException {
        KeyframeSet[] keyframeSets = readAndParse(replayFile);
        if (keyframeSets == null) {
            return Collections.emptyMap();
        }

        Map<String, Timeline> timelines = new LinkedHashMap<>();
        for (KeyframeSet keyframeSet : keyframeSets) {
            timelines.put(keyframeSet.name, convert(registry, keyframeSet));
        }
        return timelines;
    }

    private static Optional<InputStream> read(ReplayFile replayFile) throws IOException {
        Optional<InputStream> in = replayFile.get("paths.json");
        if (!in.isPresent()) {
            in = replayFile.get("paths");
        }
        return in;
    }

    private static KeyframeSet[] parse(InputStream in) {
        return new GsonBuilder()
                .registerTypeAdapter(KeyframeSet[].class, new LegacyKeyframeSetAdapter())
                .create().fromJson(new InputStreamReader(in), KeyframeSet[].class);
    }

    private static KeyframeSet[] readAndParse(ReplayFile replayFile) throws IOException {
        Optional<InputStream> optIn = read(replayFile);
        if (!optIn.isPresent()) {
            return null;
        }
        KeyframeSet[] keyframeSets;
        try (InputStream in = optIn.get()) {
            keyframeSets = parse(in);
        }
        return keyframeSets;
    }

    @SuppressWarnings("unchecked")
    private static Timeline convert(PathingRegistry registry, KeyframeSet keyframeSet) {
        Timeline timeline = registry.createTimeline();
        Property timestamp = timeline.getProperty("timestamp");
        Property cameraPosition = timeline.getProperty("camera:position");
        Property cameraRotation = timeline.getProperty("camera:rotation");

        Path timePath = timeline.createPath();
        Path positionPath = timeline.createPath();
        for (Keyframe<AdvancedPosition> positionKeyframe : keyframeSet.positionKeyframes) {
            AdvancedPosition value = positionKeyframe.value;
            com.replaymod.replaystudio.pathing.path.Keyframe keyframe = getKeyframe(positionPath, positionKeyframe.realTimestamp);
            keyframe.setValue(cameraPosition, Triple.of(value.x, value.y, value.z));
            keyframe.setValue(cameraRotation, Triple.of(value.yaw, value.pitch, value.roll));
            if (value instanceof SpectatorData) {
                // TODO Spectator keyframes
            }
        }
        for (Keyframe<TimestampValue> timeKeyframe : keyframeSet.timeKeyframes) {
            TimestampValue value = timeKeyframe.value;
            com.replaymod.replaystudio.pathing.path.Keyframe keyframe = getKeyframe(timePath, timeKeyframe.realTimestamp);
            keyframe.setValue(timestamp, (int) value.value);
        }

        Interpolator timeInterpolator = new LinearInterpolator();
        timeInterpolator.registerProperty(timestamp);
        timePath.getSegments().forEach(s -> s.setInterpolator(timeInterpolator));

        Interpolator positionInterpolator = new CubicSplineInterpolator();
        positionInterpolator.registerProperty(cameraPosition);
        positionInterpolator.registerProperty(cameraRotation);
        positionPath.getSegments().forEach(s -> s.setInterpolator(positionInterpolator));

        return timeline;
    }

    private static com.replaymod.replaystudio.pathing.path.Keyframe getKeyframe(Path path, long time) {
        com.replaymod.replaystudio.pathing.path.Keyframe keyframe = path.getKeyframe(time);
        if (keyframe == null) {
            keyframe = path.insert(time);
        }
        return keyframe;
    }

    static class KeyframeSet {
        String name;
        Keyframe<AdvancedPosition>[] positionKeyframes;
        Keyframe<TimestampValue>[] timeKeyframes;
        CustomImageObject[] customObjects;
    }
    static class Keyframe<T> {
        int realTimestamp;
        T value;
    }
    static class Position {
        double x, y, z;
    }
    static class AdvancedPosition extends Position {
        float pitch, yaw, roll;
    }
    static class SpectatorData extends AdvancedPosition {
        Integer spectatedEntityID;
        SpectatingMethod spectatingMethod;
        SpectatorDataThirdPersonInfo thirdPersonInfo;
        enum SpectatingMethod {
            FIRST_PERSON, SHOULDER_CAM;
        }
    }
    static class SpectatorDataThirdPersonInfo {
        double shoulderCamDistance;
        double shoulderCamPitchOffset;
        double shoulderCamYawOffset;
        double shoulderCamSmoothness;
    }
    static class TimestampValue {
        double value;
    }
    static class CustomImageObject {
        String name;
        UUID linkedAsset;
        float width, height;
        float textureWidth, textureHeight;
        Transformations transformations = new Transformations();
    }
    static class Transformations {
        Position defaultAnchor, defaultPosition, defaultOrientation, defaultScale;
        NumberValue defaultOpacity;

        List<Position> anchorKeyframes;
        List<Position> positionKeyframes;
        List<Position> orientationKeyframes;
        List<Position> scaleKeyframes;
        List<NumberValue> opacityKeyframes;
    }
    static class NumberValue {
        double value;
    }
}
