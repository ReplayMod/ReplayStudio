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
