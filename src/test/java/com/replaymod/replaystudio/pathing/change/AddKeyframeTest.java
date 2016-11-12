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

import com.replaymod.replaystudio.pathing.TimelineTestsBase;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AddKeyframeTest extends TimelineTestsBase {
    @Test
    public void addAsFirst() throws Exception {
        Change change = AddKeyframe.create(path, 7);
        change.apply(timeline);
        assertEquals("Keyframe was not added", 1, path.getKeyframes().size());
        assertEquals("Keyframe has wrong time", 7, actualKeyframe(0).getTime());

        change.undo(timeline);
        assertEquals("Keyframe was not removed", 0, path.getKeyframes().size());
    }

    @Test
    public void addTo1Before() throws Exception {
        Keyframe other = path.insert(10);

        Change change = AddKeyframe.create(path, 7);
        change.apply(timeline);
        assertEquals("Keyframe was not added", 2, path.getKeyframes().size());
        assertEquals("Keyframe has wrong time", 7, actualKeyframe(0).getTime());

        change.undo(timeline);
        assertEquals("Keyframe was not removed", 1, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", other, actualKeyframe(0));
    }

    @Test
    public void addTo1After() throws Exception {
        Keyframe other = path.insert(10);

        Change change = AddKeyframe.create(path, 13);
        change.apply(timeline);
        assertEquals("Keyframe was not added", 2, path.getKeyframes().size());
        assertEquals("Keyframe has wrong time", 13, actualKeyframe(1).getTime());

        change.undo(timeline);
        assertEquals("Keyframe was not removed", 1, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", other, actualKeyframe(0));
    }

    @Test
    public void addTo2As1st() throws Exception {
        Keyframe other0 = path.insert(10);
        Keyframe other1 = path.insert(15);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator);

        Change change = AddKeyframe.create(path, 7);
        change.apply(timeline);
        assertEquals("Keyframe was not added", 3, path.getKeyframes().size());
        assertEquals("Keyframe has wrong time", 7, actualKeyframe(0).getTime());

        change.undo(timeline);
        assertEquals("Keyframe was not removed", 2, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", other0, actualKeyframe(0));
        assertEquals("Wrong keyframe removed", other1, actualKeyframe(1));
        assertEquals("Interpolator not restored", interpolator, actualSegment(0).getInterpolator());
    }

    @Test
    public void addTo2As2nd() throws Exception {
        Keyframe other0 = path.insert(10);
        Keyframe other1 = path.insert(15);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator);

        Change change = AddKeyframe.create(path, 13);
        change.apply(timeline);
        assertEquals("Keyframe was not added", 3, path.getKeyframes().size());
        assertEquals("Keyframe has wrong time", 13, actualKeyframe(1).getTime());

        change.undo(timeline);
        assertEquals("Keyframe was not removed", 2, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", other0, actualKeyframe(0));
        assertEquals("Wrong keyframe removed", other1, actualKeyframe(1));
        assertEquals("Interpolator not restored", interpolator, actualSegment(0).getInterpolator());
    }

    @Test
    public void addTo2As3rd() throws Exception {
        Keyframe other0 = path.insert(10);
        Keyframe other1 = path.insert(15);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator);

        Change change = AddKeyframe.create(path, 17);
        change.apply(timeline);
        assertEquals("Keyframe was not added", 3, path.getKeyframes().size());
        assertEquals("Keyframe has wrong time", 17, actualKeyframe(2).getTime());

        change.undo(timeline);
        assertEquals("Keyframe was not removed", 2, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", other0, actualKeyframe(0));
        assertEquals("Wrong keyframe removed", other1, actualKeyframe(1));
        assertEquals("Interpolator not restored", interpolator, actualSegment(0).getInterpolator());
    }
}