package de.nava.informa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * implementation of InputStreamFactoryIF with pure JDK. 
 * it deals with gzipped content, conditionnal get.
 * @author Jean-Guy Avelin
 */
public class SimpleInputStreamFactory implements InputStreamFactoryIF {

    private static Log logger = LogFactory.getLog(SimpleInputStreamFactory.class);

    public InputStream getInputStream(URL url) throws IOException {
        logger.info("getInputStream called");
        return getInputStream(null, url.openConnection());
    }

    public InputStream getInputStream(ConditionalGetValues condGetValues, URL url) throws IOException {
        return getInputStream(condGetValues, url.openConnection());
    }

    public InputStream getInputStream(URLConnection conn) throws IOException {
        return getInputStream(null, conn);
    }

    public InputStream getInputStream(ConditionalGetValues condGetValues, URLConnection conn) throws IOException {
        InputStream feedIStream = null;
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setInstanceFollowRedirects(true);
            HttpHeaderUtils.setUserAgent(httpConn, "Informa Java API");
            HttpHeaderUtils.setAcceptGzipContent(httpConn);
            if (condGetValues != null) {
                HttpHeaderUtils.setETagValue(httpConn, condGetValues.getETag());
                HttpHeaderUtils.setIfModifiedSince(httpConn, condGetValues.getIfModifiedSince());
            }
            httpConn.connect();
            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                logger.info("cond. GET success for feed at url " + conn.getURL() + ": no change");
                return null;
            }
            if (logger.isInfoEnabled()) {
                logger.debug("cond. GET for feed at url " + conn + ": changed");
                logger.info("feed at url " + conn.getURL() + " new values : ETag" + HttpHeaderUtils.getETagValue(httpConn) + " if-modified :" + HttpHeaderUtils.getLastModified(httpConn));
                logger.info("feed at url " + conn.getURL() + " old values : ETag" + condGetValues.getETag() + " if-modified :" + condGetValues.getIfModifiedSince());
            }
            if (condGetValues != null) {
                condGetValues.setETag(HttpHeaderUtils.getETagValue(httpConn));
                condGetValues.setIfModifiedSince(HttpHeaderUtils.getLastModified(httpConn));
            }
            if (HttpHeaderUtils.isContentGZipped((HttpURLConnection) conn)) {
                logger.info("GZIPPED content for " + conn.getURL());
                feedIStream = new GZIPInputStream(conn.getInputStream());
            } else {
                feedIStream = conn.getInputStream();
            }
        } else {
            feedIStream = conn.getInputStream();
        }
        return feedIStream;
    }

    public void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                logger.warn("problem closing stream " + is, e);
            }
        }
    }
}
