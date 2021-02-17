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