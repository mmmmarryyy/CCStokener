package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.MetaMap;
import tufts.vue.MetaMap.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.Node;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * @version $Revision: 1.16 $ / $Date: 2010-02-03 19:13:16 $ / $Author: mike $
 * @author Scott Fraize
 */
public class XMLIngest {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(XMLIngest.class);

    private static final boolean XML_DEBUG = false;

    private static final boolean XML_OUTPUT = false;

    public static class XmlSchema extends tufts.vue.ds.Schema {

        final String itemPath;

        final int itemPathLen;

        DataRow curRow;

        public XmlSchema() {
            itemPath = "<unknown>";
            itemPathLen = 0;
        }

        public XmlSchema(tufts.vue.Resource source, String itemPath) {
            super.setResource(source);
            this.itemPath = itemPath;
            if (itemPath == null || itemPath.length() == 0) itemPathLen = 0; else itemPathLen = itemPath.length() + 1;
            setXMLKeyFold(itemPath != null && itemPath.startsWith("plist."));
            Log.debug("Constructed XmlSchema " + this);
        }

        @Override
        public void dumpSchema(PrintWriter ps) {
            if (itemPath != null) ps.println("ItemPath: " + itemPath);
            super.dumpSchema(ps);
        }

        void trackFieldValuePair(String name, String value) {
            if (itemPath != null && name.startsWith(itemPath) && name.length() > itemPathLen) name = name.substring(itemPathLen);
            Field field = getField(name);
            if (field == null) {
                field = addField(name);
                if (name.length() > mLongestFieldName) mLongestFieldName = name.length();
            }
            if (curRow != null) curRow.addValue(field, value); else field.trackValue(value);
        }

        void trackNodeOpen(String name) {
            if (name.equals(getRowStartNode())) {
                curRow = new DataRow(this);
                addRow(curRow);
            }
        }

        void trackNodeClose(String name) {
            if (name.equals(getRowStartNode())) {
                curRow = null;
            }
        }

        private String getRowStartNode() {
            return itemPath;
        }
    }

    static int depth = 0;

