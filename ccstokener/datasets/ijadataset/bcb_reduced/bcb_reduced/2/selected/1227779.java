package org.stanwood.media.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to create a input stream to a web resource
 */
public class WebFileInputStream extends InputStream {

    private static final String DEFAULT_USER_AGENT = "MediaManager";

    private static final Log log = LogFactory.getLog(WebFileInputStream.class);

    private java.util.Map<String, java.util.List<String>> responseHeader = null;

    private int responseCode = -1;

    private String MIMEtype = null;

    private String charset = "ISO-8859-1";

    private InputStream content;

    /**
	 * Open a web file. Uses a default user agent string.
	 *
	 * @param url The URL of the file to open
	 * @throws IOException Thrown if their is a problem fetching the web file
	 */
    public WebFileInputStream(URL url) throws IOException {
        this(url, DEFAULT_USER_AGENT);
    }

    /**
	 * Open a web file.
	 *
	 * @param url The URL of the file to open
	 * @param userAgent The user agent to use when access web resources
	 * @throws IOException Thrown if their is a problem fetching the web file
	 */
    public WebFileInputStream(URL url, String userAgent) throws IOException {
        final java.net.URLConnection uconn = url.openConnection();
        if (!(uconn instanceof java.net.HttpURLConnection)) {
            throw new java.lang.IllegalArgumentException("URL protocol must be HTTP: " + url.toExternalForm());
        }
        final java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uconn;
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-agent", userAgent);
        conn.connect();
        responseHeader = conn.getHeaderFields();
        responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            if (log.isDebugEnabled()) {
                log.debug(getErrors(conn));
            }
            if (responseCode == 404) {
                throw new IOException(MessageFormat.format(Messages.getString("WebFileInputStream.ERROR_404"), url.toExternalForm()));
            } else if (responseCode == 500) {
                throw new IOException(MessageFormat.format(Messages.getString("WebFileInputStream.ERROR_500"), url.toExternalForm()));
            } else if (responseCode == 403) {
                throw new IOException(MessageFormat.format(Messages.getString("WebFileInputStream.ERROR_403"), url.toExternalForm()));
            } else {
                throw new IOException(MessageFormat.format(Messages.getString("WebFileInputStream.ERROR_OTHER"), url.toExternalForm(), responseCode));
            }
        }
        final String type = conn.getContentType();
        if (type != null) {
            final String[] parts = type.split(";");
            MIMEtype = parts[0].trim();
            for (int i = 1; i < parts.length && charset == null; i++) {
                final String t = parts[i].trim();
                final int index = t.toLowerCase().indexOf("charset=");
                if (index != -1) {
                    charset = t.substring(index + 8);
                }
            }
        }
        Object c = conn.getContent();
        if (c instanceof InputStream) {
            content = (InputStream) c;
        } else {
            content = conn.getInputStream();
        }
    }

    private String getErrors(java.net.HttpURLConnection conn) throws IOException {
        java.io.InputStream stream = conn.getErrorStream();
        return FileHelper.readFileContents(stream);
    }

    /**
	 * Get the response code.
	 * @return The response code
	 */
    public int getResponseCode() {
        return responseCode;
    }

    /**
	 * Get the response header.
	 * @return The response header fields
	 */
    public java.util.Map<String, java.util.List<String>> getHeaderFields() {
        return responseHeader;
    }

    /**
	 * Used to get the charset of the stream
	 * @return the charset of the stream
	 */
    public String getCharset() {
        return charset;
    }

    /**
	 * Get the MIME type.
	 * @return The MIME type
	 */
    public String getMIMEType() {
        return MIMEtype;
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return content.read();
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return content.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        content.close();
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] b) throws IOException {
        return content.read(b);
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return content.read(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public long skip(long n) throws IOException {
        return content.skip(n);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void mark(int readlimit) {
        content.mark(readlimit);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void reset() throws IOException {
        content.reset();
    }

    /** {@inheritDoc} */
    @Override
    public boolean markSupported() {
        return content.markSupported();
    }
}
