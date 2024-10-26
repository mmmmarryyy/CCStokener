package com.globant.google.mendoza.malbec.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ServerSocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Implements a stand alone https/https transport.
 *
 * This is a test transport, not adequate for production installations.<p>
 *
 * This implementation starts a thread to listen to the configure port and then
 * it starts a new thread per each request.<p>
 *
 */
public final class StandAlone implements Transport {

    /** The default http over ssl port.
   */
    private static final int HTTPS_PORT = 443;

    /** The read buffer size for the sending end of the transport.
   */
    private static final int READ_BUFFER_SIZE = 1024;

    /** The class logger.
  */
    private static Log log = LogFactory.getLog(StandAlone.class);

    /** The url to connect to.
   */
    private String serverURL = "https://localhost:10001";

    /** The port to listen to connections from google.
   */
    private int port = HTTPS_PORT;

    /** The ssl socket factory used to create connections as a client.
   */
    private SSLSocketFactory clientSocketFactory;

    /** The ssl socket factory used to create connections as a server.
   */
    private ServerSocketFactory serverSocketFactory;

    /** The merchant id used for authentication.
   */
    private String merchantId;

    /** The merchant key used for authentication.
   */
    private String merchantKey;

    /** The registered receiver of the GBuy requests.
   */
    private Receiver receiver = null;

    /** The mini http server implementation.
   */
    private NanoHTTPD server = null;

    /** The error message read from the server. This is only valid if ...
   */
    private String sendErrorMessage = null;

    /** Specifies if all certificates are accepted whether the truststore is
   *  specified or not.
   */
    private boolean acceptAllCertificates;

    /** Builds an instance of the stand alone transport.
   *
   * This transport listens on port 443.
   *
   * @param theClientSocketFactory The socket factory to use to create client
   * sockets. It is used to specify a trust certificate specific for this
   * trasport. If null, it uses the jvn default trust certificates.
   *
   * @param theServerSocketFactory The socket factory to use to create a
   * listening socket. It presents the specified certificate to the server.
   *
   * @param theMerchantId The merchant id to use.
   *
   * @param theMerchantKey The merchant key to use.
   *
   * @param url The server url to connect to when sending messages. This is the
   * GBuy service url.
   */
    public StandAlone(final SSLSocketFactory theClientSocketFactory, final ServerSocketFactory theServerSocketFactory, final String theMerchantId, final String theMerchantKey, final String url) {
        if (theMerchantId == null) {
            throw new IllegalArgumentException("the merchant id cannot be null");
        }
        if (theMerchantKey == null) {
            throw new IllegalArgumentException("the merchant key cannot be null");
        }
        if (url == null) {
            throw new IllegalArgumentException("the server url cannot be null");
        }
        serverURL = url;
        merchantId = theMerchantId;
        merchantKey = theMerchantKey;
        clientSocketFactory = theClientSocketFactory;
        serverSocketFactory = theServerSocketFactory;
    }

    /** Builds an instance of the stand alone transport.
   *
   * This constructor does not request the url. You must call setServerURL
   * before sending messages.
   *
   * @param theClientSocketFactory The socket factory to use to create client
   * sockets. It is used to specify a trust certificate specific for this
   * trasport. If null, it uses the jvn default trust certificates.
   *
   * @param theServerSocketFactory The socket factory to use to create a
   * listening socket. It presents the specified certificate to the server.
   *
   * @param theMerchantId The merchant id to use.
   *
   * @param theMerchantKey The merchant key to use.
   *
   * @param thePort The port to listen to connections from google.
   */
    public StandAlone(final SSLSocketFactory theClientSocketFactory, final ServerSocketFactory theServerSocketFactory, final String theMerchantId, final String theMerchantKey, final int thePort) {
        if (theMerchantId == null) {
            throw new IllegalArgumentException("the merchant id cannot be null");
        }
        if (theMerchantKey == null) {
            throw new IllegalArgumentException("the merchant key cannot be null");
        }
        serverURL = null;
        merchantId = theMerchantId;
        merchantKey = theMerchantKey;
        clientSocketFactory = theClientSocketFactory;
        serverSocketFactory = theServerSocketFactory;
        port = thePort;
    }

    /** Builds an instance of the stand alone transport.
  *
  * @param theClientSocketFactory The socket factory to use to create client
  * sockets. It is used to specify a trust certificate specific for this
  * trasport. If null, it uses the jvn default trust certificates.
  *
  * @param theServerSocketFactory The socket factory to use to create a
  * listening socket. It presents the specified certificate to the server.
  *
  * @param theMerchantId The merchant id to use.
  *
  * @param theMerchantKey The merchant key to use.
  *
  * @param url The server url to connect to when sending messages. This is the
  * GBuy service url.
  *
  * @param thePort The port to listen to connections from google.
  *
  * @param isAcceptAllCertificates Indicates if all the certificates must be
  *  accepted.
  */
    public StandAlone(final SSLSocketFactory theClientSocketFactory, final ServerSocketFactory theServerSocketFactory, final String theMerchantId, final String theMerchantKey, final String url, final int thePort, final boolean isAcceptAllCertificates) {
        this(theClientSocketFactory, theServerSocketFactory, theMerchantId, theMerchantKey, url);
        port = thePort;
        acceptAllCertificates = isAcceptAllCertificates;
    }

