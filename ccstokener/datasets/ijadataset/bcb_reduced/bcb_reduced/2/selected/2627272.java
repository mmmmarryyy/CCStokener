package edu.uw.tcss558.team1.server;

import edu.uw.tcss558.team1.gwtclient.InterfaceType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Base class for Client to connect to Server or for Server to connect to Client.
 */
public class Connector {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(Connector.class);

    /**
     * Define where the properties file is located. This properties file will
     * contain the name of the servers and the port number for socket, rmi,
     * rest and soap web services.
     */
    private static final String SERVERS_PROPERTIES = "/servers.properties";

    /**
     * Address
     */
    private String address = null;

    /**
     * Port
     */
    private int port = 0;

    /**
     * Set the server and the port.
     * @param aServerName The server name, as defined in the properties file (server.properties)
     * @param aInterfaceType The interface type.
     */
    protected void setServerAndPort(String aServerName, InterfaceType aInterfaceType) throws IOException {
        URL url = Connector.class.getResource(SERVERS_PROPERTIES);
        if (url == null) {
            throw new NullPointerException("Problem reading " + SERVERS_PROPERTIES + " file.");
        }
        InputStream inStream = url.openStream();
        if (inStream == null) {
            throw new NullPointerException("Problem reading " + SERVERS_PROPERTIES + " file.");
        }
        Properties properties = new Properties();
        properties.load(inStream);
        String addressStr = properties.getProperty(aServerName + ".host");
        String portStr = properties.getProperty(aServerName + "." + aInterfaceType.name().toLowerCase() + ".port");
        if (addressStr == null) {
            throw new IllegalArgumentException("No host defined for " + aServerName);
        }
        if (portStr == null) {
            throw new IllegalArgumentException("No port defined for " + aServerName);
        }
        if (portStr != null) {
            portStr = portStr.trim();
            if (!portStr.matches("\\d+")) {
                throw new IllegalArgumentException("Port (" + portStr + ") is not a number for " + aServerName);
            }
        }
        address = addressStr;
        port = Integer.parseInt(portStr);
    }

    /**
     * Get the address set for this connector.
     */
    protected String getAddress() {
        return address;
    }

    /**
     * Get the port set for this connector.
     */
    protected int getPort() {
        return port;
    }
}
