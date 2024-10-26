package net.pesahov.remote.socket.rmi.http.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import net.pesahov.common.utils.decoders.Decoder;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class HttpClientUnderlyingConnection {

    /**
     * Service "connect" path.
     */
    static final String CONNECT = "connect";

    /**
     * Service "service" path.
     */
    static final String SERVICE = "service";

    /**
     * Service "serviceAccept" path.
     */
    static final String SERVICE_ACCEPT = "serviceAccept";

    /**
     * Service "serviceRead" path.
     */
    static final String SERVICE_READ = "serviceRead";

    /**
     * Service "serviceWrite" path.
     */
    static final String SERVICE_WRITE = "serviceWrite";

    /**
     * Service "disconnect" path.
     */
    static final String DISCONNECT = "disconnect";

    /**
     * Hidden constructor.
     */
    private HttpClientUnderlyingConnection() {
    }

    /**
     * Service the current user request to server.
     * @param session {@link SessionCookies} instance used to identify the user.
     * @param serviceURL {@link URL} URL of service.
     * @param service Service type. (CONNECT, SERVICE, DISCONNECT).
     * @param request Byte array of request (used only for SERVICE service type).
     * @param decoder {@link Decoder} instance.
     * @param proxy HTTP {@link Proxy} instance.
     * @return Byte array of response (used only for SERVICE service type).
     * @throws IOException if an I/O exception occurs.
     */
    public static byte[] service(SessionCookies session, URL serviceURL, String service, byte[] request, Decoder decoder, Proxy proxy) throws IOException {
        URLConnection urlConnection = new URL(serviceURL.toString() + service).openConnection(proxy);
        if (!(urlConnection instanceof HttpURLConnection)) throw new ProtocolException("Not a HTTP service url.");
        byte[] response = null;
        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        try {
            if (service.equals(CONNECT)) response = connect(connection); else if (service.startsWith(SERVICE)) response = service(connection, session, request, decoder); else if (service.equals(DISCONNECT)) disconnect(connection, session); else throw new IOException("Service request is invalid!");
        } finally {
            if (connection != null) connection.disconnect();
        }
        return response;
    }

    /**
     * Connects to server as a guest user to sign in.
     * @param connection {@link HttpURLConnection} instance to use for sign in.
     * @return Byte array of created user session instance.
     * @throws IOException if an I/O exception occurs.
     */
    private static byte[] connect(HttpURLConnection connection) throws IOException {
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setUseCaches(false);
        System.out.println("Connecting to: " + connection.getURL());
        connection.connect();
        String cookiesHeader = connection.getHeaderField(SessionCookies.HTTP_HEADER_SET_COOKIE);
        SessionCookies session = SessionCookies.parseCookies(cookiesHeader);
        System.out.print(new Date().toString() + " Cookies: ");
        System.out.println(cookiesHeader);
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(response);
        out.writeUnshared(session);
        out.flush();
        return response.toByteArray();
    }

    /**
     * Service by server given user request for given user session.
     * @param connection {@link HttpURLConnection} instance to use for service.
     * @param session {@link SessionCookies} instance used to identify the user.
     * @param request Byte array of request.
     * @return Byte array of response.
     * @throws IOException if an I/O exception occurs.
     */
    private static byte[] service(HttpURLConnection connection, SessionCookies session, byte[] request, Decoder decoder) throws IOException {
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty(SessionCookies.HTTP_HEADER_COKIE, SessionCookies.buildCookies(session));
        connection.setUseCaches(false);
        System.out.println(new Date().toString() + " Servicing by: " + connection.getURL() + " Request length: " + request.length);
        connection.setRequestProperty("Content-Length", Integer.toString(request.length));
        OutputStream out = connection.getOutputStream();
        decoder.encode(new ByteArrayInputStream(request), out);
        out.flush();
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        InputStream in = connection.getInputStream();
        decoder.decode(in, response);
        return response.toByteArray();
    }

    /**
     * Disconnects from server with given user session instance.
     * @param connection {@link HttpURLConnection} instance to use for sign in.
     * @param session {@link SessionCookies} instance used to identify the user.
     * @throws IOException if an I/O exception occurs.
     */
    private static void disconnect(HttpURLConnection connection, SessionCookies session) throws IOException {
        connection.setDoOutput(true);
        connection.setDoInput(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setRequestProperty(SessionCookies.HTTP_HEADER_COKIE, SessionCookies.buildCookies(session));
        connection.setUseCaches(false);
        System.out.println("Disconnecting from: " + connection.getURL());
        connection.connect();
    }
}
