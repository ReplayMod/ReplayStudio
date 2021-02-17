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

import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.pathing.property.PropertyPart;

import java.util.*;

public abstract class AbstractInterpolator implements Interpolator {
    private List<PathSegment> segments = new LinkedList<>();
    private boolean dirty;
    private final Set<Property> properties = new HashSet<>();

    @Override
    public Collection<Property> getKeyframeProperties() {
        return Collections.unmodifiableCollection(properties);
    }

    @Override
    public void registerProperty(Property property) {
        if (properties.add(property)) {
            dirty = true;
        }
    }

    @Override
    public void unregisterProperty(Property property) {
        if (properties.remove(property)) {
            dirty = true;
        }
    }

    @Override
    public void addSegment(PathSegment segment) {
        segments.add(segment);
        dirty = true;
    }

    @Override
    public void removeSegment(PathSegment segment) {
        segments.remove(segment);
        dirty = true;
    }

    @Override
    public List<PathSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    @Override
    public Map<PropertyPart, InterpolationParameters> bake(Map<PropertyPart, InterpolationParameters> parameters) {
        if (segments.isEmpty()) throw new IllegalStateException("No segments have been added yet.");
        Collections.sort(segments, new Comparator<PathSegment>() {
            @Override
            public int compare(PathSegment s1, PathSegment s2) {
                return Long.compare(s1.getStartKeyframe().getTime(), s2.getStartKeyframe().getTime());
            }
        });

        // Check for continuity
        Iterator<PathSegment> iter = segments.iterator();
        PathSegment last = iter.next();
        while (iter.hasNext()) {
            if (last.getEndKeyframe() != (last = iter.next()).getStartKeyframe()) {
                throw new IllegalStateException("Segments are not continuous.");
            }
        }

        return bakeInterpolation(parameters);
    }

    /**
     * Bake the interpolation of the current path segments with the specified parameters.
     * Order of {@link #getSegments()} is guaranteed.
     * @param parameters Map of parameters for some properties
     * @return Map of parameters for the next interpolator
     */
    protected abstract Map<PropertyPart, InterpolationParameters> bakeInterpolation(Map<PropertyPart, InterpolationParameters> parameters);

    @Override
    public boolean isDirty() {
        return dirty;
    }
}
