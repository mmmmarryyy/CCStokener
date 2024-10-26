package util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class reads and parses the set xml file
 * 
 * @author quark
 * @version 1.0
 * @created 02-gen-2009 18.49.29
 */
public class ConfiguratorXMLReader {

    private static ConfiguratorXMLReader instance = null;

    private File configFile = null;

    private URL url = null;

    private org.w3c.dom.Document configDoc = null;

    private DocumentBuilder builder = null;

    private XPath xpath = null;

    /**
	 * use getInstance!
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
    private ConfiguratorXMLReader() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        builder = domFactory.newDocumentBuilder();
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
    }

    /**
	 * 
	 * @return configurator instance
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
    public static synchronized ConfiguratorXMLReader getInstance() throws ParserConfigurationException, SAXException, IOException {
        if (instance == null) {
            instance = new ConfiguratorXMLReader();
        }
        return instance;
    }

    /**
	 * Gives a NodeList from a XPath query
	 */
    public NodeList getValues(String expr) throws XPathExpressionException {
        XPathExpression xexpr = xpath.compile(expr);
        Object result = xexpr.evaluate(configDoc, XPathConstants.NODESET);
        return (NodeList) result;
    }

    /**
	 * Reload parser's state
	 * 
	 * @throws SAXException
	 * @throws IOException
	 */
    public void reload() throws SAXException, IOException {
        if (url != null) {
            java.io.InputStream is = url.openStream();
            configDoc = builder.parse(is);
            is.close();
            System.out.println("XML config file read correctly from " + url);
        } else {
            configDoc = builder.parse(configFile);
            System.out.println("XML config file read correctly from " + configFile);
        }
    }

    public void setRemoteConfigFile(URL url) {
        this.url = url;
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }
}
