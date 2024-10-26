package com.incendiaryblue.msg;

import com.incendiaryblue.appframework.ServerConfig;
import com.incendiaryblue.config.XMLConfigurationException;
import com.incendiaryblue.config.XMLContext;
import com.incendiaryblue.util.ObjectPool;
import java.net.*;
import java.io.*;
import org.w3c.dom.*;

/**
 * An implementation of the {@link MessageDelivery} interface for sending and
 * receiving messages across an HTTP connection.
 */
public class HTTPMessageDeliveryImpl implements MessageDelivery {

    private URL url;

    private int numConns;

    private ObjectPool pool;

    public HTTPMessageDeliveryImpl() {
        super();
    }

    /**
	 * Create an HTTPMessageDeliveryImpl object which will send requests to
	 * the specified URL.
	 *
	 * @param url The URL of the server which will server requests.
	 * @param numConnections The maximum number of connections allowed to the
	 *        server at one time.
	 */
    public HTTPMessageDeliveryImpl(URL url, int numConnections) {
        this();
        this.url = url;
        this.numConns = numConnections;
        setupPool();
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
        URLConnection conn = (URLConnection) pool.getObject();
        OutputStream out;
        InputStream in;
        out = conn.getOutputStream();
        out.write(bytes);
        out.flush();
        out.close();
        ByteArrayOutputStream responseOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1000];
        in = conn.getInputStream();
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            responseOut.write(buffer, 0, numRead);
        }
        in.close();
        pool.releaseObject(conn);
        return responseOut.toByteArray();
    }

    /**
	 * Convenience method for configuring by XML. Currently not implemented.
	 */
    public Object configure(Element e, XMLContext context) throws XMLConfigurationException {
        String urlParam = e.getAttribute("url");
        String connsParam = e.getAttribute("maxConns");
        String url = ServerConfig.get(urlParam);
        int maxConns = Integer.parseInt(ServerConfig.get(connsParam));
        try {
            this.url = new URL(url);
        } catch (Exception ex) {
            throw new XMLConfigurationException("Error configuring HTTPMessageDeliveryImpl!", ex);
        }
        this.numConns = maxConns;
        setupPool();
        return this;
    }

    /**
	 * Convenience method for configuring by XML. Currently not implemented.
	 */
    public void registerChild(Object o) {
    }

    private void setupPool() {
        this.pool = new ObjectPool(this.numConns) {

            protected Object getNewObject() throws IOException {
                URLConnection conn = HTTPMessageDeliveryImpl.this.url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "text/xml");
                return conn;
            }
        };
    }
}
