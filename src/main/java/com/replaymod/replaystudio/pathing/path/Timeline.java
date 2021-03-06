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

import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.property.Property;

import java.util.List;
import java.util.Optional;

/**
 * A timeline is a collection of paths that are played together.
 */
public interface Timeline {
    /**
     * Returns the list of paths that compose this timeline.
     * The returned list can be modified and changes are reflected on the timeline.
     * The order of the paths is their priority in case of conflicting properties.
     * The first path to support a property is the one whose value is used.
     * @return List of paths
     */
    List<Path> getPaths();

    /**
     * Creates a new path and adds it to this timeline.
     * @return A new Path instance
     */
    Path createPath();

    /**
     * Return the value of the property at the specified point in time.
     *
     * @param property The property
     * @param time     Time in milliseconds since the start
     * @param <T>      Type of the property
     * @return Optional value of the property
     * @throws IllegalStateException If {@link Path#update()} has not yet been called
     *                               or interpolators have changed since the last call
     */
    <T> Optional<T> getValue(Property<T> property, long time);

    /**
     * Apply the values of all properties at the specified time to the game.
     *
     * @param time      The time on this path
     * @param replayHandler The ReplayHandler instance
     */
    void applyToGame(long time, Object replayHandler);

    /**
     * Registers the specified property for use in keyframes in this path.
     * @param property The property
     */
    void registerProperty(Property property);

    /**
     * Returns the property corresponding to the specified id.
     * The id is either "groupId:propertyId" or "propertyId" if the property doesn't belong to any group.
     * @param id Id of the property
     * @return The property or {@code null} if not existent
     */
    Property getProperty(String id);

    /**
     * Apply the change and push it on the undo stack.
     * Clears the redo stack.
     * @param change The change
     * @throws IllegalStateException if the change has already been applied
     */
    void applyChange(Change change);

    /**
     * Push the change on the undo stack.
     * Clears the redo stack.
     * @param change The change
     * @throws IllegalStateException if the change has not yet been applied
     */
    void pushChange(Change change);

    /**
     * Undo the last change and push it on the redo stack.
     * @throws java.util.NoSuchElementException if the stack is empty
     */
    void undoLastChange();

    /**
     * Redo the last undone change and push it back on the undo stack.
     * @throws java.util.NoSuchElementException if the stack is empty
     */
    void redoLastChange();

    /**
     * Peek at the top element of the undo stack.
     * The returned element must never be undone manually.
     * @return Top element on the undo stack, or {@code null} if the stack is empty
     */
    Change peekUndoStack();

    /**
     * Peek at the top element of the redo stack.
     * The returned element must never be redone manually.
     * @return Top element on the redo stack, or {@code null} if the stack is empty
     */
    Change peekRedoStack();
}
