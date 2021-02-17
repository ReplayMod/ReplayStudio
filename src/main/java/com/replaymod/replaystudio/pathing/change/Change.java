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
package com.replaymod.replaystudio.pathing.change;

import com.replaymod.replaystudio.pathing.path.Timeline;

/**
 * A change to any part of a timeline.
 * If {@link #undo(Timeline)} is not called in the reverse order of {@link #apply(Timeline)}, the behavior is unspecified.
 */
public interface Change {

    /**
     * Apply this change.
     *
     * @param timeline The timeline
     * @throws IllegalStateException If already applied.
     */
    void apply(Timeline timeline);

    /**
     * Undo this change.
     *
     * @param timeline The timeline
     * @throws IllegalStateException If not yet applied.
     */
    void undo(Timeline timeline);
}
