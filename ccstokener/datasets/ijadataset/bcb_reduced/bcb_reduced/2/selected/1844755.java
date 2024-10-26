package org.wikiup.database.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Blob;
import java.sql.SQLException;
import org.wikiup.util.StreamUtil;

public class URLBlob implements Blob {

    private URLConnection connection;

    private byte bytes[] = null;

    public URLBlob(URL url) throws IOException {
        connection = url.openConnection();
    }

    public URLBlob(URL url, int timeout) throws IOException {
        connection = url.openConnection();
        connection.setReadTimeout(timeout);
    }

    public long length() throws SQLException {
        return connection.getContentLength();
    }

    public byte[] getBytes(long pos, int length) throws SQLException {
        try {
            if (length > 0) {
                byte bytes[] = new byte[length];
                getBinaryStream().read(bytes, (int) pos - 1, length);
                return bytes;
            }
        } catch (IOException ex) {
        }
        return null;
    }

    public InputStream getBinaryStream() throws SQLException {
        try {
            refresh();
            return new ByteArrayInputStream(bytes);
        } catch (IOException ex) {
            return null;
        }
    }

    public InputStream getBinaryStream(long offset, long len) throws SQLException {
        try {
            refresh();
            return new ByteArrayInputStream(bytes, (int) offset, (int) len);
        } catch (IOException ex) {
            return null;
        }
    }

    public void free() {
        bytes = null;
        connection = null;
    }

    private void refresh() throws IOException {
        if (bytes == null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            StreamUtil.copy(os, connection.getInputStream());
            bytes = os.toByteArray();
        }
    }

    public long position(byte pattern[], long start) throws SQLException {
        return 0;
    }

    public long position(Blob pattern, long start) throws SQLException {
        return 0;
    }

    public int setBytes(long pos, byte[] bytes) throws SQLException {
        return 0;
    }

    public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
        return 0;
    }

    public OutputStream setBinaryStream(long pos) throws SQLException {
        return null;
    }

    public void truncate(long len) throws SQLException {
    }
}
