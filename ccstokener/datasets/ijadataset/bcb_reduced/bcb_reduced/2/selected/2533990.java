package org.gingkgo.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javolution.util.FastList;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.gingkgo.core.GingkgoException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utilities methods to simplify dealing with JAXP & DOM XML parsing
 *
 * @author <a herf="mailto:zhangsitao@gmail.com">Steven Zhang</a>
 * @version $Revision: 1.1 $ $Date: 2007/05/17 05:20:46 $
 */
public class XmlHelper {

    public static final String module = XmlHelper.class.getName();

    public static String writeXmlDocument(Document document) throws java.io.IOException {
        if (document == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Document was null, doing nothing", module);
            return null;
        }
        return writeXmlDocument(document.getDocumentElement());
    }

    public static String writeXmlDocument(Element element) throws java.io.IOException {
        if (element == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Element was null, doing nothing", module);
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeXmlDocument(bos, element);
        String outString = bos.toString("UTF-8");
        if (bos != null) bos.close();
        return outString;
    }

    public static void writeXmlDocument(String filename, Document document) throws java.io.FileNotFoundException, java.io.IOException {
        if (document == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Document was null, doing nothing", module);
            return;
        }
        writeXmlDocument(filename, document.getDocumentElement());
    }

    public static void writeXmlDocument(String filename, Element element) throws java.io.FileNotFoundException, java.io.IOException {
        if (element == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Element was null, doing nothing", module);
            return;
        }
        if (filename == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Filename was null, doing nothing", module);
            return;
        }
        File outFile = new File(filename);
        FileOutputStream fos = null;
        fos = new FileOutputStream(outFile);
        try {
            writeXmlDocument(fos, element);
        } finally {
            if (fos != null) fos.close();
        }
    }

    public static void writeXmlDocument(OutputStream os, Document document) throws java.io.IOException {
        if (document == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Document was null, doing nothing", module);
            return;
        }
        writeXmlDocument(os, document.getDocumentElement());
    }

    public static void writeXmlDocument(OutputStream os, Element element) throws java.io.IOException {
        if (element == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] Element was null, doing nothing", module);
            return;
        }
        if (os == null) {
            Debug.logWarning("[UtilXml.writeXmlDocument] OutputStream was null, doing nothing", module);
            return;
        }
        OutputFormat format = new OutputFormat(element.getOwnerDocument());
        format.setIndent(2);
        XMLSerializer serializer = new XMLSerializer(os, format);
        serializer.asDOMSerializer();
        serializer.serialize(element);
    }

    public static Document readXmlDocument(String content) throws SAXException, ParserConfigurationException, java.io.IOException {
        return readXmlDocument(content, true);
    }

    public static Document readXmlDocument(String content, boolean validate) throws SAXException, ParserConfigurationException, java.io.IOException {
        if (content == null) {
            Debug.logWarning("[UtilXml.readXmlDocument] content was null, doing nothing", module);
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes("UTF-8"));
        return readXmlDocument(bis, validate, "Internal Content");
    }

    public static Document readXmlDocument(URL url) throws SAXException, ParserConfigurationException, java.io.IOException {
        return readXmlDocument(url, true);
    }

    public static Document readXmlDocument(URL url, boolean validate) throws SAXException, ParserConfigurationException, java.io.IOException {
        if (url == null) {
            Debug.logWarning("[UtilXml.readXmlDocument] URL was null, doing nothing", module);
            return null;
        }
        return readXmlDocument(url.openStream(), validate, url.toString());
    }

    /**
     * @deprecated
     */
    public static Document readXmlDocument(InputStream is) throws SAXException, ParserConfigurationException, java.io.IOException {
        return readXmlDocument(is, true, null);
    }

    public static Document readXmlDocument(InputStream is, String docDescription) throws SAXException, ParserConfigurationException, java.io.IOException {
        return readXmlDocument(is, true, docDescription);
    }

