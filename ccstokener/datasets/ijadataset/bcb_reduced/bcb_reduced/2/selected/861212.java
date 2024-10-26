package org.makagiga.commons;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * @author Jeff Friesen
 */
final class Ico {

    private static final int FDE_OFFSET = 6;

    private static final int DE_LENGTH = 16;

    private static final int BMIH_LENGTH = 40;

    private byte[] icoimage = new byte[0];

    private int numImages;

    private BufferedImage[] bi;

    private int[] colorCount;

    public Ico(File file) throws BadIcoResException, IOException {
        this(file.getAbsolutePath());
    }

    public Ico(InputStream is) throws BadIcoResException, IOException {
        try {
            read(is);
            parseICOImage();
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
    }

    public Ico(String filename) throws BadIcoResException, IOException {
        this(new FileInputStream(filename));
    }

    public Ico(URL url) throws BadIcoResException, IOException {
        this(url.openStream());
    }

    public BufferedImage getImage(int index) {
        if (index < 0 || index >= numImages) throw new IllegalArgumentException("index out of range");
        return bi[index];
    }

    public int getNumColors(int index) {
        if (index < 0 || index >= numImages) throw new IllegalArgumentException("index out of range");
        return colorCount[index];
    }

    public int getNumImages() {
        return numImages;
    }

    private int calcScanlineBytes(int width, int bitCount) {
        return (((width * bitCount) + 31) / 32) * 4;
    }

    private void parseICOImage() throws BadIcoResException, IOException {
        if (icoimage[2] != 1 || icoimage[3] != 0) throw new BadIcoResException("Not an ICO resource");
        numImages = ubyte(icoimage[5]);
        numImages <<= 8;
        numImages |= icoimage[4];
        bi = new BufferedImage[numImages];
        colorCount = new int[numImages];
        for (int i = 0; i < numImages; i++) {
            int width = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH]);
            int height = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 1]);
            colorCount[i] = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 2]);
            int bytesInRes = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 11]);
            bytesInRes <<= 8;
            bytesInRes |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 10]);
            bytesInRes <<= 8;
            bytesInRes |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 9]);
            bytesInRes <<= 8;
            bytesInRes |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 8]);
            int imageOffset = ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 15]);
            imageOffset <<= 8;
            imageOffset |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 14]);
            imageOffset <<= 8;
            imageOffset |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 13]);
            imageOffset <<= 8;
            imageOffset |= ubyte(icoimage[FDE_OFFSET + i * DE_LENGTH + 12]);
            if (icoimage[imageOffset] == 40 && icoimage[imageOffset + 1] == 0 && icoimage[imageOffset + 2] == 0 && icoimage[imageOffset + 3] == 0) {
                int _width = ubyte(icoimage[imageOffset + 7]);
                _width <<= 8;
                _width |= ubyte(icoimage[imageOffset + 6]);
                _width <<= 8;
                _width |= ubyte(icoimage[imageOffset + 5]);
                _width <<= 8;
                _width |= ubyte(icoimage[imageOffset + 4]);
                if (width == 0) width = _width;
                int _height = ubyte(icoimage[imageOffset + 11]);
                _height <<= 8;
                _height |= ubyte(icoimage[imageOffset + 10]);
                _height <<= 8;
                _height |= ubyte(icoimage[imageOffset + 9]);
                _height <<= 8;
                _height |= ubyte(icoimage[imageOffset + 8]);
                if (height == 0) height = _height >> 1;
                int planes = ubyte(icoimage[imageOffset + 13]);
                planes <<= 8;
                planes |= ubyte(icoimage[imageOffset + 12]);
                int bitCount = ubyte(icoimage[imageOffset + 15]);
                bitCount <<= 8;
                bitCount |= ubyte(icoimage[imageOffset + 14]);
                if (colorCount[i] == 0) {
                    if (planes == 1) {
                        if (bitCount == 1) colorCount[i] = 2; else if (bitCount == 4) colorCount[i] = 16; else if (bitCount == 8) colorCount[i] = 256; else if (bitCount != 32) colorCount[i] = (int) Math.pow(2, bitCount);
                    } else colorCount[i] = (int) Math.pow(2, bitCount * planes);
                }
                bi[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                int colorTableOffset = imageOffset + BMIH_LENGTH;
                if (colorCount[i] == 2) {
                    int xorImageOffset = colorTableOffset + 2 * 4;
                    int scanlineBytes = calcScanlineBytes(width, 1);
                    int andImageOffset = xorImageOffset + scanlineBytes * height;
                    int[] masks = { 128, 64, 32, 16, 8, 4, 2, 1 };
                    for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
                        int index;
                        if ((ubyte(icoimage[xorImageOffset + row * scanlineBytes + col / 8]) & masks[col % 8]) != 0) index = 1; else index = 0;
                        int rgb = 0;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4 + 2]));
                        rgb <<= 8;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4 + 1]));
                        rgb <<= 8;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4]));
                        if ((ubyte(icoimage[andImageOffset + row * scanlineBytes + col / 8]) & masks[col % 8]) != 0) bi[i].setRGB(col, height - 1 - row, rgb); else bi[i].setRGB(col, height - 1 - row, 0xff000000 | rgb);
                    }
                } else if (colorCount[i] == 16) {
                    int xorImageOffset = colorTableOffset + 16 * 4;
                    int scanlineBytes = calcScanlineBytes(width, 4);
                    int andImageOffset = xorImageOffset + scanlineBytes * height;
                    int[] masks = { 128, 64, 32, 16, 8, 4, 2, 1 };
                    for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
                        int index;
                        if ((col & 1) == 0) {
                            index = ubyte(icoimage[xorImageOffset + row * scanlineBytes + col / 2]);
                            index >>= 4;
                        } else {
                            index = ubyte(icoimage[xorImageOffset + row * scanlineBytes + col / 2]) & 15;
                        }
                        int rgb = 0;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4 + 2]));
                        rgb <<= 8;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4 + 1]));
                        rgb <<= 8;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4]));
                        if ((ubyte(icoimage[andImageOffset + row * calcScanlineBytes(width, 1) + col / 8]) & masks[col % 8]) != 0) bi[i].setRGB(col, height - 1 - row, rgb); else bi[i].setRGB(col, height - 1 - row, 0xff000000 | rgb);
                    }
                } else if (colorCount[i] == 256) {
                    int xorImageOffset = colorTableOffset + 256 * 4;
                    int scanlineBytes = calcScanlineBytes(width, 8);
                    int andImageOffset = xorImageOffset + scanlineBytes * height;
                    int[] masks = { 128, 64, 32, 16, 8, 4, 2, 1 };
                    for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
                        int index;
                        index = ubyte(icoimage[xorImageOffset + row * scanlineBytes + col]);
                        int rgb = 0;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4 + 2]));
                        rgb <<= 8;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4 + 1]));
                        rgb <<= 8;
                        rgb |= (ubyte(icoimage[colorTableOffset + index * 4]));
                        if ((ubyte(icoimage[andImageOffset + row * calcScanlineBytes(width, 1) + col / 8]) & masks[col % 8]) != 0) bi[i].setRGB(col, height - 1 - row, rgb); else bi[i].setRGB(col, height - 1 - row, 0xff000000 | rgb);
                    }
                } else if (colorCount[i] == 0) {
                    int scanlineBytes = calcScanlineBytes(width, 32);
                    for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
                        int rgb = ubyte(icoimage[colorTableOffset + row * scanlineBytes + col * 4 + 3]);
                        rgb <<= 8;
                        rgb |= ubyte(icoimage[colorTableOffset + row * scanlineBytes + col * 4 + 2]);
                        rgb <<= 8;
                        rgb |= ubyte(icoimage[colorTableOffset + row * scanlineBytes + col * 4 + 1]);
                        rgb <<= 8;
                        rgb |= ubyte(icoimage[colorTableOffset + row * scanlineBytes + col * 4]);
                        bi[i].setRGB(col, height - 1 - row, rgb);
                    }
                }
            } else if (ubyte(icoimage[imageOffset]) == 0x89 && icoimage[imageOffset + 1] == 0x50 && icoimage[imageOffset + 2] == 0x4e && icoimage[imageOffset + 3] == 0x47 && icoimage[imageOffset + 4] == 0x0d && icoimage[imageOffset + 5] == 0x0a && icoimage[imageOffset + 6] == 0x1a && icoimage[imageOffset + 7] == 0x0a) {
                ByteArrayInputStream bais;
                bais = new ByteArrayInputStream(icoimage, imageOffset, bytesInRes);
                bi[i] = ImageIO.read(bais);
            } else throw new BadIcoResException("BITMAPINFOHEADER or PNG " + "expected");
        }
        icoimage = null;
    }

    private void read(InputStream is) throws IOException {
        int bytesToRead;
        while ((bytesToRead = is.available()) != 0) {
            byte[] icoimage2 = new byte[icoimage.length + bytesToRead];
            System.arraycopy(icoimage, 0, icoimage2, 0, icoimage.length);
            is.read(icoimage2, icoimage.length, bytesToRead);
            icoimage = icoimage2;
        }
    }

    private int ubyte(byte b) {
        return (b < 0) ? 256 + b : b;
    }

    static class BadIcoResException extends Exception {

        public BadIcoResException(String message) {
            super(message);
        }
    }
}
