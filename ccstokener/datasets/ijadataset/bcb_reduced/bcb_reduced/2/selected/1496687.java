package PPM;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.net.*;

public class PPMDecoder {

    public static int width = -1;

    public static int height = -1;

    public static int[] DoIt(String fname) throws Exception {
        int[] imageData = null;
        PPMDecoder decode = new PPMDecoder();
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(fname));
            imageData = decode.getImage(in);
        } catch (Exception e) {
            throw new IOException("Open PPM Exception - Couldn't read from Buffered Stream!");
        }
        return (imageData);
    }

    public PPMDecoder() {
    }

    public Image getImage(java.net.URL url) throws java.io.IOException {
        URLConnection u = url.openConnection();
        DataInputStream dis = new DataInputStream(u.getInputStream());
        BufferedInputStream in = new BufferedInputStream(dis);
        int[] pixels = getImage(in);
        return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(width, height, pixels, 0, width));
    }

    public int[] getImage(BufferedInputStream in) throws IOException {
        int r, g, b;
        try {
            readHeader(in);
        } catch (Exception e) {
            throw new IOException("Open PPM Exception - Couldn't read header!");
        }
        int numPixels = height * width;
        int[] imageData = new int[numPixels];
        try {
            for (int i = 0; i < numPixels; i++) {
                r = readByte(in);
                g = readByte(in);
                b = readByte(in);
                imageData[i] = makeRgb(r, g, b);
            }
        } catch (Exception e) {
            throw new IOException("Open PPM Exception - Couldn't read image data!");
        }
        return imageData;
    }

    private int type;

    private static final int PPM_RAW = 6;

    void readHeader(InputStream in) throws IOException {
        char c1, c2;
        c1 = (char) readByte(in);
        c2 = (char) readByte(in);
        if (c1 != 'P') {
            throw new IOException("not a PPM file");
        }
        if (c2 != '6') {
            throw new IOException("not a PPM file");
        }
        width = readInt(in);
        height = readInt(in);
        readInt(in);
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    private int bitshift = -1;

    private int bits;

    private boolean readBit(InputStream in) throws IOException {
        if (bitshift == -1) {
            bits = readByte(in);
            bitshift = 7;
        }
        boolean bit = (((bits >> bitshift) & 1) != 0);
        --bitshift;
        return bit;
    }

    private static char readChar(InputStream in) throws IOException {
        char c;
        c = (char) readByte(in);
        if (c == '#') {
            do {
                c = (char) readByte(in);
            } while ((c != '\n') && (c != '\r'));
        }
        return (c);
    }

    private static char readNonwhiteChar(InputStream in) throws IOException {
        char c;
        do {
            c = readChar(in);
        } while ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r'));
        return c;
    }

    private static int readInt(InputStream in) throws IOException {
        char c;
        int i;
        c = readNonwhiteChar(in);
        if ((c < '0') || (c > '9')) {
            throw new IOException("Invalid integer when reading PPM image file.");
        }
        i = 0;
        do {
            i = i * 10 + c - '0';
            c = readChar(in);
        } while ((c >= '0') && (c <= '9'));
        return (i);
    }

    private static int makeRgb(int r, int g, int b) {
        return (0xff000000 | (r << 16) | (g << 8) | b);
    }
}
