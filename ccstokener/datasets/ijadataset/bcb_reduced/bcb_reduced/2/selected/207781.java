package com.android.alberthernandez.taksinow;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.google.android.maps.GeoPoint;

/**
 * KMLParser is a class for parsing KML files provided by an URL
 * request. When the KML file is parsed the result is a
 * {@link Route} object, that contains a list of {@link Placemark}
 * and the line for plotting the route as a {@link GeoPoint} list
 * as well.
 * 
 * @author		Albert Hernández López
 * @version		1.0
 * @since       1.0
 */
public class KMLParser extends DefaultHandler {

    private String urlString;

    private StringBuilder text;

    private Route route;

    private Placemark placemark;

    /**
     * Private default constructor to avoid initializing KMLParsers w/o
     * URL.
     */
    @SuppressWarnings("unused")
    private KMLParser() {
    }

    /**
	 * KMLParser constructor with URL {@link String} as parameter. 
	 * <p>
	 * The URL String must be a well-formed URL.
	 *
	 * @param  url  an absolute URL of the query to google maps
	 */
    public KMLParser(String url) {
        urlString = url;
        text = new StringBuilder();
        placemark = null;
        route = null;
    }

    /**
     * Returns the route obtained from parsing the KML file. 
     *
     * @return      the route obtained from parsing the KML file
     * @see         Route
     */
    public Route getRoute() {
        return route;
    }

    /**
	 * Parse the request to google maps coded in KML format.
	 * 
	 * @throws ParserConfigurationException	Exception during the configuration
	 * 										of the parser.
	 * @throws SAXException					Any SAX exception, possibly
	 * 										wrapping another exception.
	 * @throws IOException					Network problems or other
	 * 										I/O problems.
	 */
    public void parse() throws ParserConfigurationException, SAXException, IOException {
        URLConnection urlConnection = null;
        InputStream urlInputStream = null;
        SAXParserFactory spf = null;
        SAXParser sp = null;
        URL url = new URL(this.urlString);
        if ((urlConnection = url.openConnection()) == null) {
            return;
        }
        urlInputStream = urlConnection.getInputStream();
        spf = SAXParserFactory.newInstance();
        if (spf != null) {
            sp = spf.newSAXParser();
            sp.parse(urlInputStream, this);
        }
        if (urlInputStream != null) urlInputStream.close();
    }

    /**
	 * This method is called every time SAX generates an event when 
	 * an XML element starts.
	 *
	 * @param  uri 			The Namespace URI, or the empty string if
	 *  					the element has no Namespace URI or if Namespace
	 *  					processing is not being performed.
	 * @param  localName	The local name (without prefix), or the empty
	 * 						string if Namespace processing is not being performed.
	 * @param  qName		The qualified name (with prefix), or the empty string
	 * 						if qualified names are not available.
	 * @param  attributes	The attributes attached to the element. If there are
	 * 						no attributes, it shall be an empty Attributes object.
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @see    org.xml.sax.helpers.DefaultHandler
	 */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (localName.equalsIgnoreCase("Document")) {
            route = new Route();
        }
        if (localName.equalsIgnoreCase("Placemark") && (route != null)) {
            placemark = new Placemark();
        }
        if (localName.equalsIgnoreCase("GeometryCollection") && (route != null)) {
            placemark = null;
        }
    }

    /**
	 * This method is called every time SAX generates an event when 
	 * an XML element ends.
	 * 
	 * @param  uri 			The Namespace URI, or the empty string if
	 *  					the element has no Namespace URI or if Namespace
	 *  					processing is not being performed.
	 * @param  localName	The local name (without prefix), or the empty
	 * 						string if Namespace processing is not being performed.
	 * @param  qName		The qualified name (with prefix), or the empty string
	 * 						if qualified names are not available.
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @see    org.xml.sax.helpers.DefaultHandler
	 */
    public void endElement(String uri, String localName, String qName) {
        if (route == null) {
            return;
        }
        if (localName.equalsIgnoreCase("Placemark")) {
            placemark = null;
        }
        if (placemark != null) {
            if (localName.equalsIgnoreCase("name")) {
                placemark.setName(text.toString().trim());
            }
            if (localName.equalsIgnoreCase("description")) {
                placemark.setDistanceFromOrigin(text.toString().trim());
            }
            if (localName.equalsIgnoreCase("coordinates")) {
                StringTokenizer st = new StringTokenizer(this.text.toString().trim(), " ,");
                while (st.hasMoreTokens()) {
                    Double lng = Float.parseFloat(((String) st.nextToken()).trim()) * 1E6;
                    Double lat = Float.parseFloat(((String) st.nextToken()).trim()) * 1E6;
                    GeoPoint geoPoint = new GeoPoint(lat.intValue(), lng.intValue());
                    placemark.setGeoPoint(geoPoint);
                    route.addPlacemark(placemark);
                    st.nextToken();
                }
            }
            if (localName.equalsIgnoreCase("GeometryCollection")) {
                placemark = null;
            }
        }
        if (placemark == null) {
            if (localName.equalsIgnoreCase("coordinates")) {
                StringTokenizer st = new StringTokenizer(text.toString().trim(), " ,");
                while (st.hasMoreTokens()) {
                    Double lng = Float.parseFloat(((String) st.nextToken()).trim()) * 1E6;
                    Double lat = Float.parseFloat(((String) st.nextToken()).trim()) * 1E6;
                    GeoPoint geoPoint = new GeoPoint(lat.intValue(), lng.intValue());
                    route.addPointToLinePlot(geoPoint);
                    st.nextToken();
                }
            }
        }
        text.setLength(0);
    }

    /**
	 * Receive notification of character data inside an element.
	 * an XML element ends.
	 * 
	 * @param  ch 			The characters.
	 * @param  start		The start position in the character array.
	 * @param  length 		The number of characters to use from the character array.
	 * @throws SAXException	Any SAX exception, possibly wrapping another exception.
	 * @see    org.xml.sax.helpers.DefaultHandler
	 */
    public void characters(char[] ch, int start, int length) {
        text.append(ch, start, length);
    }
}
