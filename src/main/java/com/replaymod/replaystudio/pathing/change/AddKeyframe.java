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
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Adds a new property.
 */
public final class AddKeyframe implements Change {
    @NonNull
    public static AddKeyframe create(Path path, long time) {
        return new AddKeyframe(path.getTimeline().getPaths().indexOf(path), time);
    }

    AddKeyframe(int path, long time) {
        this.path = path;
        this.time = time;
    }

    /**
     * Path index
     */
    private final int path;

    /**
     * Time at which the property should be injected.
     */
    private final long time;

    /**
     * Index of the newly created property.
     */
    private int index;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        Path path = timeline.getPaths().get(this.path);
        Keyframe keyframe = path.insert(time);
        index = Iterables.indexOf(path.getKeyframes(), Predicates.equalTo(keyframe));

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        Path path = timeline.getPaths().get(this.path);
        path.remove(Iterables.get(path.getKeyframes(), index), true);

        applied = false;
    }
}
