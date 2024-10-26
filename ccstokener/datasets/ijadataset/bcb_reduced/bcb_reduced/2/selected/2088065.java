package net.sf.fmj.media.protocol;

import java.io.*;
import java.net.*;
import java.util.logging.*;
import javax.media.*;
import javax.media.protocol.*;
import net.sf.fmj.media.*;
import net.sf.fmj.utility.*;
import com.lti.utils.*;

/**
 * URL DataSource. Used by default when there is no more specific protocol
 * handler available.
 * 
 * @author Ken Larson
 * 
 */
public class URLDataSource extends PullDataSource implements SourceCloneable {

    class URLSourceStream implements PullSourceStream {

        private boolean endOfStream = false;

        public boolean endOfStream() {
            return endOfStream;
        }

        public ContentDescriptor getContentDescriptor() {
            return contentType;
        }

        public long getContentLength() {
            return conn.getContentLength();
        }

        public Object getControl(String controlType) {
            return null;
        }

        public Object[] getControls() {
            return new Object[0];
        }

        public int read(byte[] buffer, int offset, int length) throws IOException {
            final int result = conn.getInputStream().read(buffer, offset, length);
            if (result == -1) endOfStream = true;
            return result;
        }

        public boolean willReadBlock() {
            try {
                return conn.getInputStream().available() <= 0;
            } catch (IOException e) {
                return true;
            }
        }
    }

    private static final Logger logger = LoggerSingleton.logger;

    protected URLConnection conn;

    protected boolean connected = false;

    private String contentTypeStr;

    private ContentDescriptor contentType;

    protected URLSourceStream[] sources;

    protected URLDataSource() {
        super();
    }

    public URLDataSource(URL url) {
        setLocator(new MediaLocator(url));
    }

    @Override
    public void connect() throws IOException {
        URL url = getLocator().getURL();
        if (url.getProtocol().equals("file")) {
            final String newUrlStr = URLUtils.createAbsoluteFileUrl(url.toExternalForm());
            if (newUrlStr != null) {
                if (!newUrlStr.toString().equals(url.toExternalForm())) {
                    logger.warning("Changing file URL to absolute for URL.openConnection, from " + url.toExternalForm() + " to " + newUrlStr);
                    url = new URL(newUrlStr);
                }
            }
        }
        conn = url.openConnection();
        if (!url.getProtocol().equals("ftp") && conn.getURL().getProtocol().equals("ftp")) {
            logger.warning("URL.openConnection() morphed " + url + " to " + conn.getURL());
            throw new IOException("URL.openConnection() returned an FTP connection for a non-ftp url: " + url);
        }
        if (conn instanceof HttpURLConnection) {
            final HttpURLConnection huc = (HttpURLConnection) conn;
            huc.connect();
            final int code = huc.getResponseCode();
            if (!(code >= 200 && code < 300)) {
                huc.disconnect();
                throw new IOException("HTTP response code: " + code);
            }
            logger.finer("URL: " + url);
            logger.finer("Response code: " + code);
            logger.finer("Full content type: " + conn.getContentType());
            boolean contentTypeSet = false;
            if (stripTrailer(conn.getContentType()).equals("text/plain")) {
                final String ext = PathUtils.extractExtension(url.getPath());
                if (ext != null) {
                    final String result = MimeManager.getMimeType(ext);
                    if (result != null) {
                        contentTypeStr = ContentDescriptor.mimeTypeToPackageName(result);
                        contentTypeSet = true;
                        logger.fine("Received content type " + conn.getContentType() + "; overriding based on extension, to: " + result);
                    }
                }
            }
            if (!contentTypeSet) contentTypeStr = ContentDescriptor.mimeTypeToPackageName(stripTrailer(conn.getContentType()));
        } else {
            conn.connect();
            contentTypeStr = ContentDescriptor.mimeTypeToPackageName(conn.getContentType());
        }
        contentType = new ContentDescriptor(contentTypeStr);
        sources = new URLSourceStream[1];
        sources[0] = new URLSourceStream();
        connected = true;
    }

    public javax.media.protocol.DataSource createClone() {
        final URLDataSource d;
        try {
            d = new URLDataSource(getLocator().getURL());
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "" + e, e);
            return null;
        }
        if (connected) {
            try {
                d.connect();
            } catch (IOException e) {
                logger.log(Level.WARNING, "" + e, e);
                return null;
            }
        }
        return d;
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        if (conn != null) {
            if (conn instanceof HttpURLConnection) {
                final HttpURLConnection huc = (HttpURLConnection) conn;
                huc.disconnect();
            }
        }
        connected = false;
    }

    @Override
    public String getContentType() {
        return contentTypeStr;
    }

    @Override
    public Object getControl(String controlName) {
        return null;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Time getDuration() {
        return Time.TIME_UNKNOWN;
    }

    @Override
    public PullSourceStream[] getStreams() {
        if (!connected) throw new Error("Unconnected source.");
        return sources;
    }

    @Override
    public void start() throws java.io.IOException {
    }

    @Override
    public void stop() throws java.io.IOException {
    }

    /**
     * Strips trailing ; and anything after it. Is generally only used for
     * multipart content.
     */
    private String stripTrailer(String contentType) {
        final int index = contentType.indexOf(";");
        if (index < 0) return contentType;
        final String result = contentType.substring(0, index);
        return result;
    }
}
