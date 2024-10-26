package com.bbn.wild.server.component.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.bbn.wild.server.location.CivicLocation;
import com.bbn.wild.server.location.GeodeticLocation;
import com.bbn.wild.server.location.LocationRequest;
import com.bbn.wild.server.location.LocationResponse;
import com.bbn.wild.server.location.Measurement;
import com.bbn.wild.server.location.WifiMeasurement;
import com.bbn.wild.server.location.Circle;
import com.bbn.wild.server.component.Component;
import com.bbn.wild.server.component.SynchronousResponder;
import com.bbn.wild.server.exception.UnregisteredComponentException;

public class SkyhookLocationSource implements SynchronousResponder {

    private static final String URI_PARAM = "com.bbn.wild.server.component.source.skyhook.uri";

    private static final String USERNAME_PARAM = "com.bbn.wild.server.component.source.skyhook.username";

    private static final String REALM_PARAM = "com.bbn.wild.server.component.source.skyhook.realm";

    private static final String DEFAULT_URI = "https://api.skyhookwireless.com/wps2/location";

    private String skyhookServerUri = DEFAULT_URI;

    private String skyhookUsername = null;

    private String skyhookRealm = null;

    public SkyhookLocationSource() {
    }

    public LocationResponse getResponse(LocationRequest lrq) throws UnregisteredComponentException {
        LocationResponse lrs = lrq.createResponse();
        try {
            String rqs, rss;
            rqs = encodeSkyhookRequest(lrq);
            if (null == rqs) {
                lrs.setError("No authentication was provided.");
                return lrs;
            }
            URL url = new URL(this.skyhookServerUri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Content-Type", "text/xml");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(rqs);
            wr.flush();
            BufferedReader rd;
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            rss = "";
            String line;
            while ((line = rd.readLine()) != null) rss += line;
            rd.close();
            decodeSkyhookResponse(rss, lrs);
        } catch (Exception e) {
            e.printStackTrace();
            lrs.setError("Error querying Skyhook");
        }
        return lrs;
    }

    /**
	 * Convert a LocationRequest object into a Skyhook request string.
	 * @param lrq LocationRequest to convert.
	 * @return String Skyhook request
	 */
    public String encodeSkyhookRequest(LocationRequest lrq) {
        String requestXML = "<LocationRQ xmlns='http://skyhookwireless.com/wps/2005' version='2.6' street-address-lookup='full'>";
        if ((skyhookUsername != null) && (skyhookRealm != null)) requestXML += "<authentication version='2.0'><simple>" + "<username>" + skyhookUsername + "</username>" + "<realm>" + skyhookRealm + "</realm>" + "</simple></authentication>"; else if ((lrq.getAccessUsername() != null) && (lrq.getAccessPassword() != null)) requestXML += "<authentication version='2.0'><simple>" + "<username>" + lrq.getAccessUsername() + "</username>" + "<realm>" + lrq.getAccessPassword() + "</realm>" + "</simple></authentication>"; else return null;
        Measurement[] measurements = lrq.getMeasurements();
        if (measurements != null) {
            for (int i = 0; i < measurements.length; ++i) {
                if (!(measurements[i] instanceof WifiMeasurement)) continue;
                String mac = ((WifiMeasurement) measurements[i]).getBssid().replaceAll("[^0-9a-z]", "");
                int rssi = ((WifiMeasurement) measurements[i]).getRssi();
                requestXML += "<access-point><mac>" + mac + "</mac> <signal-strength>" + rssi + "</signal-strength> </access-point>";
            }
        }
        requestXML += "<access-point><mac>00169CBA1BB0</mac> <signal-strength>-50</signal-strength> </access-point>";
        requestXML += "</LocationRQ>";
        return requestXML;
    }

    /**
	 * Populate a LocationResponse from the data in a skyhook response.
	 * @param skyhookResponse String the location response from the skyhook server.
	 * @param lrs LocationResponse populated with the skyhook data.
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
    public void decodeSkyhookResponse(String skyhookResponse, LocationResponse lrs) throws ParserConfigurationException, IOException {
        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            doc = parser.parse(new InputSource(new StringReader(skyhookResponse)));
        } catch (SAXException e) {
            e.printStackTrace();
            return;
        }
        Element latElt = null, lonElt = null, radElt = null, errElt = null;
        double lat = 0, lon = 0, rad = 0;
        String error = "";
        if (doc.getElementsByTagName("latitude").getLength() > 0) {
            latElt = (Element) doc.getElementsByTagName("latitude").item(0);
            lat = Double.parseDouble(latElt.getTextContent());
        }
        if (doc.getElementsByTagName("longitude").getLength() > 0) {
            lonElt = (Element) doc.getElementsByTagName("longitude").item(0);
            lon = Double.parseDouble(lonElt.getTextContent());
        }
        if (doc.getElementsByTagName("hpe").getLength() > 0) {
            radElt = (Element) doc.getElementsByTagName("hpe").item(0);
            rad = Double.parseDouble(radElt.getTextContent());
        }
        if (doc.getElementsByTagName("error").getLength() > 0) {
            errElt = (Element) doc.getElementsByTagName("error").item(0);
            error = errElt.getTextContent();
        }
        if ((latElt != null) && (lonElt != null) && (radElt != null)) lrs.setGeodetic(new Circle(lat, lon, rad));
        if (errElt != null) lrs.setError(error);
        if (doc.getElementsByTagName("street-address").getLength() > 0) {
            Element civicElement = (Element) doc.getElementsByTagName("street-address").item(0);
            Element currElt = null;
            CivicLocation civic = new CivicLocation();
            lrs.setCivic(civic);
            if (civicElement.getElementsByTagName("country").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("country").item(0);
                civic.setCountry(currElt.getTextContent());
            }
            if (civicElement.getElementsByTagName("state").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("state").item(0);
                civic.setA1(currElt.getTextContent());
            }
            if (civicElement.getElementsByTagName("county").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("county").item(0);
                civic.setA2(currElt.getTextContent());
            }
            if (civicElement.getElementsByTagName("city").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("city").item(0);
                civic.setA3(currElt.getTextContent());
            }
            if (civicElement.getElementsByTagName("postal-code").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("postal-code").item(0);
                civic.setPC(currElt.getTextContent());
            }
            if (civicElement.getElementsByTagName("street-number").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("street-number").item(0);
                civic.setHNO(currElt.getTextContent());
            }
            if (civicElement.getElementsByTagName("address-line").getLength() > 0) {
                currElt = (Element) civicElement.getElementsByTagName("address-line").item(0);
                civic.setRD(currElt.getTextContent());
            }
        }
    }

    public Component createComponent() {
        return new SkyhookLocationSource();
    }

    public void configure(Properties prop) {
        if (prop.containsKey(URI_PARAM)) this.skyhookServerUri = prop.getProperty(URI_PARAM);
        if (prop.containsKey(USERNAME_PARAM)) this.skyhookUsername = prop.getProperty(USERNAME_PARAM); else System.err.println("Missing parameter " + USERNAME_PARAM);
        if (prop.containsKey(REALM_PARAM)) this.skyhookRealm = prop.getProperty(REALM_PARAM); else System.err.println("Missing parameter " + REALM_PARAM);
    }

    public Component getSuccessor() {
        return null;
    }
}
