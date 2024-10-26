package org.openkickoff.client.net;

import org.openkickoff.server.ServerProtocol;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/** Communicate using HTTP tunneling
 * For test only! Can go through firewalls: you could even play at work
 * hum... is is <strong>really</strong> a good idea ? ;-)
 * @author Herve Foucher
 * @since 0.4
 * @version 0.4
 */
public class HTTPTransport implements TransportAbstractionLayer {

    private String _destHost;

    private int _destPort;

    private String _proxyHost;

    private int _proxyPort;

    private String _proxyEncodedIdentification;

    private int _magicNumber;

    private String _session;

    /** Constructor to be used if you are behind a proxy/firewall
	* @param destHost the remote host (ex: "www.helio.org")
	* @param destPort the remote port (ex: 8080)
	* @param proxyHost the proxy host name (ex: "proxy")
	* @param proxyPort the proxy port (ex: 3128)
	* @param proxyUsername the proxy user name (ex: "jsmith")
	* @param proxyPassword the proxy user password (ex: "dfsdJFD87")
	*/
    public HTTPTransport(String destHost, int destPort, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
        _session = null;
        _destHost = destHost;
        _destPort = destPort;
        _proxyHost = proxyHost;
        _proxyPort = proxyPort;
        _magicNumber = 0;
        Base64Encoder encoder = new Base64Encoder();
        _proxyEncodedIdentification = encoder.encode(proxyUsername + ":" + proxyPassword);
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("proxyHost", _proxyHost);
        System.getProperties().put("proxyPort", "" + _proxyPort);
    }

    public boolean sendMessage(String msg) {
        _magicNumber++;
        try {
            URL url = new URL("http://" + _destHost + ":" + _destPort + "/" + encodeURL(msg + " " + _magicNumber + (_session == null ? "" : ("?session=" + _session))));
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Proxy-Authorization", "Basic " + _proxyEncodedIdentification);
            connection.setUseCaches(false);
            connection.getInputStream().close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getMessage() {
        _magicNumber++;
        try {
            URL url = new URL("http://" + _destHost + ":" + _destPort + "/WAIT" + encodeURL(" " + _magicNumber));
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Proxy-Authorization", "Basic " + _proxyEncodedIdentification);
            connection.setUseCaches(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            System.err.println("Waiting for a message...");
            String message = in.readLine();
            if (message.indexOf(ServerProtocol.SESSION) != -1) {
                _session = message.substring(message.indexOf("=") + 1, message.indexOf(" ") - 1);
                return message.substring(message.indexOf(" ") + 1);
            } else return message;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
    }

    /** Encode using URL encoding */
    private String encodeURL(String url) {
        StringBuffer buf = new StringBuffer();
        int strlenght = url.length();
        for (int i = 0; i < strlenght; i++) {
            char c = url.charAt(i);
            if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9'))) {
                buf.append(c);
            } else {
                buf.append('%');
                buf.append(Character.forDigit((c >> 4) & 0xF, 16));
                buf.append(Character.forDigit((c >> 0) & 0xF, 16));
            }
        }
        return buf.toString();
    }
}
