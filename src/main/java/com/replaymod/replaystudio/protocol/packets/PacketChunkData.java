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
import com.replaymod.replaystudio.lib.viaversion.api.minecraft.chunks.PaletteType;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.util.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PacketChunkData {
    private Column column;

    private boolean isUnload;
    private int unloadX;
    private int unloadZ;

    public static PacketChunkData read(Packet packet, int sections) throws IOException {
        PacketChunkData chunkData = new PacketChunkData();
        try (Packet.Reader reader = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_9)) {
                if (packet.getType() == PacketType.UnloadChunk) {
                    chunkData.readUnload(reader);
                } else {
                    chunkData.readLoad(packet, reader, sections);
                }
            } else {
                chunkData.readLoad(packet, reader, sections);
            }
        }
        return chunkData;
    }

    public static PacketChunkData readUnload(Packet packet) throws IOException {
        PacketChunkData chunkData = new PacketChunkData();
        try (Packet.Reader reader = packet.reader()) {
            chunkData.readUnload(reader);
        }
        return chunkData;
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        PacketType packetType;
        boolean atLeastV1_9 = ProtocolVersion.getIndex(registry.getVersion()) >= ProtocolVersion.getIndex(ProtocolVersion.v1_9);
        if (atLeastV1_9) {
            packetType = isUnload ? PacketType.UnloadChunk : PacketType.ChunkData;
        } else {
            // On these versions, there's an ambiguity between the unload packet and a packet which loads an empty chunk
            // because the latter is special cased to unload rather than load. The only way to load an empty chunk
            // therefore is to use the BulkChunkData packet, which doesn't have this special handling.
            packetType = !isUnload && column.looksLikeUnloadOnMC1_8() ? PacketType.BulkChunkData : PacketType.ChunkData;
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
                if (packetType == PacketType.BulkChunkData) {
                    if (packet.atLeast(ProtocolVersion.v1_8)) {
                        writeBulkV1_8(packet, writer, Collections.singletonList(column));
                    } else {
                        writeBulkV1_7(packet, writer, Collections.singletonList(column));
                    }
                } else {
                    writeLoad(packet, writer);
                }
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

    private static void writeBulkV1_8(Packet packet, Packet.Writer out, List<Column> columns) throws IOException {
        out.writeBoolean(columns.stream().anyMatch(Column::hasSkyLightV1_8));
        out.writeVarInt(columns.size());

        for (Column column : columns) {
            out.writeInt(column.x);
            out.writeInt(column.z);
            out.writeBitSet(column.getChunkMask());
        }

        for (Column column : columns) {
            writeColumn(packet, out, column, true);
        }
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

    private static void writeBulkV1_7(Packet packet, Packet.Writer out, List<Column> columns) throws IOException {
        throw new UnsupportedOperationException("writeBulkV1_7 is not yet implemented");
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
        chunkData.column = new Column(chunkX, chunkZ, new Chunk[16], new byte[256], null, null, null, false, null);

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

    private void readLoad(Packet packet, Packet.Reader in, int sections) throws IOException {
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
        BitSet chunkMask;
        if (packet.atLeast(ProtocolVersion.v1_18)) {
            // With 1.18, MC always sends all sections. You might think that we may be able to infer the number of
            // sections based on how many bytes are left and while that does sound reasonable, there's one major issue:
            // The vanilla packet sizing code is broken. It allocates one byte for the IdListPalette even though that
            // one does not send any data. As a result there may be garbage padding at the end (it's just vanilla which
            // is buggy, e.g. ViaVersion is sized properly; so we can't just rely on exception that either).
            chunkMask = new BitSet();
            chunkMask.set(0, sections);
        } else {
            chunkMask = in.readBitSet();
        }
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
        if (packet.atLeast(ProtocolVersion.v1_15) && packet.olderThan(ProtocolVersion.v1_18) && fullChunk) {
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
            TileEntity[] tileEntities = new TileEntity[in.readVarInt()];
            for (int i = 0; i < tileEntities.length; i++) {
                tileEntities[i] = new TileEntity(packet, in);
            }
            this.column.tileEntities = tileEntities;
        }

        if (packet.atLeast(ProtocolVersion.v1_18)) {
            this.column.lightData = PacketUpdateLight.readData(packet, in);
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
        if (packet.olderThan(ProtocolVersion.v1_18)) {
            out.writeBitSet(mask);
        }
        if (!packet.atLeast(ProtocolVersion.v1_8)) {
            out.writeBitSet(extendedMask);
        }
        if (packet.atLeast(ProtocolVersion.v1_14)) {
            out.writeNBT(this.column.heightMaps);
        }
        int[] biomes = this.column.biomes;
        if (packet.atLeast(ProtocolVersion.v1_15) && packet.olderThan(ProtocolVersion.v1_18) && biomes != null) {
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
            for (TileEntity tileEntity : this.column.tileEntities) {
                tileEntity.write(packet, out);
            }
        }
        if (packet.atLeast(ProtocolVersion.v1_18)) {
            PacketUpdateLight.writeData(packet, out, this.column.lightData);
        }
    }

    private static Column readColumn(Packet packet, byte[] data, int x, int z, boolean fullChunk, boolean hasSkylight, BitSet mask, BitSet extendedMask, CompoundTag heightmaps, int[] biomes, boolean useExistingLightData) throws IOException {
        NetInput in = new StreamNetInput(new ByteArrayInputStream(data));
        if (packet.atLeast(ProtocolVersion.v1_17)) {
            Chunk[] chunks = new Chunk[mask.length()];
            for (int index = 0; index < chunks.length; index++) {
                if (mask.get(index)) {
                    chunks[index] = new Chunk(packet, in);
                }
            }
            return new Column(x, z, chunks, null, null, heightmaps, biomes, useExistingLightData, null);
        }

        // Pre 1.17
        Throwable ex = null;
        Column column = null;
        try {
            Chunk[] chunks = new Chunk[16];
            for(int index = 0; index < chunks.length; index++) {
                if (mask.get(index)) {
                    Chunk chunk;
                    if (packet.atLeast(ProtocolVersion.v1_9)) {
                        chunk = new Chunk(packet, in);
                        if (packet.atMost(ProtocolVersion.v1_13_2)) {
                            chunk.blockLight = in.readBytes(2048);
                            chunk.skyLight = hasSkylight ? in.readBytes(2048) : null;
                        }
                    } else {
                        chunk = new Chunk(packet);
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

            column = new Column(x, z, chunks, biomeData, null, heightmaps, biomes, useExistingLightData, null);
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
                    chunk.write(packet, out);
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
        public TileEntity[] tileEntities;
        public CompoundTag heightMaps;
        public int[] biomes; // 1.15+ pre 1.18
        public boolean useExistingLightData; // 1.16+
        public PacketUpdateLight.Data lightData; // 1.18+

        public Column(int x, int z, Chunk[] chunks, byte[] biomeData, TileEntity[] tileEntities, CompoundTag heightmaps, int[] biomes, boolean useExistingLightData, PacketUpdateLight.Data lightData) {
            this.x = x;
            this.z = z;
            this.chunks = chunks;
            this.biomeData = biomeData;
            this.tileEntities = tileEntities;
            this.heightMaps = heightmaps;
            this.biomes = biomes;
            this.useExistingLightData = useExistingLightData;
            this.lightData = lightData;
        }

        public boolean isFull() {
            return this.biomeData != null || this.biomes != null || (this.lightData != null && this.tileEntities != null);
        }

        public boolean looksLikeUnloadOnMC1_8() {
            return isFull() && Utils.containsOnlyNull(chunks);
        }

        public BitSet getChunkMask() {
            BitSet mask = new BitSet();
            for (int index = 0; index < chunks.length; index++) {
                if (chunks[index] != null) {
                    mask.set(index);
                }
            }
            return mask;
        }

        public boolean hasSkyLightV1_8() {
            for (Chunk chunk : chunks) {
                if (chunk != null && chunk.skyLight != null) {
                    return true;
                }
            }
            return false;
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
        private final int blockCount;
        public final PalettedStorage blocks;
        public final PalettedStorage biomes; // 1.18+
        public byte[] blockLight; // pre 1.14
        public byte[] skyLight; // pre 1.14

        private Chunk(Chunk from) {
            this.blockCount = from.blockCount;
            this.blocks = from.blocks != null ? from.blocks.copy() : null;
            this.biomes = from.biomes != null ? from.biomes.copy() : null;
            this.blockLight = from.blockLight != null ? from.blockLight.clone() : null;
            this.skyLight = from.skyLight != null ? from.skyLight.clone() : null;
        }

        /**
         * 1.9+
         */
        public Chunk(PacketTypeRegistry registry) {
            this.blockCount = 0;
            this.blocks = new PalettedStorage(PaletteType.BLOCKS, registry);
            if (registry.atLeast(ProtocolVersion.v1_18)) {
                this.biomes = new PalettedStorage(PaletteType.BIOMES, registry);
            } else {
                this.biomes = null;
            }
        }

        // 1.7-1.8
        Chunk(Packet packet) {
            this.blockCount = 0;
            this.blocks = new PalettedStorage(PaletteType.BLOCKS, packet);
            this.biomes = null;
        }

        // 1.9+
        Chunk(Packet packet, NetInput in) throws IOException {
            this.blockCount = packet.atLeast(ProtocolVersion.v1_14) ? in.readShort() : 0;
            this.blocks = new PalettedStorage(PaletteType.BLOCKS, packet, in);
            if (packet.atLeast(ProtocolVersion.v1_18)) {
                this.biomes = new PalettedStorage(PaletteType.BIOMES, packet, in);
            } else {
                this.biomes = null;
            }
        }

        // 1.9+
        void write(Packet packet, NetOutput out) throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_14)) {
                out.writeShort(this.blockCount + this.blocks.countDelta);
            }
            this.blocks.write(packet, out);
            if (this.biomes != null) {
                this.biomes.write(packet, out);
            }
        }

        public Chunk copy() {
            return new Chunk(this);
        }
    }

    public static class PalettedStorage {
        private final PaletteType type;
        private final PacketTypeRegistry registry;
        private int countDelta;
        private int bitsPerEntry;
        private List<Integer> states;
        private FlexibleStorage storage;
        private long[] metadata; // 1.7 only
        private long[] extended; // 1.7 only

        private PalettedStorage(PalettedStorage from) {
            this.type = from.type;
            this.registry = from.registry;
            this.countDelta = from.countDelta;
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
        public PalettedStorage(PaletteType type, PacketTypeRegistry registry) {
            this.type = type;
            this.registry = registry;
            this.bitsPerEntry = type == PaletteType.BLOCKS ? 4 : 0;

            this.states = new ArrayList<>();
            this.states.add(0);

            this.storage = FlexibleStorage.empty(registry, bitsPerEntry, type.size());
        }

        // 1.7-1.8
        PalettedStorage(PaletteType type, Packet packet) {
            this.type = type;
            this.registry = packet.getRegistry();
            this.bitsPerEntry = type.highestBitsPerValue() + 1; // these versions never use a local palette
        }

        // 1.9+
        PalettedStorage(PaletteType type, Packet packet, NetInput in) throws IOException {
            this.type = type;
            this.registry = packet.getRegistry();
            this.bitsPerEntry = in.readUnsignedByte();
            this.states = new ArrayList<>();
            int stateCount;
            if (this.bitsPerEntry > type.highestBitsPerValue() && packet.atLeast(ProtocolVersion.v1_13)) {
                stateCount = 0;
            } else if (this.bitsPerEntry == 0 && packet.atLeast(ProtocolVersion.v1_18)) {
                stateCount = 1;
            } else {
                stateCount = in.readVarInt();
            }
            for(int i = 0; i < stateCount; ++i) {
                this.states.add(in.readVarInt());
            }

            this.storage = FlexibleStorage.from(registry, bitsPerEntry, type.size(), in.readLongs(in.readVarInt()));
        }

        // 1.9+
        void write(Packet packet, NetOutput out) throws IOException {
            out.writeByte(this.bitsPerEntry);
            if (this.bitsPerEntry == 0 && packet.atLeast(ProtocolVersion.v1_18)) {
                out.writeVarInt(this.states.get(0));
            } else if (this.bitsPerEntry <= type.highestBitsPerValue() || !packet.atLeast(ProtocolVersion.v1_13)) {
                out.writeVarInt(this.states.size());
                for (Integer state : this.states) {
                    out.writeVarInt(state);
                }
            }

            out.writeVarInt(storage.data.length);
            out.writeLongs(storage.data);
        }

        private int index(int x, int y, int z) {
            if (this.type == PaletteType.BIOMES) {
                return y << 4 | z << 2 | x;
            } else {
                return y << 8 | z << 4 | x;
            }
        }

        /**
         * Only 1.8+
         */
        public int get(int x, int y, int z) {
            if (this.bitsPerEntry == 0) {
                return this.states.get(0);
            }
            int id = this.storage.get(index(x, y, z));
            return this.bitsPerEntry <= type.highestBitsPerValue() ? (id >= 0 && id < this.states.size() ? this.states.get(id) : 0) : id;
        }

        /**
         * Only 1.8+
         */
        public void set(int x, int y, int z, int state) {
            int id = this.bitsPerEntry <= type.highestBitsPerValue() ? this.states.indexOf(state) : state;
            if(id == -1) {
                this.states.add(state);
                if(this.states.size() > 1 << this.bitsPerEntry) {
                    this.bitsPerEntry++;

                    List<Integer> oldStates = this.states;
                    if(this.bitsPerEntry > type.highestBitsPerValue()) {
                        oldStates = new ArrayList<Integer>(this.states);
                        this.states.clear();
                        // These match the size of the vanilla global palette and may be incorrect when it comes to
                        // modded servers.
                        // Unfortunately there is no easy way to determine what the actual size of the global palette
                        // should be without having to parse the mod loader handshake. Which is annoying, so it has not
                        // yet been implemented.
                        if (registry.atLeast(ProtocolVersion.v1_16)) {
                            this.bitsPerEntry = 15;
                        } else if (registry.atLeast(ProtocolVersion.v1_13)) {
                            this.bitsPerEntry = 14;
                        } else {
                            this.bitsPerEntry = 13;
                        }

                        // Luckily, we currently only modify the block storage for internal tracking, and as long as
                        // we don't send it, the actual size we use doesn't really matter and we can just increase it
                        // if it turns out to have been too small.
                        // We do this here initially, and then also below immediately before `set` if the new id is OOB.
                        int bitsUsed = (1 << this.bitsPerEntry) - 1;
                        for (int i = 0; i < this.storage.entries; i++) {
                            bitsUsed |= this.storage.get(i);
                        }
                        this.bitsPerEntry = 32 - Integer.numberOfLeadingZeros(bitsUsed);
                    }

                    FlexibleStorage oldStorage = this.storage;
                    this.storage = FlexibleStorage.empty(this.registry, this.bitsPerEntry, this.storage.entries);
                    for(int index = 0; index < this.storage.entries; index++) {
                        this.storage.set(index, oldStorage.get(index));
                    }
                }

                id = this.bitsPerEntry <= type.highestBitsPerValue() ? this.states.indexOf(state) : state;
            }

            if (this.bitsPerEntry == 0) {
                return;
            }

            int ind = index(x, y, z);
            int curr = this.storage.get(ind);
            if(state != 0 && curr == 0) {
                countDelta++;
            } else if(state == 0 && curr != 0) {
                countDelta--;
            }

            if (this.bitsPerEntry > type.highestBitsPerValue() && id > this.storage.maxEntryValue) {
                // Workaround for us not knowing the size of the global palette. See the two comment blocks above.
                // Determine how many bits we need per entry to fit this id
                this.bitsPerEntry = 32 - Integer.numberOfLeadingZeros(id);
                // Convert old storage to new entry size
                FlexibleStorage oldStorage = this.storage;
                this.storage = FlexibleStorage.empty(this.registry, this.bitsPerEntry, this.storage.entries);
                for (int i = 0; i < this.storage.entries; i++) {
                    this.storage.set(i, oldStorage.get(i));
                }
            }
            this.storage.set(ind, id);
        }

        public PalettedStorage copy() {
            return new PalettedStorage(this);
        }
    }

    private static abstract class FlexibleStorage {
        protected final long[] data;
        protected final int bitsPerEntry;
        protected final int entries;
        protected final long maxEntryValue;

        protected FlexibleStorage(long[] data, int bitsPerEntry, int entries) {
            this.data = data;
            this.bitsPerEntry = bitsPerEntry;
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
            this.entriesPerLong = bitsPerEntry == 0 ? 0 : 64 / bitsPerEntry;
        }

        private static int longsForEntries(int bitsPerEntry, int entries) {
            if (bitsPerEntry == 0) {
                return 0;
            }
            int entriesPerLong = 64 / bitsPerEntry;
            return (entries + entriesPerLong - 1) / entriesPerLong;
        }

        @Override
        public int get(int index) {
            if (index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }
            if (this.bitsPerEntry == 0) {
                return 0;
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
            if (this.bitsPerEntry == 0) {
                return 0;
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

    public static class TileEntity {
        public byte xz; // 1.18+
        public short y; // 1.18+
        public int type; // 1.18+
        public CompoundTag tag;

        TileEntity(Packet packet, Packet.Reader in) throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_18)) {
                this.xz = in.readByte();
                this.y = in.readShort();
                this.type = in.readVarInt();
            }
            this.tag = in.readNBT();
        }

        void write(Packet packet, Packet.Writer out) throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_18)) {
                out.writeByte(this.xz);
                out.writeShort(this.y);
                out.writeVarInt(this.type);
            }
            out.writeNBT(this.tag);
        }
    }
}
