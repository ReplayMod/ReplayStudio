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
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketBlockChange;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData;
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.util.IPosition;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BlockStateTree extends DiffStateTree<Collection<BlockStateTree.BlockChange>>  {

    private final PacketTypeRegistry registry;

    public BlockStateTree(PacketTypeRegistry registry, int index) {
        super(index);
        this.registry = registry;
    }

    @Override
    protected Collection<BlockChange> read(NetInput in) throws IOException {
        List<BlockChange> list = new LinkedList<>(); // LinkedList to allow .descendingIterator

        for (int j = in.readVarInt(); j > 0; j--) {
            list.add(new BlockChange(
                    Packet.Reader.readPosition(registry, in),
                    in.readVarInt(),
                    in.readVarInt()
            ));
        }

        return list;
    }

    @Override
    protected void discard(Collection<BlockChange> value) {
    }

    @Override
    protected void play(PacketSink sink, Collection<BlockChange> value) throws IOException {
        for (BlockChange change : value) {
            sink.accept(new PacketBlockChange(change.pos, change.to).write(registry));
        }
    }

    @Override
    protected void rewind(PacketSink sink, Collection<BlockChange> value) throws IOException {
        for (Iterator<BlockChange> it = ((LinkedList<BlockChange>) value).descendingIterator(); it.hasNext(); ) {
            BlockChange update = it.next();
            sink.accept(PacketBlockChange.write(registry, update.pos, update.from));
        }
    }

    public static class Builder extends DiffStateTree.Builder<Collection<BlockChange>> {
        private final PacketTypeRegistry registry;
        private final DimensionType dimensionType;
        private final ListMultimap<Integer, BlockChange> blocks = Multimaps.newListMultimap(map, LinkedList::new); // LinkedList to allow .descendingIterator
        private final PacketChunkData.BlockStorage[] currentBlockState;

        public Builder(PacketTypeRegistry registry, DimensionType dimensionType, PacketChunkData.Column column) {
            this.registry = registry;
            this.dimensionType = dimensionType;
            this.currentBlockState = new PacketChunkData.BlockStorage[dimensionType.getSections()];

            PacketChunkData.Chunk[] chunks = column.chunks;
            for (int i = 0; i < currentBlockState.length; i++) {
                currentBlockState[i] = i >= chunks.length || chunks[i] == null ? new PacketChunkData.BlockStorage(registry) : chunks[i].blocks.copy();
            }
        }

        public void update(int time, PacketBlockChange record) {
            IPosition pos = record.getPosition();
            int sectionIndex = dimensionType.sectionYToIndex(pos.getY() >> 4);
            if (sectionIndex < 0 || sectionIndex >= currentBlockState.length) {
                return; // the server will send these if you try to place blocks outside the allowed range
            }
            PacketChunkData.BlockStorage blockStorage = currentBlockState[sectionIndex];
            int x = pos.getX() & 15, y = pos.getY() & 15, z = pos.getZ() & 15;
            int prevState = blockStorage.get(x, y, z);
            int newState = record.getId();
            blockStorage.set(x, y, z, newState);
            blocks.put(time, new BlockChange(pos, prevState, newState));
        }

        public void update(int time, PacketChunkData.Column column) {
            int sectionY = dimensionType.getMinY();
            int sectionIndex = 0;
            for (PacketChunkData.Chunk section : column.chunks) {
                if (section == null) {
                    sectionY++;
                    sectionIndex++;
                    continue;
                }
                PacketChunkData.BlockStorage toBlocks = section.blocks;
                PacketChunkData.BlockStorage fromBlocks = currentBlockState[sectionIndex];
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int fromState = fromBlocks.get(x, y, z);
                            int toState = toBlocks.get(x, y, z);
                            if (fromState != toState) {
                                IPosition pos = new IPosition(column.x << 4 | x, sectionY << 4 | y, column.z << 4 | z);
                                blocks.put(time, new BlockChange(pos, fromState, toState));
                            }
                        }
                    }
                }
                currentBlockState[sectionIndex] = toBlocks;
                sectionY++;
                sectionIndex++;
            }

        }

        @Override
        protected void write(NetOutput out, Collection<BlockChange> value) throws IOException {
            out.writeVarInt(value.size());
            for (BlockChange blockChange : value) {
                Packet.Writer.writePosition(registry, out, blockChange.pos);
                out.writeVarInt(blockChange.from);
                out.writeVarInt(blockChange.to);
            }
        }

        @Override
        protected void discard(Collection<BlockChange> value) {
        }
    }

    public static class BlockChange {
        public IPosition pos;
        public int from;
        public int to;

        public BlockChange(IPosition pos, int from, int to) {
            this.pos = pos;
            this.from = from;
            this.to = to;
        }
    }
}
