package com.meschbach.psi.util;

import com.meschbach.psi.PSIException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * The <code>EchoApplication</code> is a mediator around the HTTP client system
 * to provide a set of utilities for interacting with the Echo Web Application.<p>
 *
 * This class is copyright 2010-2011 by Mark Eschbach and is licensed under the
 * Apache License, Version 2.0; accessible at
 * http://www.apache.org/licenses/LICENSE-2.0.<p>
 *
 * @author "Mark Eschbach" meschbach@gmail.com
 * @since 1.0.0
 * @version 1.0.1
 */
public class EchoApplication {

    String contextURL;

    /**
     * Constructs a new EchoApplication client with the specified URL pointing
     * to address the application has been deployed
     * @param contextURL the full URL the application has been deployed too
     */
    public EchoApplication(String contextURL) {
        this.contextURL = contextURL;
    }

    /**
     * Constructs a new EchoApplication which is deployed on the web service
     * accessible by <code>aWebService</code> URL with the specified context path.
     * The context is appended to the web service URL.  The web service URL is
     * expected to be in the  form of "http://webservice-domain:port".
     *
     * @param aWebService is how to access the web service
     * @param aContext is the context to access the application at
     */
    public EchoApplication(String aWebService, String aContext) {
        this(aWebService + aContext);
    }

    public String echo(String aMessage) throws PSIException {
        DefaultHttpClient hc = new DefaultHttpClient();
        try {
            String url;
            try {
                url = contextURL + "/EchoServlet?input=" + URLEncoder.encode(aMessage, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new PSIException(uee);
            }
            HttpGet get = new HttpGet(url);
            try {
                HttpResponse response = hc.execute(get);
                InputStream entity = response.getEntity().getContent();
                String result = Util.convertToString(entity);
                return result;
            } catch (IOException t) {
                throw new PSIException(t);
            }
        } finally {
            hc.getConnectionManager().shutdown();
        }
    }

    public Properties echoInitParameters() throws IOException {
        DefaultHttpClient hc = new DefaultHttpClient();
        try {
            String url = contextURL + "/DumpInitPropsServlet";
            HttpGet get = new HttpGet(url);
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Unexpected result code: " + response.getStatusLine().getStatusCode());
            }
            Properties result = new Properties();
            result.load(response.getEntity().getContent());
            return result;
        } finally {
            hc.getConnectionManager().shutdown();
        }
    }
}
