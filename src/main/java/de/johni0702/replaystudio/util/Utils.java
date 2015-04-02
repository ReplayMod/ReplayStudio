package de.johni0702.replaystudio.util;

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

}
