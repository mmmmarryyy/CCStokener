package au.com.georgi.wave.reader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The page in XML format is great for getting character info but doesn't
 * contain any image information.
 */
public class XMLWebsiteReader {

    private static final Logger log = Logger.getLogger(XMLWebsiteReader.class.getName());

    public static Document readWebsite(URL url) {
        HttpURLConnection connection;
        try {
            log.warning("Attempting to open URL connection...");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1");
            InputStream inputStream = connection.getInputStream();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.parse(inputStream);
        } catch (IOException e) {
            log.severe(e.getMessage());
        } catch (SAXException e) {
            log.severe(e.getMessage());
        } catch (ParserConfigurationException e) {
            log.severe(e.getMessage());
        }
        throw new RuntimeException("Could not read website for url: " + url.getPath());
    }
}
