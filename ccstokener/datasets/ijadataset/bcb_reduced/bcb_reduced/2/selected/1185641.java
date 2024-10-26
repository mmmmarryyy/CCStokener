package map.model;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author balage
 *
 */
public class APIServer {

    final DocumentBuilderFactory factory;

    final DocumentBuilder builder;

    final String baseurl;

    final Capabilities capabilities;

    final File cache;

    public APIServer(String url, File cachedir) throws ParserConfigurationException, MalformedURLException, SAXException, IOException {
        System.out.println("Configuring client..");
        factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        System.out.println("Checking server: " + url + "...");
        baseurl = url;
        capabilities = getCapabilities();
        if (capabilities != null) {
            System.out.println("Server capabilities: ");
            System.out.println("Maximum area: " + capabilities.area);
        } else System.out.println("Server is in offline mode.");
        this.cache = cachedir;
    }

    private InputStream getCached(String url) throws MalformedURLException, IOException {
        if (cache == null) {
            return new URL(url).openStream();
        }
        if (!cache.exists()) cache.mkdirs();
        File c = new File(cache, URLEncoder.encode(url, "UTF-8"));
        if (c.exists()) return new FileInputStream(c);
        return new URL(url).openStream();
    }

    private Document get(String url) throws MalformedURLException, SAXException, IOException {
        File c = new File(cache, URLEncoder.encode(url, "UTF-8"));
        Document d = builder.parse(getCached(url));
        if (!c.exists()) {
            try {
                TransformerFactory.newInstance().newTransformer().transform(new DOMSource(d), new StreamResult(c));
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (TransformerFactoryConfigurationError e) {
                e.printStackTrace();
            }
        }
        return d;
    }

    private Capabilities getCapabilities() throws MalformedURLException, SAXException, IOException {
        try {
            Document doc = get(baseurl + "/api/capabilities");
            Capabilities result = new Capabilities();
            NodeList nl = doc.getDocumentElement().getElementsByTagName("api");
            Element api = (Element) nl.item(0);
            Element area = (Element) api.getElementsByTagName("area").item(0);
            result.area = Double.valueOf(area.getAttribute("maximum")).doubleValue();
            return result;
        } catch (Exception e) {
            System.err.println("Exception caught on configuring server, switching to offline mode.");
            return null;
        }
    }

    public Map readMap(Rectangle2D bounds) throws MalformedURLException, SAXException, IOException {
        double s = (capabilities == null) ? 0.05 : Math.sqrt(capabilities.area) * 0.1;
        System.out.println("Setting maximum tile size: " + s);
        int mx = (int) Math.ceil(bounds.getWidth() / s);
        int my = (int) Math.ceil(bounds.getHeight() / s);
        Map result = new Map();
        for (int x = 0; x < mx; x++) for (int y = 0; y < my; y++) {
            Rectangle2D b = new Rectangle2D.Double(bounds.getMinX() + x * s, bounds.getMinY() + y * s, Math.min(bounds.getWidth(), s), Math.min(bounds.getHeight(), s));
            parseMapTileIntoMap(result, readMapTile(b));
        }
        return result;
    }

    public Map loadMapFromOSMFile(File file) throws SAXException, IOException {
        Map result = new Map();
        parseMapTileIntoMap(result, builder.parse(file));
        return result;
    }

    private Document readMapTile(Rectangle2D bounds) throws MalformedURLException, SAXException, IOException {
        if (capabilities == null) {
        }
        System.out.println("Reading tile of " + bounds);
        return get(baseurl + "/api/0.6/map?bbox=" + bounds.getMinX() + "," + bounds.getMinY() + "," + bounds.getMaxX() + "," + bounds.getMaxY());
    }

    private void parseMapTileIntoMap(Map map, Document doc) {
        if (doc == null) return;
        Element osm = doc.getDocumentElement();
        NodeList nl = osm.getChildNodes();
        int nodes = 0;
        int ways = 0;
        int relations = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) {
                Element e = (Element) n;
                if ("node".equals(e.getNodeName())) {
                    nodes++;
                    map.addItem(new MapNode(e));
                }
                if ("way".equals(e.getNodeName())) {
                    ways++;
                    map.addItem(new MapWay(e));
                }
                if ("relation".equals(e.getNodeName())) {
                    relations++;
                }
            }
        }
        System.out.println("Found: ");
        System.out.println(nodes + " nodes");
        System.out.println(ways + " ways");
        System.out.println(relations + " relations");
    }
}
