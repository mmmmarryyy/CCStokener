package org.exist.source;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import org.apache.log4j.Logger;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.storage.DBBroker;

/**
 * A source implementation reading from an URL.
 * 
 * @author wolf
 */
public class URLSource extends AbstractSource {

    private static final Logger LOG = Logger.getLogger(URLSource.class);

    private URL url;

    private URLConnection connection = null;

    private long lastModified = 0;

    private int responseCode = HttpURLConnection.HTTP_OK;

    protected URLSource() {
    }

    public URLSource(URL url) {
        this.url = url;
    }

    protected void setURL(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return url;
    }

    private long getLastModification() {
        try {
            if (connection == null) connection = url.openConnection();
            return connection.getLastModified();
        } catch (IOException e) {
            LOG.warn("URL could not be opened: " + e.getMessage(), e);
            return 0;
        }
    }

    public Object getKey() {
        return url;
    }

    public int isValid(DBBroker broker) {
        long modified = getLastModification();
        if (modified == 0 && modified > lastModified) return INVALID; else return VALID;
    }

    public int isValid(Source other) {
        return INVALID;
    }

    public Reader getReader() throws IOException {
        try {
            if (connection == null) {
                connection = url.openConnection();
                if (connection instanceof HttpURLConnection) responseCode = ((HttpURLConnection) connection).getResponseCode();
            }
            Reader reader = null;
            if (responseCode != HttpURLConnection.HTTP_NOT_FOUND) reader = new InputStreamReader(connection.getInputStream(), "UTF-8");
            connection = null;
            return reader;
        } catch (IOException e) {
            LOG.warn("URL could not be opened: " + e.getMessage(), e);
            throw e;
        }
    }

    public InputStream getInputStream() throws IOException {
        try {
            if (connection == null) {
                connection = url.openConnection();
                if (connection instanceof HttpURLConnection) responseCode = ((HttpURLConnection) connection).getResponseCode();
            }
            InputStream is = null;
            if (responseCode != HttpURLConnection.HTTP_NOT_FOUND) is = connection.getInputStream();
            connection = null;
            return is;
        } catch (ConnectException e) {
            LOG.warn("Unable to connect to URL: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            LOG.warn("URL could not be opened: " + e.getMessage(), e);
            throw e;
        }
    }

    public String getContent() throws IOException {
        try {
            if (connection == null) {
                connection = url.openConnection();
                if (connection instanceof HttpURLConnection) responseCode = ((HttpURLConnection) connection).getResponseCode();
            }
            String content = connection.getContent().toString();
            connection = null;
            return content;
        } catch (IOException e) {
            LOG.warn("URL could not be opened: " + e.getMessage(), e);
            return null;
        }
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String toString() {
        if (url == null) return "[not set]";
        return url.toString();
    }

    @Override
    public void validate(Subject subject, int perm) throws PermissionDeniedException {
    }
}
