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
