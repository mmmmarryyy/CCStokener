package org.grailrtls.solver.traffic_solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Implementation that interfaces with the
 * MapQuest GeoCoding Web Service and retrieves latitude and longitude for a ZIP code OR 'city, state' in the
 * United States. 
 * 
 * @author Sumedh Sawant
 * 
 */
public class MapquestGeocodingService {

    /**
	 * Logging facility.
	 */
    private static final Logger log = LoggerFactory.getLogger(MapquestGeocodingService.class);

    private String zipCode = "";

    private String cityState = "";

    private String key = "";

    private String url = "";

    private String xmlString = null;

    private String latitude = null;

    private String longitude = null;

    protected static final String URL_PART_1 = "http://www.mapquestapi.com/geocoding/v1/address?key=";

    protected static final String URL_PART_2 = "&callback=renderOptions&inFormat=kvp&outFormat=xml&location=";

    public MapquestGeocodingService(String zipCode, String key) {
        this.zipCode = zipCode;
        this.key = key;
        this.url = MapquestGeocodingService.URL_PART_1 + this.key + MapquestGeocodingService.URL_PART_2 + this.zipCode;
    }

    public MapquestGeocodingService(String city, String state, String key) {
        this.cityState = MapquestGeocodingService.formatLocation(city, state);
        this.key = key;
        this.url = MapquestGeocodingService.URL_PART_1 + this.key + MapquestGeocodingService.URL_PART_2 + this.cityState;
    }

    private void getXMLData() {
        String result = null;
        URL url = null;
        URLConnection conn = null;
        BufferedReader rd = null;
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            url = new URL(this.url);
            conn = url.openConnection();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                sb.append(line + "\n");
            }
            rd.close();
            result = sb.toString();
        } catch (MalformedURLException e) {
            log.error("URL was malformed: {}", url, e);
        } catch (IOException e) {
            log.error("IOException thrown: {}", url, e);
        }
        this.xmlString = result;
    }

    private void parseXMLData() {
        if (this.xmlString == null) {
            this.getXMLData();
            if (this.xmlString == null) {
                log.error("No XML Data has been received.");
                return;
            }
        }
        Document doc = null;
        try {
            doc = MapquestTrafficService.loadXMLFromString(this.xmlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();
        String[] tags = new String[] { "result", "locations", "location", "latLng" };
        NodeList tempList = doc.getElementsByTagName("results");
        Element tempElement = null;
        String latitude = null;
        String longitude = null;
        for (String tag : tags) {
            tempElement = (Element) tempList.item(0);
            tempList = tempElement.getElementsByTagName(tag);
            if ("latLng".equalsIgnoreCase(tag)) {
                latitude = ((Element) tempList.item(0)).getElementsByTagName("lat").item(0).getTextContent();
                longitude = ((Element) tempList.item(1)).getElementsByTagName("lng").item(0).getTextContent();
            }
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String[] getLatitudeLongitude() {
        this.parseXMLData();
        String[] result = new String[] { this.latitude, this.longitude };
        return result;
    }

    private static String formatLocation(String city, String state) {
        city = city.trim();
        city = city.toLowerCase();
        char[] cityChars = new char[city.length()];
        city.getChars(0, city.length(), cityChars, 0);
        StringBuffer buffer = new StringBuffer();
        for (char c : cityChars) {
            if (c == ' ') {
                buffer.append('_');
            } else {
                buffer.append(c);
            }
        }
        state = state.trim();
        state = state.toLowerCase();
        buffer.append(',');
        buffer.append(state);
        return buffer.toString();
    }

    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static void main(String[] args) {
        MapquestGeocodingService t = new MapquestGeocodingService("55347", "Fmjtd%7Cluu2256rn1%2C2x%3Do5-huzsg");
        String[] array = t.getLatitudeLongitude();
        System.out.println("Latitude: " + array[0]);
        System.out.println("Longitude: " + array[1]);
    }
}
