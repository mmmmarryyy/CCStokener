package org.simject.remoting.client;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simject.util.Protocol;
import org.simject.util.SimConstants;

/**
 * Used to provide remote access over HTTP to a resource. HttpClientProxy uses
 * XStream for XML serialization or normal Java serialization for binary
 * protocol
 * 
 * @author Simon Martinelli
 */
public final class HttpClientProxy implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(HttpClientProxy.class.getName());

    /**
	 * Holds the requested URL
	 */
    private final URL url;

    /**
	 * Holds the protocol
	 */
    private final Protocol protocol;

    /**
	 * Creates a new instance of a proxy
	 * 
	 * @param loader
	 * @param interfaces
	 * @param url
	 * @return an instance of HttpClientProxy
	 * @throws MalformedURLException
	 */
    public static Object newInstance(final ClassLoader loader, final Class<?>[] interfaces, final String target) throws MalformedURLException {
        final String protcolString = target.substring(0, 3);
        Protocol protocol = Protocol.Xml;
        if (protcolString.equals(Protocol.Binary.getString())) {
            protocol = Protocol.Binary;
        }
        final String urlString = target.substring(4);
        final URL url = new URL(urlString);
        logger.info("Creating proxy for URL <" + url + "> using <" + protocol + "> protocol");
        return java.lang.reflect.Proxy.newProxyInstance(loader, interfaces, new HttpClientProxy(url, protocol));
    }

    /**
	 * Private Constructor
	 * 
	 * @param url
	 */
    private HttpClientProxy(final URL url, final Protocol protocol) {
        this.url = url;
        this.protocol = protocol;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        logger.info("Invoking method <" + method.getName() + "> with arguments <" + args + ">");
        Object result;
        try {
            if (protocol == Protocol.Binary) {
                result = this.invokeUrlBinary2(method, args);
            } else {
                result = this.invokeUrlXml2(method, args);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        return result;
    }

    /**
	 * Synchronous call over HTTP using binary protocol
	 * 
	 * 1. Serializes the arguments to Binary and make a remote call over HTTP
	 * with Commons HttpClient to the desired method. 2. Deserializes the binary
	 * response from the server.
	 * 
	 * @param method
	 * @param args
	 * @return
	 * @throws Throwable
	 */
    private Object invokeUrlBinary2(final Method method, final Object[] args) throws Throwable {
        final HttpURLConnection con = (HttpURLConnection) this.url.openConnection();
        con.setDoOutput(true);
        this.createHeader2(method, con);
        final ObjectOutputStream oos = new ObjectOutputStream(con.getOutputStream());
        oos.writeObject(args);
        oos.close();
        Object result = null;
        if (con.getContentLength() > 0) {
            final ObjectInputStream ois = new ObjectInputStream(con.getInputStream());
            result = ois.readObject();
            if (result instanceof Throwable) {
                throw ((Throwable) result);
            }
        }
        con.disconnect();
        return result;
    }

    /**
	 * Synchronous call over HTTP using XML protocol
	 * 
	 * 1. Serializes the arguments to XML using XMLEncoder and make a remote
	 * call over HTTP with Commons HttpClient to the desired method. 2.
	 * Deserializes the XML response from the server using XMLDecoder.
	 * 
	 * @param method
	 * @param args
	 * @return
	 * @throws Throwable
	 */
    private Object invokeUrlXml2(final Method method, final Object[] args) throws Throwable {
        final HttpURLConnection con = (HttpURLConnection) this.url.openConnection();
        con.setDoOutput(true);
        this.createHeader2(method, con);
        final XMLEncoder encoder = new XMLEncoder(con.getOutputStream());
        encoder.writeObject(args);
        Object result = null;
        if (con.getContentLength() > 0) {
            final XMLDecoder decoder = new XMLDecoder(con.getInputStream());
            result = decoder.readObject();
            if (result instanceof Throwable) {
                throw ((Throwable) result);
            }
        }
        con.disconnect();
        return result;
    }

    /**
	 * Create the necessairy Header entries
	 * 
	 * @param method
	 * @param post
	 */
    private void createHeader2(final Method method, final HttpURLConnection con) {
        con.setRequestProperty(SimConstants.PARAM_METHOD, method.getName());
        final StringBuffer params = new StringBuffer();
        for (Class<?> param : method.getParameterTypes()) {
            final String paramString = param.getName() + SimConstants.PARAM_TYPE_DELIM;
            params.append(paramString);
        }
        if (params.length() > 0) {
            final String parameters = params.toString();
            con.setRequestProperty(SimConstants.PARAM_TYPES, parameters);
        }
    }
}
