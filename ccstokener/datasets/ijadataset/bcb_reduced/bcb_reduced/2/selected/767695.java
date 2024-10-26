package com.incendiaryblue.msg;

import com.incendiaryblue.appframework.ServerConfig;
import com.incendiaryblue.config.XMLConfigurationException;
import com.incendiaryblue.config.XMLContext;
import com.incendiaryblue.logging.EventLog;
import java.net.*;
import java.net.*;
import java.io.*;
import org.w3c.dom.*;

/**
 * An implementation of the {@link MessageDelivery} interface for sending and
 * receiving messages across an HTTP connection.  This differs from the {@link HTTPMessageDeliveryImpl}
 * in being a connectionless implementation.  Each request is a URL, and the response, the page output.
 * As a result no pool is implemented, and a new connection is opened for every request.
 */
public class HTTPRequestMessageDeliveryImpl extends MessageDeliveryImpl {

    private URL url;

    public HTTPRequestMessageDeliveryImpl() {
        super();
    }

    /**
	 * Send a request as an array of bytes and return the server's response as
	 * an array of bytes.
	 *
	 * @param bytes The request to send
	 * @return The response as a byte[]
	 *
	 * @throws IOException If an error occurs communicating with the HTTP server
	 */
    public byte[] send(byte[] bytes) throws IOException {
        ByteArrayOutputStream responseOut = new ByteArrayOutputStream();
        try {
            OutputStream out;
            InputStream in;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            out = conn.getOutputStream();
            out.write(bytes);
            out.flush();
            out.close();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) throw new java.io.IOException();
            if (conn.getContentLength() > 0) {
                byte[] buffer = new byte[1000];
                in = conn.getInputStream();
                int numRead;
                while ((numRead = in.read(buffer)) != -1) {
                    responseOut.write(buffer, 0, numRead);
                }
                in.close();
            }
        } catch (IOException e) {
            EventLog.exception(e);
            throw (e);
        }
        return responseOut.toByteArray();
    }

    /**
	 * Convenience method for configuring by XML. Currently not implemented.
	 */
    public Object configure(Element e, XMLContext context) throws XMLConfigurationException {
        String urlParam = e.getAttribute("url");
        String url = ServerConfig.get(urlParam);
        try {
            this.url = new URL(url);
        } catch (Exception ex) {
            throw new XMLConfigurationException("Error configuring HTTPMessageDeliveryImpl!", ex);
        }
        return this;
    }

    /**
	 * Convenience method for configuring by XML. Currently not implemented.
	 */
    public void registerChild(Object o) {
    }
}
