package org.eclipse.help.internal.standalone;

import java.io.*;
import java.net.*;
import java.util.Properties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

/**
 * This program is used to start or stop Eclipse Infocenter application. It
 * should be launched from command line.
 */
public class EclipseConnection {

    private String host;

    private String port;

    public EclipseConnection() {
    }

    public String getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void reset() {
        host = null;
        port = null;
    }

    public boolean isValid() {
        return (host != null && port != null);
    }

    public void connect(URL url) throws InterruptedException, Exception {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection secureConnection = (HttpsURLConnection) connection;
                secureConnection.setHostnameVerifier(new HostnameVerifier() {

                    public boolean verify(String urlHostName, javax.net.ssl.SSLSession session) {
                        if (Options.isDebug()) {
                            System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                        }
                        return true;
                    }
                });
            }
            if (Options.isDebug()) {
                System.out.println("Connection  to control servlet created.");
            }
            connection.connect();
            if (Options.isDebug()) {
                System.out.println("Connection  to control servlet connected.");
            }
            int code = connection.getResponseCode();
            if (Options.isDebug()) {
                System.out.println("Response code from control servlet=" + code);
            }
            connection.disconnect();
            if (code == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectLocation = connection.getHeaderField("location");
                URL redirectURL = new URL(redirectLocation);
                if (url.equals(redirectURL)) {
                    if (Options.isDebug()) {
                        System.out.println("Redirecting to the same URL! " + redirectLocation);
                    }
                    return;
                }
                if (Options.isDebug()) {
                    System.out.println("Follows redirect to " + redirectLocation);
                }
                connect(redirectURL);
            }
            return;
        } catch (IOException ioe) {
            if (Options.isDebug()) {
                ioe.printStackTrace();
            }
        }
    }

    /**
	 * Obtains host and port from the file. Retries several times if file does
	 * not exists, and help might be starting up.
	 */
    public void renew() throws Exception {
        Properties p = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(Options.getConnectionFile());
            p.load(is);
            is.close();
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe2) {
                }
            }
        }
        host = (String) p.get("host");
        port = (String) p.get("port");
        if (Options.isDebug()) {
            System.out.println("Help server host=" + host);
        }
        if (Options.isDebug()) {
            System.out.println("Help server port=" + port);
        }
    }
}
