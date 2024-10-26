package prajna.entity.alchemy;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import prajna.data.DocData;

/**
 * DocData object which uses the Alchemy public service at
 * http://www.alchemyapi.com to retrieve the content of a web-accessible
 * document. This extractor requires an API key, which is specified with the
 * <code>setApiKey</code> method. An API key may be obtained from the Alchemy
 * web site.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class AlchemyDocData extends DocData {

    private static String apiKey = null;

    private static DocumentBuilder docBuild;

    static {
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a new AlchemyDocData object for the specified URL
     * 
     * @param url the URL of the document
     * @throws MalformedURLException if the argument is not a valid URL
     */
    public AlchemyDocData(String url) throws MalformedURLException {
        super("");
        new URL(url);
        setLink(url);
    }

    /**
     * Create a new AlchemyDocData object for the specified URL
     * 
     * @param url the URL of the document
     */
    public AlchemyDocData(URL url) {
        super("");
        setLink(url.toString());
    }

    /**
     * Get the body of the document. If the body has not already been
     * retrieved, this method issues a request to Alchemy to parse the document
     * and extract the text.
     */
    @Override
    public String getBody() {
        String body = super.getBody();
        if (body.length() == 0) {
            if (AlchemyDocData.apiKey == null) {
                throw new IllegalStateException("API Key not initialized");
            }
            try {
                String fullUrl = "http://access.alchemyapi.com/calls/url/URLGetText?" + "apikey=" + apiKey + "&url=" + URLEncoder.encode(getLink(), "UTF-8");
                body = queryServer(fullUrl, "text");
                body = body.replaceAll("[\\u00A0\\u2007\\u202F]", " ");
                body = body.replaceAll("[\\s]+", " ");
            } catch (UnsupportedEncodingException e) {
            }
            setBody(body);
        }
        return body;
    }

    /**
     * Get the name of the document. If the name has not already been
     * retrieved, this method issues a request to Alchemy to parse the document
     * and extract the title.
     */
    @Override
    public String getName() {
        String name = super.getName();
        if (name.length() == 0) {
            if (AlchemyDocData.apiKey == null) {
                throw new IllegalStateException("API Key not initialized");
            }
            try {
                String fullUrl = "http://access.alchemyapi.com/calls/url/URLGetTitle?" + "apikey=" + apiKey + "&url=" + URLEncoder.encode(getLink(), "UTF-8");
                name = queryServer(fullUrl, "title");
            } catch (UnsupportedEncodingException e) {
            }
            setName(name);
        }
        return name;
    }

    /**
     * Issue a query to the alchemy server for the given URL. This method will
     * parse the results, looking for data in the specified tag within the
     * resulting XML
     * 
     * @param fullUrl the full URL of the request
     * @param tag the tag to check for resulting data
     * @return the contents of the specified XML tag in the response
     */
    private String queryServer(String fullUrl, String tag) {
        String result = null;
        try {
            URL url = new URL(fullUrl);
            DataInputStream istream = new DataInputStream(url.openStream());
            Document doc = docBuild.parse(istream);
            Element root = doc.getDocumentElement();
            NodeList nodes = root.getElementsByTagName(tag);
            if (nodes != null && nodes.getLength() > 0) {
                Element resElem = (Element) nodes.item(0);
                result = resElem.getTextContent().trim();
            }
            istream.close();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Return the API key
     * 
     * @return the API key
     */
    static String getApiKey() {
        return apiKey;
    }

    /**
     * Set the API key for Alchemy. This must be set prior to retrieving any
     * document data.
     * 
     * @param key the Alchemy API key.
     */
    public static void setApiKey(String key) {
        apiKey = key;
    }
}
