package avoware.intchat.client.transport;

import avoware.intchat.client.api.Response;
import avoware.intchat.client.IntChatMainFrame;
import avoware.intchat.client.api.AsyncRequest;
import avoware.intchat.client.api.Cancelable;
import avoware.intchat.client.api.RequestEntity;
import avoware.intchat.client.api.ResponseEntity;
import avoware.intchat.client.api.concurrent.SwingWorker;
import avoware.intchat.shared.IntChatConstants;
import avoware.intchat.client.api.ResponseHandler;
import avoware.intchat.client.api.concurrent.ProgressEventListener;
import avoware.intchat.client.prefs.LocalSettings;
import avoware.intchat.client.api.Server;
import avoware.ssl.HostnameVerifierImpl;
import avoware.ssl.TrustManagerImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 *
 * @author Andrew Orlov
 */
public class HttpClient {

    /**
     * This parameter shows if http client connected to server successfully
     * (i.e. test request succeded).
     */
    private static volatile boolean _connected;

    private static Server _serverSelected = null;

    private static int _defaultConnectTimeout = 0;

    private static int _defaultReadTimeout = 0;

    private static int _reconnectTimeout = 0;

    private static String _sessionId = null;

    private static ArrayList<HttpURLConnection> _openedConnections = null;

    /**
     * Keystore file to save certificates
     */
    private static final File KEYSTORE_FILE = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".intchat" + System.getProperty("file.separator") + "certs.jks");

    /**
     * Password to open keystore
     */
    private static final char[] KEYSTORE_PWD = { 'c', 'h', 'a', 'n', 'g', 'e', 'i', 't' };

    /**
     * Create new instance of HttpClient, clearing all the
     * previous settings.
     * @param serverSelected
     * @param loginResponseHandler
     */
    public static void initialize(Server serverSelected, ResponseHandler loginResponseHandler) {
        _serverSelected = serverSelected;
        setDefaultConnectTimeout(LocalSettings.getConnectionTimeout() * 1000);
        setDefaultReadTimeout(LocalSettings.getReadTimeout() * 1000);
        _reconnectTimeout = LocalSettings.getReconnectTimeout() * 1000;
        _openedConnections = new ArrayList<HttpURLConnection>();
        CookieHandler.setDefault(null);
        java.security.Security.setProperty("networkaddress.cache.ttl", Integer.toString(LocalSettings.getDNSCacheTTL()));
        System.setProperty("http.maxConnections", Integer.toString(LocalSettings.getMaxConnectionsPerHost()));
        System.setProperty("http.maxRedirects", Integer.toString(Integer.MAX_VALUE));
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[] { new TrustManagerImpl(KEYSTORE_FILE, KEYSTORE_PWD) }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifierImpl());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (serverSelected.getUseProxy()) {
            if (serverSelected.getProxyType().equals(Proxy.Type.HTTP)) {
                System.setProperty("http.proxyHost", serverSelected.getProxyHost());
                System.setProperty("http.proxyPort", Integer.toString(serverSelected.getProxyPort()));
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
            } else if (serverSelected.getProxyType().equals(Proxy.Type.SOCKS)) {
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.setProperty("socksProxyHost", serverSelected.getProxyHost());
                System.setProperty("socksProxyPort", Integer.toString(serverSelected.getProxyPort()));
            } else {
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
            }
            if (serverSelected.getUseProxyAuth()) {
                class ICAuthenticator extends Authenticator {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(_serverSelected.getProxyLogin(), _serverSelected.getProxyPassword().toCharArray());
                    }

                    @Override
                    protected Authenticator.RequestorType getRequestorType() {
                        return Authenticator.RequestorType.PROXY;
                    }
                }
                Authenticator.setDefault(new ICAuthenticator());
            } else {
                Authenticator.setDefault(null);
            }
        } else {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
        }
        setConnected(true);
        new LoginRequest(loginResponseHandler, 0).execute();
    }

    /**
     * Shuts down all the opened connections
     */
    public static void shutdown() {
        setConnected(false);
        for (int i = 0; i < _openedConnections.size(); i++) {
            _openedConnections.get(i).disconnect();
        }
        _openedConnections = null;
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
    }

    /**
     * @return Client session timeout on server (sent by Internal Chat Server)
     */
    public static int getDefaultReadTimeout() {
        return _defaultReadTimeout;
    }

    /**
     * Set the client session timeout
     * @param value Client session timeout in milliseconds
     */
    public static void setDefaultReadTimeout(int value) {
        _defaultReadTimeout = value;
        System.setProperty("sun.net.client.defaultReadTimeout", Integer.toString(_defaultReadTimeout));
    }

    /**
     * Set the _connected status of client
     * @param connected
     */
    private static void setConnected(boolean connected) {
        _connected = connected;
    }

    /**
     * 
     * @return _connected status of client
     */
    public static boolean isConnected() {
        return _connected;
    }

    public static int getDefaultConnectTimeout() {
        return _defaultConnectTimeout;
    }

    public static void setDefaultConnectTimeout(int value) {
        _defaultConnectTimeout = value;
        System.setProperty("sun.net.client.defaultConnectTimeout", Integer.toString(_defaultConnectTimeout));
    }

    private static void dropContent(InputStream in) {
        try {
            if (in != null) {
                while (in.read() != -1) {
                }
                in.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static HttpURLConnection getConnection(String servlet, HashMap<String, String> queryParameters) throws MalformedURLException, IOException {
        URL url = getURL(servlet, queryParameters);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(getDefaultConnectTimeout());
        conn.setReadTimeout(getDefaultReadTimeout());
        _openedConnections.add(conn);
        return conn;
    }

    public static URL getURL(String servlet, HashMap<String, String> queryParameters) throws MalformedURLException, UnsupportedEncodingException {
        StringBuffer uri = new StringBuffer(_serverSelected.getURIString());
        if (servlet != null && servlet.trim().length() > 0) {
            if (!servlet.startsWith("/")) uri.append("/");
            uri.append(servlet);
        }
        if (_sessionId != null) uri.append(_sessionId);
        if (queryParameters != null) {
            uri.append("?");
            Iterator<Map.Entry<String, String>> iter = queryParameters.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                String key = entry.getKey();
                if (key != null && key.length() > 0) {
                    uri.append(URLEncoder.encode(key, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(entry.getValue(), IntChatConstants.ENCODING) + "&");
                }
            }
            uri.deleteCharAt(uri.length() - 1);
        }
        return new URL(uri.toString());
    }

    public static Cancelable makeRequest(final String servlet, final HashMap<String, String> queryParameters, final int readTimeout, final RequestEntity requestEntity, final ResponseEntity responseEntity, final ResponseHandler responseHandler, ProgressEventListener progressListener) {
        if (!isConnected()) return null;
        AsyncRequest ar = null;
        try {
            final HttpURLConnection conn = getConnection(servlet, queryParameters);
            if (readTimeout > 0) conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("Accept-Encoding", LocalSettings.getAcceptEncodingData());
            conn.setRequestProperty("Content-Encoding", LocalSettings.getContentEncodingData());
            if (requestEntity != null) {
                String contentType = requestEntity.getContentType();
                if (contentType != null && contentType.length() > 0) conn.setRequestProperty("Content-Type", contentType);
                HashMap<String, String> extraHeaders = requestEntity.getExtraHeaders();
                if (extraHeaders != null) {
                    Iterator<Map.Entry<String, String>> iter = extraHeaders.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<String, String> entry = iter.next();
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            ar = new AsyncRequest(conn, requestEntity, responseEntity) {

                @Override
                protected void done() {
                    if (_openedConnections != null) _openedConnections.remove(conn);
                    Response response = get();
                    if (responseHandler != null) responseHandler.handleResponse(requestEntity, response);
                }
            };
            if (progressListener != null) ar.getProgressMonitor().addProgressEventListener(progressListener);
            ar.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ar;
    }

    private static class LoginRequest extends SwingWorker<Response> {

        ResponseHandler _responseHandler = null;

        int _reconnectTimeout = 0;

        LoginRequest(ResponseHandler responseHandler, int reconnectTimeout) {
            _responseHandler = responseHandler;
            _reconnectTimeout = reconnectTimeout;
        }

        private void _testForSSL() {
            HttpURLConnection conn = null;
            try {
                _serverSelected.setScheme(Server.Scheme.HTTP);
                URL url = new URL(_serverSelected.getURIString());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                int responseCode = conn.getResponseCode();
                if (responseCode > 0 && responseCode < 400) dropContent(conn.getInputStream()); else if (responseCode >= 400) dropContent(conn.getErrorStream()); else throw new Exception("Wrong HTTP response code");
            } catch (Exception ex) {
                if (conn != null) conn.disconnect();
                _serverSelected.setScheme(Server.Scheme.HTTPS);
            }
        }

        @Override
        protected Response doInBackground() {
            try {
                sleep(_reconnectTimeout);
            } catch (InterruptedException ex) {
            }
            _testForSSL();
            HttpURLConnection conn = null;
            Response response = new Response(null);
            try {
                Socket sock;
                if (_serverSelected.getUseProxy()) {
                    sock = new Socket(_serverSelected.getProxyHost(), _serverSelected.getProxyPort());
                } else {
                    sock = new Socket(_serverSelected.getHost(), _serverSelected.getPort());
                }
                InetAddress ia = sock.getLocalAddress();
                String data = URLEncoder.encode(IntChatConstants.AuthFields.LOGIN, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(_serverSelected.getLogin(), IntChatConstants.ENCODING) + "&" + URLEncoder.encode(IntChatConstants.AuthFields.PASSWORD, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(_serverSelected.getPassword(), IntChatConstants.ENCODING) + "&" + URLEncoder.encode(IntChatConstants.AuthFields.IC_CLIENTVERSION, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(IntChatMainFrame.MAJOR_VERSION + "." + IntChatMainFrame.MINOR_VERSION + "." + IntChatMainFrame.BUILD_NUMBER, IntChatConstants.ENCODING) + "&" + URLEncoder.encode(IntChatConstants.AuthFields.IC_HOSTADDRESS, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(ia.getHostAddress(), IntChatConstants.ENCODING) + "&" + URLEncoder.encode(IntChatConstants.AuthFields.IC_HOSTNAME, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(ia.getHostName(), IntChatConstants.ENCODING) + "&" + URLEncoder.encode(IntChatConstants.AuthFields.IC_DNSNAME, IntChatConstants.ENCODING) + "=" + URLEncoder.encode(ia.getCanonicalHostName(), IntChatConstants.ENCODING);
                sock.close();
                URL url = new URL(_serverSelected.getURIString() + "/Login");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + IntChatConstants.ENCODING);
                conn.setRequestProperty("Accept-Encoding", LocalSettings.getAcceptEncodingData());
                conn.setInstanceFollowRedirects(false);
                conn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), IntChatConstants.ENCODING);
                writer.write(data);
                writer.flush();
                int responseCode = conn.getResponseCode();
                response.setResponseCode(responseCode);
                response.setResponseMessage(conn.getResponseMessage());
                if (responseCode > 0 && responseCode < 400) dropContent(conn.getInputStream()); else if (responseCode >= 400) dropContent(conn.getErrorStream()); else throw new Exception("Wrong HTTP response code");
                if (responseCode >= 300 && responseCode <= 399) {
                    String path = conn.getHeaderField("location");
                    int semicolon = path.lastIndexOf(';');
                    int question = path.indexOf('?');
                    if (semicolon > -1) _sessionId = path.substring(semicolon, question > -1 ? question : path.length());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) try {
                    conn.getInputStream().close();
                } catch (IOException ioe) {
                }
            }
            return response;
        }

        @Override
        protected void done() {
            if (isConnected()) {
                Response response = get();
                int responseCode = response.getResponseCode();
                if (responseCode < 0) {
                    new LoginRequest(_responseHandler, HttpClient._reconnectTimeout).execute();
                } else {
                    if (_responseHandler != null) _responseHandler.handleResponse(null, response);
                }
            }
        }
    }
}
