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

import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Output stream which uses a different underlying output stream depending on the current thread.
 */
public class ThreadLocalOutputStream extends OutputStream {

    /**
     * The default output stream.
     */
    private OutputStream def;

    /**
     * All thread local output streams.
     */
    private final ThreadLocal<OutputStream> outputs = new ThreadLocal<>();

    /**
     * Creates a new thread local output stream with the specified output stream as a default.
     * @param def The default output stream
     */
    public ThreadLocalOutputStream(OutputStream def) {
        this.def = checkNotNull(def);
    }

    /**
     * Returns the default output stream used for all threads which don't have their own set.
     * @return The default output stream
     */
    public OutputStream getDefault() {
        return def;
    }

    /**
     * Sets the default output stream used for all threads which don't have their own set.
     * @param def The default output stream
     */
    public void setDefault(OutputStream def) {
        this.def = checkNotNull(def);
    }

    /**
     * Sets the output stream to use for the current thread.
     * @param output The output stream
     */
    public void setOutput(OutputStream output) {
        this.outputs.set(output);
    }

    @Override
    public void write(int b) throws IOException {
        OutputStream out = outputs.get();
        if (out == null) {
            out = def;
        }
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        OutputStream out = outputs.get();
        if (out == null) {
            out = def;
        }
        out.write(b, off, len);
    }
}