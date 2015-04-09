package de.johni0702.replaystudio.util;

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