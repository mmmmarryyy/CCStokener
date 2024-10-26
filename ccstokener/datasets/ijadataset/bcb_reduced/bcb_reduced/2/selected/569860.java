package be.abeel.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author jrobinso
 * @author Thomas Abeel
 */
public class SeekableHTTPStream extends SeekableStream {

    private long position = 0;

    protected URL url;

    public SeekableHTTPStream(URL url) {
        this.url = url;
    }

    public boolean eof() throws IOException {
        return true;
    }

    public void seek(long position) {
        this.position = position;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        return readRaw(buffer, offset, length);
    }

    public int readRaw(byte[] buffer, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || (offset + length) > buffer.length) {
            throw new IndexOutOfBoundsException();
        }
        HttpURLConnection connection = null;
        InputStream is = null;
        int n = 0;
        try {
            connection = (HttpURLConnection) url.openConnection();
            String byteRange = "bytes=" + position + "-" + (position + length - 1);
            connection.setRequestProperty("Range", byteRange);
            is = connection.getInputStream();
            while (n < length) {
                int count = is.read(buffer, offset + n, length - n);
                if (count < 0) {
                    throw new EOFException();
                }
                n += count;
            }
            position += n;
            return n;
        } catch (EOFException e) {
            return n;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("We're screwed...");
            System.out.println(n);
            if (e.getMessage().contains("response code: 416")) {
                System.out.println("Trying to be mister nice guy, returning " + n);
                return n;
            } else {
                throw e;
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void close() throws IOException {
    }

    public byte[] readBytes(long position, int nBytes) throws IOException {
        this.position = position;
        byte[] buffer = new byte[nBytes];
        this.readRaw(buffer, 0, nBytes);
        return buffer;
    }

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    protected long position() throws IOException {
        return position;
    }

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public SeekableStream stream() {
        return new SeekableHTTPStream(url);
    }
}
