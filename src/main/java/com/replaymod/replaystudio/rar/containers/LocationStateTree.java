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
import com.replaymod.replaystudio.util.IOBiConsumer;
import com.replaymod.replaystudio.util.Location;

import java.io.IOException;
import java.util.function.BiConsumer;

public abstract class LocationStateTree extends FullStateTree<Location>  {

    public LocationStateTree(int index) {
        super(index);
    }

    @Override
    protected Location read(NetInput in) throws IOException {
        return new Location(in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat());
    }

    @Override
    protected void discard(Location value) {
    }

    public static class Builder extends FullStateTree.Builder<Location> {
        @Override
        protected void write(NetOutput out, Location value) throws IOException {
            out.writeDouble(value.getX());
            out.writeDouble(value.getY());
            out.writeDouble(value.getZ());
            out.writeFloat(value.getYaw());
            out.writeFloat(value.getPitch());
        }

        @Override
        protected void discard(Location value) {
        }
    }

    private static class ConsumerBased extends LocationStateTree {

        private final IOBiConsumer<PacketSink, Location> apply;

        public ConsumerBased(int index, IOBiConsumer<PacketSink, Location> apply) {
            super(index);
            this.apply = apply;
        }

        @Override
        protected void apply(PacketSink sink, Location value) throws IOException {
            apply.accept(sink, value);
        }
    }

    public static LocationStateTree withApply(int index, IOBiConsumer<PacketSink, Location> apply) {
        return new ConsumerBased(index, apply);
    }
}
