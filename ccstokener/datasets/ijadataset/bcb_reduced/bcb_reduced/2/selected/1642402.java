package com.sparkit.extracta;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.xerces.dom.*;
import org.apache.xerces.parsers.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * This is a utility class for various operations on a DOM document implementation.
 *
 * @version 1.0
 * @author Bostjan Vester
 * @author Dejan Pazin
 * @author Dominik Roblek
 */
public class DomHelper {

    private DomHelper() {
    }

    /**
   * This creates a new DOM document.
   */
    public static Document createEmptyDocument() {
        return new DocumentImpl();
    }

    /**
   * Sets whether the DOM implementation performs error checking
   * upon operations. It only works on org.apache.xerces.dom.DocumentImpl
   * document implementation. It does nothing on other implementations.
   *
   * @param document the document subject to error checking.
   * @param bCheck the status of error checking.
   * @return the previous status of error checking of the document.
   */
    public static boolean setErrorChecking(Document document, boolean bCheck) {
        if (document instanceof DocumentImpl) {
            DocumentImpl documentImpl = (DocumentImpl) document;
            boolean bOldCheck = documentImpl.getErrorChecking();
            documentImpl.setErrorChecking(bCheck);
            return bOldCheck;
        } else {
            return false;
        }
    }

    /**
   * This parses the XML source as defined in the specified URL string.
   */
    public static Document parseXml(String sUrl) throws ExtractaException {
        try {
            return parseXml(new URL(sUrl));
        } catch (MalformedURLException ex) {
            throw new ExtractaException("Malformed URL.", ex);
        }
    }

    /**
   * This parses the XML source as defined in the specified URL.
   */
    public static Document parseXml(URL url) throws ExtractaException {
        try {
            return parseXml(new BufferedInputStream(url.openStream()));
        } catch (IOException ex) {
            throw new ExtractaException("Error opening URL.", ex);
        }
    }

    /**
   * This parses the XML source as defined in the specified input stream.
   */
    public static Document parseXml(InputStream input) throws ExtractaException {
        try {
            InputSource inputSource = new InputSource(input);
            DOMParser parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
            parser.parse(inputSource);
            return parser.getDocument();
        } catch (IOException ex) {
            throw new ExtractaException("Error reading from input.", ex);
        } catch (SAXException ex) {
            throw new ExtractaException("Error parsing document.", ex);
        }
    }

    /**
   * This parses the HTML source as defined in the specified URL string.
   */
    public static Document parseHtml(String sUrl) throws ExtractaException {
        try {
            return parseHtml(new URL(sUrl));
        } catch (MalformedURLException ex) {
            throw new ExtractaException("Malformed URL.", ex);
        }
    }

    /**
   * This parses the HTML source as defined in the specified URL.
   */
    public static Document parseHtml(URL url) throws ExtractaException {
        try {
            return parseHtml(new BufferedInputStream(url.openStream()));
        } catch (IOException ex) {
            throw new ExtractaException("Error opening URL.", ex);
        }
    }

    /**
   * This parses the HTML source as defined in the specified input stream.
   */
    public static Document parseHtml(InputStream input) throws ExtractaException {
        try {
            Document tidyDocument = ExtractaHelper.createTidy().parseDOM(input, null);
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            try {
                OutputFormat outputFormat = new OutputFormat();
                XMLSerializer xmlSerializer = new XMLSerializer(outputBuffer, outputFormat);
                xmlSerializer.serialize(tidyDocument);
            } finally {
                outputBuffer.close();
            }
            ByteArrayInputStream inputBuffer = new ByteArrayInputStream(outputBuffer.toByteArray());
            try {
                InputSource inputSource = new InputSource(inputBuffer);
                DOMParser parser = new DOMParser();
                parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
                parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
                parser.parse(inputSource);
                return parser.getDocument();
            } finally {
                inputBuffer.close();
            }
        } catch (IOException ex) {
            throw new ExtractaInternalException(ex);
        } catch (SAXException ex) {
            throw new ExtractaException("Error parsing document.", ex);
        }
    }

    /**
   * Prints the specified XML element onto the output stream.
   */
    public static void printXml(Element element, OutputStream output) throws IOException {
        internalPrintXml(element, output);
    }

