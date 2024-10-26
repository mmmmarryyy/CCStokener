package com.knowgate.jcifs.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A <code>URLStreamHandler</code> used to provide NTLM authentication
 * capabilities to the default HTTP handler.  This acts as a wrapper,
 * handling authentication and passing control to the underlying
 * stream handler.
 * @version 0.9.1
 */
public class Handler extends URLStreamHandler {

    /**
     * The default HTTP port (<code>80</code>).
     */
    public static final int DEFAULT_HTTP_PORT = 80;

    private static final Map PROTOCOL_HANDLERS = new HashMap();

    private static final String HANDLER_PKGS_PROPERTY = "java.protocol.handler.pkgs";

    /**
     * Vendor-specific default packages.  If no packages are specified in
     * "java.protocol.handler.pkgs", the VM uses one or more default
     * packages, which are vendor specific.  Sun's is included below
     * for convenience; others could be as well.  If a particular vendor's
     * package isn't listed, it can be specified in
     * "java.protocol.handler.pkgs".
     */
    private static final String[] JVM_VENDOR_DEFAULT_PKGS = new String[] { "sun.net.www.protocol" };

    private static URLStreamHandlerFactory factory;

    /**
     * Sets the URL stream handler factory for the environment.  This
     * allows specification of the factory used in creating underlying
     * stream handlers.  This can be called once per JVM instance.
     *
     * @param factory The URL stream handler factory.
     */
    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory factory) {
        synchronized (PROTOCOL_HANDLERS) {
            if (Handler.factory != null) {
                throw new IllegalStateException("URLStreamHandlerFactory already set.");
            }
            PROTOCOL_HANDLERS.clear();
            Handler.factory = factory;
        }
    }

    /**
     * Returns the default HTTP port.
     *
     * @return An <code>int</code> containing the default HTTP port.
     */
    protected int getDefaultPort() {
        return DEFAULT_HTTP_PORT;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        url = new URL(url, url.toExternalForm(), getDefaultStreamHandler(url.getProtocol()));
        return new NtlmHttpURLConnection((HttpURLConnection) url.openConnection());
    }

    private static URLStreamHandler getDefaultStreamHandler(String protocol) throws IOException {
        synchronized (PROTOCOL_HANDLERS) {
            URLStreamHandler handler = (URLStreamHandler) PROTOCOL_HANDLERS.get(protocol);
            if (handler != null) return handler;
            if (factory != null) {
                handler = factory.createURLStreamHandler(protocol);
            }
            if (handler == null) {
                String path = System.getProperty(HANDLER_PKGS_PROPERTY);
                StringTokenizer tokenizer = new StringTokenizer(path, "|");
                while (tokenizer.hasMoreTokens()) {
                    String provider = tokenizer.nextToken().trim();
                    if (provider.equals("jcifs")) continue;
                    String className = provider + "." + protocol + ".Handler";
                    try {
                        Class handlerClass = null;
                        try {
                            handlerClass = Class.forName(className);
                        } catch (Exception ex) {
                        }
                        if (handlerClass == null) {
                            handlerClass = ClassLoader.getSystemClassLoader().loadClass(className);
                        }
                        handler = (URLStreamHandler) handlerClass.newInstance();
                        break;
                    } catch (Exception ex) {
                    }
                }
            }
            if (handler == null) {
                for (int i = 0; i < JVM_VENDOR_DEFAULT_PKGS.length; i++) {
                    String className = JVM_VENDOR_DEFAULT_PKGS[i] + "." + protocol + ".Handler";
                    try {
                        Class handlerClass = null;
                        try {
                            handlerClass = Class.forName(className);
                        } catch (Exception ex) {
                        }
                        if (handlerClass == null) {
                            handlerClass = ClassLoader.getSystemClassLoader().loadClass(className);
                        }
                        handler = (URLStreamHandler) handlerClass.newInstance();
                    } catch (Exception ex) {
                    }
                    if (handler != null) break;
                }
            }
            if (handler == null) {
                throw new IOException("Unable to find default handler for protocol: " + protocol);
            }
            PROTOCOL_HANDLERS.put(protocol, handler);
            return handler;
        }
    }
}
