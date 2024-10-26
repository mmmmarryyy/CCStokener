import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.Properties;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.XMLReader;
import org.xml.sax.XMLFilter;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.DeclHandler;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Some examples to show how the Simple API for Transformations
 * could be used.
 *
 * @author <a href="mailto:scott_boag@lotus.com">Scott Boag</a>
 */
public class TraxExamples {

    /**
     * Method main
     */
    public static void main(String argv[]) throws TransformerException, TransformerConfigurationException, IOException, SAXException, ParserConfigurationException, FileNotFoundException {
        String test = "all";
        if (argv.length > 0) {
            test = argv[0];
        }
        String foo_xml = "xml/foo.xml";
        String foo_xsl = "xsl/foo.xsl";
        String baz_xml = "xml/baz.xml";
        String baz_xsl = "xsl/baz.xsl";
        String foo2_xsl = "xsl/foo2.xsl";
        String foo3_xsl = "xsl/foo3.xsl";
        String text_xsl = "xsl/text.xsl";
        String embedded_xml = "xml/embedded.xml";
        if (test.equals("all") || test.equals("exampleParseOnly")) {
            System.out.println("\n\n==== exampleParseOnly ====");
            try {
                exampleParseOnly(foo_xml);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleSimple1")) {
            System.out.println("\n\n==== exampleSimple1 ====");
            try {
                exampleSimple1(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleSimple2")) {
            System.out.println("\n\n==== exampleSimple2 ====");
            try {
                exampleSimple2(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleFromStream")) {
            System.out.println("\n\n==== exampleFromStream ====");
            try {
                exampleFromStream(foo_xml, baz_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleFromReader")) {
            System.out.println("\n\n==== exampleFromReader ====");
            try {
                exampleFromReader(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleUseTemplatesObj")) {
            System.out.println("\n\n==== exampleUseTemplatesObj ====");
            try {
                exampleUseTemplatesObj(foo_xml, baz_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleUseTemplatesHandler")) {
            System.out.println("\n\n==== exampleUseTemplatesHandler ====");
            try {
                (new TraxExamples()).exampleUseTemplatesHandler(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleContentHandlerToContentHandler")) {
            System.out.println("\n\n==== exampleContentHandlerToContentHandler ====");
            try {
                exampleContentHandlerToContentHandler(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleXMLReader")) {
            System.out.println("\n\n==== exampleXMLReader ====");
            try {
                exampleXMLReader(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleXMLFilter")) {
            System.out.println("\n\n==== exampleXMLFilter ====");
            try {
                exampleXMLFilter(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleXMLFilterChain")) {
            System.out.println("\n\n==== exampleXMLFilterChain ====");
            try {
                exampleXMLFilterChain(foo_xml, foo_xsl, foo2_xsl, foo3_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleDOMtoDOM")) {
            System.out.println("\n\n==== exampleDOMtoDOM ====");
            try {
                exampleDOMtoDOM(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleDOMtoDOMNew")) {
            System.out.println("\n\n==== exampleDOMtoDOMNew ====");
            try {
                exampleDOMtoDOMNew(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleDOMtoDOMNonRoot")) {
            System.out.println("\n\n==== exampleDOMtoDOMNonRoot ====");
            try {
                exampleDOMtoDOMNonRoot(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleDOMtoDOMAlien")) {
            System.out.println("\n\n==== exampleDOMtoDOMAlien ====");
            try {
                exampleDOMtoDOMAlien(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleNodeInfo")) {
            System.out.println("\n\n==== exampleNodeInfo ====");
            try {
                exampleNodeInfo(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleSAXtoDOMNewIdentity")) {
            System.out.println("\n\n==== exampleSAXtoDOMNewIdentity ====");
            try {
                exampleSAXtoDOMNewIdentity(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleParam")) {
            System.out.println("\n\n==== exampleParam ====");
            try {
                exampleParam(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleTransformerReuse")) {
            System.out.println("\n\n==== exampleTransformerReuse ====");
            try {
                exampleTransformerReuse(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleOutputProperties")) {
            System.out.println("\n\n==== exampleOutputProperties ====");
            try {
                exampleOutputProperties(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleUseAssociated")) {
            System.out.println("\n\n==== exampleUseAssociated ====");
            try {
                exampleUseAssociated(foo_xml);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleUseEmbedded")) {
            System.out.println("\n\n==== exampleUseEmbedded ====");
            try {
                exampleUseAssociated(embedded_xml);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleContentHandlertoDOM")) {
            System.out.println("\n\n==== exampleContentHandlertoDOM ====");
            try {
                exampleContentHandlertoDOM(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleAsSerializer")) {
            System.out.println("\n\n==== exampleAsSerializer ====");
            try {
                exampleAsSerializer(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        if (test.equals("all") || test.equals("exampleUsingURIResolver")) {
            System.out.println("\n\n==== exampleUsingURIResolver ====");
            try {
                exampleUsingURIResolver(foo_xml, text_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }
        System.out.println("\n==== done! ====");
    }

    /**
     * Show the simplest possible transformation from system id
     * to output stream.
     */
    public static void exampleSimple1(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer(new StreamSource(xslID));
        transformer.transform(new StreamSource(sourceID), new StreamResult(System.out));
    }

    /**
     * Example that shows XML parsing only (no transformation)
     */
    public static void exampleParseOnly(String sourceID) throws Exception {
        System.setProperty("javax.xml.parsers.SAXParserFactory", "com.icl.saxon.aelfred.SAXParserFactoryImpl");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(sourceID, new org.xml.sax.helpers.DefaultHandler());
        System.out.println("\nParsing complete\n");
    }

    /**
     * Show the simplest possible transformation from File
     * to a File.
     */
    public static void exampleSimple2(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer(new StreamSource(xslID));
        transformer.transform(new StreamSource(sourceID), new StreamResult(new File("exampleSimple2.out")));
        System.out.println("\nOutput written to exampleSimple2.out\n");
    }

    /**
     * Show simple transformation from input stream to output stream.
     */
    public static void exampleFromStream(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, FileNotFoundException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        InputStream xslIS = new BufferedInputStream(new FileInputStream(xslID));
        StreamSource xslSource = new StreamSource(xslIS);
        Transformer transformer = tfactory.newTransformer(xslSource);
        InputStream xmlIS = new BufferedInputStream(new FileInputStream(sourceID));
        StreamSource xmlSource = new StreamSource(xmlIS);
        transformer.transform(xmlSource, new StreamResult(System.out));
    }

    /**
     * Show simple transformation from reader to output stream.  In general
     * this use case is discouraged, since the XML encoding can not be
     * processed.
     */
    public static void exampleFromReader(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, FileNotFoundException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Reader xslReader = new BufferedReader(new FileReader(xslID));
        StreamSource xslSource = new StreamSource(xslReader);
        xslSource.setSystemId(xslID);
        Transformer transformer = tfactory.newTransformer(xslSource);
        Reader xmlReader = new BufferedReader(new FileReader(sourceID));
        StreamSource xmlSource = new StreamSource(xmlReader);
        transformer.transform(xmlSource, new StreamResult(System.out));
    }

    /**
     * Perform a transformation using a compiled stylesheet (a Templates object)
     */
    public static void exampleUseTemplatesObj(String sourceID1, String sourceID2, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Templates templates = tfactory.newTemplates(new StreamSource(xslID));
        Transformer transformer1 = templates.newTransformer();
        Transformer transformer2 = templates.newTransformer();
        System.out.println("\n\n----- transform of " + sourceID1 + " -----");
        transformer1.transform(new StreamSource(sourceID1), new StreamResult(System.out));
        System.out.println("\n\n----- transform of " + sourceID2 + " -----");
        transformer2.transform(new StreamSource(sourceID2), new StreamResult(System.out));
    }

    /**
     * Perform a transformation using a compiled stylesheet (a Templates object)
     */
    public void exampleUseTemplatesHandler(String sourceID, String xslID) throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(SAXSource.FEATURE)) {
            SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);
            javax.xml.transform.sax.TemplatesHandler templatesHandler = stfactory.newTemplatesHandler();
            XMLReader reader = makeXMLReader();
            reader.setFeature("http://xml.org/sax/features/namespaces", true);
            reader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            XMLFilter filter = new ModifyStylesheetFilter();
            filter.setParent(reader);
            filter.setContentHandler(templatesHandler);
            filter.parse(new InputSource(xslID));
            Templates templates = templatesHandler.getTemplates();
            templates.newTransformer().transform(new StreamSource(sourceID), new StreamResult(System.out));
        } else {
            System.out.println("Factory doesn't implement SAXTransformerFactory");
        }
    }

    private class ModifyStylesheetFilter extends XMLFilterImpl {

        public void startDocument() throws SAXException {
            System.err.println("ModifyStylesheetFilter#startDocument");
            super.startDocument();
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            String lname = (qName.startsWith("xsl:") ? localName : ("XX" + localName));
            super.startElement(namespaceURI, lname, qName, atts);
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            String lname = (qName.startsWith("xsl:") ? localName : ("XX" + localName));
            super.endElement(namespaceURI, lname, qName);
        }
    }

    /**
     * Show the Transformer using SAX events in and SAX events out.
     */
    public static void exampleContentHandlerToContentHandler(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(SAXSource.FEATURE)) {
            SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);
            TransformerHandler handler = stfactory.newTransformerHandler(new StreamSource(xslID));
            Result result = new SAXResult(new ExampleContentHandler());
            handler.setResult(result);
            XMLReader reader = makeXMLReader();
            reader.setContentHandler(handler);
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            reader.parse(sourceID);
        } else {
            System.out.println("Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
        }
    }

    /**
     * Show the Transformer as a SAX2 XMLReader.  An XMLFilter obtained
     * from newXMLFilter should act as a transforming XMLReader if setParent is not
     * called.  Internally, an XMLReader is created as the parent for the XMLFilter.
     */
    public static void exampleXMLReader(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(SAXSource.FEATURE)) {
            XMLReader reader = ((SAXTransformerFactory) tfactory).newXMLFilter(new StreamSource(new File(xslID)));
            reader.setContentHandler(new ExampleContentHandler());
            reader.parse(new InputSource(new File(sourceID).toURL().toString()));
        } else {
            System.out.println("tfactory does not support SAX features!");
        }
    }

    /**
     * Show the Transformer as a simple XMLFilter.  This is pretty similar
     * to exampleXMLReader, except that here the parent XMLReader is created
     * by the caller, instead of automatically within the XMLFilter.  This
     * gives the caller more direct control over the parent reader.
     */
    public static void exampleXMLFilter(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        XMLReader reader = makeXMLReader();
        try {
            reader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            reader.setFeature("http://apache.org/xml/features/validation/dynamic", true);
        } catch (SAXException se) {
        }
        XMLFilter filter = ((SAXTransformerFactory) tfactory).newXMLFilter(new StreamSource(new File(xslID)));
        filter.setParent(reader);
        filter.setContentHandler(new ExampleContentHandler());
        filter.parse(new InputSource(new File(sourceID).toURL().toString()));
    }

    /**
     * This example shows how to chain events from one Transformer
     * to another transformer, using the Transformer as a
     * SAX2 XMLFilter/XMLReader.
     */
    public static void exampleXMLFilterChain(String sourceID, String xslID_1, String xslID_2, String xslID_3) throws TransformerException, TransformerConfigurationException, SAXException, IOException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Templates stylesheet1 = tfactory.newTemplates(new StreamSource(new File(xslID_1)));
        Transformer transformer1 = stylesheet1.newTransformer();
        if (tfactory.getFeature(SAXSource.FEATURE)) {
            SAXTransformerFactory stf = (SAXTransformerFactory) tfactory;
            XMLReader reader = makeXMLReader();
            XMLFilter filter1 = stf.newXMLFilter(new StreamSource(new File(xslID_1)));
            XMLFilter filter2 = stf.newXMLFilter(new StreamSource(new File(xslID_2)));
            XMLFilter filter3 = stf.newXMLFilter(new StreamSource(new File(xslID_3)));
            if (null != filter1) {
                filter1.setParent(reader);
                filter2.setParent(filter1);
                filter3.setParent(filter2);
                filter3.setContentHandler(new ExampleContentHandler());
                filter3.parse(new InputSource(new File(sourceID).toURL().toString()));
            } else {
                System.out.println("Can't do exampleXMLFilter because " + "tfactory doesn't support newXMLFilter()");
            }
        } else {
            System.out.println("Can't do exampleXMLFilter because " + "tfactory is not a SAXTransformerFactory");
        }
    }

    /**
     * Show how to transform a DOM tree into another DOM tree.
     * This uses the javax.xml.parsers to parse an XML file into a
     * DOM, and create an output DOM.
     */
    public static Node exampleDOMtoDOM(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(DOMSource.FEATURE)) {
            Templates templates;
            {
                System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.icl.saxon.om.DocumentBuilderFactoryImpl");
                DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
                System.err.println("Using DocumentBuilderFactory " + dfactory.getClass());
                dfactory.setNamespaceAware(true);
                DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
                System.err.println("Using DocumentBuilder " + docBuilder.getClass());
                org.w3c.dom.Document outNode = docBuilder.newDocument();
                Node doc = docBuilder.parse(new InputSource(new File(xslID).toURL().toString()));
                System.err.println("Stylesheet document built OK");
                DOMSource dsource = new DOMSource(doc);
                dsource.setSystemId(new File(xslID).toURL().toString());
                templates = tfactory.newTemplates(dsource);
            }
            Transformer transformer = templates.newTransformer();
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            org.w3c.dom.Document outNode = docBuilder.newDocument();
            Node doc = docBuilder.parse(new InputSource(new File(sourceID).toURL().toString()));
            System.err.println("Source document built OK");
            DOMSource ds = new DOMSource(doc);
            ds.setSystemId(new File(sourceID).toURL().toString());
            transformer.transform(ds, new DOMResult(outNode));
            System.err.println("Transformation done OK");
            Transformer serializer = tfactory.newTransformer();
            serializer.transform(new DOMSource(outNode), new StreamResult(System.out));
            return outNode;
        } else {
            throw new org.xml.sax.SAXNotSupportedException("DOM node processing not supported!");
        }
    }

    /**
     * Show how to transform a DOM tree into another DOM tree.
     * This uses the javax.xml.parsers to parse an XML file into the source
     * DOM; it leaves the XSLT processor to create the result DOM.
     */
    public static void exampleDOMtoDOMNew(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(DOMSource.FEATURE)) {
            Templates templates;
            {
                System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.icl.saxon.om.DocumentBuilderFactoryImpl");
                DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
                System.err.println("Using DocumentBuilderFactory " + dfactory.getClass());
                dfactory.setNamespaceAware(true);
                DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
                System.err.println("Using DocumentBuilder " + docBuilder.getClass());
                org.w3c.dom.Document outNode = docBuilder.newDocument();
                Node doc = docBuilder.parse(new InputSource(new File(xslID).toURL().toString()));
                System.err.println("Stylesheet document built OK");
                DOMSource dsource = new DOMSource(doc);
                dsource.setSystemId(new File(xslID).toURL().toString());
                templates = tfactory.newTemplates(dsource);
            }
            Transformer transformer = templates.newTransformer();
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            Node doc = docBuilder.parse(new InputSource(new File(sourceID).toURL().toString()));
            System.err.println("Source document built OK");
            DOMSource ds = new DOMSource(doc);
            DOMResult result = new DOMResult();
            transformer.transform(ds, result);
            System.err.println("Transformation done OK");
            Transformer serializer = tfactory.newTransformer();
            serializer.transform(new DOMSource(result.getNode()), new StreamResult(System.out));
            int k = result.getNode().getChildNodes().getLength();
            System.err.println("Result root has " + k + " children");
        } else {
            throw new org.xml.sax.SAXNotSupportedException("DOM node processing not supported!");
        }
    }

    /**
     * Show how to transform a DOM tree into another DOM tree.
     * This uses the javax.xml.parsers to parse an XML file into a
     * DOM, and create an output DOM. In this example, the start node
     * for the transformation is an element, not the root node.
     */
    public static Node exampleDOMtoDOMNonRoot(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(DOMSource.FEATURE)) {
            Templates templates;
            {
                System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.icl.saxon.om.DocumentBuilderFactoryImpl");
                DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
                System.err.println("Using DocumentBuilderFactory " + dfactory.getClass());
                dfactory.setNamespaceAware(true);
                DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
                System.err.println("Using DocumentBuilder " + docBuilder.getClass());
                org.w3c.dom.Document outNode = docBuilder.newDocument();
                Node doc = docBuilder.parse(new InputSource(new File(xslID).toURL().toString()));
                System.err.println("Stylesheet document built OK");
                DOMSource dsource = new DOMSource(doc);
                dsource.setSystemId(new File(xslID).toURL().toString());
                templates = tfactory.newTemplates(dsource);
            }
            Transformer transformer = templates.newTransformer();
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            Document outNode = docBuilder.newDocument();
            Document doc = docBuilder.parse(new InputSource(new File(sourceID).toURL().toString()));
            Node bar = doc.getDocumentElement().getFirstChild();
            while (bar.getNodeType() != Node.ELEMENT_NODE) {
                bar = bar.getNextSibling();
            }
            System.err.println("Source document built OK");
            DOMSource ds = new DOMSource(bar);
            ds.setSystemId(new File(sourceID).toURL().toString());
            transformer.transform(ds, new DOMResult(outNode));
            System.err.println("Transformation done OK");
            Transformer serializer = tfactory.newTransformer();
            serializer.transform(new DOMSource(outNode), new StreamResult(System.out));
            return outNode;
        } else {
            throw new org.xml.sax.SAXNotSupportedException("DOM node processing not supported!");
        }
    }

    /**
     * Show how to transform a DOM tree into another DOM tree.
     * This uses the javax.xml.parsers to parse an XML file into a
     * DOM, and create an output DOM. In this example, Saxon uses a
     * third-party DOM as both input and output.
     */
    public static void exampleDOMtoDOMAlien(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException, MalformedURLException {
        String factory = null;
        try {
            Class.forName("org.apache.crimson.jaxp.DocumentBuilderFactoryImpl");
            factory = "org.apache.crimson.jaxp.DocumentBuilderFactoryImpl";
        } catch (Exception e) {
            factory = null;
        }
        if (factory == null) {
            try {
                Class.forName("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
                factory = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";
            } catch (Exception e) {
                factory = null;
            }
        }
        if (factory == null) {
            System.err.println("No third-party DOM Builder found");
            return;
        }
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", factory);
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(DOMSource.FEATURE)) {
            Templates templates;
            {
                DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
                System.err.println("Using DocumentBuilderFactory " + dfactory.getClass());
                dfactory.setNamespaceAware(true);
                DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
                System.err.println("Using DocumentBuilder " + docBuilder.getClass());
                org.w3c.dom.Document outNode = docBuilder.newDocument();
                Node doc = docBuilder.parse(new InputSource(new File(xslID).toURL().toString()));
                System.err.println("Stylesheet document built OK");
                DOMSource dsource = new DOMSource(doc);
                dsource.setSystemId(new File(xslID).toURL().toString());
                templates = tfactory.newTemplates(dsource);
            }
            Transformer transformer = templates.newTransformer();
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new File(sourceID).toURL().toString()));
            Node bar = doc.getDocumentElement().getFirstChild();
            while (bar.getNodeType() != Node.ELEMENT_NODE) {
                bar = bar.getNextSibling();
            }
            System.err.println("Source document built OK");
            DOMSource ds = new DOMSource(bar);
            ds.setSystemId(new File(sourceID).toURL().toString());
            Document out = docBuilder.newDocument();
            Element extra = out.createElement("extra");
            out.appendChild(extra);
            transformer.transform(ds, new DOMResult(extra));
            System.err.println("Transformation done OK");
            Transformer serializer = tfactory.newTransformer();
            serializer.transform(new DOMSource(out), new StreamResult(System.out));
            return;
        } else {
            throw new org.xml.sax.SAXNotSupportedException("DOM node processing not supported!");
        }
    }

    /**
     * Identity transformation from a SAX Source to a new DOM result.
     * This leaves the XSLT processor to create the result DOM.
     */
    public static void exampleSAXtoDOMNewIdentity(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        SAXSource source = new SAXSource(new InputSource(sourceID));
        DOMResult result = new DOMResult();
        transformer.transform(source, result);
        System.err.println("Transformation done OK");
        Transformer serializer = tfactory.newTransformer();
        serializer.transform(new DOMSource(result.getNode()), new StreamResult(System.out));
        int k = result.getNode().getChildNodes().getLength();
        System.err.println("Result root has " + k + " children");
    }

    /**
     * Show how to transform directly from a Saxon NodeInfo object.
     * This example is peculiar to Saxon: it is not pure JAXP.
     */
    public static void exampleNodeInfo(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException, MalformedURLException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Templates templates = tfactory.newTemplates(new StreamSource(xslID));
        Transformer transformer = templates.newTransformer();
        com.icl.saxon.om.Builder builder = ((com.icl.saxon.Controller) transformer).makeBuilder();
        com.icl.saxon.om.DocumentInfo doc = builder.build(new SAXSource(new InputSource(sourceID)));
        transformer.transform(doc, new StreamResult(System.out));
    }

    /**
     * This shows how to set a parameter for use by the templates. Use
     * two transformers to show that different parameters may be set
     * on different transformers.
     */
    public static void exampleParam(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Templates templates = tfactory.newTemplates(new StreamSource(new File(xslID)));
        Transformer transformer1 = templates.newTransformer();
        Transformer transformer2 = templates.newTransformer();
        transformer1.setParameter("a-param", "hello to you!");
        transformer1.transform(new StreamSource(new File(sourceID)), new StreamResult(System.out));
        System.out.println("\n========= (and again with a different parameter value) ===");
        transformer1.setParameter("a-param", "goodbye to you!");
        transformer1.transform(new StreamSource(new File(sourceID)), new StreamResult(System.out));
        System.out.println("\n========= (and again with a no parameter value) ===");
        transformer2.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer2.transform(new StreamSource(new File(sourceID)), new StreamResult(System.out));
    }

    /**
     * Show the that a transformer can be reused, and show resetting
     * a parameter on the transformer.
     */
    public static void exampleTransformerReuse(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer(new StreamSource(new File(xslID)));
        transformer.setParameter("a-param", "hello to you!");
        transformer.transform(new StreamSource(new File(sourceID)), new StreamResult(System.out));
        System.out.println("\n=========\n");
        transformer.setParameter("a-param", "hello to me!");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new StreamSource(new File(sourceID)), new StreamResult(System.out));
    }

    /**
     * Show how to override output properties.
     */
    public static void exampleOutputProperties(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Templates templates = tfactory.newTemplates(new StreamSource(new File(xslID)));
        Properties oprops = templates.getOutputProperties();
        oprops.put(OutputKeys.INDENT, "yes");
        Transformer transformer = templates.newTransformer();
        transformer.setOutputProperties(oprops);
        transformer.transform(new StreamSource(new File(sourceID)), new StreamResult(System.out));
    }

    /**
     * Show how to get stylesheets that are associated with a given
     * xml document via the xml-stylesheet PI (see http://www.w3.org/TR/xml-stylesheet/).
     */
    public static void exampleUseAssociated(String sourceID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory instanceof SAXTransformerFactory) {
            SAXTransformerFactory stf = ((SAXTransformerFactory) tfactory);
            Source sources = stf.getAssociatedStylesheet(new StreamSource(sourceID), null, null, null);
            if (null != sources) {
                Transformer transformer = tfactory.newTransformer(sources);
                transformer.transform(new StreamSource(sourceID), new StreamResult(System.out));
            } else {
                System.out.println("Can't find the associated stylesheet!");
            }
        }
    }

    /**
     * Show the Transformer using SAX events in and DOM nodes out.
     */
    public static void exampleContentHandlertoDOM(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        if (tfactory.getFeature(SAXSource.FEATURE) && tfactory.getFeature(DOMSource.FEATURE)) {
            SAXTransformerFactory sfactory = (SAXTransformerFactory) tfactory;
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            org.w3c.dom.Document outNode = docBuilder.newDocument();
            TransformerHandler handler = sfactory.newTransformerHandler(new StreamSource(xslID));
            handler.setResult(new DOMResult(outNode));
            XMLReader reader = makeXMLReader();
            reader.setContentHandler(handler);
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            reader.parse(sourceID);
            exampleSerializeNode(outNode);
        } else {
            System.out.println("Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
        }
    }

    /**
     * Show a transformation using a user-written URI Resolver.
     */
    public static void exampleUsingURIResolver(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer(new StreamSource(xslID));
        transformer.setURIResolver(new UserURIResolver(transformer));
        transformer.transform(new StreamSource(sourceID), new StreamResult(System.out));
    }

    /**
    * A sample URIResolver. This handles a URI ending with ".txt". It loads the
    * text file identified by the URI, assuming it is in ISO-8859-1 encoding,
    * into a tree containing a single text node. It returns this
    * result tree to the transformer, exploiting the fact that a Saxon NodeInfo
    * can be used as a Source. If the URI doesn't end with ".txt", it hands over
    * to the standard URI resolver by returning null.
    */
    public static class UserURIResolver implements URIResolver {

        Transformer transformer;

        public UserURIResolver(Transformer t) {
            transformer = t;
        }

        /**
        * Resolve a URI
        * @param baseURI The base URI that should be used. May be null if uri is absolute.
        * @params uri The relative or absolute URI. May be an empty string. May contain
        * a fragment identifier starting with "#", which must be the value of an ID attribute
        * in the referenced XML document.
        * @return a Source object representing an XML document
        */
        public Source resolve(String href, String base) throws TransformerException {
            if (href.endsWith(".txt")) {
                try {
                    URL url = new URL(new URL(base), href);
                    java.io.InputStream in = url.openConnection().getInputStream();
                    java.io.InputStreamReader reader = new java.io.InputStreamReader(in, "iso-8859-1");
                    StringBuffer sb = new StringBuffer();
                    while (true) {
                        int c = reader.read();
                        if (c < 0) break;
                        sb.append((char) c);
                    }
                    com.icl.saxon.expr.TextFragmentValue tree = new com.icl.saxon.expr.TextFragmentValue(sb.toString(), url.toString(), (com.icl.saxon.Controller) transformer);
                    return tree.getFirst();
                } catch (Exception err) {
                    throw new TransformerException(err);
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Serialize a node to System.out.
     */
    public static void exampleSerializeNode(Node node) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer serializer = tfactory.newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.transform(new DOMSource(node), new StreamResult(System.out));
    }

    /**
     * A fuller example showing how the TrAX interface can be used
     * to serialize a DOM tree.
     */
    public static void exampleAsSerializer(String sourceID, String xslID) throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
        org.w3c.dom.Document outNode = docBuilder.newDocument();
        Node doc = docBuilder.parse(new InputSource(sourceID));
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer serializer = tfactory.newTransformer();
        Properties oprops = new Properties();
        oprops.put("method", "html");
        oprops.put("{http://icl.com/saxon}indent-spaces", "2");
        serializer.setOutputProperties(oprops);
        serializer.transform(new DOMSource(doc), new StreamResult(System.out));
    }

    private static void handleException(Exception ex) {
        System.out.println("EXCEPTION: " + ex);
        ex.printStackTrace();
        if (ex instanceof TransformerConfigurationException) {
            System.out.println();
            System.out.println("Test failed");
            Throwable ex1 = ((TransformerConfigurationException) ex).getException();
            if (ex1 != null) {
                ex1.printStackTrace();
                if (ex1 instanceof SAXException) {
                    Exception ex2 = ((SAXException) ex1).getException();
                    System.out.println("Internal sub-exception: ");
                    ex2.printStackTrace();
                }
            }
        }
    }

    /**
    * Make an XMLReader
    */
    private static XMLReader makeXMLReader() {
        return new com.icl.saxon.aelfred.SAXDriver();
    }
}
