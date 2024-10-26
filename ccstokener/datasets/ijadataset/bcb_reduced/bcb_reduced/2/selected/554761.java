package de.psisystems.dmachinery.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ContentReader {

    public static byte[] read(URL url) throws IOException {
        byte[] bytes;
        InputStream is = null;
        try {
            is = url.openStream();
            bytes = readAllBytes(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return bytes;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        final int BLOCKSIZE = 1024;
        byte[] data = new byte[is.available()];
        int r = 0;
        if (is != null) {
            int d = is.read();
            while (d != -1) {
                if (r == data.length) {
                    data = grow(data, BLOCKSIZE);
                }
                data[r++] = (byte) d;
                d = is.read();
            }
        }
        return shrink(data, r);
    }

    private static byte[] shrink(byte[] data, int r) {
        if (data != null && r < data.length) {
            byte[] b = new byte[r];
            System.arraycopy(data, 0, b, 0, r);
            return b;
        }
        return data;
    }

    private static byte[] grow(byte[] data, int blocksize) {
        int size = data != null ? data.length : 0;
        byte[] b = new byte[size + blocksize];
        System.arraycopy(data, 0, b, 0, size);
        return b;
    }

    public static int copyInputToOutput(InputStream is, OutputStream os) throws IOException {
        int totalBytesTransfered = 0;
        int bytesRead = 0;
        if (is != null && os != null) {
            byte[] buf = new byte[4096];
            while ((bytesRead = is.read(buf)) != -1) {
                if (bytesRead > 0) {
                    os.write(buf, 0, bytesRead);
                    os.flush();
                    totalBytesTransfered += bytesRead;
                }
            }
        }
        return totalBytesTransfered;
    }
}
