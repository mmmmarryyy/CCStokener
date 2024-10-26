package org.apache.axis.transport.jms;

import java.net.URL;
import java.net.URLConnection;

/**
 * URLStreamHandler for the "jms" protocol
 *
 * @author Ray Chun (rchun@sonicsoftware.com)
 */
public class Handler extends java.net.URLStreamHandler {

    static {
        org.apache.axis.client.Call.setTransportForProtocol(JMSConstants.PROTOCOL, org.apache.axis.transport.jms.JMSTransport.class);
    }

    /**
     * Reassembles the URL string, in the form "jms:/<dest>?prop1=value1&prop2=value2&..."
     */
    protected String toExternalForm(URL url) {
        String destination = url.getPath().substring(1);
        String query = url.getQuery();
        StringBuffer jmsurl = new StringBuffer(JMSConstants.PROTOCOL + ":/");
        jmsurl.append(destination).append("?").append(query);
        return jmsurl.toString();
    }

    protected URLConnection openConnection(URL url) {
        return new JMSURLConnection(url);
    }
}
