package loci.formats;

import java.io.*;
import java.net.*;

/**
 * Provides random access to data over HTTP using the IRandomAccess interface.
 * This is slow, but functional.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/RAUrl.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/RAUrl.java">SVN</a></dd></dl>
 *
 * @see IRandomAccess
 * @see java.net.HttpURLConnection
 *
 * @author Melissa Linkert linkert at wisc.edu
 */
public class RAUrl implements IRandomAccess {

    /** URL of open socket */
    private String url;

    /** Socket underlying this stream */
    private HttpURLConnection conn;

    /** Input stream */
    private DataInputStream is;

    /** Output stream */
    private DataOutputStream os;

    /** Stream pointer */
    private long fp;

    /** Number of bytes in the stream */
    private long length;

    /** Reset marker */
    private long mark;

    public RAUrl(String url, String mode) throws IOException {
        if (!url.startsWith("http")) url = "http://" + url;
        conn = (HttpURLConnection) (new URL(url)).openConnection();
        if (mode.equals("r")) {
            is = new DataInputStream(new BufferedInputStream(conn.getInputStream(), 65536));
        } else if (mode.equals("w")) {
            conn.setDoOutput(true);
            os = new DataOutputStream(conn.getOutputStream());
        }
        fp = 0;
        length = conn.getContentLength();
        if (is != null) is.mark((int) length);
        this.url = url;
    }

    public void close() throws IOException {
        if (is != null) is.close();
        if (os != null) os.close();
        conn.disconnect();
    }

    public long getFilePointer() throws IOException {
        return fp;
    }

    public long length() throws IOException {
        return length;
    }

    public int read() throws IOException {
        int value = is.read();
        while (value == -1 && fp < length()) value = is.read();
        if (value != -1) fp++;
        markManager();
        return value;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = is.read(b, off, len);
        if (read != -1) fp += read;
        if (read == -1) read = 0;
        markManager();
        while (read < len && fp < length()) {
            int oldRead = read;
            read += read(b, off + read, len - read);
            if (read < oldRead) read = oldRead;
        }
        return read == 0 ? -1 : read;
    }

    public void seek(long pos) throws IOException {
        if (pos >= fp) {
            skipBytes((int) (pos - fp));
            return;
        } else if (pos >= mark) {
            try {
                is.reset();
                fp = mark;
                skipBytes((int) (pos - fp));
                return;
            } catch (IOException e) {
            }
        }
        close();
        conn = (HttpURLConnection) (new URL(url)).openConnection();
        conn.setDoOutput(true);
        if (is != null) {
            is = new DataInputStream(new BufferedInputStream(conn.getInputStream(), 65536));
            is.mark((int) length());
            mark = 0;
        }
        if (os != null) os = new DataOutputStream(conn.getOutputStream());
        this.url = url;
        fp = 0;
        skipBytes((int) pos);
    }

    public void setLength(long newLength) throws IOException {
        length = newLength;
    }

    public boolean readBoolean() throws IOException {
        fp++;
        return is.readBoolean();
    }

    public byte readByte() throws IOException {
        fp++;
        return is.readByte();
    }

    public char readChar() throws IOException {
        fp++;
        return is.readChar();
    }

    public double readDouble() throws IOException {
        fp += 8;
        return is.readDouble();
    }

    public float readFloat() throws IOException {
        fp += 4;
        return is.readFloat();
    }

    public void readFully(byte[] b) throws IOException {
        fp += b.length;
        is.readFully(b);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        fp += len;
        is.readFully(b, off, len);
    }

    public int readInt() throws IOException {
        fp += 4;
        return is.readInt();
    }

    public String readLine() throws IOException {
        throw new IOException("Unimplemented");
    }

    public long readLong() throws IOException {
        fp += 8;
        return is.readLong();
    }

    public short readShort() throws IOException {
        fp += 2;
        return is.readShort();
    }

    public int readUnsignedByte() throws IOException {
        fp++;
        return is.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        fp += 2;
        return is.readUnsignedShort();
    }

    public String readUTF() throws IOException {
        fp += 2;
        return is.readUTF();
    }

    public int skipBytes(int n) throws IOException {
        int skipped = 0;
        for (int i = 0; i < n; i++) {
            if (read() != -1) skipped++;
            markManager();
        }
        return skipped;
    }

    public void write(byte[] b) throws IOException {
        os.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    public void write(int b) throws IOException {
        os.write(b);
    }

    public void writeBoolean(boolean v) throws IOException {
        os.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        os.writeByte(v);
    }

    public void writeBytes(String s) throws IOException {
        os.writeBytes(s);
    }

    public void writeChar(int v) throws IOException {
        os.writeChar(v);
    }

    public void writeChars(String s) throws IOException {
        os.writeChars(s);
    }

    public void writeDouble(double v) throws IOException {
        os.writeDouble(v);
    }

    public void writeFloat(float v) throws IOException {
        os.writeFloat(v);
    }

    public void writeInt(int v) throws IOException {
        os.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        os.writeLong(v);
    }

    public void writeShort(int v) throws IOException {
        os.writeShort(v);
    }

    public void writeUTF(String str) throws IOException {
        os.writeUTF(str);
    }

    private void markManager() throws IOException {
        if (fp >= mark + 65535) {
            mark = fp;
            is.mark((int) length());
        }
    }
}
