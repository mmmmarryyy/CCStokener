package org.jscep.transport;

import org.jscep.request.Operation;
import org.jscep.request.Request;
import org.jscep.util.LoggingUtil;
import org.slf4j.Logger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Transport representing the <code>HTTP GET</code> method
 * 
 * @author David Grant
 */
public class HttpGetTransport extends Transport {

    private static Logger LOGGER = LoggingUtil.getLogger(HttpGetTransport.class);

    HttpGetTransport(URL url) {
        super(url);
    }

    @Override
    public <T> T sendRequest(Request<T> msg) throws IOException {
        final URL url = getUrl(msg.getOperation(), msg.getMessage());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending {} to {}", msg, url);
        }
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        LOGGER.debug("Received '{} {}' when sending {} to {}", new Object[] { conn.getResponseCode(), conn.getResponseMessage(), msg, url });
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(conn.getResponseCode() + " " + conn.getResponseMessage());
        }
        return msg.getContentHandler().getContent(conn.getInputStream(), conn.getContentType());
    }

    private URL getUrl(Operation op, String message) throws MalformedURLException, UnsupportedEncodingException {
        return new URL(getUrl(op).toExternalForm() + "&message=" + URLEncoder.encode(message, "UTF-8"));
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
        return "[GET] " + url;
    }
}
