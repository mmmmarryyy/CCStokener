package ch.ethz.mxquery.extensionsModules.expathhttp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Vector;

/**
 * <code>MultiPartFormOutputStream</code> is used to write 
 * "multipart/form-data" to a <code>java.net.URLConnection</code> for 
 * POSTing.  This is primarily for file uploading to HTTP servers.  
 * 
 * @since  JDK1.3
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
    private DataOutputStream out = null;

    /**
	 * The multipart boundary string.  
	 */
    private String boundary = null;

    /**
	 * Creates a new <code>MultiPartFormOutputStream</code> object using 
	 * the specified output stream and boundary.  The boundary is required 
	 * to be created before using this method, as described in the 
	 * description for the <code>getContentType(String)</code> method.  
	 * The boundary is only checked for <code>null</code> or empty string, 
	 * but it is recommended to be at least 6 characters.  (Or use the 
	 * static createBoundary() method to create one.)
	 * 
	 * @param  os        the output stream
	 * @param  boundary  the boundary
	 * @see  #createBoundary()
	 * @see  #getContentType(String)
	 */
    public MultiPartFormOutputStream(OutputStream os, String boundary) {
        if (os == null) {
            throw new IllegalArgumentException("Output stream is required.");
        }
        if (boundary == null || boundary.length() == 0) {
            throw new IllegalArgumentException("Boundary stream is required.");
        }
        this.out = new DataOutputStream(os);
        this.boundary = boundary;
    }

    /**
	 * Writes an boolean field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, boolean value) throws java.io.IOException {
        writeField(name, new Boolean(value).toString());
    }

    /**
	 * Writes an double field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, double value) throws java.io.IOException {
        writeField(name, Double.toString(value));
    }

    /**
	 * Writes an float field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, float value) throws java.io.IOException {
        writeField(name, Float.toString(value));
    }

    /**
	 * Writes an long field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, long value) throws java.io.IOException {
        writeField(name, Long.toString(value));
    }

    /**
	 * Writes an int field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, int value) throws java.io.IOException {
        writeField(name, Integer.toString(value));
    }

    /**
	 * Writes an short field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, short value) throws java.io.IOException {
        writeField(name, Short.toString(value));
    }

    /**
	 * Writes an char field value.  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, char value) throws java.io.IOException {
        writeField(name, new Character(value).toString());
    }

    /**
	 * Writes an string field value.  If the value is null, an empty string 
	 * is sent ("").  
	 * 
	 * @param  name   the field name (required)
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeField(String name, String value) throws java.io.IOException {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (value == null) {
            value = "";
        }
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
        out.writeBytes(NEWLINE);
        out.writeBytes(NEWLINE);
        out.writeBytes(value);
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
	 * Writes an string field value.  If the value is null, an empty string 
	 * is sent ("").  
	 * @param headers Vector of part header strings 
	 * @param  value  the field value
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeFieldPart(Vector headers, String value) throws java.io.IOException {
        if (value == null) {
            value = "";
        }
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        for (int i = 0; i < headers.size(); i++) {
            out.writeBytes(headers.elementAt(i) + NEWLINE);
        }
        out.writeBytes(NEWLINE);
        out.writeBytes(value);
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
	 * Writes a input stream's contents.  If the input stream is null, a 
	 * <code>java.lang.IllegalArgumentException</code> will be thrown.  
	 * 
	 * @param headers    all headers of this part
	 * @param  is        the input stream
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void writeFilePart(Vector headers, InputStream is) throws java.io.IOException {
        if (is == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Headers cannot be null.");
        }
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        for (int i = 0; i < headers.size(); i++) {
            out.writeBytes(headers.elementAt(i) + NEWLINE);
        }
        byte[] data = new byte[1024];
        int r = 0;
        while ((r = is.read(data, 0, data.length)) != -1) {
            out.write(data, 0, r);
        }
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
	 * Flushes the stream.  Actually, this method does nothing, as the only 
	 * write methods are highly specialized and automatically flush.  
	 * 
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void flush() throws java.io.IOException {
    }

    /**
	 * Closes the stream.  <br />
	 * <br />
	 * <b>NOTE:</b> This method <b>MUST</b> be called to finalize the 
	 * multipart stream.
	 * 
	 * @throws  java.io.IOException  on input/output errors
	 */
    public void close() throws java.io.IOException {
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
	 * @return  the boundary
	 */
    public String getBoundary() {
        return this.boundary;
    }

    /**
	 * Creates a new <code>java.net.URLConnection</code> object from the 
	 * specified <code>java.net.URL</code>.  This is a convenience method 
	 * which will set the <code>doInput</code>, <code>doOutput</code>, 
	 * <code>useCaches</code> and <code>defaultUseCaches</code> fields to 
	 * the appropriate settings in the correct order.  
	 * 
	 * @return  a <code>java.net.URLConnection</code> object for the URL
	 * @throws  java.io.IOException  on input/output errors
	 */
    public static URLConnection createConnection(URL url) throws java.io.IOException {
        URLConnection urlConn = url.openConnection();
        if (urlConn instanceof HttpURLConnection) {
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
	 * Creates a multipart boundary string by concatenating 20 hyphens (-) 
	 * and the hexadecimal (base-16) representation of the current time in 
	 * milliseconds.  
	 * 
	 * @return  a multipart boundary string
	 * @see  #getContentType(String)
	 */
    public static String createBoundary() {
        return "--------------------" + Long.toString(System.currentTimeMillis(), 16);
    }

    /**
	 * Gets the content type string suitable for the 
	 * <code>java.net.URLConnection</code> which includes the multipart 
	 * boundary string.  <br />
	 * <br />
	 * This method is static because, due to the nature of the 
	 * <code>java.net.URLConnection</code> class, once the output stream 
	 * for the connection is acquired, it's too late to set the content 
	 * type (or any other request parameter).  So one has to create a 
	 * multipart boundary string first before using this class, such as 
	 * with the <code>createBoundary()</code> method.  
	 * 
	 * @param  boundary  the boundary string
	 * @return  the content type string
	 * @see  #createBoundary()
	 */
    public static String getContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }
}