    /**
   * Prints the specified XML element onto the writer.
   */
    public static void printXml(Element element, Writer writer) throws IOException {
        internalPrintXml(element, writer);
    }

    /**
   * Prints the specified XML document fragment onto the output stream.
   */
    public static void printXml(DocumentFragment documentFragment, OutputStream output) throws IOException {
        internalPrintXml(documentFragment, output);
    }

    /**
   * Prints the specified XML document fragment onto the writer.
   */
    public static void printXml(DocumentFragment documentFragment, Writer writer) throws IOException {
        internalPrintXml(documentFragment, writer);
    }

    /**
   * Prints the specified XML document onto the output stream.
   */
    public static void printXml(Document document, OutputStream output) throws IOException {
        internalPrintXml(document, output);
    }

    /**
   * Prints the specified XML document onto the writer.
   */
    public static void printXml(Document document, Writer writer) throws IOException {
        internalPrintXml(document, writer);
    }

    /**
   * Prints the specified HTML element onto the output stream.
   */
    public static void printHtml(Element element, OutputStream output) throws IOException {
        internalPrintHtml(element, output);
    }

    /**
   * Prints the specified HTML element onto the writer.
   */
    public static void printHtml(Element element, Writer writer) throws IOException {
        internalPrintHtml(element, writer);
    }

    /**
   * Prints the specified HTML document fragment onto the output stream.
   */
    public static void printHtml(DocumentFragment documentFragment, OutputStream output) throws IOException {
        internalPrintHtml(documentFragment, output);
    }

    /**
   * Prints the specified HTML document fragment onto the writer.
   */
    public static void printHtml(DocumentFragment documentFragment, Writer writer) throws IOException {
        internalPrintHtml(documentFragment, writer);
    }

    /**
   * Prints the specified HTML document onto the output stream.
   */
    public static void printHtml(Document document, OutputStream output) throws IOException {
        internalPrintHtml(document, output);
    }

    /**
   * Prints the specified HTML document onto the writer.
   */
    public static void printHtml(Document document, Writer writer) throws IOException {
        internalPrintHtml(document, writer);
    }

    private static void internalPrintXml(Object content, Object output) throws IOException {
        OutputFormat outputFormat = new OutputFormat();
        outputFormat.setIndenting(true);
        outputFormat.setIndent(2);
        XMLSerializer serializer = new XMLSerializer(outputFormat);
        internalPrintDom(serializer, content, output);
    }

    private static void internalPrintHtml(Object content, Object output) throws IOException {
        OutputFormat outputFormat = new OutputFormat();
        HTMLSerializer serializer = new HTMLSerializer(outputFormat);
        internalPrintDom(serializer, content, output);
    }

    private static void internalPrintDom(BaseMarkupSerializer serializer, Object content, Object output) throws IOException {
        if (output instanceof OutputStream) {
            serializer.setOutputByteStream((OutputStream) output);
        } else if (output instanceof Writer) {
            serializer.setOutputCharStream((Writer) output);
        } else {
            throw new ExtractaInternalException("Output is invalid");
        }
        if (content instanceof Element) {
            serializer.serialize((Element) content);
        } else if (content instanceof DocumentFragment) {
            serializer.serialize((DocumentFragment) content);
        } else if (content instanceof Document) {
            serializer.serialize((Document) content);
        } else {
            throw new ExtractaInternalException("Content is invalid");
        }
    }

    /**
   * This returns a text representation of a node.
   */
    public static String getString(Node node) {
        if (node == null) {
            return "";
        }
        if (node instanceof Text) {
            String sText = node.getNodeValue();
            return (sText != null) ? sText : "";
        } else {
            StringBuffer buffer = new StringBuffer();
            NodeList list = node.getChildNodes();
            if (list != null) {
                int nLength = list.getLength();
                for (int i = 0; i < nLength; i++) {
                    buffer.append(getString(list.item(i)));
                }
                return buffer.toString();
            }
            return "";
        }
    }

    /**
   * This returns a first child node with a specified name from node (null if nonexistent).
   */
    public static Node getChild(Node node, String sChildName) {
        java.util.List children = getChildren(node, sChildName);
        return (children.size() > 0) ? (Node) children.get(0) : null;
    }

    /**
   * This returns a first child node with a specified name from NodeList (null if nonexistent).
   */
    public static Node getChild(NodeList list, String sChildName) {
        java.util.List children = getChildren(list, sChildName);
        return (children.size() > 0) ? (Node) children.get(0) : null;
    }

