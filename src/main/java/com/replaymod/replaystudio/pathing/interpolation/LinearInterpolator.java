/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
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
                double bound = part.getUpperBound();
                if (!Double.isNaN(bound)) {
                    before = mod(before, bound);
                    after = mod(after, bound);
                    if (Math.abs(after - before) > bound / 2) {
                        // Wrapping around is quicker
                        if (before < bound / 2) {
                            after -= bound;
                        } else {
                            after += bound;
                        }
                    }
                }
                double value = (after - before) * fraction + before;
                if (!Double.isNaN(bound)) {
                    value = mod(value, bound);
                }
                interpolated = part.fromDouble(interpolated, value);
            }
        }
        return Optional.of(interpolated);
    }

    private double mod(double val, double m) {
        double off = Math.floor(val / m);
        return val - off * m;
    }
}
