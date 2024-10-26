package com.chungco.core.http;

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
 * <code>MultiPartFormOutputStream</code> is used to write
 * "multipart/form-data" to a <code>java.net.URLConnection</code> for POSTing.
 * This is primarily for file uploading to HTTP servers.
 * 
 * @see http://forum.java.sun.com/thread.jspa?forumID=31&threadID=451245
 * @since JDK1.3
 */
public class MultiPartFormOutputStream {

    /**
     * The line end characters.
     */
    private static final String NEWLINE = "\r\n";

    /**
     * The boundary prefix.
     */
    private static final String PREFIX = "--";

    /**
     * The output stream to write to.
     */
    private final DataOutputStream out;

    /**
     * The multipart boundary string.
     */
    private final String boundary;

    /**
     * Creates a new <code>MultiPartFormOutputStream</code> object using the
     * specified output stream and boundary. The boundary is required to be
     * created before using this method, as described in the description for the
     * <code>getContentType(String)</code> method. The boundary is only
     * checked for <code>null</code> or empty string, but it is recommended to
     * be at least 6 characters. (Or use the static createBoundary() method to
     * create one.)
     * 
     * @param pOs
     *            the output stream
     * @param pBoundary
     *            the boundary
     * @see #createBoundary()
     * @see #getContentType(String)
     */
    public MultiPartFormOutputStream(final OutputStream pOs, final String pBoundary) {
        if (pOs == null) {
            throw new IllegalArgumentException("Output stream is required.");
        }
        if (pBoundary == null || pBoundary.length() == 0) {
            throw new IllegalArgumentException("Boundary stream is required.");
        }
        this.out = new DataOutputStream(pOs);
        this.boundary = pBoundary;
    }

    /**
     * Writes an boolean field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Boolean pValue) throws IOException {
        writeField(pName, new Boolean(pValue).toString());
    }

    /**
     * Writes an double field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Double pValue) throws IOException {
        writeField(pName, Double.toString(pValue));
    }

    /**
     * Writes an float field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Float pValue) throws IOException {
        writeField(pName, Float.toString(pValue));
    }

    /**
     * Writes an long field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Long pValue) throws IOException {
        writeField(pName, Long.toString(pValue));
    }

    /**
     * Writes an int field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Integer pValue) throws IOException {
        writeField(pName, Integer.toString(pValue));
    }

    /**
     * Writes an short field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Short pValue) throws IOException {
        writeField(pName, Short.toString(pValue));
    }

    /**
     * Writes an char field value.
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final Character pValue) throws IOException {
        writeField(pName, new Character(pValue).toString());
    }

    /**
     * Writes an string field value. If the value is null, an empty string is
     * sent ("").
     * 
     * @param pName
     *            the field name (required)
     * @param pValue
     *            the field value
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeField(final String pName, final String pValue) throws java.io.IOException {
        if (pName == null) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        String value = pValue;
        if (value == null) {
            value = "";
        }
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        out.writeBytes("Content-Disposition: form-data; name=\"" + pName + "\"");
        out.writeBytes(NEWLINE);
        out.writeBytes(NEWLINE);
        out.writeBytes(value);
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
     * Writes a file's contents. If the file is null, does not exists, or is a
     * directory, a <code>java.lang.IllegalArgumentException</code> will be
     * thrown.
     * 
     * @param pName
     *            the field name
     * @param pMimeType
     *            the file content type (optional, recommended)
     * @param pFile
     *            the file (the file must exist)
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeFile(final String pName, final String pMimeType, final File pFile) throws IOException {
        if (pFile == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }
        if (!pFile.exists()) {
            throw new IllegalArgumentException("File does not exist.");
        }
        if (pFile.isDirectory()) {
            throw new IllegalArgumentException("File cannot be a directory.");
        }
        writeFile(pName, pMimeType, pFile.getName(), new FileInputStream(pFile));
    }

    /**
     * Writes a input stream's contents. If the input stream is null, a
     * <code>java.lang.IllegalArgumentException</code> will be thrown.
     * 
     * @param pName
     *            the field name
     * @param pMimeType
     *            the file content type (optional, recommended)
     * @param pFileName
     *            the file name (required)
     * @param pIs
     *            the input stream
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeFile(final String pName, final String pMimeType, final String pFileName, final InputStream pIs) throws IOException {
        if (pIs == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }
        if (pFileName == null || pFileName.length() == 0) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        out.writeBytes("Content-Disposition: form-data; name=\"" + pName + "\"; filename=\"" + pFileName + "\"");
        out.writeBytes(NEWLINE);
        if (pMimeType != null) {
            out.writeBytes("Content-Type: " + pMimeType);
            out.writeBytes(NEWLINE);
        }
        out.writeBytes(NEWLINE);
        final byte[] data = new byte[1024];
        int r = 0;
        while ((r = pIs.read(data, 0, data.length)) != -1) {
            out.write(data, 0, r);
        }
        try {
            pIs.close();
        } catch (Exception e) {
        }
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
     * Writes the given bytes. The bytes are assumed to be the contents of a
     * file, and will be sent as such. If the data is null, a
     * <code>java.lang.IllegalArgumentException</code> will be thrown.
     * 
     * @param pName
     *            the field name
     * @param pMimeType
     *            the file content type (optional, recommended)
     * @param pFileName
     *            the file name (required)
     * @param pData
     *            the file data
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void writeFile(final String pName, final String pMimeType, final String pFileName, final byte[] pData) throws IOException {
        if (pData == null) {
            throw new IllegalArgumentException("Data cannot be null.");
        }
        if (pFileName == null || pFileName.length() == 0) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        out.writeBytes("Content-Disposition: form-data; name=\"" + pName + "\"; filename=\"" + pFileName + "\"");
        out.writeBytes(NEWLINE);
        if (pMimeType != null) {
            out.writeBytes("Content-Type: " + pMimeType);
            out.writeBytes(NEWLINE);
        }
        out.writeBytes(NEWLINE);
        out.write(pData, 0, pData.length);
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
     * Flushes the stream. Actually, this method does nothing, as the only write
     * methods are highly specialized and automatically flush.
     * 
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void flush() throws IOException {
    }

    /**
     * Closes the stream. <br />
     * <br />
     * <b>NOTE:</b> This method <b>MUST</b> be called to finalize the
     * multipart stream.
     * 
     * @throws java.io.IOException
     *             on input/output errors
     */
    public void close() throws IOException {
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(PREFIX);
        out.writeBytes(NEWLINE);
        out.flush();
        out.close();
    }

