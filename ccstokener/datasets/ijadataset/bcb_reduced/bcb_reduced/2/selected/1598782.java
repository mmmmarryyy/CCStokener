package org.opencarto.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author julien Gaffuri
 *
 */
public class GMapElevationWebService {

    private static final Logger logger = Logger.getLogger(GMapElevationWebService.class.getName());

    /**
	 * The value returned if the service failed
	 */
    public static final double NO_VALUE_RETURNED = -999.999;

    /**
	 * The quota of point number per query (21 july 2010 is 2500)
	 * NB: "this limit may be changed in the future without notice"
	 * (http://code.google.com/intl/fr/apis/maps/documentation/elevation/#Limits)
	 */
    public static int GMAP_QUOTA = 2500;

    /**
	 * Get elevation values of a list of coordinate points from GMap web service
	 * 
	 * @param lats The latitude coordinates.
	 * @param lons The longitude coordinates.
	 * @param sensor See http://code.google.com/intl/fr/apis/maps/documentation/elevation/
	 * @return The elevation values.
	 */
    public static double[] getElevation(double[] lats, double[] lons, boolean sensor) {
        if (lats == null || lons == null) {
            logger.severe("Null latitude or longitude table");
            return null;
        }
        if (lats.length != lons.length) {
            logger.severe("Latitude and longitude tables have different sizes: " + lats.length + " and " + lons.length);
            return null;
        }
        if (lats.length == 0) return new double[0];
        if (lats.length > GMAP_QUOTA) {
            logger.severe("Quota exceeded - limit value is " + GMAP_QUOTA);
            return new double[0];
        }
        double[] elevations = new double[lats.length];
        StringBuffer strb = new StringBuffer("http://maps.google.com/maps/api/elevation/xml?locations=");
        for (int i = 0; i < lats.length; i++) {
            if (i > 0) strb.append("|");
            strb.append(Util.round(lats[i], 5));
            strb.append(",");
            strb.append(Util.round(lons[i], 5));
        }
        strb.append("&sensor=");
        strb.append(sensor);
        String url = strb.toString();
        if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, url);
        InputStream data = null;
        try {
            data = executeQuery(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (data == null) {
            logger.log(Level.WARNING, "GMap query returned a null object");
            return null;
        }
        Document XMLdoc = parse(data);
        if (XMLdoc == null) {
            logger.log(Level.WARNING, "Failed to get altitude from google map web service: returned data is not XML.");
            return null;
        }
        Element elevationResponseElt = (Element) XMLdoc.getElementsByTagName("ElevationResponse").item(0);
        if (elevationResponseElt == null) {
            logger.log(Level.WARNING, "Failed to get altitude from google map web service: bad XML format.");
            return null;
        }
        Element statusElt = (Element) elevationResponseElt.getElementsByTagName("status").item(0);
        if (statusElt == null) {
            logger.log(Level.WARNING, "Failed to get altitude from google map web service: bad XML format.");
            return null;
        }
        String status = statusElt.getFirstChild().getNodeValue();
        if ("OVER_QUERY_LIMIT".equalsIgnoreCase(status)) {
            logger.log(Level.WARNING, "Failed to get altitude from google map web service: quota exceeded - " + url);
            return null;
        } else if ("INVALID_REQUEST".equalsIgnoreCase(status)) {
            logger.log(Level.WARNING, "Failed to get altitude from google map web service: invalid request - " + url);
            return null;
        } else if (!"OK".equalsIgnoreCase(status)) {
            logger.log(Level.WARNING, "Failed to get altitude from google map web service (status = " + status + " )");
            return null;
        }
        NodeList resList = elevationResponseElt.getElementsByTagName("result");
        for (int i = 0; i < resList.getLength(); i++) {
            Element resultElt = (Element) resList.item(i);
            Element elevationElt = (Element) resultElt.getElementsByTagName("elevation").item(0);
            if (elevationElt == null) {
                elevations[i] = NO_VALUE_RETURNED;
                continue;
            }
            String str = elevationElt.getFirstChild().getNodeValue();
            if (str == null) {
                elevations[i] = NO_VALUE_RETURNED;
                continue;
            }
            elevations[i] = Double.parseDouble(str);
        }
        return elevations;
    }

    /**
	 * Get elevation values of a list of coordinate points from GMap web service
	 * 
	 * @param lats The latitude coordinates.
	 * @param lons The longitude coordinates.
	 * @return The elevation values.
	 */
    public static double[] getElevation(double[] lats, double[] lons) {
        return getElevation(lats, lons, false);
    }

    /**
	 * Retrieve the elevation at a position using googlemap web service
	 * 
	 * @param lat the latitude of the point
	 * @param lon the longitude of the point
	 * @param sensor true if the data comes from a sensor
	 * @return the elevation value (returns NO_VALUE_RETURNED in case of problem)
	 */
    public static double getElevation(double lat, final double lon, boolean sensor) {
        return getElevation(new double[] { lat }, new double[] { lon }, sensor)[0];
    }

    /**
	 * Retrieve the elevation at a position using googlemap web service
	 * 
	 * @param lat the latitude of the point
	 * @param lon the longitude of the point
	 * @return the elevation value (returns NO_VALUE_RETURNED in case of problem)
	 */
    public static double getElevation(double lat, final double lon) {
        return getElevation(lat, lon, false);
    }

    private static InputStream executeQuery(String url) throws MalformedURLException, IOException {
        InputStream data = null;
        try {
            data = (new URL(url)).openStream();
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Impossible to get elevation from GMap: maybe no connection to the Internet?");
        }
        return data;
    }

    private static Document parse(InputStream stream) {
        Document XMLDoc = null;
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        fact.setValidating(false);
        fact.setNamespaceAware(false);
        try {
            XMLDoc = fact.newDocumentBuilder().parse(stream);
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
        return XMLDoc;
    }
}
