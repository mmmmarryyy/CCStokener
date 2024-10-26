package slevnik;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class containing method calculating distances between cities
 * @author Martin Penak
 */
public class DistanceParser {

    /**
     * This method calculate distances from cities in list to given city.
     * It uses Google API and its distance matrix.
     * Method gets xml document, parse distances from it a save it to the map.
     * @param city city used to calculate distances
     * @param cities cities used to calculate dsitances
     * @return map containing cities with their distances to given city
     */
    public static Map<String, Integer> getDistances(String city, List<String> cities) {
        InputStream myStream = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            StringBuilder url = new StringBuilder("http://maps.googleapis.com/maps/api/distancematrix/xml?origins=");
            url.append(city.replace(' ', '+'));
            url.append("&destinations=");
            for (String s : cities) {
                String s2 = s.replace(' ', '+');
                url.append(s2);
                url.append("|");
            }
            url.append("&sensor=false");
            myStream = new URL(url.toString()).openStream();
            Document doc = builder.parse(myStream);
            NodeList myDistances = doc.getElementsByTagName("element");
            Map<String, Integer> distances = new HashMap<String, Integer>();
            for (int i = 0; i < myDistances.getLength(); i++) {
                if (!((Element) myDistances.item(i)).getElementsByTagName("status").item(0).getTextContent().equals("OK")) {
                    distances.put(cities.get(i).toLowerCase(), Integer.MAX_VALUE);
                } else {
                    distances.put(cities.get(i).toLowerCase(), Integer.parseInt(myDistances.item(i).getChildNodes().item(5).getChildNodes().item(1).getTextContent()));
                }
            }
            return distances;
        } catch (SAXException ex) {
            Logger.getLogger(DistanceParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DistanceParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DistanceParser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                myStream.close();
            } catch (IOException ex) {
                Logger.getLogger(DistanceParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
