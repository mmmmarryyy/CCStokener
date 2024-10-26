package org.susan.java.logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.LogManager;

public class RemoteConfigReader {

    private String urlString = "http://localhost/logging.properties";

    private URL url;

    private URLConnection urlConn = null;

    private InputStream inputStream = null;

    private LogManager logManager = null;

    public RemoteConfigReader() {
        try {
            url = new URL(urlString);
            urlConn = url.openConnection();
            inputStream = urlConn.getInputStream();
            logManager = LogManager.getLogManager();
            logManager.readConfiguration(inputStream);
        } catch (MalformedURLException mue) {
            System.err.println("Could not open url:" + urlString);
        } catch (IOException ioe) {
            System.err.println("IOException occured in reading:" + urlString);
        } catch (SecurityException se) {
            System.err.println("Security exception occured in class RemoteConfigReader");
        }
    }

    public static void main(String args[]) {
        new RemoteConfigReader();
    }
}
