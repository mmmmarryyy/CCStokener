package net.sf.bt747.j2se.system;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import net.sf.bt747.loc.LocationSender;
import bt747.sys.Generic;
import bt747.sys.interfaces.BT747Exception;
import bt747.sys.interfaces.BT747Hashtable;
import bt747.sys.interfaces.BT747HttpSender;

public class J2SEHttpSenderImpl implements BT747HttpSender {

    /**
	 * The protocol String, alsways "http" as https is not supported by this
	 * class.
	 */
    private static final String HTTP_PROTOCOL = "http";

    /**
	 * By default the URL encoding will be done using the UTF-8 encoding. This
	 * is used when no specific encoding is given as parameter to the request.
	 */
    private static final String DEFAULT_ENCODING = "UTF-8";

    public void doRequest(final String hostname, final int port, final String file, final String user, final String password, final BT747Hashtable data, final String encodingOrNull, final LocationSender caller) {
        Thread t = new Thread() {

            public void run() {
                doRequestAsynchronously(hostname, port, file, user, password, data, encodingOrNull, caller);
                super.run();
            }
        };
        t.start();
    }

    /**
	 * Do the request asynchronously.
	 * 
	 * @param hostname
	 *            the server to conect to
	 * @param port
	 *            the port
	 * @param file
	 *            the rest of the URL
	 * @param user
	 *            the username, may be null or empty, then there will be no
	 *            authentication.
	 * @param password
	 *            may be empty or null, only used when user is set.
	 * @param data
	 *            the data to send as BT747Hashtable
	 * @param encodingOrNull
	 *            an optional enncoding like "UTF-8"
	 * @param caller
	 *            the calling KocationSender instance. This object will get the
	 *            notifications about success or failure of the request.
	 */
    private void doRequestAsynchronously(String hostname, int port, String file, String user, String password, BT747Hashtable data, String encodingOrNull, final LocationSender caller) {
        String encodedData = null;
        try {
            encodedData = encodeRequestData(data, encodingOrNull);
        } catch (UnsupportedEncodingException e) {
            caller.notifyFatalFailure("Unsupported encoding.");
            return;
        }
        URL url = null;
        try {
            url = createURL(hostname, port, file, encodedData);
        } catch (MalformedURLException e) {
            caller.notifyFatalFailure("Could not prepare URL from parameters.");
            return;
        }
        Generic.debug("LocSrv - connect to " + hostname + ", sending " + encodedData);
        HttpURLConnection conn = null;
        try {
            conn = sendData(url, user, password);
        } catch (IOException e) {
            caller.notifyConnectionFailure("Connect to target server failed.");
            return;
        } catch (IllegalArgumentException e) {
            Generic.debug("Connection error", e);
            caller.notifyConnectionFailure("Connect to target server failed.");
            return;
        }
        try {
            readResult(conn);
        } catch (FileNotFoundException e) {
            caller.notifyFatalFailure("URL points to non existing location on target server.");
            return;
        } catch (IOException e) {
            caller.notifyConnectionFailure("Problem reading the target server response");
            return;
        }
        try {
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                caller.notifySuccess();
            } else {
                caller.notifyConnectionFailure("Did not receive Code 200");
            }
        } catch (IOException ioe) {
            caller.notifyConnectionFailure("Problem reading response code from response");
        }
    }

    /**
	 * Create an URL object from the data provided.
	 * 
	 * @param hostname
	 *            the hostname
	 * @param port
	 *            a port like 80
	 * @param file
	 *            the rest of the URL, may be empty, must not be null
	 * @param encodedData
	 *            the data as URL-encoded String
	 * @return a URL insatnce
	 * @throws MalformedURLException
	 *             when the creation of the URL fails becaise of wrong input.
	 */
    private URL createURL(String hostname, int port, String file, String encodedData) throws MalformedURLException {
        if ((file.length() > 0) && (file.charAt(0) != '/')) {
            file = "/" + file;
        }
        file += "?" + encodedData;
        return new URL(HTTP_PROTOCOL, hostname, port, file);
    }

    /**
	 * Send data via GET request.
	 * 
	 * @param url
	 *            the URL to send the data to
	 * @param user
	 *            the user for authetication to the server
	 * @param password
	 *            a password if user authentication is used
	 * @return the URLConnection created during the request so that this
	 *         URLConnection can be used for reading the response from the
	 *         server
	 * @throws IOException
	 *             when the sending fails.
	 */
    private HttpURLConnection sendData(URL url, String user, String password) throws IOException, IllegalArgumentException {
        String tmpAuthUserName = "";
        if (user != null) {
            tmpAuthUserName = user;
        }
        final String anAuthUserName = tmpAuthUserName;
        String tmpAuthPasswd = "";
        if (password != null) {
            tmpAuthPasswd = password;
        }
        final String anAuthPasswd = tmpAuthPasswd;
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(anAuthUserName, anAuthPasswd.toCharArray());
            }
        });
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setReadTimeout(1000);
        conn.connect();
        return conn;
    }

    /**
	 * Read from an URLConnection the result of the HTTP Request
	 * 
	 * @param conn
	 *            the URLConnection containing the response
	 * @return String the result of the request
	 * @throws IOException
	 *             when reading the result dails
	 */
    private String readResult(URLConnection conn) throws IOException {
        BufferedReader rd = null;
        StringBuilder sb = new StringBuilder();
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line + '\n');
            }
        } finally {
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException e) {
                }
            }
        }
        return sb.toString();
    }

    /**
	 * Encodes the request parameters into a String representation.
	 * 
	 * @param data
	 *            a BT747Hashtable containing the key value pairs for the
	 *            parameters.
	 * @param encoding
	 *            an encoding to use or null if the default encoding should be
	 *            used.
	 * @return the request as String representation
	 * @throws BT747Exception
	 *             when encoding contains an unsupported encoding
	 * @throws UnsupportedEncodingException
	 *             when the encoding fails due to an unsupported encoding.
	 */
    private String encodeRequestData(BT747Hashtable data, String encodingOrNull) throws UnsupportedEncodingException {
        String encoding = DEFAULT_ENCODING;
        if (encodingOrNull != null) {
            encoding = encodingOrNull;
        }
        StringBuffer encodedData = new StringBuffer();
        BT747Hashtable it = data.iterator();
        while (it.hasNext()) {
            String key = (String) it.nextKey();
            String value = (String) data.get(key);
            if (encodedData.length() > 0) {
                encodedData.append('&');
            }
            encodedData.append(URLEncoder.encode(key, encoding));
            encodedData.append('=');
            encodedData.append(URLEncoder.encode(value, encoding));
        }
        return encodedData.toString();
    }
}
