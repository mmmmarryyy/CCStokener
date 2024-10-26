package cn.lzh.common.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author <a href="mailto:sealinglip@gmail.com">Sealinglip </a> ����XML�Ĺ�����
 */
public class XmlUtils {

    private static class XmlPrinter {

        private static boolean lastIsAString = false;

        /**
		 * �������ո����ĸ��ո�Ϊ��λ
		 * 
		 * @param space
		 * @return ����ʱ�䣺2004-10-16 16:09:21
		 */
        private static String getSpace(int space) {
            char ca[] = new char[space * 4];
            for (int i = 0; i < ca.length; i += 4) {
                ca[i] = ' ';
                ca[i + 1] = ' ';
                ca[i + 2] = ' ';
                ca[i + 3] = ' ';
            }
            return new String(ca);
        }

        /**
		 * ��xml�ڵ㰴��xml���ļ���ʽ����ķ���
		 * 
		 * @param writer
		 *            �����
		 * @param node
		 *            xml�ڵ�
		 * @param deepSet
		 *            �ڵ㼶�Σ�һ��Ϊ0�� ����ʱ�䣺2004-10-16 16:10:07
		 */
        public static void printDOMTree(Writer writer, Node node, int deepSet) {
            String encoding = "GBK";
            printDOMTree(writer, node, deepSet, encoding);
        }

        public static void printDOMTree(Writer writer, Node node, int deepSet, String encoding) {
            int type = node.getNodeType();
            PrintWriter pw = null;
            if (writer instanceof OutputStreamWriter) {
                pw = new PrintWriter(writer);
            } else if (writer instanceof PrintWriter) {
                pw = (PrintWriter) writer;
            } else {
                throw new IllegalArgumentException("Illegal writer to print dom tree.");
            }
            switch(type) {
                case Node.DOCUMENT_NODE:
                    {
                        pw.print("<?xml version=\"1.0\" encoding='" + encoding + "'?>");
                        printDOMTree(pw, ((Document) node).getDocumentElement(), deepSet, encoding);
                        pw.println();
                        break;
                    }
                case Node.ELEMENT_NODE:
                    {
                        pw.println();
                        pw.print(getSpace(deepSet) + "<");
                        pw.print(node.getNodeName());
                        NamedNodeMap attrs = node.getAttributes();
                        for (int i = 0; i < attrs.getLength(); i++) {
                            Node attr = attrs.item(i);
                            pw.print(" " + attr.getNodeName() + "=\"" + getXMLString(attr.getNodeValue()) + "\"");
                            if (null == attr.getNodeValue() || attr.getNodeValue().equals("null")) lastIsAString = true;
                        }
                        pw.print(">");
                        NodeList children = node.getChildNodes();
                        if (children != null) {
                            int len = children.getLength();
                            for (int i = 0; i < len; i++) printDOMTree(pw, children.item(i), deepSet + 1, encoding);
                        }
                        break;
                    }
                case Node.ENTITY_REFERENCE_NODE:
                    {
                        pw.print("&");
                        pw.print(node.getNodeName());
                        pw.print(";");
                        break;
                    }
                case Node.CDATA_SECTION_NODE:
                    {
                        pw.print(getSpace(deepSet) + "<![CDATA[");
                        pw.print(node.getNodeValue());
                        pw.print("]]>");
                        break;
                    }
                case Node.TEXT_NODE:
                    {
                        String value = node.getNodeValue();
                        if (value != null) {
                            value = value.trim();
                            value = getXMLString(value);
                            pw.print(value);
                        }
                        lastIsAString = !"".equals(value);
                        break;
                    }
                case Node.PROCESSING_INSTRUCTION_NODE:
                    {
                        pw.print(getSpace(deepSet) + "<?");
                        pw.print(node.getNodeName());
                        String data = node.getNodeValue();
                        {
                            pw.print("");
                            pw.print(data);
                        }
                        pw.print("?>");
                        break;
                    }
                case Node.COMMENT_NODE:
                    {
                        pw.println();
                        pw.print(getSpace(deepSet) + "<!--");
                        pw.print(node.getNodeValue() + "-->");
                        break;
                    }
            }
            if (type == Node.ELEMENT_NODE) {
                if (!lastIsAString) {
                    pw.println();
                    pw.print(getSpace(deepSet) + "</");
                } else pw.print("</");
                pw.print(node.getNodeName());
                pw.print('>');
                lastIsAString = false;
            }
        }

        private static String getXMLString(String value) {
            if (value != null) {
                value = value.toString().trim();
                value = value.replaceAll("&", "&amp;");
                value = value.replaceAll("<", "&lt;");
                value = value.replaceAll(">", "&gt;");
                value = value.replaceAll("\"", "&quot;");
                value = value.replaceAll("\'", "&apos;");
            }
            return value;
        }
    }

