package de.byteholder.geoclipse.weather;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 
 * 
 * http://ws.geonames.org/findNearbyPlaceName?lat=43.30&lng=5.4
 * 
 * @author s2y
 *
 */
public class GoogleGeocoder {

    public static final String urlService = "http://ws.geonames.org/findNearbyPlaceName";

    /**
	 * Converts a Geoposition represented by latitude and longtitude to a city
	 * 
	 * @param latitude
	 * @param longtitude
	 * @return city for given latitude and longtitude
	 * @throws IOException
	 */
    public static String getCityForLocation(double latitude, double longtitude) {
        String city = null;
        String query = "http://ws.geonames.org/findNearbyPlaceName?lat=" + latitude + "&lng=" + longtitude;
        DocumentBuilder builder;
        try {
            Activator.log("Querying " + query);
            System.out.println("Querying " + query);
            URL url = new URL(query);
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(url.openStream());
            XPath xpath = XPathFactory.newInstance().newXPath();
            city = (String) xpath.evaluate("//geonames/geoname/name/text()", doc, XPathConstants.STRING);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Activator.log("CityForLocation: " + city);
        System.out.println("CityForLocation: " + city);
        return city;
    }
}
