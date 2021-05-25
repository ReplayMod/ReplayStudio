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
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.rar.PacketSink;

import java.io.IOException;

import static com.replaymod.replaystudio.util.Utils.readCompressedPacket;
import static com.replaymod.replaystudio.util.Utils.writeCompressedPacket;

public class PacketStateTree extends FullStateTree<Packet>  {

    private final PacketTypeRegistry registry;

    public PacketStateTree(PacketTypeRegistry registry, int index) {
        super(index);
        this.registry = registry;
    }

    @Override
    protected Packet read(NetInput in) throws IOException {
        return readCompressedPacket(registry, in);
    }

    @Override
    protected void discard(Packet value) {
        value.release();
    }

    @Override
    protected void apply(PacketSink sink, Packet value) throws IOException {
        sink.accept(value.retain());
    }

    public static class Builder extends FullStateTree.Builder<Packet> {
        @Override
        protected void write(NetOutput out, Packet value) throws IOException {
            writeCompressedPacket(out, value);
            value.release();
        }

        @Override
        protected void discard(Packet value) {
            value.release();
        }
    }
}
