package com.dfruits.forms;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathNodesListFactory {

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private XPathFactory xPathFactory;

    private XPath xpath;

    private XPathExpression restrictedExpr;

    private Map<String, String> nsMap = new HashMap<String, String>();

    private InputStream in;

    private String xpathExpr;

    private Document doc;

    public XPathNodesListFactory(URL url, String xpathExpr) throws IOException {
        this(url.openStream(), xpathExpr);
    }

    public XPathNodesListFactory(InputStream in, String xpathExpr) {
        this.xpathExpr = xpathExpr;
        this.in = in;
        initXPath();
    }

    public void addNameSpace(String pre, String nameSpace) {
        nsMap.put(pre, nameSpace);
    }

    private void initXPath() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        xPathFactory = XPathFactory.newInstance();
        xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                return nsMap.get(prefix);
            }

            public String getPrefix(String namespaceURI) {
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        });
    }

    public List<Node> createList() {
        try {
            restrictedExpr = xpath.compile(xpathExpr);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        List<Node> ret = new ArrayList<Node>();
        try {
            doc = builder.parse(in);
            NodeList nodes = (NodeList) restrictedExpr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                ret.add(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return ret;
    }

    public String transformBack() throws Exception {
        return transformBack(doc);
    }

    public static String transformBack(Node node) throws Exception {
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        StringWriter sw = new StringWriter();
        StreamResult streamResult = new StreamResult(sw);
        DOMSource source = new DOMSource(node);
        trans.transform(source, streamResult);
        String xmlString = sw.toString();
        return xmlString;
    }
}