    private XmlUtils() {
    }

    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    static {
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
    }

    static ThreadLocal<Object> dbcache = new ThreadLocal<Object>() {

        protected Object initialValue() {
            try {
                return dbf.newDocumentBuilder();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    };

    /**
	 * ��XML��ʽ���DOM��
	 * 
	 * @param writer  �����
	 * @param node  ��� node
	 * @param deepSet  ������ȣ�0 ������
	 */
    public static void printDOMTree(Writer writer, Node node, int deepSet) {
        XmlPrinter.printDOMTree(writer, node, deepSet);
    }

    /**
	 * ��XML��ʽ���DOM��
	 * @param writer �����
	 * @param node ��� node
	 * @param deepSet ������ȣ�0 ������
	 * @param encoding ����
	 */
    public static void printDOMTree(Writer writer, Node node, int deepSet, String encoding) {
        XmlPrinter.printDOMTree(writer, node, deepSet, encoding);
    }

    /**
	 * ȡ��һ��DocumentBuilderʵ��
	 * @return DocumentBuilder
	 */
    public static DocumentBuilder getDocumentBuilder() {
        return (DocumentBuilder) dbcache.get();
    }

    /**
	 * ��ȡһ��Documentʵ��
	 * @return Document
	 */
    public static Document getNewDocument() {
        return getDocumentBuilder().newDocument();
    }

    /**
	 * �õ���node����node
	 * 
	 * @param node  ���node
	 * @param tagName  Ҫȡ�õ���node���
	 * @return Node ��node
	 */
    public static Node getChildNodeOf(Node node, String tagName) {
        for (Node temp = node.getFirstChild(); temp != null; temp = temp.getNextSibling()) {
            if (temp.getNodeType() == Node.ELEMENT_NODE && tagName.equals(temp.getNodeName())) {
                return temp;
            }
        }
        return null;
    }

    /**
	 * 
	 * @param node
	 * @param tagName
	 * @return String
	 */
    public static String getChildNodeValueOf(Node node, String tagName) {
        for (Node temp = node.getFirstChild(); temp != null; temp = temp.getNextSibling()) {
            if (temp.getNodeType() == Node.ELEMENT_NODE && tagName.equals(temp.getNodeName())) {
                return getValueOf(temp);
            }
        }
        if (tagName.equals(node.getNodeName())) {
            return getValueOf(node);
        }
        return null;
    }

    public static final String getValueOf(Node node) {
        if (node == null) {
            return null;
        } else if (node instanceof Text) {
            return node.getNodeValue().trim();
        } else if (node instanceof Element) {
            ((Element) node).normalize();
            Node temp = node.getFirstChild();
            if (temp != null && (temp instanceof Text)) return temp.getNodeValue().trim(); else return "";
        } else {
            return node.getNodeValue().trim();
        }
    }

    public static final String getAtrributeValueOf(Node node, String attribute) {
        Node _node = node.getAttributes().getNamedItem(attribute);
        return getValueOf(_node);
    }

    public static Iterator<Element> getElementsByTagName(Element element, String tag) {
        List<Element> children = new ArrayList<Element>();
        if (element != null && tag != null) {
            NodeList nodes = element.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node child = nodes.item(i);
                children.add((Element) child);
            }
        }
        return children.iterator();
    }

    public static Iterator<Element> getElementsByTagNames(Element element, String[] tags) {
        List<Element> children = new ArrayList<Element>();
        if (element != null && tags != null) {
            List<String> tagList = Arrays.asList(tags);
            NodeList nodes = element.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node child = nodes.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && tagList.contains(((Element) child).getTagName())) {
                    children.add((Element) child);
                }
            }
        }
        return children.iterator();
    }

    /**
	 * ȡ��URL��Ӧ��xml�ĵ��ĸ�ڵ�
	 * 
	 * @param url the xml descriptor url
	 * @return Document
	 */
    public static Document getDocument(URL url) throws Exception {
        InputStream is = null;
        try {
            is = new BufferedInputStream(url.openStream());
            return getDocumentBuilder().parse(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static Document getDocument(File file) throws Exception {
        try {
            return getDocumentBuilder().parse(file);
        } finally {
        }
    }

    public static Document getDocument(String file) throws Exception {
        return getDocumentBuilder().parse(new File(file));
    }

    /**
	 * Copies the source tree into the specified place in a destination tree.
	 * The source node and its children are appended as children of the
	 * destination node.
	 * <p>
	 * <em>Note:</em> This is an iterative implementation.
	 */
    public static void copyInto(Node src, Node dest) throws DOMException {
        Document factory = dest.getOwnerDocument();
        Node start = src;
        Node parent = src;
        Node place = src;
        while (place != null) {
            Node node = null;
            int type = place.getNodeType();
            switch(type) {
                case Node.CDATA_SECTION_NODE:
                    {
                        node = factory.createCDATASection(place.getNodeValue());
                        break;
                    }
                case Node.COMMENT_NODE:
                    {
                        node = factory.createComment(place.getNodeValue());
                        break;
                    }
                case Node.ELEMENT_NODE:
                    {
                        Element element = factory.createElement(place.getNodeName());
                        node = element;
                        NamedNodeMap attrs = place.getAttributes();
                        int attrCount = attrs.getLength();
                        for (int i = 0; i < attrCount; i++) {
                            Attr attr = (Attr) attrs.item(i);
                            String attrName = attr.getNodeName();
                            String attrValue = attr.getNodeValue();
                            element.setAttribute(attrName, attrValue);
                        }
                        break;
                    }
                case Node.ENTITY_REFERENCE_NODE:
                    {
                        node = factory.createEntityReference(place.getNodeName());
                        break;
                    }
                case Node.PROCESSING_INSTRUCTION_NODE:
                    {
                        node = factory.createProcessingInstruction(place.getNodeName(), place.getNodeValue());
                        break;
                    }
                case Node.TEXT_NODE:
                    {
                        node = factory.createTextNode(place.getNodeValue());
                        break;
                    }
                default:
                    {
                        throw new IllegalArgumentException("can't copy node type, " + type + " (" + (node == null ? "null" : node.getNodeName()) + ')');
                    }
            }
            dest.appendChild(node);
            if (place.hasChildNodes()) {
                parent = place;
                place = place.getFirstChild();
                dest = node;
            } else {
                place = place.getNextSibling();
                while (place == null && parent != start) {
                    place = parent.getNextSibling();
                    parent = parent.getParentNode();
                    dest = dest.getParentNode();
                }
            }
        }
    }

    /** Finds and returns the first child element node. */
    public static Element getFirstChildElement(Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    /** Finds and returns the last child element node. */
    public static Element getLastChildElement(Node parent) {
        Node child = parent.getLastChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
            child = child.getPreviousSibling();
        }
        return null;
    }

    /** Finds and returns the next sibling element node. */
    public static Element getNextSiblingElement(Node node) {
        Node sibling = node.getNextSibling();
        while (sibling != null) {
            if (sibling.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) sibling;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /** Finds and returns the first child node with the given name. */
    public static Element getFirstChildElement(Node parent, String elemName) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equals(elemName)) {
                    return (Element) child;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    /** Finds and returns the last child node with the given name. */
    public static Element getLastChildElement(Node parent, String elemName) {
        Node child = parent.getLastChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equals(elemName)) {
                    return (Element) child;
                }
            }
            child = child.getPreviousSibling();
        }
        return null;
    }

    /** Finds and returns the next sibling node with the given name. */
    public static Element getNextSiblingElement(Node node, String elemName) {
        Node sibling = node.getNextSibling();
        while (sibling != null) {
            if (sibling.getNodeType() == Node.ELEMENT_NODE) {
                if (sibling.getNodeName().equals(elemName)) {
                    return (Element) sibling;
                }
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /**
	 * Returns the concatenated child text of the specified node. This method
	 * only looks at the immediate children of type <code>Node.TEXT_NODE</code>
	 * or the children of any child node that is of type
	 * <code>Node.CDATA_SECTION_NODE</code> for the concatenation.
	 * 
	 * @param node
	 *            The node to look at.
	 */
    public static String getChildText(Node node) {
        if (node == null) {
            return null;
        }
        StringBuffer str = new StringBuffer();
        Node child = node.getFirstChild();
        while (child != null) {
            short type = child.getNodeType();
            if (type == Node.TEXT_NODE) {
                str.append(child.getNodeValue());
            } else if (type == Node.CDATA_SECTION_NODE) {
                str.append(getChildText(child));
            }
            child = child.getNextSibling();
        }
        return str.toString();
    }

    /**
	 * ��ȡ��Ԫ��Element���ı�ֵ,���û���ı��ڵ�,����null (ע���getChildText���������)
	 * 
	 * @param ele
	 *            The node to look at.
	 */
    public static String getElementText(Element ele) {
        if (ele == null) {
            return null;
        }
        Node child = ele.getFirstChild();
        if (child != null) {
            short type = child.getNodeType();
            if (type == Node.TEXT_NODE) {
                return child.getNodeValue();
            }
        }
        return null;
    }

    public static String getFirstChildElementText(Node node) {
        return getElementText(getFirstChildElement(node));
    }

    public static String getLastChildElementText(Node node) {
        return getElementText(getLastChildElement(node));
    }

    public static String getNextSiblingElementText(Node node) {
        return getElementText(getNextSiblingElement(node));
    }

    public static String getFirstChildElementText(Node node, String elemName) {
        return getElementText(getFirstChildElement(node, elemName));
    }

    public static String getLastChildElementText(Node node, String elemName) {
        return getElementText(getLastChildElement(node, elemName));
    }

    public static String getNextSiblingElementText(Node node, String elemName) {
        return getElementText(getNextSiblingElement(node, elemName));
    }

    public static Element createLeafElement(Document doc, String eleName, String text) {
        Element ele = doc.createElement(eleName);
        if (text != null) {
            ele.appendChild(doc.createTextNode(text));
        }
        return ele;
    }

    public static void addChildElement(Element element, String child_ele_name, String text) {
        Document doc = element.getOwnerDocument();
        Element sub_element = createLeafElement(doc, child_ele_name, text);
        element.appendChild(sub_element);
    }

    /**
	 * ����ָ���ĵ���ĳ���������еĵ� index ��Ԫ�ء� �������ڣ�(01-4-23 14:27:11)
	 * 
	 * @return org.w3c.dom.Element
	 * @param doc
	 *            org.w3c.dom.Document
	 * @param tagName
	 *            java.lang.String
	 * @param index
	 *            int
	 */
    public static Element getElement(Document doc, String tagName, int index) {
        NodeList rows = doc.getDocumentElement().getElementsByTagName(tagName);
        return (Element) rows.item(index);
    }

    /**
	 * ���ظ�ڵ����ض���Ƶ�����Ҷ�ӽڵ��ֵ �������ڣ�(01-4-30 10:44:45)
	 * 
	 * @return org.w3c.dom.Node
	 * @param element
	 *            org.w3c.dom.Element
	 * @param childName
	 *            java.lang.String
	 */
    public static String[] getChildNodeValues(Element element, String childName) {
        NodeList nl = element.getChildNodes();
        List<String> v = new ArrayList<String>();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == org.w3c.dom.Node.TEXT_NODE) continue;
            Element e = (Element) nl.item(i);
            if (e.getNodeName().equals(childName)) {
                String s = null;
                if (e.getFirstChild() != null) s = e.getFirstChild().getNodeValue();
                v.add(s);
            }
        }
        String[] sa = new String[v.size()];
        v.toArray(sa);
        return sa;
    }

    /**
	 * ����������Щ���ӽڵ� childName=childValue �����Ϊ parentName ����Щ�ڵ㡣
	 * @param doc
	 * @param parentName
	 * @param childName
	 * @param childValue
	 * @return Element[]
	 */
    public static Element[] getElementsOf(Document doc, String parentName, String childName, String childValue) {
        List<Element> vector = new ArrayList<Element>();
        NodeList nodeList = doc.getElementsByTagName(parentName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.TEXT_NODE) continue;
            Element element = (Element) nodeList.item(i);
            NodeList nl = element.getChildNodes();
            for (int j = 0; j < nl.getLength(); j++) {
                if (nl.item(j).getNodeType() == Node.TEXT_NODE) continue;
                Element e = (Element) nl.item(j);
                if (e.getTagName().equals(childName)) {
                    if (e.getFirstChild() != null) {
                        if (e.getFirstChild().getNodeValue().equals(childValue)) vector.add(element);
                    }
                }
            }
        }
        Element[] buffer = new Element[vector.size()];
        vector.toArray(buffer);
        return buffer;
    }

    /**
	 * ���ظ��ĵ��У���Ҷ�ӽڵ� childName=childValue ���Ǹ��ڵ���Ϊ parentName �Ľڵ㡣 �������ڣ�(01-5-3
	 * 13:33:49)
	 * 
	 * @return org.w3c.dom.Element
	 * @param doc
	 *            org.w3c.dom.Document
	 * @param parentName
	 *            java.lang.String
	 * @param childName
	 *            java.lang.String
	 * @param childValue
	 *            java.lang.String
	 */
    public static Element getElementOf(Document doc, String parentName, String childName, String childValue) {
        NodeList nodeList = doc.getElementsByTagName(parentName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.TEXT_NODE) continue;
            Element element = (Element) nodeList.item(i);
            NodeList nl = element.getChildNodes();
            for (int j = 0; j < nl.getLength(); j++) {
                if (nl.item(j).getNodeType() == Node.TEXT_NODE) continue;
                Element e = (Element) nl.item(j);
                if (e.getTagName().equals(childName)) {
                    if (e.getFirstChild() != null) {
                        if (e.getFirstChild().getNodeValue().equals(childValue)) {
                            return element;
                        }
                    }
                }
            }
        }
        return null;
    }
}
