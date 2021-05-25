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

package com.replaymod.replaystudio.rar.containers;

import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;

import java.io.IOException;
import java.util.Map;

public abstract class FullStateTree<T> extends StateTree<T> implements RandomAccessState {

    public FullStateTree(int index) {
        super(index);
    }

    protected abstract void apply(PacketSink sink, T value) throws IOException;

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        Map.Entry<Integer, T> lastUpdate = map.floorEntry(targetTime);
        if (lastUpdate != null && lastUpdate.getKey() > currentTimeStamp) {
            apply(sink, lastUpdate.getValue());
        }
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        Map.Entry<Integer, T> lastUpdate = map.floorEntry(targetTime);
        if (lastUpdate != null && !lastUpdate.getKey().equals(map.floorKey(currentTimeStamp))) {
            apply(sink, lastUpdate.getValue());
        }
    }

    public static abstract class Builder<T> extends StateTree.Builder<T> {
        public T getLatest() {
            return map.isEmpty() ? null : map.lastEntry().getValue();
        }
    }
}
