package com.emental.mindraider.gnowsis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;

/**
 * Client used to connect to the running Gnowsis hub. It also implements methods
 * for sending resource URIs to the Gnowsis server using "Gnowsis button".
 * <br><br>
 * Gnowsis & MindRaider integration scenarios:
 * <ul>
 *   <li>Send Concept link to Gnowsis.
 *   <li>Browse concept in Gnowsis.
 *   <li>Access Gnowsis URIQA endpoint (not in production now) http://localhost:9998/gnowsis/uriqa?
 *   <li>Search service: http://localhost:9998/gnoJoseki/query.html
 * </ul>
 * 
 * @author Martin.Dvorak
 * @version $Revision: 1.3 $ ($Author: mindraider $)
 */
public class GnowsisClient {

    /**
	 * The host const.
	 */
    public static final String HOST = "localhost";

    /**
	 * The port const.
	 */
    public static final String PORT = "9998";

    /**
	 * The method send uri for Gnowsis.
	 */
    public static final String METHOD_LINK_URI = "gnowsis_linker.linkResource";

    /**
	 * Send resource URI to Gnowsis.
	 * 
	 * @param url
	 *            the resource url
	 */
    public static void linkResource(String url) {
        call(HOST, PORT, METHOD_LINK_URI, new String[] { url });
    }

    /**
     * The method to browse URI in Gnowsis. 
     */
    public static final String METHOD_BROWSE_URI = "gnowsis_browser.browse";

    /**
     * Browse resource.
     * 
     * @param uri   resource URI.
     */
    public static void browseResource(String uri) {
        call(HOST, PORT, METHOD_BROWSE_URI, new String[] { uri });
    }

    /**
	 * Logger for this class.
	 */
    private static final Logger cat = Logger.getLogger(GnowsisClient.class);

    /**
	 * The call method.
	 * 
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 * @param method
	 *            the method
	 * @param params
	 *            the parameters array
	 */
    public static void call(String host, String port, String method, String[] params) {
        cat.debug("call (host:" + host + " port:" + port + " method:" + method);
        try {
            String message = null;
            StringBuffer bufMessage = new StringBuffer();
            bufMessage.append("<?xml version='1.0' encoding='ISO-8859-1'?>");
            bufMessage.append("<methodCall>");
            bufMessage.append("<methodName>");
            bufMessage.append(method);
            bufMessage.append("</methodName>");
            bufMessage.append("<params>");
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    bufMessage.append("<param><value><![CDATA[" + params[i] + "]]></value></param>");
                }
            }
            bufMessage.append("</params></methodCall>");
            message = bufMessage.toString();
            bufMessage = null;
            String stringUrl = "http://" + host + ":" + port + "/RPC2";
            cat.debug("Sending message to: " + stringUrl + "\n" + message);
            URL url = new URL(stringUrl);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.getOutputStream().write(message.getBytes());
            urlConnection.getOutputStream().flush();
            urlConnection.getOutputStream().close();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                cat.debug("#server# " + line);
            }
            bufferedReader.close();
        } catch (Exception e) {
            cat.debug("Unable to send link to Gnowsis!", e);
        }
    }
}
