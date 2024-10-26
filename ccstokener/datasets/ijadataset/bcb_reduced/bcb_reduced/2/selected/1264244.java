package xbrowser.util;

import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import org.apache.crimson.tree.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import xbrowser.XRepository;

public final class XMLManager {

    public static String getNodeValue(Node node) {
        return ((node.getFirstChild() == null) ? "" : node.getFirstChild().getNodeValue());
    }

    public static Document newDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeDocument(String file_name, Document doc, String public_id, String system_id, String dtd_content) throws IOException {
        OutputStream out = new FileOutputStream(file_name);
        ((XmlDocument) doc).setDoctype(public_id, system_id, dtd_content);
        ((XmlDocument) doc).write(out);
        out.flush();
        out.close();
    }

    public static Document readFileDocument(String filename) throws IOException, SAXException {
        return readFileDocument(filename, null, null);
    }

    public static Document readFileDocument(String filename, String dtd_symbol, URL dtd_url) throws IOException, SAXException {
        return getDocumentBuilder(dtd_symbol, dtd_url).parse(new File(filename));
    }

    public static Document readResourceDocument(String resource) throws IOException, SAXException {
        return readResourceDocument(resource, null, null);
    }

    public static Document readResourceDocument(String resource, String dtd_symbol, URL dtd_url) throws IOException, SAXException {
        return getDocumentBuilder(dtd_symbol, dtd_url).parse(resource);
    }

    private static DocumentBuilder getDocumentBuilder(final String dtd_symbol, final URL dtd_url) {
        DocumentBuilderFactory doc_builder_factory = DocumentBuilderFactory.newInstance();
        doc_builder_factory.setValidating(true);
        DocumentBuilder doc_builder = null;
        try {
            doc_builder = doc_builder_factory.newDocumentBuilder();
        } catch (Exception e) {
            XRepository.getLogger().error(e, "An error occured while trying to create new XML document builder!");
            XRepository.getLogger().error(e, e);
            return null;
        }
        doc_builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String public_id, String system_id) {
                if (dtd_symbol != null && system_id.endsWith(dtd_symbol)) {
                    try {
                        Reader reader = new InputStreamReader(dtd_url.openStream());
                        return new InputSource(reader);
                    } catch (Exception e) {
                        XRepository.getLogger().error(this, "An error occured while trying to resolve the main DTD!");
                        XRepository.getLogger().error(this, e);
                        return null;
                    }
                } else return null;
            }
        });
        return doc_builder;
    }

    public static Node findNode(Node node, String name) {
        if (node.getNodeName().equals(name)) return node;
        if (node.hasChildNodes()) {
            NodeList list = node.getChildNodes();
            int size = list.getLength();
            for (int i = 0; i < size; i++) {
                Node found = findNode(list.item(i), name);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static String getNodeAttribute(Node node, String name) {
        if (node instanceof Element) return ((Element) node).getAttribute(name);
        return null;
    }

    public static void addDataNodeTo(Document doc, Element parent_node, String new_node_name, String new_node_data) {
        Element node = doc.createElement(new_node_name);
        node.appendChild(doc.createTextNode(new_node_data));
        parent_node.appendChild(node);
    }
}
