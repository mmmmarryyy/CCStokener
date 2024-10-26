package org.plazmaforge.framework.datawarehouse.convert.dataimport.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.plazmaforge.framework.core.exception.ApplicationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtils {

    private static final Logger log = Logger.getLogger(XMLUtils.class);

    /**
     * Parses an input source into a document.
     * 
     * @param is the input source
     * @return the parsed document
     * @throws ApplicationException
     */
    public static Document parse(InputSource is) throws ApplicationException {
        try {
            return createDocumentBuilder().parse(is);
        } catch (SAXException e) {
            throw new ApplicationException("Failed to parse the xml document", e);
        } catch (IOException e) {
            throw new ApplicationException("Failed to parse the xml document", e);
        }
    }

    /**
     * Parses a document specified by an URI.
     * 
     * @param uri the URI
     * @return the parsed document
     * @throws ApplicationException
     */
    public static Document parse(String uri) throws ApplicationException {
        return parse(new InputSource(uri));
    }

    /**
     * Parses a file into a document.
     * 
     * @param file the XML file
     * @return the document
     * @throws ApplicationException
     */
    public static Document parse(File file) throws ApplicationException {
        try {
            return createDocumentBuilder().parse(file);
        } catch (SAXException e) {
            throw new ApplicationException("Failed to parse the xmlf document", e);
        } catch (IOException e) {
            throw new ApplicationException("Failed to parse the xml document", e);
        }
    }

    /**
     * Parses an input stream into a XML document.
     * 
     * @param is the input stream
     * @return the document
     * @throws ApplicationException
     */
    public static Document parse(InputStream is) throws ApplicationException {
        return parse(new InputSource(is));
    }

    /**
     * Parses an URL stream as a XML document.
     * 
     * @param url the URL
     * @return the document
     * @throws ApplicationException
     */
    public static Document parse(URL url) throws ApplicationException {
        InputStream is = null;
        try {
            is = url.openStream();
            return createDocumentBuilder().parse(is);
        } catch (SAXException e) {
            throw new ApplicationException("Failed to parse the xmlf document", e);
        } catch (IOException e) {
            throw new ApplicationException("Failed to parse the xml document", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("Error closing stream of URL " + url, e);
                }
            }
        }
    }

    /**
     * Creates a XML document builder.
     * 
     * @return a XML document builder
     * @throws ApplicationException
     */
    public static DocumentBuilder createDocumentBuilder() throws ApplicationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        try {
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ApplicationException("Failed to create a document builder factory", e);
        }
    }

    /**
     * Creates a document having a node as root.
     * 
     * @param sourceNode the node
     * @return a document having the specified node as root
     * @throws ApplicationException
     */
    public static Document createDocument(Node sourceNode) throws ApplicationException {
        Document doc = createDocumentBuilder().newDocument();
        Node source;
        if (sourceNode.getNodeType() == Node.DOCUMENT_NODE) {
            source = ((Document) sourceNode).getDocumentElement();
        } else {
            source = sourceNode;
        }
        Node node = doc.importNode(source, true);
        doc.appendChild(node);
        return doc;
    }

    public static boolean isPlatformXMLFormat(String fileName) throws ApplicationException {
        File file = new File(fileName);
        return isPlatformXMLFormat(file);
    }

    public static boolean isPlatformXMLFormat(File file) throws ApplicationException {
        String version = getPlatformXMLVersion(file);
        return version != null;
    }

    public static String getPlatformXMLVersion(File file) throws ApplicationException {
        InputSource in = new InputSource(file.toURI().toASCIIString());
        Document document = parse(in);
        Element root = document.getDocumentElement();
        if (root == null) {
            return null;
        }
        String rootName = root.getNodeName();
        if (!"dataset".equals(rootName)) {
            return null;
        }
        NamedNodeMap attributes = root.getAttributes();
        if (attributes == null) {
            return null;
        }
        Node providerNode = attributes.getNamedItem("provider");
        Node versionNode = attributes.getNamedItem("version");
        if (providerNode == null || versionNode == null) {
            return null;
        }
        String provider = providerNode.getNodeValue();
        String version = versionNode.getNodeValue();
        if (!"Plazma Forge".equals(provider)) {
            return null;
        }
        return version;
    }
}
