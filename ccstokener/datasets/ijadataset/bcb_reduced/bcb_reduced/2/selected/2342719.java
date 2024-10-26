package org.engine.network;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * The Class MultiPartFormOutputStream.
 */
public class MultiPartFormOutputStream {

    /** The out. */
    private DataOutputStream out = null;

    /** The boundary. */
    private String boundary = null;

    /**
     * Instantiates a new multi part form output stream.
     * 
     * @param os
     *            the os
     * @param boundary
     *            the boundary
     */
    public MultiPartFormOutputStream(OutputStream os, String boundary) {
        if (os == null) {
            throw new IllegalArgumentException("Output stream is required.");
        }
        if ((boundary == null) || (boundary.length() == 0)) {
            throw new IllegalArgumentException("Boundary stream is required.");
        }
        out = new DataOutputStream(os);
        this.boundary = boundary;
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, boolean value) throws IOException {
        writeField(name, new Boolean(value).toString());
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, double value) throws IOException {
        writeField(name, Double.toString(value));
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, float value) throws IOException {
        writeField(name, Float.toString(value));
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, long value) throws IOException {
        writeField(name, Long.toString(value));
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, int value) throws IOException {
        writeField(name, Integer.toString(value));
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, short value) throws IOException {
        writeField(name, Short.toString(value));
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, char value) throws IOException {
        writeField(name, new Character(value).toString());
    }

    /**
     * Write field.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeField(String name, String value) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (value == null) {
            value = "";
        }
        out.writeBytes("--");
        out.writeBytes(boundary);
        out.writeBytes("\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
        out.writeBytes("\r\n");
        out.writeBytes("\r\n");
        out.writeBytes(value);
        out.writeBytes("\r\n");
        out.flush();
    }

    /**
     * Write file.
     * 
     * @param name
     *            the name
     * @param mimeType
     *            the mime type
     * @param file
     *            the file
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeFile(String name, String mimeType, File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist.");
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("File cannot be a directory.");
        }
        writeFile(name, mimeType, file.getCanonicalPath(), new FileInputStream(file));
    }

    /**
     * Write file.
     * 
     * @param name
     *            the name
     * @param mimeType
     *            the mime type
     * @param fileName
     *            the file name
     * @param is
     *            the is
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeFile(String name, String mimeType, String fileName, InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }
        if ((fileName == null) || (fileName.length() == 0)) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
        out.writeBytes("--");
        out.writeBytes(boundary);
        out.writeBytes("\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
        out.writeBytes("\r\n");
        if (mimeType != null) {
            out.writeBytes("Content-Type: " + mimeType);
            out.writeBytes("\r\n");
        }
        out.writeBytes("\r\n");
        byte[] data = new byte[1024];
        int r = 0;
        while ((r = is.read(data, 0, data.length)) != -1) {
            out.write(data, 0, r);
        }
        try {
            is.close();
        } catch (Exception localException) {
        }
        out.writeBytes("\r\n");
        out.flush();
    }

    /**
     * Write file.
     * 
     * @param name
     *            the name
     * @param mimeType
     *            the mime type
     * @param fileName
     *            the file name
     * @param data
     *            the data
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeFile(String name, String mimeType, String fileName, byte[] data) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null.");
        }
        if ((fileName == null) || (fileName.length() == 0)) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
        out.writeBytes("--");
        out.writeBytes(boundary);
        out.writeBytes("\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
        out.writeBytes("\r\n");
        if (mimeType != null) {
            out.writeBytes("Content-Type: " + mimeType);
            out.writeBytes("\r\n");
        }
        out.writeBytes("\r\n");
        out.write(data, 0, data.length);
        out.writeBytes("\r\n");
        out.flush();
    }

    /**
     * Flush.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void flush() throws IOException {
    }

    /**
     * Close.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void close() throws IOException {
        out.writeBytes("--");
        out.writeBytes(boundary);
        out.writeBytes("--");
        out.writeBytes("\r\n");
        out.flush();
        out.close();
    }

    /**
     * Gets the boundary.
     * 
     * @return the boundary
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * Creates the connection.
     * 
     * @param url
     *            the url
     * @return the uRL connection
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static URLConnection createConnection(URL url) throws IOException {
        URLConnection urlConn = url.openConnection();
        if ((urlConn instanceof HttpURLConnection)) {
            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setRequestMethod("POST");
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setDefaultUseCaches(false);
        return urlConn;
    }

    /**
     * Creates the boundary.
     * 
     * @return the string
     */
    public static String createBoundary() {
        return "--------------------" + Long.toString(System.currentTimeMillis(), 16);
    }

    /**
     * Gets the content type.
     * 
     * @param boundary
     *            the boundary
     * @return the content type
     */
    public static String getContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }
}
