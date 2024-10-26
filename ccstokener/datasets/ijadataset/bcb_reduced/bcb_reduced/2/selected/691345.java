package org.javalid.core.support;

import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.javalid.core.JavalidException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class provides some support for loading xml files.
 * @author  M.Reuvers
 * @version 1.1
 * @since   1.0
 */
public class XMLSupport {

    private XMLSupport() {
    }

    /**
   * Tries to load given filename using getClass().getResource(..). If not
   * found raises JavalidException
   * @param fileName The filename (full path) to load
   * @return w3 Document
   * @exception org.javalid.core.JavalidException If loading of file fails
   */
    public static final Document loadDocument(String fileName) {
        ClassLoader loader = getCurrentClassLoader();
        URL xmlFile = loader.getResource(fileName);
        if (xmlFile == null) xmlFile = loader.getResource("/" + fileName);
        if (xmlFile == null) throw new JavalidException("Did not find " + fileName + " xml file");
        return XMLSupport._loadDocument(xmlFile);
    }

    /**
   * Attempts to load given url. If not found raises JavalidException.
   * @param url The url to load the xml document from
   * @return w3 Document
   * @since 1.2
   */
    public static final Document loadDocument(URL url) {
        return XMLSupport._loadDocument(url);
    }

    /**
   * Method to get a value from an attribute, returns null if not found.
   * @param map The map to use
   * @param attributeName The attributeName to lookup in the map
   * @param defaultValue Set if return value would be null, can be left null if you really want null returned
   * @param required Set to true if the method must return a value, raises JvException if attribute would return null.
   * @return String value if found, null otherwise. If null would be returned and required=true, raises exception.
   */
    public static final String getAttributeValue(NamedNodeMap map, String attributeName, boolean required, String defaultValue) {
        Node attribute = null;
        String value = null;
        attribute = map.getNamedItem(attributeName);
        if (attribute != null) {
            value = attribute.getNodeValue();
        }
        if (value == null && defaultValue != null) {
            value = defaultValue;
        }
        if (required && value == null) {
            throw new JavalidException("Attribute name=" + attributeName + " is required, but null was found ...");
        }
        return value;
    }

    /**
   * Returns the classloader to use, by default the current thread's classloader
   * otherwise the one of this class.
   * @return ClassLoader to use
   */
    private static final ClassLoader getCurrentClassLoader() {
        ClassLoader loader = null;
        loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = XMLSupport.class.getClassLoader();
        }
        return loader;
    }

    private static final Document _loadDocument(URL url) {
        if (url == null) {
            throw new JavalidException("Cannot load document url parameter is null");
        }
        DocumentBuilderFactory factory = null;
        DocumentBuilder builder = null;
        Document document = null;
        factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);
        try {
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new CustomSaxErrorHandler());
            document = builder.parse(url.openConnection().getInputStream());
        } catch (ParserConfigurationException e) {
            throw new JavalidException("Parser error", e);
        } catch (SAXException e) {
            throw new JavalidException("Sax error", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new JavalidException("IO error", e);
        }
        return document;
    }
}
