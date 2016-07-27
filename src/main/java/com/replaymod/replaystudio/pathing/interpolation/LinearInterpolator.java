package com.replaymod.replaystudio.pathing.interpolation;

import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.pathing.property.PropertyPart;

import java.util.*;

public class LinearInterpolator extends AbstractInterpolator {
    private Map<Property, Set<Keyframe>> framesToProperty = new HashMap<>();

    private void addToMap(Property property, Keyframe keyframe) {
        Set<Keyframe> set = framesToProperty.get(property);
        if (set == null) {
            framesToProperty.put(property, set = new LinkedHashSet<>());
        }
        set.add(keyframe);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<PropertyPart, InterpolationParameters> bakeInterpolation(Map<PropertyPart, InterpolationParameters> parameters) {
        framesToProperty.clear();
        for (PathSegment segment : getSegments()) {
            for (Property property : getKeyframeProperties()) {
                if (segment.getStartKeyframe().getValue(property).isPresent()) {
                    addToMap(property, segment.getStartKeyframe());
                }
                if (segment.getEndKeyframe().getValue(property).isPresent()) {
                    addToMap(property, segment.getEndKeyframe());
                }
            }
        }

        Keyframe lastKeyframe = getSegments().get(getSegments().size() - 1).getEndKeyframe();
        Map<PropertyPart, InterpolationParameters> lastParameters = new HashMap<>();
        for (Property<?> property : getKeyframeProperties()) {
            Optional optionalValue = lastKeyframe.getValue(property);
            if (optionalValue.isPresent()) {
                Object value = optionalValue.get();
                for (PropertyPart part : property.getParts()) {
                    lastParameters.put(part, new InterpolationParameters(part.toDouble(value), 1, 0));
                }
            }
        }
        return lastParameters;
    }

    @Override
    public <T> Optional<T> getValue(Property<T> property, long time) {
        Set<Keyframe> kfSet = framesToProperty.get(property);
        if (kfSet == null) {
            return Optional.empty();
        }
        Keyframe kfBefore = null, kfAfter = null;
        for (Keyframe keyframe : kfSet) {
            if (keyframe.getTime() == time) {
                return keyframe.getValue(property);
            } else if (keyframe.getTime() < time) {
                kfBefore = keyframe;
            } else if (keyframe.getTime() > time) {
                kfAfter = keyframe;
                break;
            }
        }
        if (kfBefore == null || kfAfter == null) {
            return Optional.empty();
        }

        T valueBefore = kfBefore.getValue(property).get();
        T valueAfter = kfAfter.getValue(property).get();
        double fraction = (time - kfBefore.getTime()) / (double) (kfAfter.getTime() - kfBefore.getTime());

        T interpolated = valueBefore;
        for (PropertyPart<T> part : property.getParts()) {
            if (part.isInterpolatable()) {
                double before = part.toDouble(valueBefore);
                double after = part.toDouble(valueAfter);
                interpolated = part.fromDouble(interpolated, (after - before) * fraction + before);
            }
        }
        return Optional.of(interpolated);
    }
}
