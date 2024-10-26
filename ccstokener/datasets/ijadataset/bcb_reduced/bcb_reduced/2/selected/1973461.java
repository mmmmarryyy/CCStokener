package org.apache.fop.fonts.type1;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import org.apache.fop.tools.IOUtil;

/**
 * This class represents a parser for Adobe Type 1 PFB files.
 *
 * @see PFBData
 */
public class PFBParser {

    private static final byte[] CURRENTFILE_EEXEC;

    private static final byte[] CLEARTOMARK;

    static {
        try {
            CURRENTFILE_EEXEC = "currentfile eexec".getBytes("US-ASCII");
            CLEARTOMARK = "cleartomark".getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Incompatible VM. It doesn't support the US-ASCII encoding");
        }
    }

    /**
     * Parses a PFB file into a PFBData object.
     * @param url URL to load the PFB file from
     * @return PFBData memory representation of the font
     * @throws IOException In case of an I/O problem
     */
    public PFBData parsePFB(java.net.URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            return parsePFB(in);
        } finally {
            in.close();
        }
    }

    /**
     * Parses a PFB file into a PFBData object.
     * @param pfbFile File to load the PFB file from
     * @return PFBData memory representation of the font
     * @throws IOException In case of an I/O problem
     */
    public PFBData parsePFB(java.io.File pfbFile) throws IOException {
        InputStream in = new java.io.FileInputStream(pfbFile);
        try {
            return parsePFB(in);
        } finally {
            in.close();
        }
    }

    /**
     * Parses a PFB file into a PFBData object.
     * @param in InputStream to load the PFB file from
     * @return PFBData memory representation of the font
     * @throws IOException In case of an I/O problem
     */
    public PFBData parsePFB(InputStream in) throws IOException {
        PFBData pfb = new PFBData();
        BufferedInputStream bin = new BufferedInputStream(in);
        DataInputStream din = new DataInputStream(bin);
        din.mark(32);
        int firstByte = din.readUnsignedByte();
        din.reset();
        if (firstByte == 128) {
            pfb.setPFBFormat(PFBData.PFB_PC);
            parsePCFormat(pfb, din);
        } else {
            pfb.setPFBFormat(PFBData.PFB_RAW);
            parseRAWFormat(pfb, bin);
        }
        return pfb;
    }

    private static int swapInteger(final int value) {
        return (((value >> 0) & 0xff) << 24) + (((value >> 8) & 0xff) << 16) + (((value >> 16) & 0xff) << 8) + (((value >> 24) & 0xff) << 0);
    }

    private void parsePCFormat(PFBData pfb, DataInputStream din) throws IOException {
        int segmentHead;
        int segmentType;
        int bytesRead;
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        int len1 = swapInteger(din.readInt());
        byte[] headerSegment = new byte[len1];
        din.readFully(headerSegment);
        pfb.setHeaderSegment(headerSegment);
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        int len2 = swapInteger(din.readInt());
        byte[] encryptedSegment = new byte[len2];
        din.readFully(encryptedSegment);
        pfb.setEncryptedSegment(encryptedSegment);
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        int len3 = swapInteger(din.readInt());
        byte[] trailerSegment = new byte[len3];
        din.readFully(trailerSegment);
        pfb.setTrailerSegment(trailerSegment);
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        if (segmentType != 3) {
            throw new IOException("Expected segment type 3, but found: " + segmentType);
        }
    }

    private static final boolean byteCmp(byte[] src, int srcOffset, byte[] cmp) {
        for (int i = 0; i < cmp.length; i++) {
            if (src[srcOffset + i] != cmp[i]) {
                return false;
            }
        }
        return true;
    }

    private void calcLengths(PFBData pfb, byte[] originalData) {
        int len1 = 30;
        while (!byteCmp(originalData, len1 - CURRENTFILE_EEXEC.length, CURRENTFILE_EEXEC)) {
            len1++;
        }
        len1++;
        int len3 = 0;
        len3 -= CLEARTOMARK.length;
        while (!byteCmp(originalData, originalData.length + len3, CLEARTOMARK)) {
            len3--;
        }
        len3 = -len3;
        len3++;
        int numZeroes = 0;
        byte[] ws1 = new byte[] { 0x0D };
        byte[] ws2 = new byte[] { 0x0A };
        byte[] ws3 = new byte[] { 0x30 };
        while ((originalData[originalData.length - len3] == ws1[0] || originalData[originalData.length - len3] == ws2[0] || originalData[originalData.length - len3] == ws3[0]) && numZeroes < 512) {
            len3++;
            if (originalData[originalData.length - len3] == ws3[0]) {
                numZeroes++;
            }
        }
        byte[] buffer = new byte[len1];
        System.arraycopy(originalData, 0, buffer, 0, len1);
        pfb.setHeaderSegment(buffer);
        int len2 = originalData.length - len3 - len1;
        buffer = new byte[len2];
        System.arraycopy(originalData, len1, buffer, 0, len2);
        pfb.setEncryptedSegment(buffer);
        buffer = new byte[len3];
        System.arraycopy(originalData, len1 + len2, buffer, 0, len3);
        pfb.setTrailerSegment(buffer);
    }

    private void parseRAWFormat(PFBData pfb, BufferedInputStream bin) throws IOException {
        calcLengths(pfb, IOUtil.toByteArray(bin, 32768));
    }
}
