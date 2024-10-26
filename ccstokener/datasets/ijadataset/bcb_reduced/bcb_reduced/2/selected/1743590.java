package org.form4j.form.util.xml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import com.wutka.dtd.DTD;
import com.wutka.dtd.DTDAttlist;
import com.wutka.dtd.DTDAttribute;
import com.wutka.dtd.DTDParser;

/**
 * Pretty Printing DOM Documents.
 * Prints DOM Document with indenting. 
 * <H4>Example</H4>
 * <TABLE border=1 width=100% bgcolor=#cceeff><TR><TD><PRE><B>
 * PrintWriter out = new PrintWriter(System.err);
 * Document xml    = XMLKit.createDocumentFromURL("file:" + args[0]);
 * <font color="#0000aa">DOMPrinter.printSubtree(out,xml.getDocumentElement())</font>;
 * </B></PRE></TD></TR></TABLE>
 *
 * @author         Christian Juon (cjuon@bluewin.ch)
 **/
public class DOMPrinter {

    /**
     * Constructs DOM Printer.
     */
    public DOMPrinter() {
    }

    /**
     * Constructs DOM Printer.
     * @param defaultProperties the per element defaults properties.
     *       Defaults found among these Properties are suppressed.
     */
    public DOMPrinter(final Properties defaultProperties) {
        attListTable = defaultProperties;
    }

    /**
     * @return Returns the attributeSorting.
     */
    public boolean isAttributeSorting() {
        return attributeSorting;
    }

    /**
     * @param attributeSorting The attributeSorting to set.
     */
    public void setAttributeSorting(boolean attributeSorting) {
        this.attributeSorting = attributeSorting;
    }

    /**
     * Print the subtree of the given node in XML format
     */
    public void printSubtree(final Node node) {
        try {
            dtd = getDTD(node.getOwnerDocument());
            LOG.debug("dtd " + dtd);
        } catch (Exception e) {
            LOG.error(e);
        }
        printSubtree(node, 0);
    }

    /**
     * Print the subtree of the given node to the given output stream in a tree
     * format
     */
    public void printSubtree(final PrintWriter p, final Node node) {
        try {
            dtd = getDTD(node.getOwnerDocument());
            LOG.debug("dtd " + dtd);
        } catch (Exception e) {
            LOG.error(e, e);
        }
        printNode(p, node, 0);
    }

