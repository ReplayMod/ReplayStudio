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

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public abstract class StateTree<T> implements RandomAccessState {
    protected final int index;

    protected final TreeMap<Integer, T> map = new TreeMap<>();

    public StateTree(int index) {
        this.index = index;
    }

    protected abstract T read(NetInput in) throws IOException;
    protected abstract void discard(T value);

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        NetInput in = cache.seek(index);
        int time = 0;
        for (int i = in.readVarInt(); i > 0; i--) {
            time += in.readVarInt();
            map.put(time, read(in));
        }
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        map.values().forEach(this::discard);
        map.clear();
    }

    public static abstract class Builder<T> {

        protected final TreeMap<Integer, T> map = new TreeMap<>();

        protected abstract void write(NetOutput out, T value, int time) throws IOException;
        protected abstract void discard(T value);

        public void put(int time, T value) {
            T oldValue = map.put(time, value);
            if (oldValue != null) {
                discard(oldValue);
            }
        }

        public int build(WriteableCache cache) throws IOException {
            WriteableCache.Deferred out = cache.deferred();
            out.writeVarInt(map.size());
            int lastTime = 0;
            for (Map.Entry<Integer, T> entry : map.entrySet()) {
                int time = entry.getKey();
                out.writeVarInt(time - lastTime);
                lastTime = time;

                T value = entry.getValue();
                write(out, value, time);
            }

            map.clear();

            return out.commit();
        }
    }
}
