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
