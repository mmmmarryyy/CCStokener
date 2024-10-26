package org.isi.monet.modelling.views.utils;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class UtilsXML {

    public static Document file2Document(String path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        ClassLoader loader = (UtilsXML.class).getClassLoader();
        URL urlFile = loader.getResource(path);
        Document xmlDoc = factory.newDocumentBuilder().parse(new InputSource(urlFile.openStream()));
        return xmlDoc;
    }

    public static Document inputStream2Document(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document xmlDoc = factory.newDocumentBuilder().parse(input);
        return xmlDoc;
    }

    public static String node2String(Node node) throws Exception {
        StringWriter writer = new StringWriter();
        StreamResult streamResult = new StreamResult(writer);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(node), streamResult);
        return writer.toString();
    }

    public static String nodes2String(NodeList nodes) throws Exception {
        StringBuffer buffer = new StringBuffer();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = (Node) nodes.item(index);
            buffer.append(UtilsXML.node2String(node));
        }
        return buffer.toString();
    }

    public static boolean isNode(Node node, String name) {
        return (isNode(node) && (node.getNodeName().equalsIgnoreCase(name)));
    }

    public static boolean isNode(Node node) {
        return (node.getNodeType() == Node.ELEMENT_NODE);
    }

    public static boolean isText(Node node) {
        return (node.getNodeType() == Node.TEXT_NODE);
    }

    public static String getText(Node node) {
        NodeList childrens = node.getChildNodes();
        Node currentNode = null;
        for (int index = 0; index < childrens.getLength(); index++) {
            currentNode = childrens.item(index);
            if (currentNode == null) return null;
            if ((currentNode.getNodeType() == Node.CDATA_SECTION_NODE) || (currentNode.getNodeType() == Node.TEXT_NODE)) return currentNode.getNodeValue();
        }
        return "";
    }

    public static String getAttribute(String name, Node node) {
        NamedNodeMap map = node.getAttributes();
        String value = null;
        if (map != null) {
            Node nodeAttr = map.getNamedItem(name);
            if (nodeAttr != null) value = nodeAttr.getNodeValue();
        }
        return value;
    }
}
