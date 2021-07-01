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
package com.replaymod.replaystudio.protocol.packets;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PacketChunkData {
    private Column column;

    private boolean isUnload;
    private int unloadX;
    private int unloadZ;

    public static PacketChunkData read(Packet packet) throws IOException {
        PacketChunkData chunkData = new PacketChunkData();
        try (Packet.Reader reader = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_9)) {
                if (packet.getType() == PacketType.UnloadChunk) {
                    chunkData.readUnload(reader);
                } else {
                    chunkData.readLoad(packet, reader);
                }
            } else {
                chunkData.readLoad(packet, reader);
            }
        }
        return chunkData;
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        PacketType packetType;
        boolean atLeastV1_9 = ProtocolVersion.getIndex(registry.getVersion()) >= ProtocolVersion.getIndex(ProtocolVersion.v1_9);
        if (atLeastV1_9) {
            packetType = isUnload ? PacketType.UnloadChunk : PacketType.ChunkData;
        } else {
            packetType = PacketType.ChunkData;
        }
        Packet packet = new Packet(registry, packetType);
        try (Packet.Writer writer = packet.overwrite()) {
            if (atLeastV1_9) {
                if (isUnload) {
                    writeUnload(writer);
                } else {
                    writeLoad(packet, writer);
                }
            } else {
                writeLoad(packet, writer);
            }
        }
        return packet;
    }

    public static List<Column> readBulk(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                return readBulkV1_8(packet, in);
            } else {
                return readBulkV1_7(packet, in);
            }
        }
    }

    private static List<Column> readBulkV1_8(Packet packet, Packet.Reader in) throws IOException {
        List<Column> result = new ArrayList<>();

        boolean skylight = in.readBoolean();
        int columns = in.readVarInt();

        int[] xs = new int[columns];
        int[] zs = new int[columns];
        BitSet[] masks = new BitSet[columns];
        int[] lengths = new int[columns];
        for (int column = 0; column < columns; column++) {
            xs[column] = in.readInt();
            zs[column] = in.readInt();
            masks[column] = in.readBitSet();
            int nChunks = masks[column].cardinality();
            int length = (nChunks * ((4096 * 2) + 2048)) + (skylight ? nChunks * 2048 : 0) + 256;
            lengths[column] = length;
        }

        for (int column = 0; column < columns; column++) {
            byte[] buf = new byte[lengths[column]];
            in.readBytes(buf);
            result.add(readColumn(packet, buf, xs[column], zs[column], true, skylight, masks[column], new BitSet(), null, null, false));
        }
        return result;
    }

    private static List<Column> readBulkV1_7(Packet packet, Packet.Reader in) throws IOException {
        List<Column> result = new ArrayList<>();

        // Read packet base data.
        short columns = in.readShort();
        int deflatedLength = in.readInt();
        boolean skylight = in.readBoolean();
        byte[] deflatedBytes = in.readBytes(deflatedLength);
        // Inflate chunk data.
        byte[] inflated = new byte[196864 * columns];
        Inflater inflater = new Inflater();
        inflater.setInput(deflatedBytes, 0, deflatedLength);
        try {
            inflater.inflate(inflated);
        } catch(DataFormatException e) {
            throw new IOException("Bad compressed data format");
        } finally {
            inflater.end();
        }

        // Cycle through and read all columns.
        int pos = 0;
        for(int count = 0; count < columns; count++) {
            // Read column-specific data.
            int x = in.readInt();
            int z = in.readInt();
            BitSet chunkMask = in.readBitSet();
            BitSet extendedChunkMask = in.readBitSet();
            // Determine column data length.
            int chunks = chunkMask.cardinality();
            int extended = extendedChunkMask.cardinality();

            int length = (8192 * chunks + 256) + (2048 * extended);
            if(skylight) {
                length += 2048 * chunks;
            }

            // Copy column data into a new array.
            byte[] buf = new byte[length];
            System.arraycopy(inflated, pos, buf, 0, length);
            // Read data into chunks and biome data.
            result.add(readColumn(packet, buf, x, z, true, skylight, chunkMask, extendedChunkMask, null, null, false));
            pos += length;
        }

        return result;
    }

    public static PacketChunkData load(Column column) {
        PacketChunkData chunkData = new PacketChunkData();
        chunkData.column = column;
        return chunkData;
    }

    public static PacketChunkData unload(int chunkX, int chunkZ) {
        PacketChunkData chunkData = new PacketChunkData();
        chunkData.isUnload = true;

        // Post 1.9
        chunkData.unloadX = chunkX;
        chunkData.unloadZ = chunkZ;

        // Pre 1.9
        chunkData.column = new Column(chunkX, chunkZ, new Chunk[16], new byte[256], null, null, null, false);

        return chunkData;
    }

    private PacketChunkData() {
    }

    public Column getColumn() {
        return column;
    }

    public boolean isUnload() {
        return isUnload;
    }

    public int getUnloadX() {
        return unloadX;
    }

    public int getUnloadZ() {
        return unloadZ;
    }

    private void readUnload(NetInput in) throws IOException {
        this.isUnload = true;
        this.unloadX = in.readInt();
        this.unloadZ = in.readInt();
    }

    private void writeUnload(NetOutput out) throws IOException {
        out.writeInt(this.unloadX);
        out.writeInt(this.unloadZ);
    }

    private void readLoad(Packet packet, Packet.Reader in) throws IOException {
        int x = in.readInt();
        int z = in.readInt();
        boolean fullChunk;
        if (packet.atLeast(ProtocolVersion.v1_17)) {
            fullChunk = true;
        } else {
            fullChunk = in.readBoolean();
        }
        boolean useExistingLightData = fullChunk;
        if (packet.atLeast(ProtocolVersion.v1_16) && !packet.atLeast(ProtocolVersion.v1_16_2)) {
            useExistingLightData = in.readBoolean();
        }
        BitSet chunkMask = in.readBitSet();
        BitSet extendedChunkMask;
        if (packet.atLeast(ProtocolVersion.v1_8)) {
            extendedChunkMask = new BitSet();
        } else {
            extendedChunkMask = BitSet.valueOf(new long[] { in.readUnsignedShort() });
        }
        CompoundTag heightmaps = null;
        if (packet.atLeast(ProtocolVersion.v1_14)) {
            heightmaps = in.readNBT();
        }
        int[] biomes = null;
        if (packet.atLeast(ProtocolVersion.v1_15) && fullChunk) {
            if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                biomes = new int[in.readVarInt()];
                for (int i = 0; i < biomes.length; i++) {
                    biomes[i] = in.readVarInt();
                }
            } else {
                biomes = in.readInts(1024);
            }
        }

        byte[] data;
        if (packet.atLeast(ProtocolVersion.v1_8)) {
            data = in.readBytes(in.readVarInt());
        } else {
            byte[] deflated = in.readBytes(in.readInt());
            // Determine inflated data length.
            int len = 12288 * chunkMask.cardinality();
            if (fullChunk) {
                len += 256;
            }
            data = new byte[len];
            // Inflate chunk data.
            Inflater inflater = new Inflater();
            inflater.setInput(deflated, 0, deflated.length);
            try {
                inflater.inflate(data);
            } catch (DataFormatException e) {
                throw new IOException("Bad compressed data format");
            } finally {
                inflater.end();
            }
        }
        this.column = readColumn(packet, data, x, z, fullChunk, false, chunkMask, extendedChunkMask, heightmaps, biomes, useExistingLightData);

        if (packet.atLeast(ProtocolVersion.v1_9_3)) {
            CompoundTag[] tileEntities = new CompoundTag[in.readVarInt()];
            for (int i = 0; i < tileEntities.length; i++) {
                tileEntities[i] = in.readNBT();
            }
            this.column.tileEntities = tileEntities;
        }

        if (packet.atMost(ProtocolVersion.v1_8) && fullChunk && chunkMask.isEmpty()) {
            isUnload = true;
            unloadX = x;
            unloadZ = z;
        }
    }

    private void writeLoad(Packet packet, Packet.Writer out) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        NetOutput netOut = new StreamNetOutput(byteOut);
        Pair<BitSet, BitSet> masks = writeColumn(packet, netOut, this.column, this.column.isFull());
        BitSet mask = masks.getKey();
        BitSet extendedMask = masks.getValue();

        out.writeInt(this.column.x);
        out.writeInt(this.column.z);
        if (packet.olderThan(ProtocolVersion.v1_17)) {
            out.writeBoolean(this.column.isFull());
        }
        if (packet.atLeast(ProtocolVersion.v1_16) && !packet.atLeast(ProtocolVersion.v1_16_2)) {
            out.writeBoolean(this.column.useExistingLightData);
        }
        out.writeBitSet(mask);
        if (!packet.atLeast(ProtocolVersion.v1_8)) {
            out.writeBitSet(extendedMask);
        }
        if (packet.atLeast(ProtocolVersion.v1_14)) {
            out.writeNBT(this.column.heightMaps);
        }
        int[] biomes = this.column.biomes;
        if (packet.atLeast(ProtocolVersion.v1_15) && biomes != null) {
            if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                out.writeVarInt(biomes.length);
                for (int biome : biomes) {
                    out.writeVarInt(biome);
                }
            } else {
                out.writeInts(biomes);
            }
        }
        int len;
        byte[] data;
        if (packet.atLeast(ProtocolVersion.v1_8)) {
            len = byteOut.size();
            data = byteOut.toByteArray();
        } else {
            Deflater deflater = new Deflater(-1);
            len = byteOut.size();
            data = new byte[len];
            try {
                deflater.setInput(byteOut.toByteArray(), 0, len);
                deflater.finish();
                len = deflater.deflate(data);
            } finally {
                deflater.end();
            }
        }
        out.writeVarInt(len);
        out.writeBytes(data, len);
        if (packet.atLeast(ProtocolVersion.v1_9_3)) {
            out.writeVarInt(this.column.tileEntities.length);
            for (CompoundTag tag : this.column.tileEntities) {
                out.writeNBT(tag);
            }
        }
    }

    private static Column readColumn(Packet packet, byte[] data, int x, int z, boolean fullChunk, boolean hasSkylight, BitSet mask, BitSet extendedMask, CompoundTag heightmaps, int[] biomes, boolean useExistingLightData) throws IOException {
        NetInput in = new StreamNetInput(new ByteArrayInputStream(data));
        if (packet.atLeast(ProtocolVersion.v1_17)) {
            Chunk[] chunks = new Chunk[mask.length()];
            for (int index = 0; index < chunks.length; index++) {
                if (mask.get(index)) {
                    Chunk chunk = new Chunk();
                    chunk.blocks = new BlockStorage(packet, in);
                    chunks[index] = chunk;
                }
            }
            return new Column(x, z, chunks, null, null, heightmaps, biomes, useExistingLightData);
        }

        // Pre 1.17
        Throwable ex = null;
        Column column = null;
        try {
            Chunk[] chunks = new Chunk[16];
            for(int index = 0; index < chunks.length; index++) {
                if (mask.get(index)) {
                    Chunk chunk = new Chunk();
                    if (packet.atLeast(ProtocolVersion.v1_9)) {
                        chunk.blocks = new BlockStorage(packet, in);
                        if (packet.atMost(ProtocolVersion.v1_13_2)) {
                            chunk.blockLight = in.readBytes(2048);
                            chunk.skyLight = hasSkylight ? in.readBytes(2048) : null;
                        }
                    } else {
                        chunk.blocks = new BlockStorage(packet);
                    }
                    chunks[index] = chunk;
                }
            }

            if (!packet.atLeast(ProtocolVersion.v1_9)) {
                if (packet.atLeast(ProtocolVersion.v1_8)) {
                    for (Chunk chunk : chunks) {
                        if (chunk != null) chunk.blocks.storage = FlexibleStorage.from(packet.getRegistry(), 0, 4096, in.readLongs(1024));
                    }
                } else {
                    for (Chunk chunk : chunks) {
                        if (chunk != null) chunk.blocks.storage = FlexibleStorage.from(packet.getRegistry(), 0, 4096, in.readLongs(512));
                    }
                    for (Chunk chunk : chunks) {
                        if (chunk != null) chunk.blocks.metadata = in.readLongs(256);
                    }
                }
                for (Chunk chunk : chunks) {
                    if (chunk != null) chunk.blockLight = in.readBytes(2048);
                }
                if (hasSkylight) {
                    for (Chunk chunk : chunks) {
                        if (chunk != null) chunk.skyLight = in.readBytes(2048);
                    }
                }
                // extendedMask should be 0 for 1.8+
                for (int index = 0; index < chunks.length; index++) {
                    if (extendedMask.get(index)) {
                        if (chunks[index] == null) {
                            in.readLongs(256);
                        } else {
                            chunks[index].blocks.extended = in.readLongs(256);
                        }
                    }
                }
            }

            byte[] biomeData = null;
            if (fullChunk && in.available() > 0 && !packet.atLeast(ProtocolVersion.v1_15)) {
                biomeData = in.readBytes(packet.atLeast(ProtocolVersion.v1_13) ? 1024 : 256);
            }

            column = new Column(x, z, chunks, biomeData, null, heightmaps, biomes, useExistingLightData);
        } catch(Throwable e) {
            ex = e;
        }

        // Unfortunately, this is needed to detect whether the chunks contain skylight or not.
        if((in.available() > 0 || ex != null) && !hasSkylight) {
            return readColumn(packet, data, x, z, fullChunk, true, mask, extendedMask, heightmaps, biomes, useExistingLightData);
        } else if(ex != null) {
            throw new IOException("Failed to read chunk data.", ex);
        }

        return column;
    }

    private static Pair<BitSet, BitSet> writeColumn(Packet packet, NetOutput out, Column column, boolean fullChunk) throws IOException {
        BitSet mask = new BitSet();
        BitSet extendedMask = new BitSet();
        Chunk[] chunks = column.chunks;
        for (int index = 0; index < chunks.length; index++) {
            Chunk chunk = chunks[index];
            if (chunk != null) {
                mask.set(index);
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    chunk.blocks.write(packet, out);
                    if (packet.atMost(ProtocolVersion.v1_13_2)) {
                        out.writeBytes(chunk.blockLight);
                        if (chunk.skyLight != null) {
                            out.writeBytes(chunk.skyLight);
                        }
                    }
                }
            }
        }

        if (!packet.atLeast(ProtocolVersion.v1_9)) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                for (Chunk chunk : chunks) {
                    if (chunk != null) out.writeLongs(chunk.blocks.storage.data);
                }
            } else {
                for (Chunk chunk : chunks) {
                    if (chunk != null) out.writeLongs(chunk.blocks.storage.data);
                }
                for (Chunk chunk : chunks) {
                    if (chunk != null) out.writeLongs(chunk.blocks.metadata);
                }
            }
            for (Chunk chunk : chunks) {
                if (chunk != null) out.writeBytes(chunk.blockLight);
            }
            for (Chunk chunk : chunks) {
                if (chunk != null && chunk.skyLight != null) out.writeBytes(chunk.skyLight);
            }
            // extended should be null for 1.8+
            for (int index = 0; index < chunks.length; index++) {
                if (chunks[index] != null && chunks[index].blocks.extended != null) {
                    extendedMask.set(index);
                    out.writeLongs(chunks[index].blocks.extended);
                }
            }
        }

        if (fullChunk && column.biomeData != null && !packet.atLeast(ProtocolVersion.v1_15)) {
            out.writeBytes(column.biomeData);
        }

        return Pair.of(mask, extendedMask);
    }

    public static class Column {
        public int x;
        public int z;
        public Chunk[] chunks;
        public byte[] biomeData; // pre 1.15
        public CompoundTag[] tileEntities;
        public CompoundTag heightMaps;
        public int[] biomes; // 1.15+
        public boolean useExistingLightData; // 1.16+

        public Column(int x, int z, Chunk[] chunks, byte[] biomeData, CompoundTag[] tileEntities, CompoundTag heightmaps, int[] biomes, boolean useExistingLightData) {
            this.x = x;
            this.z = z;
            this.chunks = chunks;
            this.biomeData = biomeData;
            this.tileEntities = tileEntities;
            this.heightMaps = heightmaps;
            this.biomes = biomes;
            this.useExistingLightData = useExistingLightData;
        }

        public boolean isFull() {
            return this.biomeData != null || this.biomes != null;
        }

        public static long coordToLong(int x, int z) {
            return (long)x << 32 | (long)z & 0xFFFFFFFFL;
        }

        public static int longToX(long v) {
            return (int) (v >> 32);
        }

        public static int longToZ(long v) {
            return (int) (v & 0xFFFFFFFFL);
        }

        public long coordToLong() {
            return coordToLong(x, z);
        }
    }

    public static class Chunk {
        public BlockStorage blocks;
        public byte[] blockLight;
        public byte[] skyLight;

        public Chunk copy() {
            Chunk copy = new Chunk();
            copy.blocks = this.blocks != null ? this.blocks.copy() : null;
            copy.blockLight = this.blockLight != null ? this.blockLight.clone() : null;
            copy.skyLight = this.skyLight != null ? this.skyLight.clone() : null;
            return copy;
        }
    }

    public static class BlockStorage {
        private final PacketTypeRegistry registry;
        private int blockCount;
        private int bitsPerEntry;
        private List<Integer> states;
        private FlexibleStorage storage;
        private long[] metadata; // 1.7 only
        private long[] extended; // 1.7 only

        private BlockStorage(BlockStorage from) {
            this.registry = from.registry;
            this.blockCount = from.blockCount;
            this.bitsPerEntry = from.bitsPerEntry;
            if (from.states != null) {
                this.states = new ArrayList<>(from.states);
            }
            if (from.storage != null) {
                this.storage = FlexibleStorage.from(registry, bitsPerEntry, from.storage.entries, from.storage.data.clone());
            }
            if (from.metadata != null) {
                this.metadata = from.metadata.clone();
            }
            if (from.extended != null) {
                this.extended = from.extended.clone();
            }
        }

        /**
         * 1.9+
         */
        public BlockStorage(PacketTypeRegistry registry) {
            this.registry = registry;
            this.blockCount = 0;
            this.bitsPerEntry = 4;

            this.states = new ArrayList<>();
            this.states.add(0);

            this.storage = FlexibleStorage.empty(registry, bitsPerEntry, 4096);
        }

        // 1.7-1.8
        BlockStorage(Packet packet) {
            this.registry = packet.getRegistry();
        }

        // 1.9+
        BlockStorage(Packet packet, NetInput in) throws IOException {
            this.registry = packet.getRegistry();
            if (packet.atLeast(ProtocolVersion.v1_14)) {
                this.blockCount = in.readShort();
            }
            this.bitsPerEntry = in.readUnsignedByte();
            this.states = new ArrayList<>();
            int stateCount = this.bitsPerEntry > 8 && packet.atLeast(ProtocolVersion.v1_13) ? 0 : in.readVarInt();
            for(int i = 0; i < stateCount; ++i) {
                this.states.add(in.readVarInt());
            }

            this.storage = FlexibleStorage.from(registry, bitsPerEntry, 4096, in.readLongs(in.readVarInt()));
        }

        // 1.9+
        void write(Packet packet, NetOutput out) throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_14)) {
                out.writeShort(this.blockCount);
            }
            out.writeByte(this.bitsPerEntry);
            if (this.bitsPerEntry <= 8 || !packet.atLeast(ProtocolVersion.v1_13)) {
                out.writeVarInt(this.states.size());
                for (Integer state : this.states) {
                    out.writeVarInt(state);
                }
            }

            out.writeVarInt(storage.data.length);
            out.writeLongs(storage.data);
        }

        private static int index(int x, int y, int z) {
            return y << 8 | z << 4 | x;
        }

        /**
         * Only 1.9+
         */
        public int get(int x, int y, int z) {
            int id = this.storage.get(index(x, y, z));
            return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : 0) : id;
        }

        /**
         * Only 1.9+
         */
        public void set(int x, int y, int z, int state) {
            int id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state;
            if(id == -1) {
                this.states.add(state);
                if(this.states.size() > 1 << this.bitsPerEntry) {
                    this.bitsPerEntry++;

                    List<Integer> oldStates = this.states;
                    if(this.bitsPerEntry > 8) {
                        oldStates = new ArrayList<Integer>(this.states);
                        this.states.clear();
                        this.bitsPerEntry = 13;
                    }

                    FlexibleStorage oldStorage = this.storage;
                    this.storage = FlexibleStorage.empty(this.registry, this.bitsPerEntry, this.storage.entries);
                    for(int index = 0; index < this.storage.entries; index++) {
                        this.storage.set(index, oldStorage.get(index));
                    }
                }

                id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state;
            }

            int ind = index(x, y, z);
            int curr = this.storage.get(ind);
            if(state != 0 && curr == 0) {
                this.blockCount++;
            } else if(state == 0 && curr != 0) {
                this.blockCount--;
            }

            this.storage.set(ind, id);
        }

        public BlockStorage copy() {
            return new BlockStorage(this);
        }
    }

    private static abstract class FlexibleStorage {
        protected final long[] data;
        protected final int bitsPerEntry;
        protected final int entries;
        protected final long maxEntryValue;

        protected FlexibleStorage(long[] data, int bitsPerEntry, int entries) {
            this.data = data;
            this.bitsPerEntry = Math.max(bitsPerEntry, 4);
            this.entries = entries;
            this.maxEntryValue = (1L << this.bitsPerEntry) - 1;
        }

        public abstract int get(int index);
        public abstract void set(int index, int value);

        static FlexibleStorage empty(PacketTypeRegistry registry, int bitsPerEntry, int entries) {
            if (registry.atLeast(ProtocolVersion.v1_16)) {
                return new PaddedFlexibleStorage(bitsPerEntry, entries);
            } else {
                return new CompactFlexibleStorage(bitsPerEntry, entries);
            }
        }

        static FlexibleStorage from(PacketTypeRegistry registry, int bitsPerEntry, int entries, long[] data) {
            if (registry.atLeast(ProtocolVersion.v1_16)) {
                return new PaddedFlexibleStorage(bitsPerEntry, entries, data);
            } else {
                return new CompactFlexibleStorage(bitsPerEntry, entries, data);
            }
        }
    }

    private static class PaddedFlexibleStorage extends FlexibleStorage {
        private final int entriesPerLong;

        public PaddedFlexibleStorage(int bitsPerEntry, int entries) {
            this(bitsPerEntry, entries, new long[longsForEntries(bitsPerEntry, entries)]);
        }

        public PaddedFlexibleStorage(int bitsPerEntry, int entries, long[] data) {
            super(data, bitsPerEntry, entries);
            this.entriesPerLong = 64 / this.bitsPerEntry;
        }

        private static int longsForEntries(int bitsPerEntry, int entries) {
            int entriesPerLong = 64 / Math.max(bitsPerEntry, 4);
            return (entries + entriesPerLong - 1) / entriesPerLong;
        }

        @Override
        public int get(int index) {
            if (index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }

            int blockIndex = index / this.entriesPerLong;
            int subIndex = index % this.entriesPerLong;
            int subIndexBits = subIndex * this.bitsPerEntry;
            return (int) (this.data[blockIndex] >>> subIndexBits & this.maxEntryValue);
        }

        @Override
        public void set(int index, int value) {
            if (index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }

            if (value < 0 || value > this.maxEntryValue) {
                throw new IllegalArgumentException("Value cannot be outside of accepted range.");
            }

            int blockIndex = index / this.entriesPerLong;
            int subIndex = index % this.entriesPerLong;
            int subIndexBits = subIndex * this.bitsPerEntry;
            this.data[blockIndex] = this.data[blockIndex] & ~(this.maxEntryValue << subIndexBits) | ((long) value & this.maxEntryValue) << subIndexBits;
        }
    }

    private static class CompactFlexibleStorage extends FlexibleStorage {
        public CompactFlexibleStorage(int bitsPerEntry, int entries) {
            this(bitsPerEntry, entries, new long[roundToNearest(entries * bitsPerEntry, 64) / 64]);
        }

        public CompactFlexibleStorage(int bitsPerEntry, int entries, long[] data) {
            super(data, bitsPerEntry, entries);
        }

        private static int roundToNearest(int value, int roundTo) {
            if(roundTo == 0) {
                return 0;
            } else if(value == 0) {
                return roundTo;
            } else {
                if(value < 0) {
                    roundTo *= -1;
                }

                int remainder = value % roundTo;
                return remainder != 0 ? value + roundTo - remainder : value;
            }
        }

        @Override
        public int get(int index) {
            if(index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }

            int bitIndex = index * this.bitsPerEntry;
            int startIndex = bitIndex / 64;
            int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
            int startBitSubIndex = bitIndex % 64;
            if(startIndex == endIndex) {
                return (int) (this.data[startIndex] >>> startBitSubIndex & this.maxEntryValue);
            } else {
                int endBitSubIndex = 64 - startBitSubIndex;
                return (int) ((this.data[startIndex] >>> startBitSubIndex | this.data[endIndex] << endBitSubIndex) & this.maxEntryValue);
            }
        }

        @Override
        public void set(int index, int value) {
            if(index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }

            if(value < 0 || value > this.maxEntryValue) {
                throw new IllegalArgumentException("Value cannot be outside of accepted range.");
            }

            int bitIndex = index * this.bitsPerEntry;
            int startIndex = bitIndex / 64;
            int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
            int startBitSubIndex = bitIndex % 64;
            this.data[startIndex] = this.data[startIndex] & ~(this.maxEntryValue << startBitSubIndex) | ((long) value & this.maxEntryValue) << startBitSubIndex;
            if(startIndex != endIndex) {
                int endBitSubIndex = 64 - startBitSubIndex;
                this.data[endIndex] = this.data[endIndex] >>> endBitSubIndex << endBitSubIndex | ((long) value & this.maxEntryValue) >> endBitSubIndex;
            }
        }
    }
}
