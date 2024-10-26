package RssIO;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Given a file, returns the appropriate class to parse that file.
 */
public class FeedFetcher {

    protected final String[] VERSIONS = { "0.91", "2.0" };

    public FeedObjectIfc getFeedObject(String filePath) {
        FeedReaderIfc ifc = null;
        String ver = readVersion(filePath);
        if (ver != null) {
            if (ver.equals(VERSIONS[0])) {
                ifc = new FeedReaderRss091(filePath);
            } else if (ver.equals(VERSIONS[1])) {
                ifc = new FeedReaderRss20(filePath);
            } else {
                ifc = new FeedReaderRDF(filePath);
            }
        }
        if (ifc != null) {
            return ifc.read();
        }
        return null;
    }

    private String readVersion(String path) {
        DocumentBuilderFactory dbFactory = null;
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(false);
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            Document doc = db.parse(path);
            doc.setXmlVersion("1.0");
            doc.getDocumentElement().normalize();
            NodeList rssList = doc.getElementsByTagName("rss");
            for (int r = 0; r < rssList.getLength(); r++) {
                Node firstNode = rssList.item(r);
                if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstElement = (Element) firstNode;
                    return firstElement.getAttribute("version");
                }
            }
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public FeedObjectIfc getFeedObject(URL urlPath) {
        FeedReaderIfc ifc = null;
        BufferedInputStream bufIn = null;
        try {
            bufIn = new BufferedInputStream(urlPath.openStream());
            String ver = readVersion(bufIn);
            if (ver != null) {
                if (ver.equals(VERSIONS[0])) {
                    ifc = new FeedReaderRss091(urlPath);
                } else if (ver.equals(VERSIONS[1])) {
                    ifc = new FeedReaderRss20(urlPath);
                }
            }
            if (ifc != null) {
                return ifc.read();
            }
        } catch (SSLHandshakeException ex) {
            ex.printStackTrace();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (bufIn != null) {
                try {
                    bufIn.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

    private String readVersion(BufferedInputStream bufferedStream) {
        DocumentBuilderFactory dbFactory = null;
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(false);
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            Document doc = db.parse(bufferedStream);
            doc.setXmlVersion("1.0");
            doc.getDocumentElement().normalize();
            NodeList rssList = doc.getElementsByTagName("rss");
            for (int r = 0; r < rssList.getLength(); r++) {
                Node firstNode = rssList.item(r);
                if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstElement = (Element) firstNode;
                    return firstElement.getAttribute("version");
                }
            }
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