    /**
   * This returns a java.util.List of child nodes with a specified name from node.
   */
    public static java.util.List getChildren(Node node, String sChildName) {
        return getChildren(node.getChildNodes(), sChildName);
    }

    /**
   * This returns a java.util.List of child nodes with a specified name from NodeList.
   */
    public static java.util.List getChildren(NodeList nodeList, String sChildName) {
        java.util.List list = new ArrayList();
        if ((nodeList != null) && (sChildName != null)) {
            int nListSize = nodeList.getLength();
            for (int i = 0; i < nListSize; i++) {
                Node child = nodeList.item(i);
                if (child.getNodeName().equalsIgnoreCase(sChildName)) {
                    list.add(child);
                }
            }
        }
        return list;
    }

    /**
   * This returns an attribute with a specified name from node (null if nonexistent).
   */
    public static Attr getAttribute(Node node, String sAttributeName) {
        if ((node != null) && (sAttributeName != null)) {
            return (Attr) getNode(node.getAttributes(), sAttributeName);
        }
        return null;
    }

    /**
   * This returns an attribute with a specified name from node (a new attribute with
   * a default value if nonexistent).
   */
    public static Attr getAttribute(Node node, String sAttributeName, String sDefaultValue) {
        Attr attr = getAttribute(node, sAttributeName);
        if (attr == null) {
            attr = node.getOwnerDocument().createAttribute(sAttributeName);
            attr.setValue(sDefaultValue);
        }
        return attr;
    }

    /**
   * This returns a node with a specified name from NamedNodeMap (null if nonexistent).
   */
    public static Node getNode(NamedNodeMap map, String sNodeName) {
        if ((map != null) && (sNodeName != null)) {
            return map.getNamedItem(sNodeName);
        }
        return null;
    }

    /**
   * This returns a list of last children of a node. A last child of a node is defined as follows:
   * if a node has exactly one child, a last child of this node is a node's child's last child.
   * Otherwise a node itself is a last child. Simply, this method reduces a single-child branch
   * of a tree in a single child: A - B - C thus becomes C.
   */
    public static List getLastChildren(Node node) {
        return getLastChildren(node, null);
    }

    /**
   * This returns a list of last children of a node with specific name.
   * A last child of a node is defined as follows:
   * if a node has exactly one child, a last child of this node is a node's child's last child.
   * Otherwise a node itself is a last child. Simply, this method reduces a single-child branch
   * of a tree in a single child: A - B - C thus becomes C.
   */
    public static List getLastChildren(Node node, String sNodeName) {
        List list = new ArrayList();
        NodeList nodeList = node.getChildNodes();
        int nChildCount = nodeList.getLength();
        for (int i = 0; i < nChildCount; i++) {
            Node endChild = getLastSingleChild(nodeList.item(i));
            if ((sNodeName == null) || endChild.getNodeName().equalsIgnoreCase(sNodeName)) {
                list.add(endChild);
            }
        }
        return list;
    }

    /**
   * This returns a last child of a node. A last child of a node is defined as follows:
   * if a node has exactly one child, a last child of this node is a node's child's last child.
   * Otherwise a node itself is a last child. Simply, this method reduces a single-child branch
   * of a tree in a single child: A - B - C thus becomes C.
   */
    public static Node getLastSingleChild(Node node) {
        return getLastSingleChild(node, null);
    }

    /**
   * This returns a last child of a node with specified name. A last child of a node is defined as follows:
   * if a node has exactly one child, a last child of this node is a node's child's last child.
   * Otherwise a node itself is a last child. Simply, this method reduces a single-child branch
   * of a tree in a single child: A - B - C thus becomes C.
   */
    public static Node getLastSingleChild(Node node, String sNodeName) {
        NodeList children = node.getChildNodes();
        if ((children.getLength() == 0) || (children.getLength() > 1)) {
            if (((sNodeName != null) && node.getNodeName().equalsIgnoreCase(sNodeName)) || (sNodeName == null)) {
                return node;
            } else {
                return null;
            }
        }
        Node nextEquivalentNode = getLastSingleChild(children.item(0), sNodeName);
        return (nextEquivalentNode != null) ? nextEquivalentNode : node;
    }
}
