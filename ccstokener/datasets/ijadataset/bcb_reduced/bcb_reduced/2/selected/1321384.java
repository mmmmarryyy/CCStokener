package org.jscep.transport;

import org.jscep.request.PKCSReq;
import org.jscep.request.Request;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Transport representing the <code>HTTP POST</code> method
 * 
 * @author David Grant
 */
public class HttpPostTransport extends Transport {

    HttpPostTransport(URL url) {
        super(url);
    }

    @Override
    public <T> T sendRequest(Request<T> msg) throws IOException {
        if (!(msg instanceof PKCSReq)) {
            throw new IllegalArgumentException("POST transport may not be used for " + msg.getOperation() + " messages.");
        }
        final URL url = getUrl(msg.getOperation());
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        final OutputStream stream = new BufferedOutputStream(conn.getOutputStream());
        try {
            msg.write(stream);
        } finally {
            stream.close();
        }
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(conn.getResponseCode() + " " + conn.getResponseMessage());
        }
        return msg.getContentHandler().getContent(conn.getInputStream(), conn.getContentType());
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
        return "[POST] " + url;
    }
}
