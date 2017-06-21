/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.util;

import com.github.steveice10.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
     * Reads a VarInt from a ByteBuf
     * @param buffer The ByteBuf
     * @return The VarInt
     */
    public static int readVarInt(ByteBuf buffer) {
        int number = 0;
        int round = 0;
        byte currentByte;

        do {
            currentByte = buffer.readByte();
            number |= (currentByte & 127) << round++ * 7;

            if (round > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((currentByte & 128) == 128);
        return number;
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

}