    static void XPathExtract(XmlSchema schema, Document document) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "/rss/channel/item";
            errout("Extracting " + expression);
            Node nodeValue = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
            errout("   Node: " + nodeValue);
            String stringValue = (String) xpath.evaluate(expression, document, XPathConstants.STRING);
            System.out.println(" String: " + stringValue);
            NodeList nodeSet = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
            errout("NodeSet: " + Util.tag(nodeSet) + "; size=" + nodeSet.getLength());
            for (int i = 0; i < nodeSet.getLength(); i++) {
                scanNode(schema, nodeSet.item(i), null, null);
            }
        } catch (XPathExpressionException e) {
            System.err.println("XPathExpressionException caught...");
            e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static Schema ingestXML(XmlSchema schema, org.xml.sax.InputSource input, String itemKey) {
        final org.w3c.dom.Document doc = parseXML(input, false);
        if (DEBUG.DR) {
            try {
                errout("XML parsed, document built:");
                errout("org.w3c.dom.Document: " + Util.tags(doc));
                final org.w3c.dom.DocumentType type = doc.getDoctype();
                errout("docType: " + Util.tags(type));
                if (type != null) {
                    errout("docType.name: " + Util.tags(type.getName()));
                    errout("docType.entities: " + Util.tags(type.getEntities()));
                    errout("docType.notations: " + Util.tags(type.getNotations()));
                    errout("docType.publicId: " + Util.tags(type.getPublicId()));
                    errout("docType.systemId: " + Util.tags(type.getSystemId()));
                }
                errout("impl: " + Util.tags(doc.getImplementation().getClass()));
                errout("docElement: " + Util.tags(doc.getDocumentElement().getClass()));
            } catch (Throwable t) {
                Log.error("debug failure", t);
            }
        }
        if (schema == null) schema = new XmlSchema(tufts.vue.Resource.instance(input), itemKey); else schema.flushData();
        if (false) XPathExtract(schema, doc); else scanNode(schema, doc.getDocumentElement(), null, null);
        if (DEBUG.DR || DEBUG.SCHEMA) schema.dumpSchema(System.err);
        return schema;
    }

    private static boolean isText(int type) {
        return type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE;
    }

    private static boolean isText(Node node) {
        return isText(node.getNodeType());
    }

    private static final String getNodeType(Node n) {
        return getNodeType(n.getNodeType());
    }

    private static final String getNodeType(int t) {
        if (t == Node.ATTRIBUTE_NODE) return "attr";
        if (t == Node.CDATA_SECTION_NODE) return "cdata";
        if (t == Node.COMMENT_NODE) return "comment";
        if (t == Node.DOCUMENT_NODE) return "document";
        if (t == Node.ELEMENT_NODE) return "element";
        if (t == Node.ENTITY_NODE) return "entity";
        if (t == Node.TEXT_NODE) return "text";
        return "" + t;
    }

    private static void scanNode(XmlSchema schema, org.w3c.dom.Node n, String parentPath, String parentName) {
        final int type = n.getNodeType();
        final String value = n.getNodeValue();
        final boolean isAttribute = (type == Node.ATTRIBUTE_NODE);
        String name = n.getNodeName();
        scanNode(schema, n, type, parentPath, parentName, name, value);
    }

    private static void scanNode(final XmlSchema schema, final org.w3c.dom.Node node, final int type, final String parentPath, final String parentName, final String nodeName, final String value) {
        final boolean isAttribute = (type == Node.ATTRIBUTE_NODE);
        final boolean isMergedText = FOLD_TEXT && isText(type);
        final boolean hasAttributes = (!isAttribute && node != null && node.hasAttributes());
        Node firstChild = null, lastChild = null;
        if (node != null) {
            firstChild = node.getFirstChild();
            lastChild = node.getLastChild();
        }
        final String XMLName;
        if (isAttribute) XMLName = parentName + ATTR_SEPARATOR + nodeName; else XMLName = nodeName;
        final String fullName;
        if (parentPath != null) {
            if (isMergedText) fullName = parentPath; else if (isAttribute) fullName = parentPath + ATTR_SEPARATOR + nodeName; else fullName = parentPath + '.' + nodeName;
        } else {
            fullName = nodeName;
        }
        if (type == Node.ELEMENT_NODE) schema.trackNodeOpen(fullName);
        if (depth < REPORT_THRESH) {
            if (depth < REPORT_THRESH - 1) {
                if (type == Node.TEXT_NODE) eoutln(String.format("node(%s) {%s} (len=%d)", getNodeType(type), fullName, value.length())); else eoutln(String.format("NODE(%s) {%s} %.192s", getNodeType(type), fullName, node, Util.tags(firstChild)));
            } else if (XML_DEBUG) System.err.print(".");
        }
        if (hasAttributes && ATTRIBUTES_IMMEDIATE) scanAttributes(schema, fullName, nodeName, node.getAttributes());
        String outputValue = null;
        if (value != null) {
            outputValue = value.trim();
            if (outputValue.length() > 0) {
                schema.trackFieldValuePair(fullName, outputValue);
            } else outputValue = null;
        }
        final NodeList children = node == null ? null : node.getChildNodes();
        final boolean DO_TAG;
        if (isMergedText) {
            DO_TAG = false;
        } else if (outputValue == null && node != null) {
            if (!node.hasChildNodes()) {
                DO_TAG = false;
            } else if (children.getLength() == 1 && isText(firstChild) && firstChild.getNodeValue().trim().length() == 0) {
                DO_TAG = false;
            } else DO_TAG = true;
        } else DO_TAG = true;
        boolean closeOnSameLine = false;
        if (DO_TAG) {
            iout("<");
            out(XMLName);
            out(">");
            if (firstChild == null || (isText(firstChild) && firstChild == lastChild)) {
                closeOnSameLine = true;
            } else if (XML_OUTPUT) System.out.print('\n');
            if (FOLD_TEXT && (type != Node.ELEMENT_NODE && type != Node.ATTRIBUTE_NODE)) {
                final String err = "UNHANDLED TYPE=" + type + "; " + nodeName;
                outln("<" + err + ">");
                errout(err);
            }
        }
        if (outputValue != null) {
            if (type == Node.CDATA_SECTION_NODE) {
                out("<![CDATA[");
                out(outputValue);
                out("]]>");
            } else {
                out(XMLEntityEncode(outputValue));
            }
        }
        if (!isAttribute && node != null) {
            depth++;
            if (FOLD_KEYS || schema.isXMLKeyFold()) {
                scanFoldedChildren(schema, children, fullName, nodeName);
            } else {
                for (int i = 0; i < children.getLength(); i++) scanNode(schema, children.item(i), fullName, nodeName);
            }
            depth--;
        }
        if (DO_TAG) {
            if (closeOnSameLine) outln("</" + XMLName + ">"); else ioutln("</" + XMLName + ">");
        }
        if (type == Node.ELEMENT_NODE) schema.trackNodeClose(fullName);
        if (hasAttributes && !ATTRIBUTES_IMMEDIATE) scanAttributes(schema, fullName, nodeName, node.getAttributes());
    }

    private static void scanAttributes(XmlSchema schema, String fullName, String nodeName, NamedNodeMap attr) {
        if (attr != null && attr.getLength() > 0) {
            for (int i = 0; i < attr.getLength(); i++) {
                final Node a = attr.item(i);
                scanNode(schema, a, fullName, nodeName);
            }
        }
    }

    private static void scanFoldedChildren(XmlSchema schema, final NodeList children, final String fullName, final String nodeName) {
        for (int i = 0; i < children.getLength(); i++) {
            final Node item = children.item(i);
            final Node next = children.item(i + 1);
            if (next != null) {
                final String nextName = next.getNodeName();
                if ("key".equals(item.getNodeName())) {
                    String newNodeName = item.getChildNodes().item(0).getNodeValue();
                    if (newNodeName != null) newNodeName = newNodeName.replace(' ', '_');
                    final String newNodeValue;
                    if ("true".equals(nextName)) {
                        newNodeValue = "true";
                    } else if ("false".equals(nextName)) {
                        newNodeValue = "false";
                    } else if ("dict".equals(nextName) || "array".equals(nextName)) {
                        continue;
                    } else {
                        newNodeValue = next.getChildNodes().item(0).getNodeValue();
                    }
                    scanNode(schema, null, Node.ELEMENT_NODE, fullName, nodeName, newNodeName, newNodeValue);
                    i++;
                    continue;
                }
            }
            scanNode(schema, item, fullName, nodeName);
        }
    }

    private static org.w3c.dom.Document parseXML(Object input, boolean validating) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            factory.setValidating(validating);
            final org.w3c.dom.Document doc;
            if (input instanceof String) {
                doc = factory.newDocumentBuilder().parse(new File((String) input));
            } else if (input instanceof InputSource) {
                doc = factory.newDocumentBuilder().parse((InputSource) input);
            } else if (input instanceof InputStream) {
                doc = factory.newDocumentBuilder().parse((InputStream) input);
            } else throw new Error("Unhandled input type: " + Util.tags(input));
            return doc;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static String XMLEntityEncode(final String text) {
        StringBuilder buf = null;
        final int len = (text == null ? -1 : text.length());
        for (int i = 0; i < len; i++) {
            final char c = text.charAt(i);
            String entity = null;
            switch(c) {
                case '&':
                    entity = "&amp;";
                    break;
                case '<':
                    entity = "&lt;";
                    break;
                case '>':
                    entity = "&gt;";
                    break;
                case '"':
                    entity = "&quot;";
                    break;
                default:
                    if (buf != null) buf.append(c);
                    continue;
            }
            if (buf == null) {
                buf = new StringBuilder(len + 12);
                buf.append(text, 0, i);
            }
            buf.append(entity);
        }
        return buf == null ? text : buf.toString();
    }

    public static void iout(String s) {
        iout(depth, s);
    }

    public static void ioutln(String s) {
        ioutln(depth, s);
    }

    static final String TAB = "    ";

    public static void iout(int _depth, String s) {
        if (XML_OUTPUT) {
            for (int x = 0; x < _depth; x++) System.out.print(TAB);
            System.out.print(s);
        }
    }

    public static void ioutln(int _depth, String s) {
        if (XML_OUTPUT) {
            for (int x = 0; x < _depth; x++) System.out.print(TAB);
            System.out.println(s);
        }
    }

    public static void eoutln(int _depth, String s) {
        if (XML_OUTPUT) {
            for (int x = 0; x < _depth; x++) System.err.print(TAB);
            System.err.println(s);
        }
    }

    public static void eoutln(String s) {
        eoutln(depth, s);
    }

    public static void out(String s) {
        if (XML_OUTPUT) System.out.print(s == null ? "null" : s);
    }

    public static void outln(String s) {
        if (XML_OUTPUT) System.out.println(s == null ? "null" : s);
    }

    public static void errout(String s) {
        Log.debug(s == null ? "null" : s);
    }

    static final boolean ATTRIBUTES_IMMEDIATE = false;

    static final boolean FOLD_TEXT = true;

    static final boolean FOLD_KEYS = false;

    static final int REPORT_THRESH = 4;

    static final char ATTR_SEPARATOR = '@';

    private static final String JIRA_VUE_URL = "http://bugs.atech.tufts.edu/secure/IssueNavigator.jspa?view=rss&pid=10001&tempMax=9999&reset=true&decorator=none";

    private static final String JIRA_SFRAIZE_COOKIE = "seraph.os.cookie=LkPlQkOlJlHkHiEpGiOiGjJjFi";

    private static InputStream getTestXMLStream() throws IOException {
        URL url = new URL(JIRA_VUE_URL);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Cookie", JIRA_SFRAIZE_COOKIE);
        errout("Opening connection to " + url);
        conn.connect();
        errout("Getting InputStream...");
        InputStream in = conn.getInputStream();
        errout("Got " + Util.tags(in));
        errout("Getting headers...");
        Map<String, List<String>> headers = conn.getHeaderFields();
        errout("HEADERS:");
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            errout(e.getKey() + ": " + e.getValue());
        }
        return in;
    }

    public static void main(String[] args) throws IOException {
        DEBUG.Enabled = DEBUG.DR = DEBUG.IO = DEBUG.SCHEMA = true;
        tufts.vue.VUE.parseArgs(args);
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().addAppender(new org.apache.log4j.ConsoleAppender(tufts.vue.VUE.MasterLogPattern, "System.err"));
        errout("Max mem: " + Util.abbrevBytes(Runtime.getRuntime().maxMemory()));
        final String file = args[0];
        final String key = args[1];
        Log.debug("File: " + file);
        Log.debug("Key: " + key);
        final InputSource is = new InputSource(file);
        is.setCharacterStream(new FileReader(file));
        Schema schema = ingestXML(null, is, key);
        System.err.println("\n");
        Log.debug("done");
    }
}
