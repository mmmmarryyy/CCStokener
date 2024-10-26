package j3dworkbench.volume.engine;

import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.Raster;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DicomReader {

    static final boolean DEBUG = false;

    int bitsStored, bitsAllocated;

    DicomHeaderReader dHR;

    String filename;

    boolean ignoreNegValues;

    int numberOfFrames;

    byte[] pixData;

    int samplesPerPixel;

    boolean signed;

    int w, h, highBit, n;

    private Map<String, Integer> map = new HashMap<String, Integer>();

    public DicomReader(byte[] array) throws java.io.IOException {
        this(new DicomHeaderReader(array));
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
        dbg("Number of Frames " + numberOfFrames);
    }

    public DicomReader(URL url) throws java.io.IOException {
        final URLConnection u = url.openConnection();
        final int size = u.getContentLength();
        final byte[] array = new byte[size];
        int bytes_read = 0;
        final DataInputStream in = new DataInputStream(u.getInputStream());
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
        dbg("Number of Frames " + numberOfFrames);
    }

    void dbg(String s) {
        if (DEBUG) {
            System.out.println(this.getClass().getName() + s);
        }
    }

    public void flush() {
        pixData = null;
        System.gc();
    }

    public BufferedImage getBufferedImage(final int shift) {
        if (map.isEmpty()) {
            getStats();
        }
        return getBufferedImage1(shift);
    }

    final void getStats() {
        final List<Integer> uniqueVals = new ArrayList<Integer>(1000);
        int j = 0;
        int numPixels = pixData.length / 2;
        int temp = 0;
        int numZeroValues = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        long sum2 = 0;
        int brightAvg;
        int brightAvgRMS;
        for (int i = 0; i < numPixels; i++) {
            j = i * 2;
            temp = ((pixData[j + 1]) << 8) | (pixData[j] & 0xff);
            if (!uniqueVals.contains(temp)) {
                uniqueVals.add(temp);
            }
            if (temp <= 0) {
                numZeroValues++;
            } else {
                sum += temp;
                sum2 += (temp * temp);
            }
            if (temp > max) {
                max = temp;
            }
            if (temp < min) {
                min = temp;
            }
        }
        brightAvg = Math.round(sum / (numPixels - numZeroValues));
        brightAvgRMS = (int) Math.sqrt(sum2 / (numPixels - numZeroValues));
        map.put("MIN", min);
        map.put("MAX", max);
        map.put("NUM0", numZeroValues);
        map.put("AVG", brightAvg);
        map.put("RMS", brightAvgRMS);
    }

    public final BufferedImage getBufferedImage1(int shift) {
        final byte[] pixArray = new byte[pixData.length / 2];
        int j = 0;
        int temp = 0;
        for (int i = 0; i < pixArray.length; i++) {
            j = i * 2;
            temp = ((pixData[j + 1]) << 8) | (pixData[j] & 0xff);
            temp = temp - shift;
            if (temp <= 0) {
                pixArray[i] = 0;
            } else if (temp > 255) {
                pixArray[i] = (byte) 0xff;
            } else {
                pixArray[i] = (byte) (temp);
            }
        }
        return createBIByte(pixArray);
    }

    BufferedImage createBIByte(byte[] pixArray) {
        final DataBufferByte buffer = new DataBufferByte(pixArray, pixArray.length);
        final BandedSampleModel sampleModel = new BandedSampleModel(DataBuffer.TYPE_BYTE, w, h, 1);
        final BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        final Raster ras = Raster.createRaster(sampleModel, buffer, new Point(0, 0));
        bimage.setData(ras);
        return bimage;
    }

    public DicomHeaderReader getDicomHeaderReader() {
        return dHR;
    }

    /** method getImage() uses the Toolkit to create a 256 shades of gray image */
    public Image getImage() {
        if (w > 2048) {
            dbg(" w > 2048 " + "  width  : " + w + "   height  : " + h);
            return scaleImage();
        }
        final ColorModel cm = grayColorModel();
        dbg("  width  : " + w + "   height  : " + h);
        if (n == 1) {
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, pixData, 0, w));
        } else if (!signed) {
            final byte[] destPixels = to8PerPix(pixData);
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, destPixels, 0, w));
        } else if (signed) {
            final byte[] destPixels = signedTo8PerPix(pixData);
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, cm, destPixels, 0, w));
        } else {
            return null;
        }
    }

    public Image[] getImages() throws IOException {
        final Image[] images = new Image[numberOfFrames - 1];
        for (int i = 1; i == numberOfFrames; i++) {
            pixData = dHR.getPixels(i);
            images[i - 1] = getImage();
        }
        return images;
    }

    public String[] getInfos() {
        return dHR.getInfo();
    }

    public int getNumberOfFrames() {
        return numberOfFrames;
    }

    public byte[] getPixels() {
        return pixData;
    }

    protected ColorModel grayColorModel() {
        final byte[] r = new byte[256];
        for (int i = 0; i < 256; i++) {
            r[i] = (byte) (i & 0xff);
        }
        return (new IndexColorModel(8, 256, r, r, r));
    }

    protected Image scaleImage() {
        final ColorModel cm = grayColorModel();
        final int scaledWidth = w / 2;
        final int scaledHeight = h / 2;
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
            final int len = w * h;
            for (int i = 0; i < len; i++) {
                value = (pixData[i * 2]) & 0xff;
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
                        value = ((pixData[(2 * (i * w + j)) + 1] & 0xff) << 8) | (pixData[2 * (i * w + j)] & 0xff);
                        if (value > maxValue) {
                            maxValue = value;
                        }
                        if (value < minValue) {
                            minValue = value;
                        }
                        intPixels[index++] = value;
                    }
                }
            }
            int scale = maxValue - minValue;
            if (scale == 0) {
                scale = 1;
            }
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
        }
        return null;
    }

    private byte[] signedTo8PerPix(byte[] pixData) {
        final int[] pixels = new int[w * h];
        short shValue = 0;
        int value = 0;
        if (highBit >= 8) {
            for (int i = 0; i < pixels.length; i++) {
                shValue = (short) (((pixData[(2 * i) + 1] & 0xff) << 8) | (pixData[(2 * i)] & 0xff));
                value = shValue;
                if (value < 0 && ignoreNegValues) {
                    value = 0;
                }
                pixels[i] = value;
            }
        }
        if (highBit <= 7) {
            for (int i = 0; i < pixels.length; i++) {
                shValue = (short) (((pixData[(2 * i) + 1] & 0xff) << 8) | (pixData[(2 * i)] & 0xff));
                value = shValue;
                if (value < 0 && ignoreNegValues) {
                    value = 0;
                }
                pixels[i] = value;
            }
        }
        int maxValue = 0;
        int minValue = 0xffff;
        for (int element : pixels) {
            if (element > maxValue) {
                maxValue = element;
            }
            if (element < minValue) {
                minValue = element;
            }
        }
        System.out.println("DicomReader.signedTo8PerPix(): min=" + minValue + ": max=" + maxValue);
        final byte[] destPixels = new byte[w * h];
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

    private byte[] to8PerPix(byte[] pixData) {
        if (bitsStored <= 8) {
            dbg("w =   " + w + "  h ==  " + h);
            dbg("PixData.length = " + pixData.length);
            dbg(" h * w  =  " + (h * w));
            final byte[] destPixels = new byte[w * h];
            final int len = w * h;
            int value = 0;
            for (int i = 0; i < len; i++) {
                value = (pixData[i * 2]) & 0xff;
                destPixels[i] = (byte) value;
            }
            return destPixels;
        }
        final int[] pixels = new int[w * h];
        int value = 0;
        if (highBit >= 8) {
            for (int i = 0; i < pixels.length; i++) {
                value = ((pixData[(2 * i) + 1] & 0xff) << 8) | (pixData[(2 * i)] & 0xff);
                pixels[i] = value;
            }
        } else if (highBit <= 7) {
            for (int i = 0; i < pixels.length; i++) {
                value = ((pixData[(2 * i)] & 0xff) << 8) | (pixData[(2 * i) + 1] & 0xff);
                pixels[i] = value;
            }
        }
        int maxValue = 0;
        int minValue = 0xffff;
        for (int element : pixels) {
            if (element > maxValue) {
                maxValue = element;
            }
            if (element < minValue) {
                minValue = element;
            }
        }
        int scale = maxValue - minValue;
        if (scale == 0) {
            scale = 1;
            System.out.println("DicomReader.to8PerPix :scale == error ");
        }
        final byte[] destPixels = new byte[w * h];
        for (int i = 0; i < pixels.length; i++) {
            value = ((pixels[i] - minValue) * 255) / scale;
            destPixels[i] = (byte) (value & 0xff);
        }
        return destPixels;
    }
}
