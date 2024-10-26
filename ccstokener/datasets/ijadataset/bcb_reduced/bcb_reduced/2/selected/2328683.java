package org.qtitools.qti.node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.qtitools.qti.exception.QTIParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class encapsulates XML parser.
 * 
 * @author Jiri Kajaba
 * @author Jonathon Hare
 */
public class XmlUtils {

    /** Name of schema language attribute. */
    public static final String ATTRIBUTE_SCHEMA_LANGUAGE_NAME = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    /** Value of schema language attribute. */
    public static final String ATTRIBUTE_SCHEMA_LANGUAGE_VALUE = "http://www.w3.org/2001/XMLSchema";

    /** Name of schema location attribute. */
    public static final String ATTRIBUTE_SCHEMA_SOURCE_NAME = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /** Value of schema location attribute. */
    public static final String ATTRIBUTE_SCHEMA_SOURCE_VALUE = "imsqti_v2p1.xsd";

    /**
	 * Gets document builder factory.
	 *
	 * @param validate whether validate document
	 * @return document builder factory
	 */
    public static DocumentBuilderFactory getFactory(boolean validate) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validate);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        factory.setExpandEntityReferences(true);
        factory.setAttribute(ATTRIBUTE_SCHEMA_LANGUAGE_NAME, ATTRIBUTE_SCHEMA_LANGUAGE_VALUE);
        return factory;
    }

    /**
	 * Gets document builder.
	 *
	 * @param validate whether validate document
	 * @return document builder
	 * @throws QTIParseException if any parsing error occurs
	 */
    public static DocumentBuilder getBuilder(boolean validate) throws QTIParseException {
        try {
            DocumentBuilderFactory factory = getFactory(validate);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException exception) {
                    throw new QTIParseException(exception);
                }

                public void fatalError(SAXParseException exception) {
                    throw new QTIParseException(exception);
                }

                public void warning(SAXParseException exception) {
                    throw new QTIParseException(exception);
                }
            });
            return builder;
        } catch (ParserConfigurationException ex) {
            throw new QTIParseException(ex);
        }
    }

    /**
     * Gets document from given source file.
     * 
     * <strong>MathAssess Change:</strong> This no longer reads the File into a String
     * beforehand as we're not pre-escaping the input like pure JQTI does.
     *
     * @param file source file
     * @param validate whether validate document
     * @return document from given source file
     * @throws QTIParseException if any parsing error occurs
     */
    public static Document getDocument(File file, boolean validate) throws QTIParseException {
        try {
            return getDocument(new InputSource(new FileInputStream(file)), validate, file.getParentFile().toURI().toURL().toString());
        } catch (IOException ex) {
            throw new QTIParseException(ex);
        }
    }

    /**
	 * Gets document from given source string.
	 *
	 * @param string source string
	 * @param validate whether validate document
	 * @return document from given source string
	 * @throws QTIParseException if any parsing error occurs
	 */
    public static Document getDocument(String string, boolean validate) throws QTIParseException {
        return getDocument(new InputSource(new StringReader(string)), validate, null);
    }

    public static Document getDocument(URL url, boolean validate) throws QTIParseException {
        try {
            return getDocument(new InputSource(url.openStream()), validate, null);
        } catch (IOException ex) {
            throw new QTIParseException(ex);
        }
    }

    private static Document getDocument(InputSource inputSource, boolean validate, String systemId) throws QTIParseException {
        try {
            if (systemId != null) {
                inputSource.setSystemId(systemId);
            }
            return getBuilder(validate).parse(inputSource);
        } catch (SAXException ex) {
            throw new QTIParseException(ex);
        } catch (IOException ex) {
            throw new QTIParseException(ex);
        }
    }

    /**
     * Gets first element node from given source url.
     *
     * @param url source url
     * @param validate whether validate document
     * @return first element node from given source url
     * @throws QTIParseException if any parsing error occurs
     */
    public static Node getFirstElementNode(URL url, boolean validate) throws QTIParseException {
        Document document = getDocument(url, validate);
        return getFirstElementNode(document);
    }

    /**
	 * Gets first element node from given source file.
	 *
	 * @param file source file
	 * @param validate whether validate document
	 * @return first element node from given source file
	 * @throws QTIParseException if any parsing error occurs
	 */
    public static Node getFirstElementNode(File file, boolean validate) throws QTIParseException {
        Document document = getDocument(file, validate);
        return getFirstElementNode(document);
    }

    /**
	 * Gets first element node of given source string.
	 *
	 * @param string source string
	 * @param validate whether validate document
	 * @return first element node from given source string
	 * @throws QTIParseException if any parsing error occurs
	 */
    public static Node getFirstElementNode(String string, boolean validate) throws QTIParseException {
        Document document = getDocument(string, validate);
        return getFirstElementNode(document);
    }

    /**
	 * Gets first element node from given source document.
	 *
	 * @param document source document
	 * @return first element node from given source document
	 * @throws QTIParseException if given document does not contain any element node
	 */
    public static Node getFirstElementNode(Document document) throws QTIParseException {
        NodeList nodes = document.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) return node;
        }
        throw new QTIParseException("Cannot find any element node.");
    }
}