    public static Document readXmlDocument(InputStream is, boolean validate, String docDescription) throws SAXException, ParserConfigurationException, java.io.IOException {
        if (is == null) {
            Debug.logWarning("[UtilXml.readXmlDocument] InputStream was null, doing nothing", module);
            return null;
        }
        long startTime = System.currentTimeMillis();
        Document document = null;
        DocumentBuilderFactory factory = new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
        factory.setValidating(validate);
        factory.setNamespaceAware(true);
        factory.setAttribute("http://xml.org/sax/features/validation", Boolean.TRUE);
        factory.setAttribute("http://apache.org/xml/features/validation/schema", Boolean.TRUE);
        DocumentBuilder builder = factory.newDocumentBuilder();
        if (validate) {
            LocalResolver lr = new LocalResolver(new DefaultHandler());
            ErrorHandler eh = new LocalErrorHandler(docDescription, lr);
            builder.setEntityResolver(lr);
            builder.setErrorHandler(eh);
        }
        document = builder.parse(is);
        double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        if (Debug.timingOn()) Debug.logTiming("XML Read " + totalSeconds + "s: " + docDescription, module);
        return document;
    }

    public static Document makeEmptyXmlDocument() {
        return makeEmptyXmlDocument(null);
    }

    public static Document makeEmptyXmlDocument(String rootElementName) {
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
        } catch (Exception e) {
            Debug.logError(e, module);
        }
        if (document == null) return null;
        if (rootElementName != null) {
            Element rootElement = document.createElement(rootElementName);
            document.appendChild(rootElement);
        }
        return document;
    }

    /** Creates a child element with the given name and appends it to the element child node list. */
    public static Element addChildElement(Element element, String childElementName, Document document) {
        Element newElement = document.createElement(childElementName);
        element.appendChild(newElement);
        return newElement;
    }

    /** Creates a child element with the given name and appends it to the element child node list.
     *  Also creates a Text node with the given value and appends it to the new elements child node list.
     */
    public static Element addChildElementValue(Element element, String childElementName, String childElementValue, Document document) {
        Element newElement = addChildElement(element, childElementName, document);
        newElement.appendChild(document.createTextNode(childElementValue));
        return newElement;
    }

    /** Creates a child element with the given name and appends it to the element child node list.
     *  Also creates a CDATASection node with the given value and appends it to the new elements child node list.
     */
    public static Element addChildElementCDATAValue(Element element, String childElementName, String childElementValue, Document document) {
        Element newElement = addChildElement(element, childElementName, document);
        newElement.appendChild(document.createCDATASection(childElementValue));
        return newElement;
    }

    /** Return a List of Element objects that are children of the given element */
    public static List childElementList(Element element) {
        if (element == null) return null;
        List<Element> elements = FastList.newInstance();
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) node;
                    elements.add(childElement);
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return elements;
    }

    /** Return a List of Element objects that have the given name and are
     * immediate children of the given element; if name is null, all child
     * elements will be included. */
    public static List childElementList(Element element, String childElementName) {
        if (element == null) return null;
        List elements = FastList.newInstance();
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE && (childElementName == null || childElementName.equals(node.getNodeName()))) {
                    Element childElement = (Element) node;
                    elements.add(childElement);
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return elements;
    }

    /** Return a List of Element objects that have the given name and are
     * immediate children of the given element; if name is null, all child
     * elements will be included. */
    public static List childElementList(Element element, Set childElementNames) {
        if (element == null) return null;
        List<Element> elements = FastList.newInstance();
        if (childElementNames == null) return elements;
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE && childElementNames.contains(node.getNodeName())) {
                    Element childElement = (Element) node;
                    elements.add(childElement);
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return elements;
    }

    /** Return the first child Element
     * returns the first element. */
    public static Element firstChildElement(Element element, Set childElementNames) {
        if (element == null) return null;
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE && childElementNames.contains(node.getNodeName())) {
                    Element childElement = (Element) node;
                    return childElement;
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return null;
    }

    /** Return the first child Element
     * returns the first element. */
    public static Element firstChildElement(Element element) {
        if (element == null) return null;
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) node;
                    return childElement;
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return null;
    }

    /** Return the first child Element with the given name; if name is null
     * returns the first element. */
    public static Element firstChildElement(Element element, String childElementName) {
        if (element == null) return null;
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE && (childElementName == null || childElementName.equals(node.getNodeName()))) {
                    Element childElement = (Element) node;
                    return childElement;
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return null;
    }

    /** Return the first child Element with the given name; if name is null
     * returns the first element. */
    public static Element firstChildElement(Element element, String childElementName, String attrName, String attrValue) {
        if (element == null) return null;
        Node node = element.getFirstChild();
        if (node != null) {
            do {
                if (node.getNodeType() == Node.ELEMENT_NODE && (childElementName == null || childElementName.equals(node.getNodeName()))) {
                    Element childElement = (Element) node;
                    String value = childElement.getAttribute(attrName);
                    if (value != null && value.equals(attrValue)) {
                        return childElement;
                    }
                }
            } while ((node = node.getNextSibling()) != null);
        }
        return null;
    }

    /** Return the text (node value) contained by the named child node. */
    public static String childElementValue(Element element, String childElementName) {
        if (element == null) return null;
        Element childElement = firstChildElement(element, childElementName);
        return elementValue(childElement);
    }

    /** Return the text (node value) contained by the named child node or a default value if null. */
    public static String childElementValue(Element element, String childElementName, String defaultValue) {
        if (element == null) return defaultValue;
        Element childElement = firstChildElement(element, childElementName);
        String elementValue = elementValue(childElement);
        if (elementValue == null || elementValue.length() == 0) return defaultValue; else return elementValue;
    }

    /** Return the text (node value) of the first node under this, works best if normalized. */
    public static String elementValue(Element element) {
        if (element == null) return null;
        element.normalize();
        Node textNode = element.getFirstChild();
        if (textNode == null) return null;
        StringBuffer valueBuffer = new StringBuffer();
        do {
            if (textNode.getNodeType() == Node.CDATA_SECTION_NODE || textNode.getNodeType() == Node.TEXT_NODE) {
                valueBuffer.append(textNode.getNodeValue());
            }
        } while ((textNode = textNode.getNextSibling()) != null);
        return valueBuffer.toString();
    }

    public static String checkEmpty(String string) {
        if (string != null && string.length() > 0) return string; else return "";
    }

    public static String checkEmpty(String string1, String string2) {
        if (string1 != null && string1.length() > 0) return string1; else if (string2 != null && string2.length() > 0) return string2; else return "";
    }

    public static String checkEmpty(String string1, String string2, String string3) {
        if (string1 != null && string1.length() > 0) return string1; else if (string2 != null && string2.length() > 0) return string2; else if (string3 != null && string3.length() > 0) return string3; else return "";
    }

    public static boolean checkBoolean(String str) {
        return checkBoolean(str, false);
    }

    public static boolean checkBoolean(String str, boolean defaultValue) {
        if (defaultValue) {
            return !"false".equals(str);
        } else {
            return "true".equals(str);
        }
    }

    /**
     * Local entity resolver to handle J2EE DTDs. With this a http connection
     * to sun is not needed during deployment.
     * Function boolean hadDTD() is here to avoid validation errors in
     * descriptors that do not have a DOCTYPE declaration.
     */
    public static class LocalResolver implements EntityResolver {

        private boolean hasDTD = false;

        private EntityResolver defaultResolver;

        public LocalResolver(EntityResolver defaultResolver) {
            this.defaultResolver = defaultResolver;
        }

        /**
         * Returns DTD inputSource. If DTD was found in the dtds Map and inputSource was created
         * flag hasDTD is set to true.
         * @param publicId - Public ID of DTD
         * @param systemId - System ID of DTD
         * @return InputSource of DTD
         */
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            Debug.logInfo("resolving XML entity with publicId [" + publicId + "], systemId [" + systemId + "]", module);
            hasDTD = false;
            String dtd = UtilProperties.getSplitPropertyValue(URLHelper.fromResource("localdtds.properties"), publicId);
            if (ValidateHelper.isNotEmpty(dtd)) {
                if (Debug.verboseOn()) Debug.logVerbose("[UtilXml.LocalResolver.resolveEntity] resolving DTD with publicId [" + publicId + "], systemId [" + systemId + "] and the dtd file is [" + dtd + "]", module);
                try {
                    URL dtdURL = URLHelper.fromResource(dtd);
                    if (dtdURL == null) {
                        throw new GingkgoException("Local DTD not found - " + dtd);
                    }
                    InputStream dtdStream = dtdURL.openStream();
                    InputSource inputSource = new InputSource(dtdStream);
                    inputSource.setPublicId(publicId);
                    hasDTD = true;
                    if (Debug.verboseOn()) Debug.logVerbose("[UtilXml.LocalResolver.resolveEntity] got LOCAL DTD input source with publicId [" + publicId + "] and the dtd file is [" + dtd + "]", module);
                    return inputSource;
                } catch (Exception e) {
                    Debug.logWarning(e, module);
                }
            } else {
                int lastSlash = systemId.lastIndexOf("/");
                String filename = null;
                if (lastSlash == -1) {
                    filename = systemId;
                } else {
                    filename = systemId.substring(lastSlash + 1);
                }
                URL resourceUrl = URLHelper.fromResource(filename);
                if (resourceUrl != null) {
                    InputStream resStream = resourceUrl.openStream();
                    InputSource inputSource = new InputSource(resStream);
                    if (ValidateHelper.isNotEmpty(publicId)) {
                        inputSource.setPublicId(publicId);
                    }
                    hasDTD = true;
                    if (Debug.verboseOn()) Debug.logVerbose("[UtilXml.LocalResolver.resolveEntity] got LOCAL DTD/Schema input source with publicId [" + publicId + "] and the file/resource is [" + filename + "]", module);
                    return inputSource;
                } else {
                    Debug.logWarning("[UtilXml.LocalResolver.resolveEntity] could not find LOCAL DTD/Schema with publicId [" + publicId + "] and the file/resource is [" + filename + "]", module);
                    return null;
                }
            }
            Debug.logInfo("[UtilXml.LocalResolver.resolveEntity] local resolve failed for DTD with publicId [" + publicId + "] and the dtd file is [" + dtd + "], trying defaultResolver", module);
            return defaultResolver.resolveEntity(publicId, systemId);
        }

        /**
         * Returns the boolean value to inform id DTD was found in the XML file or not
         * @return boolean - true if DTD was found in XML
         */
        public boolean hasDTD() {
            return hasDTD;
        }
    }

    /** Local error handler for entity resolver to DocumentBuilder parser.
     * Error is printed to output just if DTD was detected in the XML file.
     */
    public static class LocalErrorHandler implements ErrorHandler {

        private String docDescription;

        private LocalResolver localResolver;

        public LocalErrorHandler(String docDescription, LocalResolver localResolver) {
            this.docDescription = docDescription;
            this.localResolver = localResolver;
        }

        public void error(SAXParseException exception) {
            if (localResolver.hasDTD()) {
                Debug.logError("XmlFileLoader: File " + docDescription + " process error. Line: " + String.valueOf(exception.getLineNumber()) + ". Error message: " + exception.getMessage(), module);
            }
        }

        public void fatalError(SAXParseException exception) {
            if (localResolver.hasDTD()) {
                Debug.logError("XmlFileLoader: File " + docDescription + " process fatal error. Line: " + String.valueOf(exception.getLineNumber()) + ". Error message: " + exception.getMessage(), module);
            }
        }

        public void warning(SAXParseException exception) {
            if (localResolver.hasDTD()) {
                Debug.logError("XmlFileLoader: File " + docDescription + " process warning. Line: " + String.valueOf(exception.getLineNumber()) + ". Error message: " + exception.getMessage(), module);
            }
        }
    }
}
