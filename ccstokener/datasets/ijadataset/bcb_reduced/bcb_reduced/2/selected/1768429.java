package org.grailrtls.solver.traffic_solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Implementation that interfaces with the
 * MapQuest Traffic Web Service and retrieves Traffic Alerts for a ZIP code OR 'city, state' in the
 * United States. Utilizes the MapQuest GeoCoding Web Service to retrieve the longitude and latitude 
 * required for your region.
 * 
 * @author Sumedh Sawant
 * 
 */
public class MapquestTrafficService {

    /**
	 * Logging facility.
	 */
    private static final Logger log = LoggerFactory.getLogger(MapquestTrafficService.class);

    private String boundingBox = null;

    private String key = null;

    private String url = null;

    private String xmlString = null;

    private static final String URL_PART_1 = "http://www.mapquestapi.com/traffic/v1/incidents?key=";

    private static final String URL_PART_2 = "&callback=handleIncidentsResponse&boundingBox=";

    private static final String URL_PART_3 = "&filters=construction,incidents&inFormat=kvp&outFormat=xml";

    private ArrayList<String[]> trafficAlerts = null;

    public MapquestTrafficService(String zipCode, String key) {
        this.key = key;
        MapquestGeocodingService gService = new MapquestGeocodingService(zipCode, key);
        String[] latLng = gService.getLatitudeLongitude();
        double latitude = Double.parseDouble(latLng[0]);
        double longitude = Double.parseDouble(latLng[1]);
        Double lat1 = new Double(latitude + (double) 0.25);
        Double lat2 = new Double(latitude - (double) 0.25);
        Double long1 = new Double(longitude - (double) 0.25);
        Double long2 = new Double(longitude + (double) 0.25);
        this.boundingBox = lat1.toString() + "," + long1.toString() + "," + lat2.toString() + "," + long2.toString();
        this.url = MapquestTrafficService.URL_PART_1 + this.key + MapquestTrafficService.URL_PART_2 + this.boundingBox + MapquestTrafficService.URL_PART_3;
    }

    public MapquestTrafficService(String city, String state, String key) {
        this.key = key;
        MapquestGeocodingService gService = new MapquestGeocodingService(city, state, key);
        String[] latLng = gService.getLatitudeLongitude();
        double latitude = Double.parseDouble(latLng[0]);
        double longitude = Double.parseDouble(latLng[1]);
        Double lat1 = new Double(latitude + (double) 0.25);
        Double lat2 = new Double(latitude - (double) 0.25);
        Double long1 = new Double(longitude - (double) 0.25);
        Double long2 = new Double(longitude + (double) 0.25);
        this.boundingBox = lat1.toString() + "," + long1.toString() + "," + lat2.toString() + "," + long2.toString();
        this.url = MapquestTrafficService.URL_PART_1 + this.key + MapquestTrafficService.URL_PART_2 + this.boundingBox + MapquestTrafficService.URL_PART_3;
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
        NodeList nodeLst = doc.getElementsByTagName("Incident");
        ArrayList<String[]> trafficAlerts = new ArrayList<String[]>();
        for (int i = 0; i < nodeLst.getLength(); i++) {
            Node temp = nodeLst.item(i);
            if (temp.getNodeType() == Node.ELEMENT_NODE) {
                Element tempElement = (Element) temp;
                NodeList severityList = tempElement.getElementsByTagName("severity");
                Element currentSeverityElement = (Element) severityList.item(0);
                NodeList severityValues = currentSeverityElement.getChildNodes();
                String severity = ((Node) severityValues.item(0)).getNodeValue();
                NodeList descriptionList = tempElement.getElementsByTagName("shortDesc");
                Element currentDescriptionElement = (Element) descriptionList.item(0);
                NodeList descriptionValues = currentDescriptionElement.getChildNodes();
                String description = ((Node) descriptionValues.item(0)).getNodeValue();
                trafficAlerts.add(new String[] { severity, description });
            }
        }
        this.trafficAlerts = trafficAlerts;
    }

    public ArrayList<String[]> getTrafficAlerts() {
        this.parseXMLData();
        return this.trafficAlerts;
    }

    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static void main(String[] args) {
        MapquestTrafficService t = new MapquestTrafficService("08817", "Fmjtd%7Cluu2256rn1%2C2x%3Do5-huzsg");
        for (String[] arr : t.getTrafficAlerts()) {
            System.out.println("Severity: " + arr[0]);
            System.out.println("Description: " + arr[1]);
            System.out.println(" ");
        }
    }
}
