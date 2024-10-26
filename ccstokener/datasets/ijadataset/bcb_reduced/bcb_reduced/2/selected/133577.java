package swisseph;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * This class is meant to be a wrapper to some read functionality of the
 * http connection.
 */
public class FilePtrHttp implements FilePtr {

    protected static final int STRING_BUFFER_SIZE = 200;

    protected String fnamp;

    protected URL url;

    protected HttpURLConnection urlCon;

    protected long fpos;

    protected int bufsize;

    protected long savedLength = -1;

    protected long startIdx = -1;

    protected long endIdx = -1;

    protected byte[] data;

    protected boolean bigendian = true;

    public static FilePtr get(String fnamp, int bufsize) throws IOException {
        if (!fnamp.startsWith("http")) return null;
        fnamp = fnamp.replace('\\', '/');
        FilePtrHttp fp = null;
        try {
            fp = new FilePtrHttp(fnamp, bufsize);
            if (fp.init()) {
                fp.data = new byte[fp.bufsize];
            } else {
                fp.close();
                fp = null;
            }
        } catch (MalformedURLException ex) {
        } catch (ProtocolException ex) {
        }
        return fp;
    }

    /**
  * Creates a new FilePtr instance. Well, the parameters are rather
  * &quot;funny&quot; for now, but there were reasons for it. I will
  * change it later (hopefully)...<br/>
  * If you do not need to read randomly and you have access to the file
  * directly, you should use the BufferedInputStream etc. -classes, as
  * they are MUCH faster than the RandomAccessFile class that is used
  * here.
  */
    protected FilePtrHttp(String fnamp, int bufsize) throws MalformedURLException {
        this.fnamp = fnamp;
        this.bufsize = bufsize;
        this.url = new URL(fnamp.replace('\\', '/'));
        fpos = 0;
    }

    public void setBigendian(boolean bigendian) {
        this.bigendian = bigendian;
    }

    public byte readByte() throws IOException, EOFException {
        if (startIdx < 0 || fpos < startIdx || fpos >= endIdx) {
            readToBuffer();
        }
        return data[(int) (fpos++ - startIdx)];
    }

    public int readUnsignedByte() throws IOException, EOFException {
        return ((int) readByte()) & 0xff;
    }

    public short readShort() throws IOException, EOFException {
        if (bigendian) {
            return (short) ((readByte() << 8) | readUnsignedByte());
        }
        return (short) (readUnsignedByte() | (readByte() << 8));
    }

    public int readInt() throws IOException, EOFException {
        if (bigendian) {
            return (((int) readByte()) << 24) | (((int) readUnsignedByte()) << 16) | (((int) readUnsignedByte()) << 8) | (int) readUnsignedByte();
        }
        return (int) readUnsignedByte() | (((int) readUnsignedByte()) << 8) | (((int) readUnsignedByte()) << 16) | (((int) readByte()) << 24);
    }

    public double readDouble() throws IOException, EOFException {
        long ldb = (bigendian ? ((((long) readUnsignedByte()) << 56) | (((long) readUnsignedByte()) << 48) | (((long) readUnsignedByte()) << 40) | (((long) readUnsignedByte()) << 32) | (((long) readUnsignedByte()) << 24) | (((long) readUnsignedByte()) << 16) | (((long) readUnsignedByte()) << 8) | (long) readUnsignedByte()) : ((long) readUnsignedByte() | (((long) readUnsignedByte()) << 8) | (((long) readUnsignedByte()) << 16) | (((long) readUnsignedByte()) << 24) | (((long) readUnsignedByte()) << 32) | (((long) readUnsignedByte()) << 40) | (((long) readUnsignedByte()) << 48) | (((long) readUnsignedByte()) << 56)));
        return Double.longBitsToDouble(ldb);
    }

    public String readLine() throws IOException, EOFException {
        StringBuilder sout = new StringBuilder(STRING_BUFFER_SIZE);
        try {
            char ch;
            while ((ch = (char) readUnsignedByte()) != '\n') {
                sout.append(ch);
            }
        } catch (EOFException e) {
            if (sout.length() == 0) {
                throw e;
            }
        }
        return sout.toString();
    }

    public void close() throws IOException {
        fnamp = null;
        if (urlCon != null) urlCon.disconnect();
        urlCon = null;
    }

    public boolean isClosed() {
        return urlCon == null;
    }

    public long getFilePointer() {
        return fpos;
    }

    public long length() throws IOException {
        return savedLength;
    }

    public void seek(long pos) {
        fpos = pos;
    }

    private boolean init() throws ProtocolException, IOException {
        urlCon = (HttpURLConnection) url.openConnection();
        urlCon.setRequestMethod("HEAD");
        urlCon.setRequestProperty("User-Agent", useragent);
        if (urlCon.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
        if ((savedLength = urlCon.getHeaderFieldInt("Content-Length", -1)) < 0) return false;
        String s;
        if ((s = urlCon.getHeaderField("Accept-Ranges")) == null || !s.equals("bytes")) {
            System.err.println("Server does not accept HTTP range requests." + " Aborting!");
            return false;
        }
        return true;
    }

    protected void readToBuffer() throws IOException {
        urlCon = (HttpURLConnection) url.openConnection();
        urlCon.setRequestProperty("User-Agent", useragent);
        urlCon.setRequestProperty("Range", "bytes=" + fpos + "-" + SMath.min(length() - 1, fpos + bufsize - 1));
        urlCon.connect();
        boolean isError = false;
        int rc = urlCon.getResponseCode();
        int length = -1;
        if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_PARTIAL) {
            isError = true;
        } else {
            InputStream is = urlCon.getInputStream();
            length = is.read(data);
            if (length > bufsize || (length < bufsize && savedLength >= 0 && fpos + length != savedLength)) {
                isError = true;
            }
            startIdx = fpos;
            endIdx = fpos + length;
        }
        if (isError) {
            throw new IOException("HTTP read failed with HTTP response " + rc + ". Read " + length + " bytes, requested " + bufsize + " bytes.");
        }
    }
}
