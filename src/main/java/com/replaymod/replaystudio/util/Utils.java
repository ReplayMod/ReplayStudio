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
package com.replaymod.replaystudio.util;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.Unpooled;
import com.github.steveice10.netty.util.ReferenceCountUtil;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Various utilities.
 */
public class Utils {

    /**
     * Reads an integer from the input stream.
     * @param in The input stream
     * @return The integer
     * @throws IOException if an I/O error occurs.
     */
    public static int readInt(InputStream in) throws IOException {
        int b0 = in.read();
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        if ((b0 | b1 | b2 | b3) < 0) {
            return -1;
        }
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    /**
     * Writes an integer to the output stream.
     * @param out The output stream
     * @param x The integer
     * @throws IOException if an I/O error occurs.
     */
    public static void writeInt(OutputStream out, int x) throws IOException {
        out.write((x >>> 24) & 0xFF);
        out.write((x >>> 16) & 0xFF);
        out.write((x >>>  8) & 0xFF);
        out.write(x & 0xFF);
    }

    /**
     * Checks whether the specified array contains only {@code null} elements.
     * If there is one element that is not null in the array, this method will return {@code false}.
     * @param array The array
     * @return {@code true} if this array contains only {@code null} entries
     */
    public static boolean containsOnlyNull(Object[] array) {
        for (Object o : array) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make sure that the returned value is within the specified bounds (inclusive).
     * If the value is greater than {@code max} then {@code max} is returned.
     * If the value is smaller than {@code min} then {@code min} is returned.
     * @param i The value
     * @param min Lower bound
     * @param max Upper bound
     * @return The value within max and min
     */
    public static long within(long i, long min, long max) {
        if (i > max) {
            return max;
        }
        if (i < min) {
            return min;
        }
        return i;
    }

    /**
     * Create a new input stream delegating to the specified source.
     * The new input stream has its own closed state and does not close the
     * source stream.
     * @param source The source input stream
     * @return The delegating input stream
     */
    public static InputStream notCloseable(InputStream source) {
        return new InputStream() {
            boolean closed;

            @Override
            public void close() throws IOException {
                closed = true;
            }

            @Override
            public int read() throws IOException {
                if (closed) {
                    return -1;
                }
                return source.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (closed) {
                    return -1;
                }
                return source.read(b, off, len);
            }

            @Override
            public int available() throws IOException {
                return source.available();
            }

            @Override
            public long skip(long n) throws IOException {
                if (closed) {
                    return 0;
                }
                return source.skip(n);
            }

            @Override
            public synchronized void mark(int readlimit) {
                source.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                source.reset();
            }

            @Override
            public boolean markSupported() {
                return source.markSupported();
            }

            @Override
            public int read(byte[] b) throws IOException {
                if (closed) {
                    return -1;
                }
                return source.read(b);
            }
        };
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);
        }
        in.close();
    }

    public static ByteBuf readRetainedSlice(NetInput in, int len) throws IOException {
        if (in instanceof ByteBufExtNetInput) {
            ByteBuf inBuf = ((ByteBufExtNetInput) in).getBuf();
            return inBuf.readRetainedSlice(len);
        }
        return Unpooled.wrappedBuffer(in.readBytes(len));
    }

    public static void writeBytes(NetOutput out, ByteBuf buf) throws IOException {
        if (out instanceof ByteBufExtNetOutput) {
            ByteBuf outBuf = ((ByteBufExtNetOutput) out).getBuf();
            outBuf.writeBytes(buf);
            return;
        }

        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        out.writeBytes(bytes);
    }

    public static Packet readCompressedPacket(PacketTypeRegistry registry, NetInput in) throws IOException {
        ByteBuf byteBuf = null;
        try {
            int prefix = in.readVarInt();
            int len = prefix >> 1;
            if ((prefix & 1) == 1) {
                int fullLen = in.readVarInt();
                byteBuf = Unpooled.buffer(fullLen);

                Inflater inflater = new Inflater();
                inflater.setInput(in.readBytes(len));
                inflater.inflate(byteBuf.array(), byteBuf.arrayOffset(), fullLen);
                byteBuf.writerIndex(fullLen);
            } else {
                byteBuf = readRetainedSlice(in, len);
            }

            int packetId = new ByteBufNetInput(byteBuf).readVarInt();
            return new Packet(registry, packetId, registry.getType(packetId), byteBuf.retain());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ReferenceCountUtil.release(byteBuf);
        }
    }

    public static void writeCompressedPacket(NetOutput out, Packet packet) throws IOException {
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            new ByteBufNetOutput(byteBuf).writeVarInt(packet.getId());
            byteBuf.writeBytes(packet.getBuf());

            int rawIndex = byteBuf.readerIndex();
            int size = byteBuf.readableBytes();

            byteBuf.ensureWritable(size);
            Deflater deflater = new Deflater();
            deflater.setInput(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(), size);
            deflater.finish();
            int compressedSize = 0;
            while (!deflater.finished() && compressedSize < size) {
                compressedSize += deflater.deflate(
                        byteBuf.array(),
                        byteBuf.arrayOffset() + byteBuf.writerIndex() + compressedSize,
                        size - compressedSize
                );
            }

            if (compressedSize < size) {
                byteBuf.readerIndex(rawIndex + size);
                byteBuf.writerIndex(rawIndex + size + compressedSize);
                out.writeVarInt(compressedSize << 1 | 1);
                out.writeVarInt(size);
            } else {
                byteBuf.readerIndex(rawIndex);
                byteBuf.writerIndex(rawIndex + size);
                out.writeVarInt(size << 1);
            }
            writeBytes(out, byteBuf);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ReferenceCountUtil.release(byteBuf);
        }
    }
}
