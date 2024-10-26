package prajna.entity.alchemy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import prajna.entity.EntityExtractor;
import prajna.entity.EntityTag;
import prajna.entity.EntityType;

/**
 * Entity extractor which uses the Alchemy public service at
 * http://www.alchemyapi.com. This extractor requires an API key, which is
 * specified with the <code>setModel</code> method. An API key may be obtained
 * from the Alchemy web site.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class AlchemyExtractor extends EntityExtractor {

    private String key = null;

    private String requestUri = "http://access.alchemyapi.com/calls/";

    private static HashMap<String, EntityType> entMap = new HashMap<String, EntityType>();

    private static DocumentBuilder docBuild;

    static {
        entMap.put("City", EntityType.LOCATION);
        entMap.put("Company", EntityType.ORGANIZATION);
        entMap.put("Continent", EntityType.LOCATION);
        entMap.put("Country", EntityType.LOCATION);
        entMap.put("Facility", EntityType.LOCATION);
        entMap.put("FieldTerminology", EntityType.REJECTED);
        entMap.put("GeographicFeature", EntityType.LOCATION);
        entMap.put("HealthCondition", EntityType.REJECTED);
        entMap.put("Holiday", EntityType.DATE_TIME);
        entMap.put("NaturalDisaster", EntityType.EVENT);
        entMap.put("OperatingSystem", EntityType.REJECTED);
        entMap.put("Organization", EntityType.ORGANIZATION);
        entMap.put("Person", EntityType.PERSON);
        entMap.put("PrintMedia", EntityType.ORGANIZATION);
        entMap.put("Region", EntityType.LOCATION);
        entMap.put("StateOrCounty", EntityType.LOCATION);
        entMap.put("TelevisionStation", EntityType.ORGANIZATION);
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a new, uninitialized Alchemy Entity Extractor. When using this
     * constructor, setModel() must be called prior to extracting entities
     */
    public AlchemyExtractor() {
    }

    /**
     * Create a AlchemyExtractor, using the API key specified
     * 
     * @param alchemyKey the API key
     */
    public AlchemyExtractor(String alchemyKey) {
        setModel(alchemyKey);
    }

    /**
     * Perform the request to the specified HttpURLConnection, and retrieve the
     * XML result as a document
     * 
     * @param conn the connection
     * @return the document representation of the XML that is returned
     * @throws IOException if there is a problem retrieving the data
     */
    private Document doRequest(HttpURLConnection conn) throws IOException {
        DataInputStream istream = new DataInputStream(conn.getInputStream());
        Document doc = null;
        try {
            doc = docBuild.parse(istream);
        } catch (SAXException e) {
            throw new IOException(e);
        }
        istream.close();
        conn.disconnect();
        return doc;
    }

    /**
     * Extract the entities from the text provided. This method queries the
     * Alchemy server to run entity extraction on the text, and then determine
     * what the matching entity types are for each raw type discovered. This
     * method calls <code>extractEntityRawTypes()</code>, then uses an internal
     * map to map the raw entity types to EntityType objects.
     * 
     * @param text the text to extract
     * @param tokenMap the map to contain the tokens in.
     */
    @Override
    protected void extractEntities(String text, Map<String, EntityTag> tokenMap) {
        HashMap<String, String> rawTypes = extractEntityRawTypes(text);
        for (String term : rawTypes.keySet()) {
            String type = rawTypes.get(term);
            EntityType entType = entMap.get(type);
            if (entType == null) {
                entType = EntityType.UNKNOWN;
            }
            if (!entType.equals(EntityType.REJECTED)) {
                tokenMap.put(term, new EntityTag(term, entType, type));
            }
        }
    }

    /**
     * Retrieve the entities and the raw types for this entity extractor. This
     * method runs the extraction process on the text, and generates a map that
     * identifies each entity with a type. The types returned from this method
     * are not normalized, and dependent on the entity extractor
     * implementation.
     * 
     * @param text the text to parse for entities
     * @return the raw entity types
     */
    @Override
    public HashMap<String, String> extractEntityRawTypes(String text) {
        HashMap<String, String> rawTypes = new HashMap<String, String>();
        if (key == null) {
            throw new IllegalStateException("API Key not initialized");
        }
        try {
            Document doc = post("TextGetRankedNamedEntities", "text", "text", text);
            Element root = doc.getDocumentElement();
            NodeList nodes = root.getElementsByTagName("entities");
            if (nodes.getLength() > 0) {
                Element entList = (Element) nodes.item(0);
                NodeList entities = entList.getElementsByTagName("entity");
                for (int i = 0; i < entities.getLength(); i++) {
                    Element entElem = (Element) entities.item(i);
                    String type = getChildValue(entElem, "type");
                    String name = getChildValue(entElem, "text");
                    rawTypes.put(name, type);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rawTypes;
    }

    /**
     * Get the child value for the specified tag. This method extracts the text
     * value of the child node with the given name. If there are multiple
     * children with the same name, this method returns the value of the first
     * text element.
     * 
     * @param elem the parent element
     * @param tag the child tag
     * @return the text value of the child element matching the tag
     */
    private String getChildValue(Element elem, String tag) {
        String val = null;
        NodeList list = elem.getElementsByTagName(tag);
        if (list.getLength() > 0) {
            Element child = (Element) list.item(0);
            val = child.getTextContent().trim();
        }
        return val;
    }

    /**
     * Get the model string. This returns the API key for the AlchemyExtractor
     * 
     * @return the API key
     */
    @Override
    public String getModel() {
        return key;
    }

    /**
     * Post a request to the Alchemy server
     * 
     * @param callName the name of the call to the server, used as the command
     * @param callPrefix the call prefix, which specifies the directory on the
     *            server where the command occurs
     * @param param additional parameters to pass
     * @return a DOM document containing the results of the query
     * @throws IOException if there is a problem with the request
     */
    private Document post(String callName, String callPrefix, String... param) throws IOException {
        URL url = new URL(requestUri + callPrefix + "/" + callName);
        HttpURLConnection handle = (HttpURLConnection) url.openConnection();
        handle.setDoOutput(true);
        StringBuilder data = new StringBuilder();
        data.append("apikey=").append(key).append("&outputMode=xml");
        for (int i = 0; i < param.length; ++i) {
            data.append('&').append(param[i]);
            if (++i < param.length) {
                data.append('=').append(URLEncoder.encode(param[i], "UTF8"));
            }
        }
        handle.addRequestProperty("Content-Length", Integer.toString(data.length()));
        DataOutputStream ostream = new DataOutputStream(handle.getOutputStream());
        ostream.write(data.toString().getBytes());
        ostream.close();
        return doRequest(handle);
    }

    /**
     * Set the model for this entity extractor. The AlchemyExtractor uses an
     * API key, which should be provided as the model argument.
     * 
     * @param model the API key
     */
    @Override
    public void setModel(String model) {
        key = model;
        AlchemyDocData.setApiKey(key);
    }
}
