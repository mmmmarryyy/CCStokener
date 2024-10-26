package wayic.http;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import wayic.commons.WayicResourceEx;

/**
 * @author Ashesh Nishant
 *
 */
public class UrlFetcher extends AbstractUrlLoader {

    private static final Logger LOGGER = LogManager.getLogger(UrlFetcher.class);

    private URL url;

    /**
	 * Partial constructor.
	 * These should be set after calling the constructor: userAgent, redirectEnabled, timeout.
	 * @param url
	 */
    public UrlFetcher(URL url) throws WayicResourceEx {
        super();
        if (url == null) {
            throw new WayicResourceEx("Cannot construct DefaultUrlLoader with null URL.");
        }
        this.url = url;
    }

    /**
	 * Partial constructor.
	 * @param urlStr
	 * @throws MalformedURLException. If urlStr cannot be resolved to a URL.
	 */
    public UrlFetcher(String urlStr) throws MalformedURLException, WayicResourceEx {
        this(new URL(urlStr));
    }

    /**
	 * Full constructor.
	 * @param userAgent
	 * @param redirectEnabled
	 * @param timeout
	 */
    public UrlFetcher(String userAgent, boolean redirectEnabled, int timeout, URL url) {
        super(userAgent, redirectEnabled, timeout);
        this.url = url;
    }

    /**
	 * Full constructor.
	 * @param userAgent
	 * @param redirectEnabled
	 * @param timeout
	 * @param urlStr
	 */
    public UrlFetcher(String userAgent, boolean redirectEnabled, int timeout, String urlStr) throws MalformedURLException {
        this(userAgent, redirectEnabled, timeout, new URL(urlStr));
    }

    public URL getUrl() {
        return this.url;
    }

    /** 
	 * inHeader's values are used to initialize the connection. 
	 * inHeader can be the previously stored header of an existing feed.
	 * 
	 * Returned header contains the new header of the loaded input stream.
	 * 
	 * Call a getter for input stream, reader or string after load(header).Returns null ContentHeaderI if load-url is not an HTTP url. 
	 * 
	 * If content at load-url has not modified since inHeader.getLastModified(), 
	 * returned ContentHeaderI.getLastModified() == ContentHeaderI.UNCHANGED is true.
	 * 
	 * @see wayic.http.wayic.utils.AbstractUrlLoader#load(wayic.http.ContentHeader.elem.ContentHeaderI)
	 */
    @Override
    public ContentHeader load(ContentHeader inHeader) throws IOException {
        URLConnection connection = url.openConnection();
        initConnection(connection, inHeader);
        in = connection.getInputStream();
        encoding = connection.getContentEncoding();
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }
        ContentHeader outHeader = newContentHeader();
        ;
        if (connection instanceof HttpURLConnection) {
            String etag = connection.getHeaderField("ETag");
            long lastModified = connection.getHeaderFieldDate("Last-Modified", 0L);
            if (lastModified == 0l && inHeader != null) {
                lastModified = inHeader.getLastModified();
            }
            if (etag == null && inHeader != null) {
                etag = inHeader.getEtag();
            }
            outHeader.setEtag(etag);
            outHeader.setLastModified(lastModified);
        } else {
            try {
                File f = new File(getUrl().toURI());
                if (f.exists()) {
                    outHeader.setLastModified(f.lastModified());
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        LOGGER.debug("Loaded " + url);
        return outHeader;
    }

    /**
	 * Override this if a different implementation of ContentHeaderI is to be used.
	 * @return. wayic.feed.impl.basic.ContentHeader implementation of ContentHeaderI interface.
	 */
    public ContentHeader newContentHeader() {
        return new HttpContentHeader(getUrl());
    }
}
