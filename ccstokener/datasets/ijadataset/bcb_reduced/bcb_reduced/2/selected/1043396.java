package DicomDecoder;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class DicomReader {

    int w, h, highBit, n;

    boolean signed;

    static final boolean DEBUG = true;

    boolean ignoreNegValues;

    int bitsStored, bitsAllocated;

    int samplesPerPixel;

    int numberOfFrames;

    byte[] pixData;

    String filename;

    DicomHeaderReader dHR;

    public DicomReader(DicomHeaderReader dHR) throws java.io.IOException {
        this.dHR = dHR;
        h = dHR.getRows();
        w = dHR.getColumns();
        highBit = dHR.getHighBit();
        bitsStored = dHR.getBitStored();
        bitsAllocated = dHR.getBitAllocated();
        n = (bitsAllocated / 8);
        signed = (dHR.getPixelRepresentation() == 1);
        samplesPerPixel = dHR.getSamplesPerPixel();
        this.pixData = dHR.getPixels();
        ignoreNegValues = true;
        samplesPerPixel = dHR.getSamplesPerPixel();
        numberOfFrames = dHR.getNumberOfFrames();
    }

    public DicomReader(byte[] array) throws java.io.IOException {
        this(new DicomHeaderReader(array));
    }

    public DicomReader(URL url) throws java.io.IOException {
        URLConnection u = url.openConnection();
        int size = u.getContentLength();
        byte[] array = new byte[size];
        int bytes_read = 0;
        DataInputStream in = new DataInputStream(u.getInputStream());
        while (bytes_read < size) {
            bytes_read += in.read(array, bytes_read, size - bytes_read);
        }
        in.close();
        this.dHR = new DicomHeaderReader(array);
        h = dHR.getRows();
        w = dHR.getColumns();
        highBit = dHR.getHighBit();
        bitsStored = dHR.getBitStored();
        bitsAllocated = dHR.getBitAllocated();
        n = (bitsAllocated / 8);
        signed = (dHR.getPixelRepresentation() == 1);
        this.pixData = dHR.getPixels();
        ignoreNegValues = true;
        samplesPerPixel = dHR.getSamplesPerPixel();
        numberOfFrames = dHR.getNumberOfFrames();
    }

    public DicomReader(byte[] pixels, int w, int h, int highBit, int bitsStored, int bitsAllocated, boolean signed, int samplesPerPixel, int numberOfFrames, boolean ignoreNegValues) {
        this.h = h;
        this.w = w;
        this.highBit = highBit;
        this.bitsStored = bitsStored;
        this.bitsAllocated = bitsAllocated;
        this.n = bitsAllocated / 8;
        this.signed = signed;
        this.pixData = pixels;
        this.ignoreNegValues = ignoreNegValues;
        this.samplesPerPixel = samplesPerPixel;
        this.numberOfFrames = numberOfFrames;
    }

    public DicomHeaderReader getDicomHeaderReader() {
        return dHR;
    }

    public int getNumberOfFrames() {
        return numberOfFrames;
    }

    public String[] getInfos() {
        return dHR.getInfo();
    }

    public byte[] getPixels() {
        return pixData;
    }

    /** method getImage()  uses the Toolkit to create a 256 shades of gray image  */
    public Image getImage() {
        if (w > 2048) {
            dbg(" w > 2048 " + "  width  : " + w + "   height  : " + h);
            return scaleImage();
        }
        ColorModel cm = grayColorModel();
        dbg("  width  : " + w + "   height  : " + h);
        if (n == 1) {
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, pixData, 0, w));
        } else if (!signed) {
            dbg(" pas signé: ");
            byte[] destPixels = to8PerPix(pixData);
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, destPixels, 0, w));
        } else if (signed) {
            byte[] destPixels = signedTo8PerPix(pixData);
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, destPixels, 0, w));
        } else return null;
    }

    public Image[] getImages() throws IOException {
        Image[] images = new Image[numberOfFrames];
        for (int i = 1; i <= numberOfFrames; i++) {
            pixData = dHR.getPixels(i);
            images[i - 1] = getImage();
        }
        return images;
    }

    protected Image scaleImage() {
        ColorModel cm = grayColorModel();
        int scaledWidth = w / 2;
        int scaledHeight = h / 2;
        int index = 0;
        int value = 0;
        byte[] destPixels = null;
        System.gc();
        if (n == 1) {
            destPixels = new byte[scaledWidth * scaledHeight];
            for (int i = 0; i < h; i += 2) {
                for (int j = 0; j < w; j += 2) {
                    destPixels[index++] = pixData[(i * w) + j];
                }
            }
            pixData = null;
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w / 2, h / 2, cm, destPixels, 0, w / 2));
        } else if (n == 2 && bitsStored <= 8) {
            dbg("w =   " + w + "  h ==  " + h);
            dbg("PixData.length = " + pixData.length);
            dbg(" h * w  =  " + (h * w));
            destPixels = new byte[w * h];
            int len = w * h;
            for (int i = 0; i < len; i++) {
                value = (int) (pixData[i * 2]) & 0xff;
                destPixels[i] = (byte) value;
            }
            pixData = null;
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, destPixels, 0, w));
        } else if (!signed) {
            int[] intPixels = new int[scaledWidth * scaledHeight];
            dbg(" !signed");
            int maxValue = 0;
            int minValue = 0xffff;
            if (highBit >= 8) {
                for (int i = 0; i < h; i += 2) {
                    for (int j = 0; j < w; j += 2) {
                        value = ((int) (pixData[(2 * (i * w + j)) + 1] & 0xff) << 8) | (int) (pixData[2 * (i * w + j)] & 0xff);
                        if (value > maxValue) maxValue = value;
                        if (value < minValue) minValue = value;
                        intPixels[index++] = value;
                    }
                }
            }
            int scale = maxValue - minValue;
            if (scale == 0) scale = 1;
            pixData = null;
            destPixels = new byte[scaledWidth * scaledHeight];
            for (int i = 0; i < intPixels.length; i++) {
                value = (intPixels[i] - minValue) * 256;
                value /= scale;
                destPixels[i] = (byte) (value & 0xff);
            }
            intPixels = null;
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w / 2, h / 2, cm, destPixels, 0, w / 2));
        } else if (signed) {
            byte[] pixels = signedTo8PerPix(pixData);
            pixData = pixels;
            for (int i = 0; i < h; i += 2) {
                for (int j = 0; j < w; j += 2) {
                    destPixels[index++] = pixData[(i * w) + j];
                }
            }
            pixData = null;
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w / 2, h / 2, cm, destPixels, 0, w / 2));
        }
        return null;
    }

    private byte[] to8PerPix(byte[] pixData) {
        if (bitsStored <= 8) {
            dbg("w =   " + w + "  h ==  " + h);
            dbg("PixData.length = " + pixData.length);
            dbg(" h * w  =  " + (h * w));
            byte[] destPixels = new byte[w * h];
            int len = w * h;
            int value = 0;
            for (int i = 0; i < len; i++) {
                value = (int) (pixData[i * 2]) & 0xff;
                destPixels[i] = (byte) value;
            }
            return destPixels;
        }
        int[] pixels = new int[w * h];
        int value = 0;
        int msb = 0;
        int lsb = 0;
        if (highBit >= 8) {
            int maxsb = 1;
            for (int i = 1; i <= (highBit - 7); i++) maxsb = maxsb * 2;
            dbg(" Mask:" + maxsb + " / Highbit: " + highBit);
            for (int i = 0; i < pixels.length; i++) {
                msb = Math.min(maxsb, (int) (pixData[(2 * i) + 1] & 0xff));
                lsb = (int) (pixData[(2 * i)] & 0xff);
                pixels[i] = msb * 256 + lsb;
            }
        } else if (highBit <= 7) {
            dbg("DicomReader.to8PerPix highBit == 7 ");
            for (int i = 0; i < pixels.length; i++) {
                value = ((int) (pixData[(2 * i)] & 0xff) << 8) | (int) (pixData[(2 * i) + 1] & 0xff);
                pixels[i] = value;
            }
        }
        int maxValue = 0;
        int minValue = 0xffff;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > maxValue) maxValue = pixels[i];
            if (pixels[i] < minValue) minValue = pixels[i];
        }
        dbg(" minValue: " + minValue + "; maxValue: " + maxValue);
        int scale = maxValue - minValue;
        if (scale == 0) {
            scale = 1;
            System.out.println("DicomReader.to8PerPix :scale == error ");
        }
        byte[] destPixels = new byte[w * h];
        for (int i = 0; i < pixels.length; i++) {
            value = ((pixels[i] - minValue) * 255) / scale;
            destPixels[i] = (byte) (value & 0xff);
        }
        return destPixels;
    }

    private byte[] signedTo8PerPix(byte[] pixData) {
        int[] pixels = new int[w * h];
        short shValue = 0;
        int value = 0;
        if (highBit >= 8) {
            for (int i = 0; i < pixels.length; i++) {
                shValue = (short) (((pixData[(2 * i) + 1] & 0xff) << 8) | (pixData[(2 * i)] & 0xff));
                value = (int) shValue;
                if (value < 0 && ignoreNegValues) value = 0;
                pixels[i] = value;
            }
        }
        if (highBit <= 7) {
            for (int i = 0; i < pixels.length; i++) {
                shValue = (short) (((pixData[(2 * i) + 1] & 0xff) << 8) | (pixData[(2 * i)] & 0xff));
                value = (int) shValue;
                if (value < 0 && ignoreNegValues) value = 0;
                pixels[i] = value;
            }
        }
        int maxValue = 0;
        int minValue = 0xffff;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > maxValue) maxValue = pixels[i];
            if (pixels[i] < minValue) minValue = pixels[i];
        }
        byte[] destPixels = new byte[w * h];
        int scale = maxValue - minValue;
        if (scale == 0) {
            scale = 1;
            System.out.println(" Error in VR form SignedTo8..DicomReader");
        }
        for (int i = 0; i < pixels.length; i++) {
            value = ((pixels[i] - minValue) * 255) / scale;
            destPixels[i] = (byte) (value & 0xff);
        }
        return destPixels;
    }

    protected ColorModel grayColorModel() {
        byte[] r = new byte[256];
        for (int i = 0; i < 256; i++) r[i] = (byte) (i & 0xff);
        return (new IndexColorModel(8, 256, r, r, r));
    }

    public void flush() {
        pixData = null;
        System.gc();
        System.gc();
    }

    void dbg(String s) {
        if (DEBUG) System.out.println(this.getClass().getName() + s);
    }
}
