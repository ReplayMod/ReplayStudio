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

import com.replaymod.replaystudio.pathing.change.Change;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Represents a group of property properties.
 * These groups are displayed and stored together.
 * The same property id may exist in multiple groups.<br>
 * Groups may also define a way of setting their properties in one simple way (e.g. "set to current position").
 */
public interface PropertyGroup {
    /**
     * Returns the localized name of this group.
     *
     * @return Localized name.
     */
    @NonNull
    String getLocalizedName();

    /**
     * Returns an ID unique for this group.
     *
     * @return Unique ID
     */
    @NonNull
    String getId();

    /**
     * Return a list of all properties in this group.
     *
     * @return List of properties or empty list if none.
     */
    @NonNull
    List<Property> getProperties();

    /**
     * Return a Callable which can be used to set all properties in this group at once.
     * It is up to the caller to apply those changes to the path. If those changes are not applied
     * immediately, their content may become invalid and applying them results in undefined behavior.
     *
     * @return Optional callable
     */
    @NonNull
    Optional<Callable<Change>> getSetter();
}
