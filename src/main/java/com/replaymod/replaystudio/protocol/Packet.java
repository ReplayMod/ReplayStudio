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
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.util.IPosition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            return new IPosition((int) (x << 38 >> 38), (int) (y & 0xfff), (int) (z << 38 >> 38));
        }

        public CompoundTag readNBT() throws IOException {
            return readNBT(packet.registry, this);
        }

        public static CompoundTag readNBT(PacketTypeRegistry registry, NetInput in) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_8)) {
                byte b = in.readByte();
                if (b == 0) {
                    return null;
                } else {
                    return (CompoundTag) NBTIO.readTag(new InputStream() {
                        private boolean first = true;

                        @Override
                        public int read() throws IOException {
                            if (first) {
                                first = false;
                                return b;
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
                    return (CompoundTag) NBTIO.readTag(new GZIPInputStream(new ByteArrayInputStream(in.readBytes(length))));
                }
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

        public void writeNBT(CompoundTag tag) throws IOException {
            writeNBT(packet.registry, this, tag);
        }

        public static void writeNBT(PacketTypeRegistry registry, NetOutput out, CompoundTag tag) throws IOException {
            if (registry.atLeast(ProtocolVersion.v1_8)) {
                if(tag == null) {
                    out.writeByte(0);
                } else {
                    NBTIO.writeTag(new OutputStream() {
                        @Override
                        public void write(int i) throws IOException {
                            out.writeByte(i);
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
    }
}
