package uk.org.ogsadai.client.toolkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import uk.org.ogsadai.client.toolkit.exception.ClientToolkitException;
import uk.org.ogsadai.client.toolkit.exception.DAIClassNotFoundException;
import uk.org.ogsadai.client.toolkit.exception.PresentationLayerNotSupportedException;
import uk.org.ogsadai.client.toolkit.exception.ServerFactoryInitialisationException;
import uk.org.ogsadai.client.toolkit.exception.ServerInitialisationException;
import uk.org.ogsadai.client.toolkit.exception.ServerURLInvalidException;

/**
 * A utility class for providing OGSA-DAI server proxy classes based
 * upon WSDL interrogation and CTk configuration files specifying
 * platform-specific server proxies available.
 *
 * @author The OGSA-DAI Project Team.
 */
public class ServerFactory {

    /** Copyright statement. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007";

    /** 
     * The <tt>wsrf.string</tt> constant in the
     * <tt>config.properties</tt> file used by this class whose value
     * will contain a string identifying an OGSA-DAI presentation
     * layer (as found in OGSA-DAI presentation layer-specific WSDL) 
     * to server proxy class names. 
     */
    private static final String WSRF_STRING = "wsrf.string";

    /** 
     * The <tt>server.class</tt> constant in the
     * <tt>config.properties</tt> file used by this class whose
     * value will be a server proxy class name.
     */
    private static final String SERVER_CLASS = "server.class";

    /** * <tt>config.properties</tt>. file used by this class. */
    private static final String PROPERTIES_FILE_NAME = "/uk/org/ogsadai/client/toolkit/presentation/config.properties";

    /** 
     * A mapping from strings which denote OGSA-DAI presentation
     * layers (as found in OGSA-DAI presentation layer-specific WSDL)
     * to server proxy class names. 
     */
    private static Map mServerMap;

    /**
     * Get a <tt>Server</tt> proxy for the given service.
     * <p>
     * Gets the WSDL of the service with the given URL, parses it and
     * sees if there is a mapping from a presentation layer ID to a
     * <tt>Server</tt> proxy class in the CTk configuration file. If
     * so then return an instance of that class.
     * 
     * @param url
     *     Service URL.
     * @return server proxy corresponding to the URL..
     * @throws ClientToolkitException
     *     If there is an internal client toolkit error.
     */
    public static Server getServer(final URL url) throws ClientToolkitException {
        final URL urlWSDL;
        try {
            urlWSDL = new URL(url.toExternalForm() + "?wsdl");
        } catch (MalformedURLException e) {
            throw new ServerInitialisationException(e);
        }
        String wsdl = getWSDL(urlWSDL);
        Map serverMap = getServerMap();
        Iterator serverIDs = serverMap.keySet().iterator();
        while (serverIDs.hasNext()) {
            String serverID = (String) serverIDs.next();
            if (wsdl.indexOf(serverID) >= 0) {
                String serverClassName = (String) serverMap.get(serverID);
                try {
                    Class serverClass = Class.forName(serverClassName);
                    Constructor constructor = serverClass.getConstructor(new Class[] {});
                    Server server = (Server) constructor.newInstance(new Object[] {});
                    return server;
                } catch (ClassNotFoundException e) {
                    DAIClassNotFoundException exception = new DAIClassNotFoundException(serverClassName);
                    throw exception;
                } catch (Throwable e) {
                    throw new ServerInitialisationException(e);
                }
            }
        }
        throw new PresentationLayerNotSupportedException();
    }

    /**
     * Read the <tt>ServerFactory</tt> configuration file and
     * build a map from server IDs to class names.
     * 
     * @return map from server ID strings to class names.
     * @throws ServerFactoryInitialisationException
     *     If there is a problem reading the <tt>ServerFactory</tt>
     *     configuration file.
     */
    private static synchronized Map getServerMap() throws ServerFactoryInitialisationException {
        if (mServerMap != null) {
            return mServerMap;
        }
        mServerMap = new HashMap();
        try {
            InputStream inputStream = ServerFactory.class.getResourceAsStream(PROPERTIES_FILE_NAME);
            Properties properties = new Properties();
            properties.load(inputStream);
            Enumeration keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (key.endsWith(WSRF_STRING)) {
                    String wsrfString = properties.getProperty(key);
                    String prezName = key.substring(0, key.length() - WSRF_STRING.length());
                    String serverClassKey = prezName + SERVER_CLASS;
                    String serverClass = properties.getProperty(serverClassKey);
                    mServerMap.put(wsrfString, serverClass);
                }
            }
            return mServerMap;
        } catch (Throwable e) {
            throw new ServerFactoryInitialisationException(PROPERTIES_FILE_NAME, e);
        }
    }

    /**
     * Reads the WSDL of a service at the given URL and return a
     * string which can identify the service type.
     * 
     * @param url
     *     URL of OGSA-DAI service.
     * @return WSDL document as a string.
     * @throws ClientToolkitException
     *     If there is an internal client toolkit error.
     */
    private static String getWSDL(final URL url) throws ClientToolkitException {
        try {
            InputStreamReader input = new InputStreamReader(url.openStream());
            StringBuffer buf = new StringBuffer();
            char[] charArray = new char[1024];
            int readBytes;
            while ((readBytes = input.read(charArray)) >= 0) {
                buf.append(charArray, 0, readBytes);
            }
            String wsdl = buf.toString();
            return wsdl;
        } catch (IOException e) {
            ServerURLInvalidException exception = new ServerURLInvalidException(url, e);
            throw exception;
        }
    }
}
