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
