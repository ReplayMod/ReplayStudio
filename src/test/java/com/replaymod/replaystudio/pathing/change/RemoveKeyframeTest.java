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
import static org.junit.Assert.assertTrue;

public class RemoveKeyframeTest extends TimelineTestsBase {
    @Test
    public void remove1stOf1() throws Exception {
        Keyframe keyframe = path.insert(0);

        Change change = RemoveKeyframe.create(path, keyframe);
        change.apply(timeline);
        assertTrue("Keyframe was not removed", path.getKeyframes().isEmpty());

        change.undo(timeline);
        assertEquals("Keyframe was not restored", 1, path.getKeyframes().size());
        assertEquals("Restored keyframe did not match original", keyframe, actualKeyframe(0));
    }

    @Test
    public void remove1stOf2() throws Exception {
        Keyframe keyframe0 = path.insert(0);
        Keyframe keyframe1 = path.insert(1);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator);

        Change change = RemoveKeyframe.create(path, keyframe0);
        change.apply(timeline);
        assertEquals("Keyframe was not removed", 1, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", keyframe1, actualKeyframe(0));

        change.undo(timeline);
        assertEquals("Keyframe was not restored", 2, path.getKeyframes().size());
        assertEquals("Restored keyframes did not match original", keyframe0, actualKeyframe(0));
        assertEquals("Restored keyframes did not match original", keyframe1, actualKeyframe(1));
        assertEquals("Interpolator was not restored", interpolator, actualSegment(0).getInterpolator());
    }

    @Test
    public void remove2ndOf2() throws Exception {
        Keyframe keyframe0 = path.insert(0);
        Keyframe keyframe1 = path.insert(1);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator);

        Change change = RemoveKeyframe.create(path, keyframe1);
        change.apply(timeline);
        assertEquals("Keyframe was not removed", 1, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", keyframe0, actualKeyframe(0));

        change.undo(timeline);
        assertEquals("Keyframe was not restored", 2, path.getKeyframes().size());
        assertEquals("Restored keyframes did not match original", keyframe0, actualKeyframe(0));
        assertEquals("Restored keyframes did not match original", keyframe1, actualKeyframe(1));
        assertEquals("Interpolator was not restored", interpolator, actualSegment(0).getInterpolator());
    }

    @Test
    public void remove1stOf3() throws Exception {
        Keyframe keyframe0 = path.insert(0);
        Keyframe keyframe1 = path.insert(1);
        Keyframe keyframe2 = path.insert(2);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator);

        Change change = RemoveKeyframe.create(path, keyframe0);
        change.apply(timeline);
        assertEquals("Keyframe was not removed", 2, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", keyframe1, actualKeyframe(0));
        assertEquals("Wrong keyframe removed", keyframe2, actualKeyframe(1));

        change.undo(timeline);
        assertEquals("Keyframe was not restored", 3, path.getKeyframes().size());
        assertEquals("Restored keyframes did not match original", keyframe0, actualKeyframe(0));
        assertEquals("Restored keyframes did not match original", keyframe1, actualKeyframe(1));
        assertEquals("Restored keyframes did not match original", keyframe2, actualKeyframe(2));
        assertEquals("Interpolator was not restored", interpolator, actualSegment(0).getInterpolator());
    }

    @Test
    public void remove2ndOf3() throws Exception {
        Keyframe keyframe0 = path.insert(0);
        Keyframe keyframe1 = path.insert(1);
        Keyframe keyframe2 = path.insert(2);
        Interpolator interpolator0 = new LinearInterpolator();
        Interpolator interpolator1 = new LinearInterpolator();
        actualSegment(0).setInterpolator(interpolator0);
        actualSegment(1).setInterpolator(interpolator1);

        Change change = RemoveKeyframe.create(path, keyframe1);
        change.apply(timeline);
        assertEquals("Keyframe was not removed", 2, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", keyframe0, actualKeyframe(0));
        assertEquals("Wrong keyframe removed", keyframe2, actualKeyframe(1));

        change.undo(timeline);
        assertEquals("Keyframe was not restored", 3, path.getKeyframes().size());
        assertEquals("Restored keyframes did not match original", keyframe0, actualKeyframe(0));
        assertEquals("Restored keyframes did not match original", keyframe1, actualKeyframe(1));
        assertEquals("Restored keyframes did not match original", keyframe2, actualKeyframe(2));
        assertEquals("Interpolator was not restored", interpolator0, actualSegment(0).getInterpolator());
        assertEquals("Interpolator was not restored", interpolator1, actualSegment(1).getInterpolator());
    }

    @Test
    public void remove3rdOf3() throws Exception {
        Keyframe keyframe0 = path.insert(0);
        Keyframe keyframe1 = path.insert(1);
        Keyframe keyframe2 = path.insert(2);
        Interpolator interpolator = new LinearInterpolator();
        actualSegment(1).setInterpolator(interpolator);

        Change change = RemoveKeyframe.create(path, keyframe2);
        change.apply(timeline);
        assertEquals("Keyframe was not removed", 2, path.getKeyframes().size());
        assertEquals("Wrong keyframe removed", keyframe0, actualKeyframe(0));
        assertEquals("Wrong keyframe removed", keyframe1, actualKeyframe(1));

        change.undo(timeline);
        assertEquals("Keyframe was not restored", 3, path.getKeyframes().size());
        assertEquals("Restored keyframes did not match original", keyframe0, actualKeyframe(0));
        assertEquals("Restored keyframes did not match original", keyframe1, actualKeyframe(1));
        assertEquals("Restored keyframes did not match original", keyframe2, actualKeyframe(2));
        assertEquals("Interpolator was not restored", interpolator, actualSegment(1).getInterpolator());
    }
}