    private DTD getDTD(final Document doc) throws Exception {
        if (doc == null || doc.getDoctype() == null) return null;
        String dtdURL = doc.getDoctype().getSystemId();
        LOG.debug("dtdURL " + dtdURL + " systemId " + doc.getDoctype().getSystemId() + " xxx " + doc.getDoctype().getInternalSubset());
        if (dtdURL == null) return null;
        URL url = new URL(dtdURL);
        URLConnection connection = url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "ISO-8859-1"));
        DTDParser dtdParser = new DTDParser(reader);
        DTD parsedDtd = dtdParser.parse();
        reader.close();
        return parsedDtd;
    }

    private void printSubtree(final Node node, final int level) {
        if (node instanceof Element) {
            for (int i = 0; i < level * 2; i++) System.out.print(level + " ");
            System.out.println("<" + node.getNodeName() + ">");
            if (node.hasChildNodes()) {
                NodeList list = node.getChildNodes();
                int size = list.getLength();
                for (int i = 0; i < size; i++) {
                    printSubtree(list.item(i), level + 1);
                }
            }
            for (int i = 0; i < level * 2; i++) System.out.print(level + " ");
            System.out.println("</" + node.getNodeName() + ">");
        } else if (node instanceof Text) {
            for (int i = 0; i < level * 2; i++) System.out.print(level + " ");
            System.out.println(node.getNodeValue().trim());
        } else {
            NodeList list = node.getChildNodes();
            int size = list.getLength();
            for (int i = 0; i < size; i++) {
                printSubtree(list.item(i), level + 1);
            }
        }
    }

    public boolean attributeIsSuppressed(final Attr attr) {
        return false;
    }

    private String normalizeValue(final String valueParm) {
        String value = valueParm;
        if (value == null) return null;
        try {
            while (value.startsWith("\n")) value = value.substring(1).trim();
            value = value.trim();
            while (value.endsWith("\n")) value = value.substring(0, value.length() - 1).trim();
        } catch (Exception e) {
            LOG.error(e);
        }
        return value;
    }

    public void printNode(final PrintWriter p, final Node node, final int level) {
        String type = null;
        if (node != null) {
            type = translateNodeType(node.getNodeType());
            LOG.debug("Type: " + type);
            if (type.equals("ELEMENT_NODE")) {
                printLevel(p, level);
                p.print("<" + node.getNodeName());
                if (node.hasAttributes()) {
                    NamedNodeMap attrs = node.getAttributes();
                    printAttributes(p, attrs, 0);
                }
                NodeList children = node.getChildNodes();
                if (children.getLength() > 0) {
                    p.println(">");
                    printChildNodes(p, children, level);
                    printLevel(p, level);
                    p.println("</" + node.getNodeName() + ">");
                } else {
                    p.println("/>");
                }
            } else if (type.equals("CDATA_SECTION_NODE")) {
                printLevel(p, level);
                p.print("<![CDATA[" + node.getNodeValue() + "]]>\n");
            } else if (type.equals("COMMENT_NODE")) {
                printComment(p, (Comment) node, level);
            } else if (type.equals("ATTRIBUTE_NODE")) {
                if (!attributeIsDefault((Attr) node) && !attributeIsSuppressed((Attr) node)) p.print(" " + node.getNodeName() + "=\"" + escape(node.getNodeValue()) + "\"");
            } else if (type.equals("TEXT_NODE")) {
                String text = normalizeValue(node.getNodeValue());
                String parentNodeType = translateNodeType(node.getParentNode().getNodeType());
                if (text != null && !parentNodeType.equals("ATTRIBUTE_NODE") && !text.equals("") && !text.equals("\\n")) {
                    p.println(escape(text));
                }
            } else if (type.equals("DOCUMENT_NODE")) {
                NodeList children = node.getChildNodes();
                if (children.getLength() > 0) {
                    printChildNodes(p, children, level);
                }
            } else if (type.equals("DOCUMENT_TYPE_NODE")) {
                printDocType(p, (DocumentType) node, level);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot handle nodeType " + type);
            }
        }
    }

    private void printLevel(final PrintWriter p, final int level) {
        for (int i = 0; i < level; i++) {
            p.print("  ");
        }
    }

    private void printComment(final PrintWriter p, final Comment comment, final int level) {
        StringTokenizer st = new StringTokenizer("<!--" + comment.getNodeValue() + "-->", "\n");
        while (st.hasMoreTokens()) {
            printLevel(p, level);
            String value = st.nextToken().trim();
            if (value.startsWith("!") || value.startsWith("-->")) p.print(" " + value + "\n"); else p.print(value + "\n");
        }
    }

    private void printDocType(final PrintWriter p, final DocumentType docType, final int level) {
        printLevel(p, level);
        p.println("<!-- doc type sys: " + docType.getSystemId() + " pub: " + docType.getPublicId() + " val: " + docType.getNodeValue() + "-->");
        if (docType.getSystemId() != null) {
            String rootName = docType.getName();
            printLevel(p, level);
            p.println("<!DOCTYPE " + rootName + " SYSTEM \"" + docType.getSystemId() + "\">");
        } else {
            printLevel(p, level);
            p.println("<!-- internal doctype unavailable -->");
        }
    }

    private void printAttributes(final PrintWriter p, final NamedNodeMap map, final int level) {
        if (map == null || map.getLength() == 0) return;
        if (!isAttributeSorting()) for (int i = 0; i < map.getLength(); i++) {
            Node item = map.item(i);
            printNode(p, item, level + 1);
        } else {
            Vector v = new Vector();
            for (int i = 0; i < map.getLength(); i++) v.add(map.item(i).getNodeName());
            Collections.sort(v);
            for (int i = 0; i < v.size(); i++) printNode(p, map.getNamedItem((String) v.elementAt(i)), level + 1);
        }
    }

    private void printChildNodes(PrintWriter p, NodeList map, int level) {
        if (map == null || map.getLength() == 0) return;
        for (int i = 0; i < map.getLength(); i++) {
            Node item = map.item(i);
            printNode(p, item, level + 1);
        }
    }

    public static String translateNodeType(short type) {
        switch(type) {
            case Node.ATTRIBUTE_NODE:
                return "ATTRIBUTE_NODE";
            case Node.CDATA_SECTION_NODE:
                return "CDATA_SECTION_NODE";
            case Node.COMMENT_NODE:
                return "COMMENT_NODE";
            case Node.DOCUMENT_FRAGMENT_NODE:
                return "DOCUMENT_FRAGMENT_NODE";
            case Node.DOCUMENT_NODE:
                return "DOCUMENT_NODE";
            case Node.DOCUMENT_TYPE_NODE:
                return "DOCUMENT_TYPE_NODE";
            case Node.ELEMENT_NODE:
                return "ELEMENT_NODE";
            case Node.ENTITY_NODE:
                return "ENTITY_NODE";
            case Node.ENTITY_REFERENCE_NODE:
                return "ENTITY_REFERENCE_NODE";
            case Node.NOTATION_NODE:
                return "NOTATION_NODE";
            case Node.PROCESSING_INSTRUCTION_NODE:
                return "PROCESSING_INSTRUCTION_NODE";
            case Node.TEXT_NODE:
                return "TEXT_NODE";
            default:
                return ("[Unknown = " + type + "]");
        }
    }

    private String escape(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) switch(s.charAt(i)) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '\t':
                sb.append("&#009;");
                break;
            default:
                sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    private boolean attributeIsDefault(Attr attr) {
        try {
            String defaultValue = getAttributeDefault(attr);
            if (defaultValue == null) return false;
            if (defaultValue.equals(attr.getValue())) return true;
        } catch (Exception e) {
            LOG.error(e, e);
        }
        return false;
    }

    private String getAttributeDefault(Attr attr) {
        if (dtd == null) return null;
        String nodeName = attr.getOwnerElement().getNodeName();
        if (attListTable == null) {
            attListTable = new Hashtable();
            Object items[] = dtd.getItems();
            for (int i = 0; i < items.length; i++) {
                Object item = items[i];
                if (item instanceof DTDAttlist) {
                    DTDAttlist attlist = (DTDAttlist) item;
                    DTDAttribute attributes[] = attlist.getAttribute();
                    for (int a = 0; a < attributes.length; a++) {
                        DTDAttribute attribute = (DTDAttribute) attributes[a];
                        if (attribute.getDefaultValue() != null) {
                            attListTable.put(attlist.getName() + "." + attribute.getName(), attribute.getDefaultValue());
                        }
                    }
                }
            }
        }
        return (String) attListTable.get(nodeName + "." + attr.getName());
    }

    /** main for standalone testing (OUTCOMMENT FOR PRODUCTION) **/
    public static void main(String args[]) {
        try {
            PrintWriter out = new PrintWriter(System.err);
            Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("file:" + args[0]);
            new DOMPrinter().printSubtree(out, xml.getDocumentElement());
            out.close();
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("usage: ch.nimbus.common.xml.DOMPrinter file:file.xml");
        }
    }

    DTD dtd = null;

    Hashtable attListTable = null;

    private boolean attributeSorting = false;

    private static final Logger LOG = Logger.getLogger(DOMPrinter.class.getName());
}
