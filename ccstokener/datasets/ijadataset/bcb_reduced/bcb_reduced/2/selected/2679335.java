package fi.iki.asb.util.config.handler;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import fi.iki.asb.util.config.*;

/**
 * This class handled &lt;include&gt; elements.
 *
 * @author Antti S. Brax
 * @version 1.0
 */
public class ElementHandler_include extends ElementHandler {

    /**
     * Handle a &lt;include&gt; element.
     *
     * @param elem the &lt;include&gt; element
     * @param conf the configuration that is loaded
     */
    public Value handle(Node elem, Map conf) throws XmlConfigException {
        String source = getTextContent(elem, conf);
        URL url = null;
        try {
            url = new URL(source);
        } catch (MalformedURLException e) {
            url = XmlConfig.class.getResource(source);
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        Document doc;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(url.openStream());
        } catch (ParserConfigurationException ex) {
            throw new XmlConfigException("Could not create parser for file " + source, ex);
        } catch (SAXException ex) {
            throw new XmlConfigException("Could not parse file " + source, ex);
        } catch (IOException ex) {
            throw new XmlConfigException("Error while reading file " + source, ex);
        }
        try {
            handleNode(doc.getDocumentElement(), conf);
        } catch (XmlConfigException ex) {
            throw new XmlConfigException("Error while processing file " + source, ex);
        }
        return new Value(conf, Map.class);
    }
}
