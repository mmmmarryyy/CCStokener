package net.laubenberger.bogatyr.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.laubenberger.bogatyr.helper.encoder.EncoderBase64;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionIsEmpty;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionIsNull;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionMustBeGreater;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionMustBeSmaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper class for network operations.
 *
 * @author Stefan Laubenberger
 * @version 0.9.4 (20101119)
 * @since 0.5.0
 */
public abstract class HelperNet {

    private static final Logger log = LoggerFactory.getLogger(HelperNet.class);

    public static final String PROPERTY_HTTP_PROXY_HOST = "http.proxyHost";

    public static final String PROPERTY_HTTP_PROXY_PORT = "http.proxyPort";

    public static final String PROPERTY_HTTPS_PROXY_HOST = "https.proxyHost";

    public static final String PROPERTY_HTTPS_PROXY_PORT = "https.proxyPort";

    public static final String PROPERTY_FTP_PROXY_HOST = "ftp.proxyHost";

    public static final String PROPERTY_FTP_PROXY_PORT = "ftp.proxyPort";

    private static final int TIMEOUT = 2000;

    /**
	 * Enable VM wide use of a proxy for HTTP.
	 *
	 * @param host	  of the proxy
	 * @param port	  of the proxy
	 * @param username for authentication
	 * @param password for authentication
	 * @since 0.5.0
	 */
    public static void enableProxyHttp(final String host, final int port, final String username, final String password) {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(host, port, username, password));
        if (null == host) {
            throw new RuntimeExceptionIsNull("host");
        }
        if (!HelperString.isValid(host)) {
            throw new RuntimeExceptionIsEmpty("host");
        }
        if (0 >= port) {
            throw new RuntimeExceptionMustBeGreater("port", port, 0);
        }
        if (HelperNumber.NUMBER_65536.intValue() <= port) {
            throw new RuntimeExceptionMustBeSmaller("port", port, 65535);
        }
        if (null == username) {
            throw new RuntimeExceptionIsNull("username");
        }
        if (!HelperString.isValid(username)) {
            throw new RuntimeExceptionIsEmpty("username");
        }
        if (null == password) {
            throw new RuntimeExceptionIsNull("password");
        }
        System.setProperty(PROPERTY_HTTP_PROXY_HOST, host);
        System.setProperty(PROPERTY_HTTP_PROXY_PORT, Integer.toString(port));
        authenticate(username, password);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    /**
	 * Disable VM wide use of a proxy for HTTP.
	 *
	 * @since 0.5.0
	 */
    public static void disableProxyHttp() {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        System.clearProperty(PROPERTY_HTTP_PROXY_HOST);
        System.clearProperty(PROPERTY_HTTP_PROXY_PORT);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    /**
	 * Enable VM wide use of a proxy for HTTPS.
	 *
	 * @param host	  of the proxy
	 * @param port	  of the proxy
	 * @param username for authentication
	 * @param password for authentication
	 * @since 0.5.0
	 */
    public static void enableProxyHttps(final String host, final int port, final String username, final String password) {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(host, port, username, password));
        if (null == host) {
            throw new RuntimeExceptionIsNull("host");
        }
        if (!HelperString.isValid(host)) {
            throw new RuntimeExceptionIsEmpty("host");
        }
        if (0 >= port) {
            throw new RuntimeExceptionMustBeGreater("port", port, 0);
        }
        if (HelperNumber.NUMBER_65536.intValue() <= port) {
            throw new RuntimeExceptionMustBeSmaller("port", port, 65535);
        }
        if (null == username) {
            throw new RuntimeExceptionIsNull("username");
        }
        if (!HelperString.isValid(username)) {
            throw new RuntimeExceptionIsEmpty("username");
        }
        if (null == password) {
            throw new RuntimeExceptionIsNull("password");
        }
        System.setProperty(PROPERTY_HTTPS_PROXY_HOST, host);
        System.setProperty(PROPERTY_HTTPS_PROXY_PORT, Integer.toString(port));
        authenticate(username, password);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    /**
	 * Disable VM wide use of a proxy for HTTPS.
	 *
	 * @since 0.5.0
	 */
    public static void disableProxyHttps() {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        System.clearProperty(PROPERTY_HTTPS_PROXY_HOST);
        System.clearProperty(PROPERTY_HTTPS_PROXY_PORT);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    /**
	 * Enable VM wide use of a proxy for FTP.
	 *
	 * @param host	  of the proxy
	 * @param port	  of the proxy
	 * @param username for authentication
	 * @param password for authentication
	 * @since 0.5.0
	 */
    public static void enableProxyFtp(final String host, final int port, final String username, final String password) {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(host, port, username, password));
        if (null == host) {
            throw new RuntimeExceptionIsNull("host");
        }
        if (!HelperString.isValid(host)) {
            throw new RuntimeExceptionIsEmpty("host");
        }
        if (0 >= port) {
            throw new RuntimeExceptionMustBeGreater("port", port, 0);
        }
        if (HelperNumber.NUMBER_65536.intValue() <= port) {
            throw new RuntimeExceptionMustBeSmaller("port", port, 65535);
        }
        if (null == username) {
            throw new RuntimeExceptionIsNull("username");
        }
        if (!HelperString.isValid(username)) {
            throw new RuntimeExceptionIsEmpty("username");
        }
        if (null == password) {
            throw new RuntimeExceptionIsNull("password");
        }
        System.setProperty(PROPERTY_FTP_PROXY_HOST, host);
        System.setProperty(PROPERTY_FTP_PROXY_PORT, Integer.toString(port));
        authenticate(username, password);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    /**
	 * Disable VM wide use of a proxy for FTP.
	 *
	 * @since 0.5.0
	 */
    public static void disableProxyFtp() {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        System.clearProperty(PROPERTY_FTP_PROXY_HOST);
        System.clearProperty(PROPERTY_FTP_PROXY_PORT);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    /**
	 * Tests if a host is pingable.
	 *
	 * @param host for the ping command
	 * @return true/false
	 * @throws IOException
	 * @since 0.5.0
	 */
    public static boolean isPingable(final String host) throws IOException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(host));
        if (null == host) {
            throw new RuntimeExceptionIsNull("host");
        }
        if (!HelperString.isValid(host)) {
            throw new RuntimeExceptionIsEmpty("host");
        }
        final InetAddress address = InetAddress.getByName(host);
        final boolean result = address.isReachable(TIMEOUT);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns the IP for an host name.
	 *
	 * @param host for IP
	 * @return IP of the host name
	 * @throws UnknownHostException
	 * @since 0.5.0
	 */
    public static String getIp(final String host) throws UnknownHostException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(host));
        if (null == host) {
            throw new RuntimeExceptionIsNull("host");
        }
        if (!HelperString.isValid(host)) {
            throw new RuntimeExceptionIsEmpty("host");
        }
        final InetAddress address = InetAddress.getByName(host);
        final String result = address.getHostAddress();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns the host name of an IP address.
	 *
	 * @param ip for host name
	 * @return host name
	 * @throws UnknownHostException
	 * @since 0.5.0
	 */
    public static String getHostname(final String ip) throws UnknownHostException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(ip));
        if (null == ip) {
            throw new RuntimeExceptionIsNull("ip");
        }
        if (!HelperString.isValid(ip)) {
            throw new RuntimeExceptionIsEmpty("ip");
        }
        final InetAddress address = InetAddress.getByName(ip);
        final String result = address.getHostName();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns the local host name of the current machine.
	 *
	 * @return host name of the current machine
	 * @throws UnknownHostException
	 * @since 0.5.0
	 */
    public static String getLocalHostname() throws UnknownHostException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        final InetAddress address = InetAddress.getLocalHost();
        final String result = address.getHostName();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns the local IP address of the current machine.
	 *
	 * @return IP of the current machine
	 * @throws UnknownHostException
	 * @since 0.5.0
	 */
    public static String getLocalIp() throws UnknownHostException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        final InetAddress address = InetAddress.getLocalHost();
        final String result = address.getHostAddress();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns a {@link List} of all IP addresses of the current machine.
	 *
	 * @return {@link List} containing all IP addresses of the current machine
	 * @throws UnknownHostException
	 * @since 0.5.0
	 */
    public static List<String> getLocalIps() throws UnknownHostException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        final List<String> result = new ArrayList<String>();
        final String localHost = InetAddress.getLocalHost().getHostName();
        for (final InetAddress address : InetAddress.getAllByName(localHost)) {
            result.add(address.getHostAddress());
        }
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns {@link List} of all {@link NetworkInterface} of the current machine.
	 *
	 * @return {@link List} containing all {@link NetworkInterface} of the current machine
	 * @throws SocketException
	 * @see NetworkInterface
	 * @since 0.5.0
	 */
    public static List<NetworkInterface> getNetworkInterfaces() throws SocketException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart());
        final List<NetworkInterface> result = Collections.list(NetworkInterface.getNetworkInterfaces());
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns the MAC address of a given network interface.
	 *
	 * @param ni {@link NetworkInterface} to determine the MAC address
	 * @return MAC address of a given {@link NetworkInterface}
	 * @throws SocketException
	 * @see NetworkInterface
	 * @since 0.7.0
	 */
    public static String getMacAddress(final NetworkInterface ni) throws SocketException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(ni));
        if (null == ni) {
            throw new RuntimeExceptionIsNull("ni");
        }
        final StringBuilder sb = new StringBuilder();
        final byte[] hardwareAddress = ni.getHardwareAddress();
        if (null != hardwareAddress) {
            for (int ii = 0; ii < hardwareAddress.length; ii++) {
                sb.append(String.format((0 == ii ? HelperString.EMPTY_STRING : '-') + "%02X", hardwareAddress[ii]));
            }
        }
        final String result = sb.toString();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns an {@link InputStream} linked to an {@link URL}.
	 *
	 * @param url to read
	 * @return {@link InputStream} for the content
	 * @throws IOException
	 * @see URL
	 * @see InputStream
	 * @since 0.5.0
	 */
    public static InputStream readUrl(final URL url) throws IOException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(url));
        if (null == url) {
            throw new RuntimeExceptionIsNull("url");
        }
        final URLConnection con = url.openConnection();
        con.setConnectTimeout(TIMEOUT);
        con.connect();
        final InputStream result = con.getInputStream();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    /**
	 * Returns an {@link InputStream} linked to an {@link URL} with HTTP basic authentication.
	 *
	 * @param url		to read
	 * @param username for the HTTP authentication
	 * @param password for the HTTP authentication
	 * @return {@link InputStream} for the content
	 * @throws IOException
	 * @see URL
	 * @see InputStream
	 * @since 0.5.0
	 */
    public static InputStream readUrl(final URL url, final String username, final String password) throws IOException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(url, username, password));
        if (null == url) {
            throw new RuntimeExceptionIsNull("url");
        }
        if (null == username) {
            throw new RuntimeExceptionIsNull("username");
        }
        if (!HelperString.isValid(username)) {
            throw new RuntimeExceptionIsEmpty("username");
        }
        if (null == password) {
            throw new RuntimeExceptionIsNull("password");
        }
        final URLConnection con = url.openConnection();
        con.setRequestProperty("Authorization", "Basic " + EncoderBase64.encode(username + ':' + password));
        con.setConnectTimeout(TIMEOUT);
        con.connect();
        final InputStream result = con.getInputStream();
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    private static void authenticate(final String username, final String password) {
        if (log.isTraceEnabled()) log.trace(HelperLog.methodStart(username, password));
        Authenticator.setDefault(new WebAuthenticator(username, password));
        if (log.isTraceEnabled()) log.trace(HelperLog.methodExit());
    }

    private static class WebAuthenticator extends Authenticator {

        private final String username;

        private final String password;

        WebAuthenticator(final String username, final String password) {
            super();
            this.username = username;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }
}
