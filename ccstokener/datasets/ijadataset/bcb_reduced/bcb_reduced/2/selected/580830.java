package hu.schmidtsoft.map.util;

import hu.schmidtsoft.map.model.MCoordinate;
import hu.schmidtsoft.map.model.MCoordinateArray;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BinaryReader {

    InputStream istr;

    public BinaryReader(InputStream istr) {
        super();
        this.istr = istr;
    }

    public int readShort() throws IOException {
        int ret = istr.read() & 0xFF;
        ret <<= 8;
        ret += istr.read() & 0xFF;
        if ((ret & 0x8000) != 0) {
            ret |= 0xFFFF0000;
        }
        return ret;
    }

    public byte readByte() throws IOException {
        return (byte) istr.read();
    }

    public int readInt() throws IOException {
        byte[] bs = new byte[4];
        istr.read(bs);
        int ret = bs[0] & 0xFF;
        ret <<= 8;
        ret += bs[1] & 0xFF;
        ret <<= 8;
        ret += bs[2] & 0xFF;
        ret <<= 8;
        ret += bs[3] & 0xFF;
        return ret;
    }

    public long readLong() throws IOException {
        byte[] bs = new byte[8];
        istr.read(bs);
        long ret = bs[0] & 0xFF;
        ret <<= 8;
        ret += bs[1] & 0xFF;
        ret <<= 8;
        ret += bs[2] & 0xFF;
        ret <<= 8;
        ret += bs[3] & 0xFF;
        ret <<= 8;
        ret += bs[4] & 0xFF;
        ret <<= 8;
        ret += bs[5] & 0xFF;
        ret <<= 8;
        ret += bs[6] & 0xFF;
        ret <<= 8;
        ret += bs[7] & 0xFF;
        return ret;
    }

    public String readString() throws IOException {
        int length = readShort();
        if (length < 0) {
            throw new IOException("Error in string deserialization");
        }
        byte[] bs = new byte[length];
        istr.read(bs);
        return new String(bs, "UTF-8");
    }

    public byte[] readBytesRaw(int l) throws IOException {
        byte[] bs = new byte[l];
        istr.read(bs);
        return bs;
    }

    byte[] buff = new byte[13];

    private long decode5Bytes(byte[] arr, int ptr) {
        return (arr[ptr] & 0xFF) + ((arr[ptr + 1] << 8) & 0xFF00) + ((arr[ptr + 2] << 16) & 0xFF0000) + ((arr[ptr + 3] << 24) & 0xFF000000) + ((((long) arr[ptr + 4]) << 32) & 0xFF00000000L);
    }

    private long decode3Bytes(byte[] arr, int ptr) {
        return (arr[ptr] & 0xFF) + ((arr[ptr + 1] << 8) & 0xFF00) + ((arr[ptr + 2] << 16) & 0xFF0000);
    }

    private MCoordinate readCoordinate() throws IOException {
        istr.read(buff);
        long x = decode5Bytes(buff, 0);
        if ((buff[4] & 0x80) != 0) {
            x |= 0xFFF00000;
        }
        long y = decode5Bytes(buff, 5);
        if ((buff[9] & 0x80) != 0) {
            y |= 0xFFF00000;
        }
        long z = decode3Bytes(buff, 10);
        if ((buff[12] & 0x80) != 0) {
            z |= 0xFFFFF000;
        }
        double cX = ((double) x) * BinaryWriter.divCoo;
        double cY = ((double) y) * BinaryWriter.divCoo;
        double cZ = ((double) z) * BinaryWriter.divHeight;
        MCoordinate coo = new MCoordinate(cX, cY, cZ);
        return coo;
    }

    private void readCoordinate(MCoordinateArray ret) throws IOException {
        istr.read(buff);
        long x = decode5Bytes(buff, 0);
        if ((buff[4] & 0x80) != 0) {
            x |= 0xFFF00000;
        }
        long y = decode5Bytes(buff, 5);
        if ((buff[9] & 0x80) != 0) {
            y |= 0xFFF00000;
        }
        long z = decode3Bytes(buff, 10);
        if ((buff[12] & 0x80) != 0) {
            z |= 0xFFFFF000;
        }
        ret.addCoordinate((int) x, (int) y, (int) z);
    }

    private MCoordinate[] readCoordinates() throws IOException {
        int n = readShort();
        MCoordinate[] ret = new MCoordinate[n];
        for (int i = 0; i < n; ++i) {
            MCoordinate coo = readCoordinate();
            ret[i] = coo;
        }
        return ret;
    }

    public MCoordinateArray readCoordinates2() throws IOException {
        int n = readShort();
        MCoordinateArray ret = new MCoordinateArray(n);
        for (int i = 0; i < n; ++i) {
            readCoordinate(ret);
        }
        return ret;
    }

    public void close() throws IOException {
        istr.close();
    }

    public static BinaryReader forUrl(String s) throws IOException {
        URL url = new URL(s);
        InputStream istr = url.openStream();
        return new BinaryReader(istr);
    }
}
