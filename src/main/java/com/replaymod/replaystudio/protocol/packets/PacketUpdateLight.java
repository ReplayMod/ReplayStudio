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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;

public class PacketUpdateLight {
    @SuppressWarnings("MismatchedReadAndWriteOfArray") // it's supposed to be empty. duh.
    private static final byte[] EMPTY = new byte[2048];
    private int x;
    private int z;
    private Data data;

    public static PacketUpdateLight read(Packet packet) throws IOException {
        if (packet.getType() != PacketType.UpdateLight) {
            throw new IllegalArgumentException("Can only read packets of type UpdateLight.");
        }
        PacketUpdateLight updateLight = new PacketUpdateLight();
        try (Packet.Reader reader = packet.reader()) {
            updateLight.read(packet, reader);
        }
        return updateLight;
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.UpdateLight);
        try (Packet.Writer writer = packet.overwrite()) {
            write(packet, writer);
        }
        return packet;
    }

    private PacketUpdateLight() {
    }

    public PacketUpdateLight(int x, int z, List<byte[]> skyLight, List<byte[]> blockLight) {
        this(x, z, new Data(skyLight, blockLight));
    }

    public PacketUpdateLight(int x, int z, Data data) {
        this.x = x;
        this.z = z;
        this.data = data;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public Data getData() {
        return this.data;
    }

    public List<byte[]> getSkyLight() {
        return this.data.skyLight;
    }

    public List<byte[]> getBlockLight() {
        return this.data.blockLight;
    }

    private void read(Packet packet, Packet.Reader in) throws IOException {
        this.x = in.readVarInt();
        this.z = in.readVarInt();
        this.data = readData(packet, in);
    }

    static Data readData(Packet packet, Packet.Reader in) throws IOException {
        Data data = new Data();

        if (packet.atLeast(ProtocolVersion.v1_16) && packet.olderThan(ProtocolVersion.v1_20)) {
            in.readBoolean(); // unknown
        }

        BitSet skyLightMask = in.readBitSet();
        BitSet blockLightMask = in.readBitSet();
        BitSet emptySkyLightMask = in.readBitSet();
        BitSet emptyBlockLightMask = in.readBitSet();

        int skySections = Math.max(skyLightMask.length(), emptySkyLightMask.length());
        int blockSections = Math.max(blockLightMask.length(), emptyBlockLightMask.length());

        if (packet.atLeast(ProtocolVersion.v1_17)) {
            int skyLightsSent = in.readVarInt();
            if (skyLightMask.cardinality() != skyLightsSent) {
                throw new IOException("Expected " + skyLightMask.cardinality() + " sky light sections but got " + skyLightsSent);
            }
        }
        data.skyLight = new ArrayList<>(skySections);
        for (int i = 0; i < skySections; i++) {
            if (skyLightMask.get(i)) {
                if (in.readVarInt() != 2048) {
                    throw new IOException("Expected sky light byte array to be of length 2048");
                }
                data.skyLight.add(in.readBytes(2048)); // 2048 bytes read = 4096 entries
            } else if (emptySkyLightMask.get(i)) {
                data.skyLight.add(new byte[2048]);
            } else {
                data.skyLight.add(null);
            }
        }

        if (packet.atLeast(ProtocolVersion.v1_17)) {
            int blockLightsSent = in.readVarInt();
            if (blockLightMask.cardinality() != blockLightsSent) {
                throw new IOException("Expected " + blockLightMask.cardinality() + " block light sections but got " + blockLightsSent);
            }
        }
        data.blockLight = new ArrayList<>(blockSections);
        for (int i = 0; i < blockSections; i++) {
            if (blockLightMask.get(i)) {
                if (in.readVarInt() != 2048) {
                    throw new IOException("Expected block light byte array to be of length 2048");
                }
                data.blockLight.add(in.readBytes(2048)); // 2048 bytes read = 4096 entries
            } else if (emptyBlockLightMask.get(i)) {
                data.blockLight.add(new byte[2048]);
            } else {
                data.blockLight.add(null);
            }
        }

        return data;
    }

    private void write(Packet packet, Packet.Writer out) throws IOException {
        out.writeVarInt(this.x);
        out.writeVarInt(this.z);
        writeData(packet, out, this.data);
    }

    static void writeData(Packet packet, Packet.Writer out, Data data) throws IOException {
        if (packet.atLeast(ProtocolVersion.v1_16) && packet.olderThan(ProtocolVersion.v1_20)) {
            out.writeBoolean(true); // unknown, ViaVersion always writes true, so we'll do so as well
        }

        BitSet skyLightMask = new BitSet();
        BitSet blockLightMask = new BitSet();
        BitSet emptySkyLightMask = new BitSet();
        BitSet emptyBlockLightMask = new BitSet();
        List<byte[]> skyLights = new ArrayList<>();
        List<byte[]> blockLights = new ArrayList<>();

        for (int i = 0; i < data.skyLight.size(); i++) {
            byte[] skyLight = data.skyLight.get(i);
            if (skyLight != null) {
                if (Arrays.equals(EMPTY, skyLight)) {
                    emptySkyLightMask.set(i);
                } else {
                    skyLightMask.set(i);
                    skyLights.add(skyLight);
                }
            }
        }
        for (int i = 0; i < data.blockLight.size(); i++) {
            byte[] blockLight = data.blockLight.get(i);
            if (blockLight != null) {
                if (Arrays.equals(EMPTY, blockLight)) {
                    emptyBlockLightMask.set(i);
                } else {
                    blockLightMask.set(i);
                    blockLights.add(blockLight);
                }
            }
        }

        out.writeBitSet(skyLightMask);
        out.writeBitSet(blockLightMask);
        out.writeBitSet(emptySkyLightMask);
        out.writeBitSet(emptyBlockLightMask);

        if (packet.atLeast(ProtocolVersion.v1_17)) {
            out.writeVarInt(skyLights.size()); // dunno why Minecraft feels the need to send these
        }
        for (byte[] bytes : skyLights) {
            out.writeVarInt(2048); // dunno why Minecraft feels the need to send these
            out.writeBytes(bytes);
        }

        if (packet.atLeast(ProtocolVersion.v1_17)) {
            out.writeVarInt(blockLights.size()); // dunno why Minecraft feels the need to send these
        }
        for (byte[] bytes : blockLights) {
            out.writeVarInt(2048); // dunno why Minecraft feels the need to send these
            out.writeBytes(bytes);
        }
    }

    public static class Data {
        public List<byte[]> skyLight;
        public List<byte[]> blockLight;

        public Data() {
        }

        public Data(List<byte[]> skyLight, List<byte[]> blockLight) {
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }
    }
}
