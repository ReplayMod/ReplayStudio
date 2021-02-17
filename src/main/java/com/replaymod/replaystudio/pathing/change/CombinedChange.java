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

import com.google.common.base.Preconditions;
import com.replaymod.replaystudio.pathing.path.Timeline;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Represents multiple changes as one change.
 */
public class CombinedChange implements Change {

    /**
     * Combines the specified changes into one change in the order given.
     * All changes must not yet have been applied.
     *
     * @param changes List of changes
     * @return A new CombinedChange instance
     */
    @NonNull
    public static CombinedChange create(Change... changes) {
        return new CombinedChange(Arrays.asList(changes), false);
    }

    /**
     * Combines the specified changes into one change in the order given.
     * All changes must have been applied.
     *
     * @param changes List of changes
     * @return A new CombinedChange instance
     */
    @NonNull
    public static CombinedChange createFromApplied(Change... changes) {
        return new CombinedChange(Arrays.asList(changes), true);
    }

    CombinedChange(List<Change> changeList, boolean applied) {
        this.changeList = changeList;
        this.applied = applied;
    }

    private final List<Change> changeList;
    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        for (Change change : changeList) {
            change.apply(timeline);
        }

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        ListIterator<Change> iterator = changeList.listIterator(changeList.size());
        while (iterator.hasPrevious()) {
            iterator.previous().undo(timeline);
        }

        applied = false;
    }
}
