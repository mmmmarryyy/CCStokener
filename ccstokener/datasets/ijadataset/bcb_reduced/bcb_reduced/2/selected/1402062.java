package netspotter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import net.sf.saxon.om.NamespaceConstant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLDNS {

    public static void convert(URL url, PrintWriter writer, String server) {
        try {
            XPathFactory xpf = XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON);
            XPath xpe = xpf.newXPath();
            InputStream is = null;
            try {
                is = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Document doc = readFromStream(is);
            xpe.setNamespaceContext(new NamespaceContext() {

                public String getNamespaceURI(String s) {
                    if (s.equals("tns")) {
                        return "http://services.remote/";
                    } else if (s.equals("xsd")) {
                        return "http://www.w3.org/2001/XMLSchema";
                    } else if (s.equals("soap")) {
                        return "http://schemas.xmlsoap.org/wsdl/soap/";
                    } else if (s.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                        return "http://schemas.xmlsoap.org/wsdl/";
                    } else {
                        return null;
                    }
                }

                public String getPrefix(String s) {
                    return null;
                }

                public Iterator getPrefixes(String s) {
                    return null;
                }
            });
            Element defs = (Element) xpe.compile("/*:definitions").evaluate(doc, XPathConstants.NODE);
            defs.setAttribute("xmlns", "http://schemas.xmlsoap.org/wsdl/");
            Node schemaLocation = (Node) xpe.compile("/*:definitions/*:types/xsd:schema/xsd:import/@schemaLocation").evaluate(doc, XPathConstants.NODE);
            String sl = schemaLocation.getNodeValue();
            for (int i = 0; i < 3; i++) sl = sl.substring(sl.indexOf('/') + 1);
            schemaLocation.setNodeValue(server + "/" + sl);
            Node location = (Node) xpe.compile("/*:definitions/*:service/*:port/soap:address/@location").evaluate(doc, XPathConstants.NODE);
            String l = location.getNodeValue();
            for (int i = 0; i < 3; i++) l = l.substring(l.indexOf('/') + 1);
            location.setNodeValue(server + "/" + l);
            write(doc, writer);
        } catch (XPathFactoryConfigurationException e) {
            e.printStackTrace();
            System.err.println("Error:" + e);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            System.err.println("Error:" + e);
        }
    }

    private static Document readFromStream(InputStream is) {
        Document document = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            document = docBuilder.parse(is);
            document.getDocumentElement().normalize();
        } catch (SAXParseException err) {
            String msg = "** SAXParseException" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() + "\n" + "   " + err.getMessage();
            System.err.println(msg);
            Exception x = err.getException();
            ((x == null) ? err : x).printStackTrace();
        } catch (SAXException e) {
            String msg = "SAXException";
            System.err.println(msg);
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }

    private static void write(Document document, PrintWriter writer) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            Properties props = new Properties();
            props.put("indent", "yes");
            transformer.setOutputProperties(props);
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            System.err.println("Error:" + e);
        } catch (TransformerException e) {
            System.err.println("Error:" + e);
        }
    }

    public static void main(String args[]) {
        try {
            XMLDNS.convert(new URL("http://10.0.0.1:8080/DiscoverService/DiscoverServiceBean?WSDL"), new PrintWriter(new File("teszt.xml")), "http://10.0.0.1");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
