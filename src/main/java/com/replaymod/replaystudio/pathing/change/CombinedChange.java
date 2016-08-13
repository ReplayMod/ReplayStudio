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
