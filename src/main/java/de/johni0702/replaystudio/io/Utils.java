package de.johni0702.replaystudio.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

    public static int readInt(InputStream in) throws IOException {
        int b0 = in.read();
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        if (b3 == -1) {
            return -1;
        }
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    public static void writeInt(OutputStream out, int x) throws IOException {
        byte[] r = new byte[4];
        r[0] = (byte) (x >> 24);
        r[1] = (byte) (x >> 16);
        r[2] = (byte) (x >> 8);
        r[3] = (byte) x;
        out.write(r);
    }

}
