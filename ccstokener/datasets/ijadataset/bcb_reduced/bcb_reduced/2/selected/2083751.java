package info.wisl;

import java.io.*;
import java.net.*;

/**
 * Performs all communication with the central server and returns responses to
 * the invoking object
 *
 * @author Bill Tice
 */
public class ServerLiaison {

    private static final String SERVERFILEPATH = "config";

    private static final String ADDRESS = "http://cs.wisl.info/";

    private String servletName;

    private String serverAddress;

    private Wisl wisl;

    public ServerLiaison(String servletName, Wisl wisl) {
        this.wisl = wisl;
        this.servletName = servletName + ".php";
        if ((wisl.settings.get("ServerAddress") != null) && (wisl.settings.get("ServerAddress") != "")) {
            serverAddress = wisl.settings.get("ServerAddress");
        } else {
            serverAddress = ADDRESS;
        }
    }

    public String send(StringBuffer buffer) throws Exception {
        StringBuffer result = null;
        URL url;
        if ((wisl.settings.get("HTTPProxyHost") != null) && (wisl.settings.get("HTTPProxyHost") != "")) {
            if ((wisl.settings.get("HTTPProxyPort") != null) && (wisl.settings.get("HTTPProxyPort") != "")) {
                try {
                    int port = Short.parseShort(wisl.settings.get("HTTPProxyPort"));
                    url = new URL("http", wisl.settings.get("HTTPProxyHost"), port, serverAddress.concat(servletName));
                    Debugger.print(Wisl.DEBUG_VERBOSE, "Using a proxy server");
                } catch (NumberFormatException nfe) {
                    Debugger.print(Wisl.DEBUG_IMPORTANT, "Error parsing proxy server port" + ", skipping proxy: " + nfe.getMessage());
                    url = new URL(serverAddress.concat(servletName));
                }
            } else {
                url = new URL("http", wisl.settings.get("HTTPProxyHost"), -1, serverAddress.concat(servletName));
                Debugger.print(Wisl.DEBUG_VERBOSE, "Using a proxy server with a default port");
            }
        } else {
            url = new URL(serverAddress.concat(servletName));
        }
        try {
            result = post(url, buffer);
        } catch (MalformedURLException mue) {
            if (serverAddress.equals(ADDRESS)) throw mue; else {
                serverAddress = ADDRESS;
                return send(buffer);
            }
        } catch (Exception e) {
            throw e;
        }
        if (result.length() == 0) {
            if (serverAddress.equals(ADDRESS)) return null; else {
                serverAddress = ADDRESS;
                return send(buffer);
            }
        } else return result.toString();
    }

    public static final StringBuffer post(URL url, StringBuffer data) throws IOException, MalformedURLException {
        return post(url, data, "application/x-www-form-urlencoded");
    }

    public static final StringBuffer post(URL url, StringBuffer data, String contentType) throws IOException, MalformedURLException {
        StringBuffer result;
        DataOutputStream streamOut = null;
        BufferedReader streamIn = null;
        try {
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", contentType);
            streamOut = new DataOutputStream(new java.io.BufferedOutputStream(conn.getOutputStream()));
            streamOut.writeBytes(data.toString());
            streamOut.flush();
            streamOut.close();
            streamOut = null;
            result = new StringBuffer();
            String str = null;
            streamIn = new java.io.BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((str = streamIn.readLine()) != null) {
                result.append(str);
            }
            streamIn.close();
        } catch (MalformedURLException murle) {
            throw murle;
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            try {
                if (streamIn != null) {
                    streamIn.close();
                }
            } catch (Exception ein) {
            }
            try {
                if (streamOut != null) {
                    streamOut.close();
                }
            } catch (Exception eout) {
            }
        }
        return result;
    }
}
