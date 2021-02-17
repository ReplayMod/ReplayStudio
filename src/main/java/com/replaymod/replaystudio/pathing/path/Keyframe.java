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
