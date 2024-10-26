package com.itbs.aimcer.commune;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Utility class to help with Web Sites.
 * Has helper functions to get/post pages.
 *
 * @author Alex Rass
 * @since Oct 10, 2004
 */
public class WebHelper {

    /** This is the only way to specify timeouts in java for connections.  Sad. */
    static {
        System.getProperties().setProperty("sun.net.client.defaultConnectTimeout", "" + (2 * 60 * 1000));
        System.getProperties().setProperty("sun.net.client.defaultReadTimeout", "" + (2 * 60 * 1000));
    }

    /**
     * Returns the page.
     * @param url or the page
     * @return page info
     * @throws Exception page fails to load.
     */
    public static String getPage(URL url) throws Exception {
        return getPage(url.getHost(), url.getPort() == -1 ? 80 : url.getPort(), url.getFile());
    }

    /**
     * Generic web query method for web page referenced by url.
     * Return the page as a string.
     *
     * @param host - website
     * @param port - port (80)
     * @param url - url portion of address.
     * @return page info
     */
    public static String getPage(String host, int port, String url) throws Exception {
        Socket httpPipe;
        InputStream inn;
        OutputStream outt;
        PrintStream out;
        InetAddress webServer;
        webServer = InetAddress.getByName(host);
        httpPipe = new Socket(webServer, port);
        inn = httpPipe.getInputStream();
        outt = httpPipe.getOutputStream();
        DataInputStream in = new DataInputStream(inn);
        out = new PrintStream(outt);
        if (inn == null || outt == null) {
            System.err.println("Failed to open streams to socket.");
            return null;
        }
        out.println("GET " + url + " HTTP/1.0");
        out.println("\n");
        String response;
        BufferedReader source = new BufferedReader(new InputStreamReader(in));
        StringBuffer buf = new StringBuffer();
        while ((response = source.readLine()) != null) {
            buf.append(response);
            buf.append("\n");
        }
        out.close();
        source.close();
        if (buf.indexOf("301 Moved Permanently") > 0 || buf.indexOf("302 Moved Temporarily") > 0) {
            int indexOfLocation = buf.indexOf("\nLocation:");
            if (indexOfLocation > 0) {
                indexOfLocation += "\nLocation:".length();
                int len = buf.indexOf("\n", indexOfLocation);
                if (len > 0) {
                    String newUrl = buf.substring(indexOfLocation, len).trim();
                    return getPage(new URL(newUrl));
                }
            }
        }
        return buf.toString();
    }

    public static String getPage(String url, String post) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        if (post != null) {
            connection.connect();
            connection.setDoOutput(true);
            PrintStream out = new PrintStream(connection.getOutputStream());
            out.println(post);
            out.close();
        } else {
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.0.13) Gecko/2009073021 Firefox/3.0.13");
            connection.setRequestMethod("GET");
            connection.connect();
        }
        String response;
        BufferedReader source = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer buf = new StringBuffer();
        while ((response = source.readLine()) != null) {
            buf.append(response);
        }
        source.close();
        return buf.toString();
    }
}
