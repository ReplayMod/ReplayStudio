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
package com.replaymod.replaystudio.pathing.impl;

import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;

import java.util.*;

public class TimelineImpl implements Timeline {
    private final List<Path> paths = new ArrayList<>();
    private Map<String, Property> properties = new HashMap<>();
    private Deque<Change> undoStack = new ArrayDeque<>();
    private Deque<Change> redoStack = new ArrayDeque<>();
    private List<Long> tickTimestamps;
    

    @Override
    public Optional<List<Long>> getTickTimestamps(){
        if (tickTimestamps == null) return Optional.empty();
        return Optional.of(tickTimestamps);
    }

    @Override
    public void setTickTimestamps(List<Long> tickTimestamps){
        this.tickTimestamps = tickTimestamps;
    }

    @Override
    public List<Path> getPaths() {
        return paths;
    }

    @Override
    public Path createPath() {
        Path path = new PathImpl(this);
        paths.add(path);
        return path;
    }

    @Override
    public <T> Optional<T> getValue(Property<T> property, long time) {
        for (Path path : paths) {
            if (path.isActive()) {
                Optional<T> value = path.getValue(property, time);
                if (value.isPresent()) {
                    return value;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void applyToGame(long time, Object replayHandler) {
        for (Property<?> property : properties.values()) {
            applyToGame(time, replayHandler, property);
        }
    }

    private <T> void applyToGame(long time, Object replayHandler, Property<T> property) {
        Optional<T> value = getValue(property, time);
        if (value.isPresent()) {
            property.applyToGame(value.get(), replayHandler);
        }
    }

    @Override
    public void registerProperty(Property property) {
        String id = (property.getGroup() == null ? "" : property.getGroup().getId() + ":") + property.getId();
        properties.put(id, property);
    }

    @Override
    public Property getProperty(String id) {
        return properties.get(id);
    }

    @Override
    public void applyChange(Change change) {
        change.apply(this);
        pushChange(change);
    }

    @Override
    public void pushChange(Change change) {
        undoStack.push(change);
        redoStack.clear();
    }

    @Override
    public void undoLastChange() {
        Change change = undoStack.pop();
        change.undo(this);
        redoStack.push(change);
    }

    @Override
    public void redoLastChange() {
        Change change = redoStack.pop();
        change.apply(this);
        undoStack.push(change);
    }

    @Override
    public Change peekUndoStack() {
        return undoStack.peek();
    }

    @Override
    public Change peekRedoStack() {
        return undoStack.peek();
    }
}
