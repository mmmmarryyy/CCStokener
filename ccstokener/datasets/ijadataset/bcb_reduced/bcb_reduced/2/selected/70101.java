package org.gvsig.remoteClient.wcs;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import org.gvsig.remoteClient.utils.CapabilitiesTags;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author jaume dominguez faus
 *
 */
public class WCSProtocolHandlerFactory {

    public org.gvsig.remoteClient.wcs.WCSProtocolHandler wCSProtocolHandler;

    private static ArrayList supportedVersions = new ArrayList();

    static {
        supportedVersions.add("1.0.0");
    }

    /**
	 * M�todo que dada una respuesta de getCapabilities y un iterador sobre una
	 * coleccion de WCSClient's ordenada descendentemente devuelve el cliente
	 * cuya version es igual o inmediatamente inferior
	 *
	 * @param caps Capabilities con la respuesta del servidor
	 * @param clients Iterador de conjunto ordenado descendientemente
	 *
	 * @return cliente cuya version es igual o inmediatamente inferior
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 *
	 */
    private static String getDriverVersion(String version, Iterator clients) throws InstantiationException, IllegalAccessException {
        while (clients.hasNext()) {
            String clientVersion = (String) clients.next();
            int ret = version.compareTo(clientVersion);
            if (ret >= 0) {
                return clientVersion;
            }
        }
        return null;
    }

    /**
	 * Establece la versi�n con la que se comunicar� con el servidor y devuelve
	 * el objeto Capabilities obtenido con dicha versi�n
	 *
	 * @param host maquina con la que se negocia
	 *
	 * @return instancia de un cliente capaz de negociar con el host que se
	 *         pasa como par�metro
	 */
    public static WCSProtocolHandler negotiate(String host) throws ConnectException, IOException {
        if (supportedVersions.size() == 0) {
            return null;
        }
        try {
            String highestVersionSupportedByServer = getSuitableWCSVersion(host, "");
            if (supportedVersions.contains(highestVersionSupportedByServer)) {
                return createVersionDriver(highestVersionSupportedByServer);
            } else {
                Iterator iVersion = supportedVersions.iterator();
                String wcsVersion;
                String gvSIGVersion;
                while (iVersion.hasNext()) {
                    gvSIGVersion = (String) iVersion.next();
                    wcsVersion = getSuitableWCSVersion(host, gvSIGVersion);
                    int res = wcsVersion.compareTo(gvSIGVersion);
                    if (res == 0) {
                        return createVersionDriver(gvSIGVersion);
                    } else if (res > 0) {
                        throw new Exception("Server Version too high: " + wcsVersion);
                    } else {
                        String lowerVersion = WCSProtocolHandlerFactory.getDriverVersion(wcsVersion, iVersion);
                        if (lowerVersion == null) {
                            throw new Exception("Lowest server version is " + wcsVersion);
                        } else {
                            if (lowerVersion.equals(wcsVersion)) {
                                return createVersionDriver(lowerVersion);
                            } else {
                            }
                        }
                    }
                }
            }
            return null;
        } catch (ConnectException conEx) {
            throw conEx;
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * Sends a GetCapabilities to the WCS server to get the version
	 * if the version parameter is null, the WCS will return the highest version supported
	 * if not it will return the lower highest version than the one requested.
	 * @param host
	 * @param version
	 * @return suitable version supported by the server
	 */
    private static String getSuitableWCSVersion(String host, String _version) throws ConnectException, IOException {
        String request = WCSProtocolHandler.buildCapabilitiesSuitableVersionRequest(host, _version);
        String version = new String();
        StringReader reader = null;
        DataInputStream dis = null;
        try {
            URL url = new URL(request);
            byte[] buffer = new byte[1024];
            dis = new DataInputStream(url.openStream());
            dis.readFully(buffer);
            reader = new StringReader(new String(buffer));
            KXmlParser kxmlParser = null;
            kxmlParser = new KXmlParser();
            kxmlParser.setInput(reader);
            kxmlParser.nextTag();
            if (kxmlParser.getEventType() != KXmlParser.END_DOCUMENT) {
                if ((kxmlParser.getName().compareTo(CapabilitiesTags.WCS_CAPABILITIES_ROOT1_0_0) == 0)) {
                    version = kxmlParser.getAttributeValue("", CapabilitiesTags.VERSION);
                }
            }
            reader.close();
            dis.close();
            return version;
        } catch (ConnectException conEx) {
            throw new ConnectException(conEx.getMessage());
        } catch (IOException ioEx) {
            throw new IOException(ioEx.getMessage());
        } catch (XmlPullParserException xmlEx) {
            xmlEx.printStackTrace();
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
	 * It creates an instance of a WCSDriver class.
	 *
	 * @param String, with the version of the driver to be created
	 * @return WCSDriver.
	 */
    private static WCSProtocolHandler createVersionDriver(String version) {
        try {
            Class driver;
            version = version.replace('.', '_');
            driver = Class.forName("org.gvsig.remoteClient.wcs.wcs_" + version + ".WCSProtocolHandler" + version);
            return (WCSProtocolHandler) driver.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
