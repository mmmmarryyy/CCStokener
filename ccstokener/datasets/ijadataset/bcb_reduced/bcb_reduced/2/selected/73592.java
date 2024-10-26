package com.meterware.httpunit;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The context for a series of HTTP requests. This class manages cookies used to maintain
 * session context, computes relative URLs, and generally emulates the browser behavior
 * needed to build an automated test of a web site.
 * 
 * This is just a copy of {@link WebConversation} plus some modifications 
 * so that the response is downloaded to a file instead of returning it back.
 * 
 * @author Alvaro Egana:
 * 
 * 
 **/
public class DownloadWebConversation extends WebClient {

    private String _proxyHost;

    private int _proxyPort;

    private File targetDir;

    /**
     * Creates a new web conversation.
     **/
    public DownloadWebConversation() {
        targetDir = new File(System.getProperty("java.io.tmpdir"));
    }

    public DownloadWebConversation(File targetFile) {
        this.targetDir = targetFile;
    }

    /**
     * Creates a web response object which represents the response to the specified web request.
     **/
    protected WebResponse newResponse(WebRequest request, FrameSelector targetFrame) throws MalformedURLException, IOException {
        Properties savedProperties = (Properties) System.getProperties().clone();
        try {
            if (_proxyHost != null) {
                System.setProperty("proxyHost", _proxyHost);
                System.setProperty("proxyPort", Integer.toString(_proxyPort));
            }
            URLConnection connection = openConnection(getRequestURL(request));
            if (HttpUnitOptions.isLoggingHttpHeaders()) {
                String urlString = request.getURLString();
                System.out.println("\nConnecting to " + request.getURL().getHost());
                System.out.println("Sending:: " + request.getMethod() + " " + urlString);
            }
            sendHeaders(connection, getHeaderFields(request.getURL()));
            sendHeaders(connection, request.getHeaderDictionary());
            request.completeRequest(connection);
            if (!targetDir.canRead() || !targetDir.canWrite()) throw new IOException("User has not enough permissions on '" + targetDir.getPath() + "' directory");
            if (!targetDir.isDirectory()) throw new IOException("'" + targetDir.getPath() + "' is not a directory.");
            return new DownloadHttpWebResponse(this, targetFrame, request, connection, getExceptionsThrownOnErrorStatus(), targetDir);
        } finally {
            System.setProperties(savedProperties);
        }
    }

    public void clearProxyServer() {
        _proxyHost = null;
    }

    public void setProxyServer(String proxyHost, int proxyPort) {
        _proxyHost = proxyHost;
        _proxyPort = proxyPort;
    }

    private URL getRequestURL(WebRequest request) throws MalformedURLException {
        DNSListener dnsListener = getClientProperties().getDnsListener();
        if (dnsListener == null) return request.getURL();
        String hostName = request.getURL().getHost();
        String portPortion = request.getURL().getPort() == -1 ? "" : (":" + request.getURL().getPort());
        setHeaderField("Host", hostName + portPortion);
        String actualHost = dnsListener.getIpAddress(hostName);
        if (HttpUnitOptions.isLoggingHttpHeaders()) System.out.println("Rerouting request to :: " + actualHost);
        return new URL(request.getURL().getProtocol(), actualHost, request.getURL().getPort(), request.getURL().getFile());
    }

    private URLConnection openConnection(URL url) throws MalformedURLException, IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        return connection;
    }

    private void sendHeaders(URLConnection connection, Dictionary headers) {
        for (Enumeration e = headers.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            connection.setRequestProperty(key, (String) headers.get(key));
            if (HttpUnitOptions.isLoggingHttpHeaders()) {
                if (key.equalsIgnoreCase("authorization") || key.equalsIgnoreCase("proxy-authorization")) {
                    System.out.println("Sending:: " + key + ": " + headers.get(key));
                } else {
                    System.out.println("Sending:: " + key + ": " + connection.getRequestProperty(key));
                }
            }
        }
    }
}
