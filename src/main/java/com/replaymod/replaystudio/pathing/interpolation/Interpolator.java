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

import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.pathing.property.PropertyPart;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interpolates multiple keyframes. Also provides all Properties it can handle.
 * Each Interpolator may provide certain outputs for the next Interpolator.
 * Interpolators are evaluated first to last.
 */
public interface Interpolator {
    /**
     * Register the specified property to be handled by this interpolator.
     * @param property The property
     */
    void registerProperty(Property property);

    /**
     * Removes the specified property from being handled by this interpolator
     * @param property The property
     */
    void unregisterProperty(Property property);

    /**
     * Returns a collection of all properties applicable for this Interpolator.
     *
     * @return Collection of properties or empty collection if none
     */
    @NonNull
    Collection<Property> getKeyframeProperties();

    /**
     * Add the specified path segment to this interpolator.
     * <p/>
     * This method should be called by the PathSegment itself.
     *
     * @param segment The path segments
     */
    void addSegment(PathSegment segment);

    /**
     * Remove the specified path segment from this interpolator.
     * <p/>
     * This method should be called by the PathSegment itself.
     *
     * @param segment The path segments
     */
    void removeSegment(PathSegment segment);

    /**
     * Returns an immutable list of all path segments handled by this interpolator.
     * Ordering is only guaranteed after {@link #bake(Map)} has been called and only until
     * the list is modified.
     *
     * @return List of path segments or empty list if none
     */
    @NonNull
    List<PathSegment> getSegments();

    /**
     * Bake the interpolation of the current path segments with the specified parameters.
     * Note that when this method is called directly, the path might need to be updated via {@link Path#update()}
     *
     * @param parameters Map of parameters for some properties
     * @return Map of parameters for the next interpolator
     * @throws IllegalStateException If no segments have been set yet
     *                               or the segments are not continuous
     */
    @NonNull
    Map<PropertyPart, InterpolationParameters> bake(Map<PropertyPart, InterpolationParameters> parameters);

    /**
     * Returns whether the segments handled by this interpolator have changed since the last
     * call of {@link #bake(Map)}.
     * This only includes the segments themselves, not the properties of their keyframes, those
     * have to be tracked manually.
     * @return {@code true} if segments have changed, {@code false} otherwise
     */
    boolean isDirty();

    /**
     * Return the value of the property at the specified point in time.
     *
     * @param property The property
     * @param time     Time in milliseconds since the start
     * @param <T>      Type of the property
     * @return Optional value of the property
     * @throws IllegalStateException If {@link #bake(Map)} has not yet been called
     *                               or {@link #addSegment(PathSegment)}/{@link #removeSegment(PathSegment)}
     *                               has been changed since the last bake
     */
    <T> Optional<T> getValue(Property<T> property, long time);
}
