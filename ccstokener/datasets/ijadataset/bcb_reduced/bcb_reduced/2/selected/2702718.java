package de.nava.informa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * implementation of InputStreamFactory using jakarta-commons
 * httpClient<br/>
 * implement conditionnal get, gzipped content support and connection
 * timeout.
 * <code>timeout</code> for socket is hardcoded.
 * @author Jean-Guy Avelin
 */
public class WithTimeOutInputStreamFactory implements InputStreamFactoryIF {

    static final int maxRedirect = 10;

    private static Log logger = LogFactory.getLog(WithTimeOutInputStreamFactory.class);

    private static HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

    static {
        client.setTimeout(5000);
        logger.info("WithTimeOutInputStreamFactory INIT :" + System.getProperty("proxySet"));
        if ("true".equals(System.getProperty("proxySet"))) {
            logger.info("System proxy settings values read");
            String pHost = System.getProperty("http.proxyHost");
            String pPort = System.getProperty("http.proxyPort");
            HostConfiguration hc = new HostConfiguration();
            hc.setProxy(pHost, Integer.valueOf(pPort).intValue());
            client.setHostConfiguration(hc);
        }
    }

    public InputStream getInputStream(URL url) throws IOException {
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
        HttpMethod meth = null;
        if (conn instanceof HttpURLConnection) {
            meth = new GetMethod(conn.getURL().toExternalForm());
            meth.setFollowRedirects(true);
            meth.addRequestHeader(new Header("User-Agent", "Informa Java API"));
            meth.addRequestHeader(new Header("Accept-Encoding", "gzip"));
            if (condGetValues != null) {
                if (condGetValues.getETag() != null) meth.addRequestHeader(new Header("If-None-Match", condGetValues.getETag()));
                meth.addRequestHeader(new Header("If-Modified-Since", condGetValues.getIfModifiedSinceAsString()));
            }
            client.executeMethod(meth);
            int result = meth.getStatusCode();
            if (logger.isDebugEnabled()) {
                logger.debug("***return get  " + result + " for " + conn.getURL().toExternalForm());
            }
            for (int nbRedirect = 0; HttpHeaderUtils.isHttpRedirectCode(result) && nbRedirect < maxRedirect; nbRedirect++) {
                Header locationHeader = meth.getResponseHeader("location");
                meth.recycle();
                if (locationHeader == null || locationHeader.getValue() == null) {
                    logger.error("oops, location header ==  " + locationHeader);
                    logger.error(conn.getURL().toExternalForm() + "get return " + result + " " + HttpHeaderUtils.isHttpRedirectCode(result));
                }
                URI newLocation = new URI(locationHeader.getValue());
                HostConfiguration hc = meth.getHostConfiguration();
                hc.setHost(newLocation.getHost(), newLocation.getPort(), newLocation.getScheme());
                meth.setPath(newLocation.getEscapedPath());
                meth.setQueryString(newLocation.getEscapedQuery());
                meth.addRequestHeader(new Header("User-Agent", "Informa Java API"));
                meth.addRequestHeader(new Header("Accept-Encoding", "gzip"));
                if (condGetValues != null) {
                    meth.addRequestHeader(new Header("If-None-Match", condGetValues.getETag()));
                    meth.addRequestHeader(new Header("If-Modified-Since", condGetValues.getIfModifiedSinceAsString()));
                }
                result = client.executeMethod(meth);
                logger.info(conn.getURL().toExternalForm() + " redirected to : " + locationHeader.getValue());
            }
            if (result == HttpStatus.SC_NOT_MODIFIED) {
                logger.debug("cond. GET success for feed at url " + conn.getURL() + ": no change");
                meth.releaseConnection();
                return null;
            }
            logger.debug("cond. GET for feed at url " + conn + ": changed");
            if (condGetValues != null) {
                Header etags = meth.getResponseHeader("ETag");
                Header ifModified = meth.getResponseHeader("Last-Modified");
                if (logger.isDebugEnabled()) {
                    logger.debug("feed at url " + conn.getURL() + " new values : Etags" + etags + " if-modified :" + ifModified);
                    logger.debug("feed at url " + conn.getURL() + " old values : ETags" + condGetValues.getETag() + " if-modified :" + condGetValues.getIfModifiedSince());
                }
                if (etags != null) {
                    condGetValues.setETag(etags.getValue());
                }
                if (ifModified != null && ifModified.getValue() != null) {
                    condGetValues.setIfModifiedSince(ifModified.getValue());
                }
            }
            feedIStream = meth.getResponseBodyAsStream();
            if (meth != null) {
                Header gzipped = meth.getResponseHeader("content-encoding");
                if (gzipped != null && "gzip".equals(gzipped.getValue())) {
                    logger.info("GZIPPED feed content for " + conn.getURL());
                    feedIStream = new GZIPInputStream(feedIStream);
                }
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
