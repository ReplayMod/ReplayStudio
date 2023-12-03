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
package com.replaymod.replaystudio.protocol;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.Unpooled;
import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.data.StringOrNbtText;
import com.replaymod.replaystudio.util.IGlobalPosition;
import com.replaymod.replaystudio.util.IOConsumer;
import com.replaymod.replaystudio.util.IOSupplier;
import com.replaymod.replaystudio.util.IPosition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Packet {
    private final PacketTypeRegistry registry;
    private final int id;
    private final PacketType type;
    private final ByteBuf buf;

    public Packet(PacketTypeRegistry registry, PacketType type) {
        this(registry, type, Unpooled.buffer());
    }

    public Packet(PacketTypeRegistry registry, PacketType type, ByteBuf buf) {
        this(registry, registry.getId(type), type, buf);
    }

    public Packet(PacketTypeRegistry registry, int packetId, ByteBuf buf) {
        this(registry, packetId, registry.getType(packetId), buf);
    }

    public Packet(PacketTypeRegistry registry, int id, PacketType type, ByteBuf buf) {
        this.registry = registry;
        this.id = id;
        this.type = type;
        this.buf = buf;
    }

    public PacketTypeRegistry getRegistry() {
        return registry;
    }

    public ProtocolVersion getProtocolVersion() {
        return registry.getVersion();
    }

    public int getId() {
        return id;
    }

    public PacketType getType() {
        return type;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    public Packet retain() {
        buf.retain();
        return this;
    }

    public Packet copy() {
        return new Packet(registry, id, type, buf.retainedSlice());
    }

    public boolean release() {
        return buf.release();
    }

    public Reader reader() {
        return new Reader(this, buf);
    }

    public Writer overwrite() {
        buf.writerIndex(buf.readerIndex());
        return new Writer(this, buf);
    }

    public boolean atLeast(ProtocolVersion protocolVersion) {
        return registry.atLeast(protocolVersion);
    }

    public boolean atMost(ProtocolVersion protocolVersion) {
        return registry.atMost(protocolVersion);
    }

    public boolean olderThan(ProtocolVersion protocolVersion) {
        return registry.olderThan(protocolVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Packet packet = (Packet) o;
        return id == packet.id &&
                registry.equals(packet.registry) &&
                buf.equals(packet.buf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registry, id, buf);
    }

    public static class Reader extends ByteBufNetInput implements AutoCloseable {
        private final Packet packet;
        private final ByteBuf buf;
        private int orgReaderIndex;

        Reader(Packet packet, ByteBuf buf) {
            super(buf);
            this.packet = packet;
            this.buf = buf;
            this.orgReaderIndex = buf.readerIndex();
        }

        @Override
        public void close() {
            buf.readerIndex(orgReaderIndex);
        }

        public IPosition readPosition() throws IOException {
            return readPosition(packet.registry, this);
        }

        public static IPosition readPosition(PacketTypeRegistry registry, NetInput in) throws IOException {
            long val = in.readLong();
            long x, y, z;
            if (registry.atLeast(ProtocolVersion.v1_14)) {
                x = val >> 38;
                y = val;
                z = val >> 12;
            } else {
                x = val >> 38;
                y = val >> 26;
                z = val;
            }
            return new IPosition((int) (x << 38 >> 38), (int) (y << 52 >> 52), (int) (z << 38 >> 38));
        }

        public IGlobalPosition readGlobalPosition() throws IOException {
            return readGlobalPosition(packet.registry, this);
        }

        public static IGlobalPosition readGlobalPosition(PacketTypeRegistry registry, NetInput in) throws IOException {
            String dimension = in.readString();
            return new IGlobalPosition(dimension, readPosition(registry, in));
        }

        public CompoundTag readNBT() throws IOException {
            return readNBT(packet.registry, this);
        }

        public static CompoundTag readNBT(PacketTypeRegistry registry, NetInput in) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_8)) {
                byte type = in.readByte();
                if (type == 0) {
                    return null;
                } else {
                    return NBTIO.readTag(new InputStream() {
                        private int read;

                        @Override
                        public int read() throws IOException {
                            int index = read++;
                            if (index == 0) {
                                return type & 0xff;
                            }  else if ((index == 1 || index == 2) && registry.atLeast(ProtocolVersion.v1_20_2)) {
                                return 0; // length of empty name
                            } else {
                                return in.readUnsignedByte();
                            }
                        }
                    });
                }
            } else {
                short length = in.readShort();
                if (length < 0) {
                    return null;
                } else {
                    return NBTIO.readTag(new GZIPInputStream(new ByteArrayInputStream(in.readBytes(length))));
                }
            }
        }

        public BitSet readBitSet() throws IOException {
            return readBitSet(packet.registry, this);
        }

        public static BitSet readBitSet(PacketTypeRegistry registry, NetInput in) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_17)) {
                return BitSet.valueOf(in.readLongs(in.readVarInt()));
            } else if (registry.atLeast(ProtocolVersion.v1_9)) {
                int value = in.readVarInt();
                // There appear to be some broken server implementations which write the lower 31 bits as they should be
                // but then have the 32nd bit set (i.e. the whole number is negative).
                // Pre-1.18 only ever reads the first 18 bits (for PacketUpdateLight) or 16 bits (for PacketChunkData),
                // so this issue does not affect vanilla playback. It does affect our parsing though cause we take the
                // masks at face value.
                // So, to work around these broken masks, we ignore the highest bit.
                value = value & ~0x80000000;
                return BitSet.valueOf(new long[] { value });
            } else {
                return BitSet.valueOf(new long[] { in.readUnsignedShort() });
            }
        }

        public <T> List<T> readList(IOSupplier<T> entryReader) throws IOException {
            return readList(packet.registry, this, entryReader);
        }

        public static <T> List<T> readList(PacketTypeRegistry registry, NetInput in, IOSupplier<T> entryReader) throws IOException {
            int len = in.readVarInt();
            List<T> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(entryReader.get());
            }
            return result;
        }

        public StringOrNbtText readText() throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_20_3)) {
                return new StringOrNbtText(readNBT());
            } else {
                return new StringOrNbtText(readString());
            }
        }
    }

    public static class Writer extends ByteBufNetOutput implements AutoCloseable {
        private final Packet packet;

        private Writer(Packet packet, ByteBuf buf) {
            super(buf);
            this.packet = packet;
        }

        @Override
        public void close() {
        }

        public void writePosition(IPosition pos) throws IOException {
            writePosition(packet.registry, this, pos);
        }

        public static void writePosition(PacketTypeRegistry registry, NetOutput out, IPosition pos) throws IOException {
            long x = pos.getX() & 0x3ffffff;
            long y = pos.getY() & 0xfff;
            long z = pos.getZ() & 0x3ffffff;
            if (registry.atLeast(ProtocolVersion.v1_14)) {
                out.writeLong(x << 38 | z << 12 | y);
            } else {
                out.writeLong(x << 38 | y << 26 | z);
            }
        }

        public void writeGlobalPosition(IGlobalPosition pos) throws IOException {
            writeGlobalPosition(packet.registry, this, pos);
        }

        public static void writeGlobalPosition(PacketTypeRegistry registry, NetOutput out, IGlobalPosition pos) throws IOException {
            out.writeString(pos.getDimension());
            writePosition(registry, out, pos.getPosition());
        }

        public void writeNBT(CompoundTag tag) throws IOException {
            writeNBT(packet.registry, this, tag);
        }

        public static void writeNBT(PacketTypeRegistry registry, NetOutput out, CompoundTag tag) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_8)) {
                if(tag == null) {
                    out.writeByte(0);
                } else {
                    NBTIO.writeTag(new OutputStream() {
                        private int written;

                        @Override
                        public void write(int b) throws IOException {
                            int index = written++;
                            // Skip empty name
                            if ((index == 1 || index == 2) && registry.atLeast(ProtocolVersion.v1_20_2)) {
                                return;
                            }
                            out.writeByte(b);
                        }
                    }, tag);
                }
            } else {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(output);
                NBTIO.writeTag(gzip, tag);
                gzip.close();
                output.close();
                byte[] bytes = output.toByteArray();
                out.writeShort(bytes.length);
                out.writeBytes(bytes);
            }
        }

        public void writeBitSet(BitSet bitSet) throws IOException {
            writeBitSet(packet.registry, this, bitSet);
        }

        public static void writeBitSet(PacketTypeRegistry registry, NetOutput out, BitSet bitSet) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_17)) {
                long[] longs = bitSet.toLongArray();
                out.writeVarInt(longs.length);
                out.writeLongs(longs);
            } else {
                long[] longs = bitSet.toLongArray();
                long value;
                if (longs.length == 0) {
                    value = 0;
                } else if (longs.length == 1) {
                    value = longs[0];
                } else {
                    throw new IllegalArgumentException("Pre-1.17 bitset cannot encode more than 64 bits.");
                }
                if (registry.atLeast(ProtocolVersion.v1_9)) {
                    out.writeVarInt((int) value);
                } else {
                    out.writeShort((int) value);
                }
            }
        }

        public <T> void writeList(List<T> list, IOConsumer<T> entryWriter) throws IOException {
            writeList(packet.registry, this, list, entryWriter);
        }

        public static <T> void writeList(PacketTypeRegistry registry, NetOutput out, List<T> list, IOConsumer<T> entryWriter) throws IOException {
            out.writeVarInt(list.size());
            for (T entry : list) {
                entryWriter.consume(entry);
            }
        }

        public void writeText(StringOrNbtText value) throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_20_3)) {
                writeNBT(value.nbt);
            } else {
                writeString(value.str);
            }
        }
    }
}
