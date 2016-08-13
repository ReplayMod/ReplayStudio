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

import com.replaymod.replaystudio.pathing.property.Property;
import lombok.NonNull;

import java.util.Optional;
import java.util.Set;

/**
 * Represents a key frame in time that is used to compute frames in between multiple key frames.
 * Keyframes have different properties depending on the Interpolator tying them together.
 * One property tied by a different Interpolator to the previous one than to the next one inherits both properties.
 */
public interface Keyframe {

    /**
     * Return the time at which this property is set.
     *
     * @return Time in milliseconds since the start
     */
    long getTime();

    /**
     * Return the value of the property set at this property.
     *
     * @param property The property
     * @param <T>      Type of the property
     * @return Optional value of the property
     */
    @NonNull
    <T> Optional<T> getValue(Property<T> property);

    /**
     * Set the value for the property at this property.
     * If the property is not present, adds it.
     *
     * @param property The property
     * @param value    Value of the property, may be {@code null}
     * @param <T>      Type of the property
     */
    <T> void setValue(Property<T> property, T value);

    /**
     * Remove the specified property from this property.
     *
     * @param property The property to be removed
     */
    void removeProperty(Property property);

    /**
     * Returns all properties of this property
     * @return Set of properties
     */
    Set<Property> getProperties();
}
