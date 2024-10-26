package com.sun.cldc.io.j2se.http;

import java.io.*;
import java.net.*;
import javax.microedition.io.*;
import com.sun.cldc.io.j2se.*;

/**
 * HTTP connection for J2SE.
 *
 * @author  Nik Shaylor
 * @version 1.0 01/03/2000
 */
public class Protocol extends com.sun.cldc.io.ConnectionBase implements ContentConnection {

    /** URL Connection object */
    URLConnection conn = null;

    /**
     * Open the http connection
     * @param name the target for the connection
     * @param mode The access mode
     * @param timeouts A flag to indicate that the called wants timeout exceptions
     * <p>
     * The name string for this protocol should be:
     * "<name or IP number>:<port number>
     */
    public void open(String name, int mode, boolean timeouts) throws IOException {
        if (name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" " + name);
        }
        name = name.substring(2);
        URL url = new URL(name);
        conn = url.openConnection();
        conn.connect();
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream() throws IOException {
        return new UniversalFilterInputStream(this, conn.getInputStream());
    }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          output stream.
     */
    public OutputStream openOutputStream() throws IOException {
        return new UniversalFilterOutputStream(this, conn.getOutputStream());
    }

    /**
     * Returns the value of the <code>content-type</code> header field.
     *
     * @return  the content type of the resource that the URL references,
     *          or <code>null</code> if not known.
     */
    public String getType() {
        return conn.getContentType();
    }

    /**
     * Returns the value of the <code>content-encoding</code> header field.
     *
     * @return  the content encoding of the resource that the URL references,
     *          or <code>null</code> if not known.
     */
    public String getEncoding() {
        return conn.getContentEncoding();
    }

    public String getHeaderField(String name) {
        return conn.getHeaderField(name);
    }

    /**
     * Returns the value of the <code>content-length</code> header field.
     *
     * @return  the content length of the resource that this connection's URL
     *          references, or <code>-1</code> if the content length is
     *          not known.
     */
    public long getLength() {
        return conn.getContentLength();
    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public void close() throws IOException {
    }
}
