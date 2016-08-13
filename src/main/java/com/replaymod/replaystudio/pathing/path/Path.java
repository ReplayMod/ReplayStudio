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
package com.replaymod.replaystudio.pathing.path;

import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.property.Property;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a path for some object consisting of keyframes, path segments and interpolators.
 */
public interface Path {
    /**
     * Returns the timeline this path belongs to.
     * @return The timeline
     */
    Timeline getTimeline();

    /**
     * Return an immutable collection of all keyframes in this path.
     *
     * @return Collection of keyframes or empty list if none
     */
    @NonNull
    Collection<Keyframe> getKeyframes();

    /**
     * Return an immutable collection of all segments of this path.
     *
     * @return Collection of segments or empty list if none
     */
    @NonNull
    Collection<PathSegment> getSegments();

    /**
     * Update all interpolators that need updating.
     * This does <b>not</b> detect changes to keyframes, only interpolators.
     * If a property has changed, call {@link Interpolator#bake(Map)} before calling this method
     * or call {@link #updateAll()}.
     *
     * @throws IllegalStateException If any path segments do not have an interpolator set.
     */
    void update();

    /**
     * Update all interpolations.
     * This method is significantly slower than {@link #update()}.
     *
     * @throws IllegalStateException If any path segments do not have an interpolator set.
     */
    void updateAll();

    /**
     * Return the value of the property at the specified point in time.
     *
     * @param property The property
     * @param time     Time in milliseconds since the start
     * @param <T>      Type of the property
     * @return Optional value of the property
     * @throws IllegalStateException If {@link #update()} has not yet been called
     *                               or interpolators have changed since the last call
     */
    <T> Optional<T> getValue(Property<T> property, long time);

    /**
     * Insert a new property at the specified time.
     * The two new path segments inherit the interpolator of the previous one.
     * Does <b>not</b> update the interpolators, call {@link #update()} to do so.
     *
     * @param time Time in milliseconds
     * @return The new property
     */
    Keyframe insert(long time);

    /**
     * Returns the property at the specified time.
     * @param time Time in milliseconds
     * @return The property or {@code null} if none exists at that timestamp
     */
    Keyframe getKeyframe(long time);

    /**
     * Insert the specified property.
     * The two new path segments inherit the interpolator of the previous one.
     * Does <b>not</b> update the interpolators, call {@link #update()} to do so.
     *
     * @param keyframe The property
     */
    void insert(Keyframe keyframe);

    /**
     * Removes the specified property.
     * Does <b>not</b> update the interpolators, call {@link #update()} to do so.
     *
     * @param keyframe             The property to remove
     * @param useFirstInterpolator {@code true} if the new path segment inherits the interpolator of the first segment,
     *                             {@code false} if it inherits the one of the second segment
     */
    void remove(Keyframe keyframe, boolean useFirstInterpolator);

    /**
     * Set whether this path is active.
     * If this path is not active, it shouldn't be applied to the game during playback.
     * @param active {@code true} if active, {@code false} otherwise
     */
    void setActive(boolean active);

    /**
     * Returns whether this path is active.
     * @return {@code true} if active, {@code false} otherwise
     * @see #setActive(boolean)
     */
    boolean isActive();
}