    /** Gets the port that the server is listening on.
   *
   * Only call this function after start.
   *
   * @return Returns the port the server is listening on, usually the one set
   * with setPort, except when setPort is called with 0, in wich case it
   * returns the effective port used by the server.
   */
    public int getPort() {
        if (server == null) {
            throw new IllegalStateException("You must start the transport first");
        }
        return server.getPort();
    }

    /** Sets the url to use to send messages to GBuy.
   *
   * @param url The url. Cannot be null.
   */
    public void setServerURL(final String url) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        serverURL = url;
    }

    /** Sends a message to the server through an ssl connection.
   *
   * @param message The message to send to the server. It cannot be null.
   *
   * @return Returns the server response, usually an acknowledge or an error
   * related to the validity of the message structure.
   */
    public String send(final String message) {
        log.trace("Entering send");
        HttpURLConnection connection = getConnection("application/xml");
        String response = null;
        log.debug("Writing message:\n" + message);
        try {
            writeRequest(connection, message);
            response = readResponse(connection);
        } finally {
            connection.disconnect();
        }
        if (response == null) {
            throw new RuntimeException(sendErrorMessage);
        }
        log.debug("Received response:\n" + response);
        log.trace("Leaving send");
        return response;
    }

    /** Registers a listener that receives messages from GBuy.
   *
   * For every message it receives, it calls receiver.receive. Implementors of
   * receive must return the message that wants to be sent to GBuy.
   *
   * The trasport starts to listen to server connections only when it has a
   * registered receiver.
   *
   * @param theReceiver The listener. It cannot be null.
   */
    public void registerReceiver(final Receiver theReceiver) {
        log.trace("Entering registerReceiver");
        if (theReceiver == null) {
            throw new IllegalArgumentException("receiver cannot be null");
        }
        receiver = theReceiver;
        log.trace("Leaving registerReceiver");
    }

    /** Starts the server.
   *
   * You must have registered a receiver before calling start.
   */
    public void start() {
        log.trace("Entering start");
        if (receiver == null) {
            throw new IllegalStateException("Must register a receiver before" + " starting the server");
        }
        server = new NanoHTTPD(port, receiver, serverSocketFactory, merchantId, merchantKey);
        log.trace("Leaving start");
    }

    /** Stops the server.
   *
   * You must have called start before calling this function.
   */
    public void stop() {
        if (server == null) {
            throw new RuntimeException("You must call start before stopping the" + " transport");
        }
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /** Gets a properly initialized connection to the server.
   *
   * @param contentType The content type that will be sent to the server. Can
   * be null, in which case no content type header is sent.
   *
   * @return Returns the connection. This function never returns null.
   */
    private HttpURLConnection getConnection(final String contentType) {
        log.trace("Entering getConnection");
        if (serverURL == null) {
            throw new IllegalStateException("Must set the server url first");
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverURL);
            connection = (HttpURLConnection) url.openConnection();
            String encoding = new String(Base64.encodeBase64((merchantId + ":" + merchantKey).getBytes()));
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            if (acceptAllCertificates) {
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
                    sslConnection.setHostnameVerifier(new HostnameVerifier() {

                        public boolean verify(final String hostName, final SSLSession session) {
                            return true;
                        }
                    });
                }
            } else {
                if (clientSocketFactory != null) {
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
                        sslConnection.setSSLSocketFactory(clientSocketFactory);
                    }
                }
            }
            connection.setDoOutput(true);
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to server", e);
        }
        log.trace("Leaving getConnection");
        if (connection == null) {
            throw new RuntimeException("Attempted to return a null connection");
        }
        return connection;
    }

    /** Reads the data from the connection and returns it as a string.
   *
   * @param connection The connection to read the data from.
   *
   * @return Returns the string with the data read from the connection. It is
   * null if an error happens. Read the error message with getError().
   */
    private String readResponse(final HttpURLConnection connection) {
        log.trace("Entering readResponse");
        BufferedReader in = null;
        String response = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            if (in != null) {
                sendErrorMessage = readResponse(in);
            }
            log.trace("Leaving readResponse with null");
            return null;
        }
        try {
            response = readResponse(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error closing connection", e);
                }
            }
        }
        log.trace("Leaving readResponse");
        return response;
    }

    /** Reads the data from the stream realted to a connection and returns it as
   * a string.
   *
   * @param in The reader to read the data from.
   *
   * @return Returns the string with the data read from the connection.
   */
    private String readResponse(final BufferedReader in) {
        StringBuffer response = new StringBuffer();
        try {
            char[] line = new char[READ_BUFFER_SIZE];
            int read = 0;
            while ((read = in.read(line)) != -1) {
                response.append(line, 0, read);
            }
        } catch (IOException e) {
            log.error("Error reading data from the server", e);
            throw new RuntimeException("Unable to read response", e);
        }
        return response.toString();
    }

    /** Sends a request to the server.
   *
   * @param connection The connection to the server.
   *
   * @param message The message to send to the server.
   */
    private void writeRequest(final HttpURLConnection connection, final String message) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(connection.getOutputStream());
            out.write(message);
            out.flush();
        } catch (Exception e) {
            log.error("Error sending data to the client", e);
            throw new RuntimeException("Unable to send message", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
