package uk.ac.lkl.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.lkl.common.util.restlet.RuntimeRestletException;
import uk.ac.lkl.migen.system.util.URLLocalFileCache;

public class JREXMLUtilities {

    public static void printDocument(Document document, OutputStream outputStream) {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer serializer;
        try {
            serializer = factory.newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            serializer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (TransformerException e) {
            System.out.println(e);
        }
    }

    public static void printDocument(Document document) {
        printDocument(document, System.out);
    }

    public static void printElement(Element element, OutputStream outputStream) throws ParserConfigurationException {
        Document document = createDocument();
        Node importedNode = document.importNode(element, true);
        document.appendChild(importedNode);
        printDocument(document, outputStream);
    }

    public static void printElement(Element element) throws ParserConfigurationException {
        printElement(element, System.out);
    }

    public static String nodeToString(Node node) {
        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        return document;
    }

    public static Document createDocument(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    /**
     * @param urlString -- URL to an XML file
     * @return the Document that is the parsing of the contents of the urlString
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws Exception 
     */
    public static Document createDocument(String urlString) throws IOException, SAXException, ParserConfigurationException {
        InputStream inputStream = getInputStreamOfURL(urlString);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(inputStream);
    }

    /**
     * @param urlString - any URL
     * @return - InputStream to that URL (or its local cached equivalent) or null if none exist
     * @throws IOException
     */
    public static InputStream getInputStreamOfURL(String urlString) throws IOException {
        InputStream inputStream;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            inputStream = URLLocalFileCache.getInputStream(urlString);
            if (inputStream == null) {
                throw e;
            }
        }
        return inputStream;
    }

    public static Document createDocumentFromString(String xml) throws ParserConfigurationException, SAXException, IOException {
        StringReader in = new StringReader(xml);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(new InputSource(in));
    }

    public static Element getChildWithTagName(Node node, String tagName) {
        ArrayList<String> tagNames = new ArrayList<String>();
        tagNames.add(tagName);
        return JREXMLUtilities.getChildWithTagNames(node, tagNames);
    }

    public static Element getChildWithTagNames(Node node, List<String> tagNames) {
        NodeList childNodes = node.getChildNodes();
        int size = childNodes.getLength();
        for (int i = 0; i < size; i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                if (tagNames.contains(childElement.getTagName())) {
                    return childElement;
                }
            }
        }
        return null;
    }

    public static Element getNextChildWithTagName(Element element, String tagName, Node previousNode) {
        ArrayList<String> tagNames = new ArrayList<String>();
        tagNames.add(tagName);
        return getNextChildWithTagNames(element, tagNames, previousNode);
    }

    public static Element getNextChildWithTagNames(Element element, List<String> tagNames, Node previousNode) {
        Node nextSibling = previousNode.getNextSibling();
        while (nextSibling != null) {
            if (nextSibling instanceof Element) {
                Element nextSiblingElement = (Element) nextSibling;
                if (tagNames.contains(nextSiblingElement.getTagName())) {
                    return nextSiblingElement;
                }
            }
            nextSibling = nextSibling.getNextSibling();
        }
        return null;
    }

    /**
     * @param element
     * @return The contents of the first CDATA child node or null if there are
     *         none.
     */
    public static String getFirstCData(Element element) {
        NodeList childNodes = element.getChildNodes();
        int size = childNodes.getLength();
        for (int i = 0; i < size; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                return child.getNodeValue();
            }
        }
        return null;
    }

    public static ArrayList<String> getAllCData(Element element) {
        ArrayList<String> result = new ArrayList<String>();
        NodeList childNodes = element.getChildNodes();
        int size = childNodes.getLength();
        for (int i = 0; i < size; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                result.add(child.getNodeValue());
            }
        }
        return result;
    }

    public static Integer getIntegerAttribute(Element element, String name) {
        String valueString = element.getAttribute(name);
        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean getBooleanAttribute(Element element, String name) {
        String valueString = element.getAttribute(name);
        if (valueString.equals("true")) {
            return true;
        } else if (valueString.equals("false")) {
            return false;
        } else {
            throw new RuntimeRestletException("Expected attribute value to be true or false " + valueString);
        }
    }

    public static Boolean getBooleanAttribute(Element element, String name, Boolean defaultValue) {
        String valueString = element.getAttribute(name);
        if (valueString.equals("true")) {
            return true;
        } else if (valueString.equals("false")) {
            return false;
        } else {
            return defaultValue;
        }
    }
}
