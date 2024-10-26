package ws;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Lorenzo
 */
public class Geonames {

    public static String getCoordinates(String city) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
        String url = "http://ws.geonames.org/search?q=" + city + "&maxRows=1&lang=it&username=lorenzo.abram";
        URLConnection conn = new URL(url).openConnection();
        InputStream response = conn.getInputStream();
        GeonamesHandler handler = new GeonamesHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(response, handler);
        return handler.getCoord();
    }

    public static String getWikiPage(String city) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
        String url = "http://api.geonames.org/wikipediaSearch?q=" + city + "&maxRows=1&lang=it&username=lorenzo.abram";
        URLConnection conn = new URL(url).openConnection();
        InputStream response = conn.getInputStream();
        GeonamesHandler handler = new GeonamesHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(response, handler);
        return handler.getUrl();
    }
}