    /**
     * Gets the multipart boundary string being used by this stream.
     * 
     * @return the boundary
     */
    public String getBoundary() {
        return this.boundary;
    }

    /**
     * Creates a new <code>java.net.URLConnection</code> object from the
     * specified <code>java.net.URL</code>. This is a convenience method
     * which will set the <code>doInput</code>, <code>doOutput</code>,
     * <code>useCaches</code> and <code>defaultUseCaches</code> fields to
     * the appropriate settings in the correct order.
     * 
     * @return a <code>java.net.URLConnection</code> object for the URL
     * @throws java.io.IOException
     *             on input/output errors
     */
    public static URLConnection createConnection(final URL pUrl) throws IOException {
        final URLConnection urlConn = pUrl.openConnection();
        if (urlConn instanceof HttpURLConnection) {
            final HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setRequestMethod("POST");
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setDefaultUseCaches(false);
        return urlConn;
    }

    /**
     * Creates a multipart boundary string by concatenating 20 hyphens (-) and
     * the hexadecimal (base-16) representation of the current time in
     * milliseconds.
     * 
     * @return a multipart boundary string
     * @see #getContentType(String)
     */
    public static String createBoundary() {
        return "--------------------" + Long.toString(System.currentTimeMillis(), 16);
    }

    /**
     * Gets the content type string suitable for the
     * <code>java.net.URLConnection</code> which includes the multipart
     * boundary string. <br />
     * <br />
     * This method is static because, due to the nature of the
     * <code>java.net.URLConnection</code> class, once the output stream for
     * the connection is acquired, it's too late to set the content type (or any
     * other request parameter). So one has to create a multipart boundary
     * string first before using this class, such as with the
     * <code>createBoundary()</code> method.
     * 
     * @param boundary
     *            the boundary string
     * @return the content type string
     * @see #createBoundary()
     */
    public static String getContentType(final String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }
}
