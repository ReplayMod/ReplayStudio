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
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.pathing.property.PropertyGroup;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.indexOf;

/**
 * Updates some properties of a property.
 */
public final class UpdateKeyframeProperties implements Change {

    private static String toId(Property property) {
        assert property != null;
        PropertyGroup group = property.getGroup();
        return (group != null ? group.getId() + ":" : "") + property.getId();
    }

    public static class Builder {
        private final int path;
        private final int keyframe;
        private final Map<String, Optional<Object>> updates = new HashMap<>();

        private Builder(int path, int keyframe) {
            this.path = path;
            this.keyframe = keyframe;
        }

        /**
         * Set the value for the property at this property.
         * If the property is not present, adds it.
         *
         * @param property The property
         * @param value    Value of the property, may be {@code null}
         * @param <T>      Type of the property
         * @return {@code this} for chaining
         */
        public <T> Builder setValue(Property<T> property, T value) {
            updates.put(toId(property), Optional.of((Object) value));
            return this;
        }

        /**
         * Remove the specified property from this property.
         *
         * @param property The property to be removed
         * @return {@code this} for chaining
         */
        public Builder removeProperty(Property property) {
            updates.put(toId(property), Optional.empty());
            return this;
        }

        public UpdateKeyframeProperties done() {
            return new UpdateKeyframeProperties(path, keyframe, updates);
        }
    }

    @NonNull
    public static UpdateKeyframeProperties.Builder create(@NonNull Path path, @NonNull Keyframe keyframe) {
        return new UpdateKeyframeProperties.Builder(path.getTimeline().getPaths().indexOf(path),
                indexOf(path.getKeyframes(), equalTo(keyframe)));
    }

    UpdateKeyframeProperties(int path, int index, Map<String, Optional<Object>> newValues) {
        this.path = path;
        this.index = index;
        this.newValues = newValues;
    }

    private final int path;
    private final int index;
    private final Map<String, Optional<Object>> newValues;
    private final Map<String, Optional<Object>> oldValues = new HashMap<>();
    private boolean applied;

    @SuppressWarnings("unchecked")
    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        Path path = timeline.getPaths().get(this.path);
        Keyframe keyframe = get(path.getKeyframes(), index);
        for (Map.Entry<String, Optional<Object>> entry : newValues.entrySet()) {
            Property property = timeline.getProperty(entry.getKey());
            if (property == null) throw new IllegalStateException("Property " + entry.getKey() + " unknown.");
            Optional<Object> newValue = entry.getValue();
            oldValues.put(entry.getKey(), keyframe.getValue(property));
            if (newValue.isPresent()) {
                keyframe.setValue(property, newValue.get());
            } else {
                keyframe.removeProperty(property);
            }
        }

        applied = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        Path path = timeline.getPaths().get(this.path);
        Keyframe keyframe = get(path.getKeyframes(), index);
        for (Map.Entry<String, Optional<Object>> entry : oldValues.entrySet()) {
            Property property = timeline.getProperty(entry.getKey());
            if (property == null) throw new IllegalStateException("Property " + entry.getKey() + " unknown.");
            Optional<Object> oldValue = entry.getValue();
            newValues.put(entry.getKey(), keyframe.getValue(property));
            if (oldValue.isPresent()) {
                keyframe.setValue(property, oldValue.get());
            } else {
                keyframe.removeProperty(property);
            }
        }

        applied = false;
    }
}
