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
package com.replaymod.replaystudio.pathing.property;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.NonNull;

import java.io.IOException;
import java.util.Collection;

/**
 * Represents a property of a property.
 * Such properties may or may not be interpolated between keyframes.
 * <br>
 * If a property cannot be interpolated between keyframes, it is only active between two keyframes having the same
 * value for that property.
 *
 * @param <T> The type of the property, must be immutable
 */
public interface Property<T> {
    /**
     * Returns the localized name of this property.
     *
     * @return Localized name.
     */
    @NonNull
    String getLocalizedName();

    /**
     * Returns the group this property belongs to.
     *
     * @return The group
     */
    PropertyGroup getGroup();

    /**
     * Returns an ID unique for this property.
     * There may be multiple instances for the same property ID
     * if they all represent the same concept (all x-Coordinate).
     *
     * @return Unique ID
     */
    @NonNull
    String getId();

    /**
     * Returns a new value for this property.
     *
     * @return New value, may be {@code null}
     */
    T getNewValue();

    /**
     * Returns (optionally) interpolatable parts of this property.
     *
     * @return Collection of parts
     */
    Collection<PropertyPart<T>> getParts();

    /**
     * Appy the specified value of this property to the game.
     *
     * @param value         The value of this property
     * @param replayHandler The ReplayHandler instance
     */
    void applyToGame(T value, Object replayHandler);

    /**
     * Writes the specified value of this property to JSON.
     *
     * @param writer The json writer
     * @param value  The value
     */
    void toJson(JsonWriter writer, T value) throws IOException;

    /**
     * Reads the value of this property from JSON.
     *
     * @param reader The json reader
     * @return The value
     */
    T fromJson(JsonReader reader) throws IOException;
}
