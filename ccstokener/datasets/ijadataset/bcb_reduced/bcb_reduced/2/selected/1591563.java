package org.jivesoftware.smack;

import junit.framework.TestCase;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import javax.net.SocketFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Base class for all the test cases which provides a pre-configured execution context. This
 * means that any test case that subclassifies this base class will have access to a pool of
 * connections and to the user of each connection. The maximum number of connections in the pool
 * can be controlled by the message {@link #getMaxConnections()} which every subclass must
 * implement.<p>
 *
 * This base class defines a default execution context (i.e. host, port, chat domain and muc
 * domain) which can be found in the file "config/test-case.xml". However, each subclass could
 * redefine the default configuration by providing its own configuration file (if desired). The
 * name of the configuration file must be of the form <test class name>.xml (e.g. RosterTest.xml).
 * The file must be placed in the folder "config". This folder is where the default configuration
 * file is being held.
 *
 * @author Gaston Dombiak
 */
public abstract class SmackTestCase extends TestCase {

    private String host = "localhost";

    private String serviceName = "localhost";

    private int port = 5222;

    private String usernamnePrefix = "user";

    private String chatDomain = "chat";

    private String mucDomain = "conference";

    private XMPPConnection[] connections = null;

    /**
     * Constructor for SmackTestCase.
     * @param arg0 arg for SmackTestCase
     */
    public SmackTestCase(String arg0) {
        super(arg0);
    }

    /**
     * Returns the maximum number of connections to initialize for this test case. All the
     * initialized connections will be connected to the server using a new test account for
     * each conection.
     *
     * @return the maximum number of connections to initialize for this test case.
     */
    protected abstract int getMaxConnections();

    /**
     * Returns a SocketFactory that will be used to create the socket to the XMPP server. By
     * default no SocketFactory is used but subclasses my want to redefine this method.<p>
     *
     * A custom SocketFactory allows fine-grained control of the actual connection to the XMPP
     * server. A typical use for a custom SocketFactory is when connecting through a SOCKS proxy.
     *
     * @return a SocketFactory that will be used to create the socket to the XMPP server.
     */
    protected SocketFactory getSocketFactory() {
        return null;
    }

    /**
     * Returns the XMPPConnection located at the requested position. Each test case holds a
     * pool of connections which is initialized while setting up the test case. The maximum
     * number of connections is controlled by the message {@link #getMaxConnections()} which
     * every subclass must implement.<p>
     *
     * If the requested position is greater than the connections size then an
     * IllegalArgumentException will be thrown.
     *
     * @param index the position in the pool of the connection to look for.
     * @return the XMPPConnection located at the requested position.
     */
    protected XMPPConnection getConnection(int index) {
        if (index > getMaxConnections()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return connections[index];
    }

    /**
     * Creates a new XMPPConnection using the connection preferences. This is useful when
     * not using a connection from the connection pool in a test case.
     *
     * @return a new XMPP connection.
     */
    protected XMPPConnection createConnection() {
        ConnectionConfiguration config = new ConnectionConfiguration(host, port);
        config.setCompressionEnabled(Boolean.getBoolean("test.compressionEnabled"));
        if (getSocketFactory() == null) {
            config.setSocketFactory(getSocketFactory());
        }
        return new XMPPConnection(config);
    }

    /**
     * Returns the name of the user (e.g. johndoe) that is using the connection
     * located at the requested position.
     *
     * @param index the position in the pool of the connection to look for.
     * @return the user of the user (e.g. johndoe).
     */
    protected String getUsername(int index) {
        if (index > getMaxConnections()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return usernamnePrefix + index;
    }

    /**
     * Returns the bare XMPP address of the user (e.g. johndoe@jabber.org) that is
     * using the connection located at the requested position.
     *
     * @param index the position in the pool of the connection to look for.
     * @return the bare XMPP address of the user (e.g. johndoe@jabber.org).
     */
    protected String getBareJID(int index) {
        return getUsername(index) + "@" + getConnection(index).getServiceName();
    }

    /**
     * Returns the full XMPP address of the user (e.g. johndoe@jabber.org/Smack) that is
     * using the connection located at the requested position.
     *
     * @param index the position in the pool of the connection to look for.
     * @return the full XMPP address of the user (e.g. johndoe@jabber.org/Smack).
     */
    protected String getFullJID(int index) {
        return getBareJID(index) + "/Smack";
    }

    protected String getHost() {
        return host;
    }

    protected int getPort() {
        return port;
    }

    protected String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the default groupchat service domain.
     *
     * @return the default groupchat service domain.
     */
    protected String getChatDomain() {
        return chatDomain;
    }

    /**
     * Returns the default MUC service domain.
     *
     * @return the default MUC service domain.
     */
    protected String getMUCDomain() {
        return mucDomain + "." + serviceName;
    }

    protected void setUp() throws Exception {
        super.setUp();
        init();
        if (getMaxConnections() < 1) {
            return;
        }
        connections = new XMPPConnection[getMaxConnections()];
        try {
            for (int i = 0; i < getMaxConnections(); i++) {
                connections[i] = createConnection();
                connections[i].connect();
            }
            host = connections[0].getHost();
            serviceName = connections[0].getServiceName();
            if (!getConnection(0).getAccountManager().supportsAccountCreation()) fail("Server does not support account creation");
            for (int i = 0; i < getMaxConnections(); i++) {
                try {
                    getConnection(i).getAccountManager().createAccount(usernamnePrefix + i, usernamnePrefix + i);
                } catch (XMPPException e) {
                    if (e.getXMPPError() == null || e.getXMPPError().getCode() != 409) {
                        throw e;
                    }
                }
                getConnection(i).login(usernamnePrefix + i, usernamnePrefix + i, "Smack", sendInitialPresence());
            }
            Thread.sleep(150);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < getMaxConnections(); i++) {
            try {
                if (!getConnection(i).isConnected()) {
                    XMPPConnection con = getConnection(i);
                    con.connect();
                    con.login(getUsername(i), getUsername(i));
                } else if (!getConnection(i).isAuthenticated()) {
                    getConnection(i).login(getUsername(i), getUsername(i));
                }
                getConnection(i).getAccountManager().deleteAccount();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (getConnection(i).isConnected()) {
                getConnection(i).disconnect();
            }
        }
    }

    protected boolean sendInitialPresence() {
        return true;
    }

    /**
     * Initializes the context of the test case. We will first try to load the configuration from
     * a file whose name is conformed by the test case class name plus an .xml extension
     * (e.g RosterTest.xml). If no file was found under that name then we will try to load the
     * default configuration for all the test cases from the file "config/test-case.xml".
     *
     */
    private void init() {
        try {
            boolean found = false;
            Enumeration resources = ClassLoader.getSystemClassLoader().getResources(getConfigurationFilename());
            while (resources.hasMoreElements()) {
                found = parseURL((URL) resources.nextElement());
            }
            if (!found) {
                resources = ClassLoader.getSystemClassLoader().getResources("config/test-case.xml");
                while (resources.hasMoreElements()) {
                    found = parseURL((URL) resources.nextElement());
                }
            }
            if (!found) {
                System.err.println("File config/test-case.xml not found. Using default config.");
            }
        } catch (Exception e) {
        }
    }

    /**
     * Returns true if the given URL was found and parsed without problems. The file provided
     * by the URL must contain information useful for the test case configuration, such us,
     * host and port of the server.
     *
     * @param url the url of the file to parse.
     * @return true if the given URL was found and parsed without problems.
     */
    private boolean parseURL(URL url) {
        boolean parsedOK = false;
        InputStream systemStream = null;
        try {
            systemStream = url.openStream();
            XmlPullParser parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(systemStream, "UTF-8");
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("host")) {
                        host = parser.nextText();
                    } else if (parser.getName().equals("port")) {
                        port = parseIntProperty(parser, port);
                    } else if (parser.getName().equals("serviceName")) {
                        serviceName = parser.nextText();
                    } else if (parser.getName().equals("chat")) {
                        chatDomain = parser.nextText();
                    } else if (parser.getName().equals("muc")) {
                        mucDomain = parser.nextText();
                    } else if (parser.getName().equals("username")) {
                        usernamnePrefix = parser.nextText();
                    }
                }
                eventType = parser.next();
            } while (eventType != XmlPullParser.END_DOCUMENT);
            parsedOK = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                systemStream.close();
            } catch (Exception e) {
            }
        }
        return parsedOK;
    }

    private static int parseIntProperty(XmlPullParser parser, int defaultValue) throws Exception {
        try {
            return Integer.parseInt(parser.nextText());
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * Returns the name of the configuration file related to <b>this</b> test case. By default all
     * the test cases will use the same configuration file. However, it's possible to override the
     * default configuration by providing a file of the form <test case class name>.xml
     * (e.g. RosterTest.xml).
     *
     * @return the name of the configuration file related to this test case.
     */
    private String getConfigurationFilename() {
        String fullClassName = this.getClass().getName();
        int firstChar = fullClassName.lastIndexOf('.') + 1;
        return "config/" + fullClassName.substring(firstChar) + ".xml";
    }

    /**
     * Compares two contents of two byte arrays to make sure that they are equal
     *
     * @param message The message to show in the case of failure
     * @param byteArray1 The first byte array.
     * @param byteArray2 The second byte array.
     */
    public static void assertEquals(String message, byte[] byteArray1, byte[] byteArray2) {
        if (byteArray1.length != byteArray2.length) {
            fail(message);
        }
        for (int i = 0; i < byteArray1.length; i++) {
            assertEquals(message, byteArray1[i], byteArray2[i]);
        }
    }
}
