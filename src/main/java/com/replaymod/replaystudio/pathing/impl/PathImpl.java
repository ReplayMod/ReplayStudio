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
package com.replaymod.replaystudio.pathing.impl;

import com.replaymod.replaystudio.pathing.interpolation.InterpolationParameters;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.pathing.property.PropertyPart;

import java.util.*;

public class PathImpl implements Path {
    private final Timeline timeline;
    private Map<Long, Keyframe> keyframes = new TreeMap<>();
    private List<PathSegment> segments = new LinkedList<>();
    private boolean active = true;

    public PathImpl(Timeline timeline) {
        this.timeline = timeline;
    }

    @Override
    public Timeline getTimeline() {
        return timeline;
    }

    @Override
    public Collection<Keyframe> getKeyframes() {
        return Collections.unmodifiableCollection(keyframes.values());
    }

    @Override
    public Collection<PathSegment> getSegments() {
        return Collections.unmodifiableCollection(segments);
    }

    @Override
    public void update() {
        update(false);
    }

    @Override
    public void updateAll() {
        update(false);
    }

    private void update(boolean force) {
        Interpolator interpolator = null;
        Map<PropertyPart, InterpolationParameters> parameters = new HashMap<>();
        for (PathSegment segment : segments) {
            if (segment.getInterpolator() != interpolator) {
                interpolator = segment.getInterpolator();
                if (force || interpolator.isDirty()) {
                    parameters = interpolator.bake(parameters);
                }
            }
        }
    }

    @Override
    public <T> Optional<T> getValue(Property<T> property, long time) {
        PathSegment segment = getSegment(time);
        if (segment != null) {
            Interpolator interpolator = segment.getInterpolator();
            if (interpolator != null) {
                if (interpolator.getKeyframeProperties().contains(property)) {
                    return interpolator.getValue(property, time);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Keyframe insert(long time) {
        Keyframe keyframe = new KeyframeImpl(time);
        insert(keyframe);
        return keyframe;
    }

    @Override
    public Keyframe getKeyframe(long time) {
        return keyframes.get(time);
    }

    @Override
    public void insert(Keyframe keyframe) {
        if (keyframes.containsKey(keyframe.getTime())) {
            throw new IllegalStateException("A keyframe at " + keyframe.getTime() + " already exists.");
        }
        keyframes.put(keyframe.getTime(), keyframe);

        if (segments.isEmpty()) {
            if (keyframes.size() >= 2) {
                Iterator<Keyframe> iter = keyframes.values().iterator();
                segments.add(new PathSegmentImpl(this, iter.next(), iter.next()));
            }
            return;
        }

        ListIterator<PathSegment> iter = segments.listIterator();
        PathSegment next = iter.next();
        if (keyframe.getTime() < next.getStartKeyframe().getTime()) {
            iter.previous();
            iter.add(new PathSegmentImpl(this, keyframe, next.getStartKeyframe(), next.getInterpolator()));
            return;
        }

        while (true) {
            if (next.getStartKeyframe().getTime() <= keyframe.getTime()
                    && next.getEndKeyframe().getTime() >= keyframe.getTime()) {
                iter.remove();
                iter.add(new PathSegmentImpl(this, next.getStartKeyframe(), keyframe, next.getInterpolator()));
                iter.add(new PathSegmentImpl(this, keyframe, next.getEndKeyframe(), next.getInterpolator()));
                next.setInterpolator(null);
                return;
            }
            if (iter.hasNext()) {
                next = iter.next();
            } else {
                iter.add(new PathSegmentImpl(this, next.getEndKeyframe(), keyframe, next.getInterpolator()));
                return;
            }
        }
    }

    @Override
    public void remove(Keyframe keyframe, boolean useFirstInterpolator) {
        if (keyframes.get(keyframe.getTime()) != keyframe) {
            throw new IllegalArgumentException("The keyframe " + keyframe + " is not part of this path.");
        }
        keyframes.remove(keyframe.getTime());

        if (segments.size() < 2) {
            for (PathSegment segment : segments) {
                segment.setInterpolator(null);
            }
            segments.clear();
            return;
        }

        ListIterator<PathSegment> iter = segments.listIterator();
        while (iter.hasNext()) {
            PathSegment next = iter.next();
            if (next.getEndKeyframe() == keyframe) {
                iter.remove();
                if (iter.hasNext()) {
                    PathSegment next2 = iter.next();
                    iter.remove();
                    iter.add(new PathSegmentImpl(this, next.getStartKeyframe(), next2.getEndKeyframe(),
                            (useFirstInterpolator ? next : next2).getInterpolator()));
                    next2.setInterpolator(null);
                }
                next.setInterpolator(null);
                return;
            }
            if (next.getStartKeyframe() == keyframe) {
                next.setInterpolator(null);
                iter.remove();
                return;
            }
        }
        throw new AssertionError("No segment for keyframe found!");
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private PathSegment getSegment(long time) {
        for (PathSegment segment : segments) {
            if (segment.getStartKeyframe().getTime() <= time && segment.getEndKeyframe().getTime() >= time) {
                return segment;
            }
        }
        return null;
    }
}
