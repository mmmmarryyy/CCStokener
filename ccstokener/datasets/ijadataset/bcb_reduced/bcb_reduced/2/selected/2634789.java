package edu.columbia.hypercontent;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ModelLoader;
import org.apache.xml.serialize.DOMSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.*;
import org.jasig.portal.PropertiesManager;
import org.jasig.portal.ResourceMissingException;
import org.jasig.portal.services.LogService;
import org.jasig.portal.utils.ResourceLoader;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.cyberneko.html.filters.ElementRemover;
import org.cyberneko.html.filters.DefaultFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.util.*;
import edu.columbia.filesystem.io.Utf8StreamReader;
import edu.columbia.filesystem.io.Utf8StreamWriter;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
public class DocumentFactory implements ICMSConstants {

    private static DocumentFactory _instance;

    protected static final boolean stylesheetRootCacheEnabled = PropertiesManager.getPropertyAsBoolean("org.jasig.portal.utils.XSLT.stylesheet_root_caching");

    protected static final Map templates = new HashMap();

    private static javax.xml.parsers.DocumentBuilderFactory dbFactory = null;

    private static final LocalRDFWriter localWriter = new LocalRDFWriter();

    private static final LocalDocumentBuilder localDocBuilder = new LocalDocumentBuilder();

    private static final LocalTransformerFactory localTransFactory = new LocalTransformerFactory();

    private static final EHandler ehandler = new EHandler();

    private static final LexicalHandler lhandler = new LHandler();

    private static final LocalXMLReaderArray localXmlReaders = new LocalXMLReaderArray();

    protected static class LocalXMLReaderArray extends ThreadLocal {

        protected Object initialValue() {
            return new XMLReader[10];
        }
    }

    protected static class LocalTransformerFactory extends ThreadLocal {

        protected Object initialValue() {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            tFactory.setErrorListener(getErrorListener());
            return tFactory;
        }
    }

    protected static class LocalRDFWriter extends ThreadLocal {

        protected Object initialValue() {
            RDFWriter writer = ModelFactory.createDefaultModel().getWriter(ModelLoader.langXMLAbbrev);
            writer.setProperty("blockRules", "propertyAttr");
            writer.setProperty("allowBadURIs", "true");
            writer.setProperty("relativeURIs", "");
            writer.setProperty("tab", "0");
            return writer;
        }
    }

    protected static class LocalDocumentBuilder extends ThreadLocal {

        protected Object initialValue() {
            Object r = null;
            try {
                r = instance().dbFactory.newDocumentBuilder();
            } catch (Exception e) {
                LogService.log(LogService.ERROR, e);
            }
            return r;
        }
    }

    private DocumentFactory() {
        try {
            dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setIgnoringElementContentWhitespace(true);
            dbFactory.setCoalescing(true);
            dbFactory.setIgnoringComments(true);
            dbFactory.setValidating(false);
        } catch (Exception e) {
            LogService.log(LogService.ERROR, "DocumentFactory: unable to initialize DocumentBuilderFactory");
            LogService.log(LogService.ERROR, e);
        }
    }

    public static TransformerFactory getTransformerFactory(URIResolver resolver) {
        TransformerFactory tf = (TransformerFactory) localTransFactory.get();
        tf.setURIResolver(resolver);
        return tf;
    }

    public static javax.xml.parsers.DocumentBuilder newDocumentBuilder(EntityResolver resolver) {
        DocumentBuilder builder = (DocumentBuilder) localDocBuilder.get();
        builder.setEntityResolver(resolver);
        return builder;
    }

    public static org.w3c.dom.Document newDocument(EntityResolver resolver) {
        Document doc = null;
        DocumentBuilder builder = newDocumentBuilder(resolver);
        doc = builder.newDocument();
        return doc;
    }

    public static XMLReader getXMLReader(EntityResolver resolver) throws SAXException {
        XMLReader[] xmlReaders = (XMLReader[]) localXmlReaders.get();
        XMLReader reader = null;
        for (int i = 0; i < xmlReaders.length; i++) {
            if (xmlReaders[i] != null) {
                reader = xmlReaders[i];
                xmlReaders[i] = null;
                break;
            }
        }
        if (reader == null) {
            try {
                reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
                reader.setFeature("http://xml.org/sax/features/validation", false);
                reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (Exception e) {
                reader = XMLReaderFactory.createXMLReader();
            } finally {
                reader.setFeature("http://xml.org/sax/features/validation", false);
            }
        }
        reader.setErrorHandler(ehandler);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", lhandler);
        reader.setEntityResolver(resolver);
        return reader;
    }

    public static void recycleXMLReader(XMLReader reader) {
        XMLReader[] xmlReaders = (XMLReader[]) localXmlReaders.get();
        for (int i = 0; i < xmlReaders.length; i++) {
            if (xmlReaders[i] == null) {
                xmlReaders[i] = reader;
                break;
            }
        }
    }

    public static InputStream getStreamFromSystemIdentifier(String systemId, EntityResolver resolver) throws Exception {
        InputSource source = null;
        InputStream stream = null;
        if (resolver != null) {
            try {
                source = resolver.resolveEntity(null, systemId);
            } catch (Exception e) {
                LogService.instance().log(LogService.ERROR, "DocumentFactory: Unable to resolve '" + systemId + "'");
                LogService.instance().log(LogService.ERROR, e);
            }
        }
        if (source != null) {
            try {
                stream = source.getByteStream();
            } catch (Exception e) {
                LogService.instance().log(LogService.ERROR, "DocumentFactory: Unable to get bytestream from '" + source.getSystemId() + "'");
                LogService.instance().log(LogService.ERROR, e);
            }
        }
        if (stream == null) {
            URL url = new URL(systemId);
            stream = url.openStream();
        }
        return stream;
    }

    public static char[] getCharArrayFromDocument(Document doc) throws CMSException {
        return getCharArrayFromDocument(doc, false);
    }

    protected static void writeDocument(Document doc, Writer writer, boolean asFragment) throws CMSException {
        OutputFormat of = new OutputFormat(doc);
        of.setEncoding("UTF-8");
        if (asFragment) {
            of.setOmitDocumentType(true);
            of.setOmitXMLDeclaration(true);
        } else {
            of.setOmitDocumentType(false);
            of.setOmitXMLDeclaration(false);
        }
        try {
            DOMSerializer serializer = new XMLSerializer(writer, of);
            serializer.serialize(doc);
            writer.close();
        } catch (Exception e) {
            throw new CMSException(CMSException.PROCESSING_ERROR, e);
        }
    }

    public static char[] getCharArrayFromDocument(Document doc, boolean asFragment) throws CMSException {
        CharArrayWriter writer = new CharArrayWriter();
        Writer buf = new BufferedWriter(writer, BUFFER_SIZE);
        writeDocument(doc, buf, asFragment);
        return writer.toCharArray();
    }

    public static InputStream getStreamFromDocument(Document doc) throws CMSException {
        return getStreamFromDocument(doc, false);
    }

    public static InputStream getStreamFromDocument(Document doc, boolean asFragment) throws CMSException {
        return new ByteArrayInputStream(getByteArrayFromDocument(doc, asFragment));
    }

    public static byte[] getByteArrayFromDocument(Document doc) throws CMSException {
        return getByteArrayFromDocument(doc, false);
    }

    public static byte[] getByteArrayFromDocument(Document doc, boolean asFragment) throws CMSException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = Utf8StreamWriter.getThreadLocal().setOutputStream(out);
        writeDocument(doc, writer, asFragment);
        return out.toByteArray();
    }

    public static void parseHTMLFragment(DocumentFragment frag, String html) throws CMSException {
        try {
            DOMFragmentParser parser = new DOMFragmentParser();
            parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");
            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
            parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");
            ElementRemover remover = new ElementRemover();
            remover.acceptElement("a", new String[] { "href", "target", "name", "rel", "shape", "coords", "title" });
            remover.acceptElement("area", new String[] { "alt", "coords", "href", "nohref", "shape", "target" });
            remover.acceptElement("b", null);
            remover.acceptElement("big", null);
            remover.acceptElement("blockquote", new String[] { "cite" });
            remover.acceptElement("br", null);
            remover.acceptElement("caption", null);
            remover.acceptElement("center", null);
            remover.acceptElement("code", null);
            remover.acceptElement("div", new String[] { "align" });
            remover.acceptElement("em", null);
            remover.acceptElement("form", new String[] { "action", "enctype", "method", "type", "target", "onsubmit" });
            remover.acceptElement("h1", null);
            remover.acceptElement("h2", null);
            remover.acceptElement("h3", null);
            remover.acceptElement("h4", null);
            remover.acceptElement("h5", null);
            remover.acceptElement("h6", null);
            remover.acceptElement("hr", null);
            remover.acceptElement("i", null);
            remover.acceptElement("img", new String[] { "src", "width", "height", "border", "vspace", "hspace", "alt", "title", "align", "ismap", "usemap" });
            remover.acceptElement("input", new String[] { "alt", "align", "checked", "disabled", "maxlength", "name", "readonly", "src", "size", "type", "value" });
            remover.acceptElement("li", new String[] { "type", "value" });
            remover.acceptElement("map", new String[] { "id", "name" });
            remover.acceptElement("ol", new String[] { "compact", "start", "type" });
            remover.acceptElement("option", new String[] { "disabled", "label", "selected", "value" });
            remover.acceptElement("p", new String[] { "align" });
            remover.acceptElement("pre", new String[] { "width" });
            remover.acceptElement("s", null);
            remover.acceptElement("select", new String[] { "disabled", "multiple", "name", "size" });
            remover.acceptElement("small", null);
            remover.acceptElement("strike", null);
            remover.acceptElement("strong", null);
            remover.acceptElement("sub", null);
            remover.acceptElement("sup", null);
            remover.acceptElement("table", new String[] { "align", "border", "cellpadding", "cellspacing", "summary", "width" });
            remover.acceptElement("td", new String[] { "abbr", "align", "colspan", "height", "nowrap", "rowspan", "valign", "width" });
            remover.acceptElement("textarea", new String[] { "cols", "rows", "disabled", "readonly", "name" });
            remover.acceptElement("th", new String[] { "abbr", "align", "colspan", "height", "nowrap", "rowspan", "valign", "width" });
            remover.acceptElement("tr", new String[] { "align", "valign" });
            remover.acceptElement("tt", null);
            remover.acceptElement("u", null);
            remover.acceptElement("ul", new String[] { "compact", "type" });
            remover.acceptElement("var", null);
            remover.removeElement("head");
            remover.removeElement("style");
            remover.removeElement("meta");
            remover.removeElement("link");
            remover.removeElement("title");
            remover.removeElement("script");
            remover.removeElement("noscript");
            HTMLCleaner cleaner = new HTMLCleaner();
            cleaner.acceptEmptyElement("area");
            cleaner.acceptEmptyElement("br");
            cleaner.acceptEmptyElement("img");
            cleaner.acceptEmptyElement("input");
            cleaner.acceptEmptyElement("hr");
            cleaner.acceptEmptyElement("li");
            cleaner.acceptEmptyElement("option");
            cleaner.acceptEmptyElement("p");
            cleaner.acceptEmptyElement("select");
            cleaner.acceptEmptyElement("td");
            cleaner.acceptEmptyElement("textarea");
            cleaner.acceptEmptyElement("th");
            cleaner.acceptEmptyElement("tr");
            cleaner.acceptEmptyElement("td");
            cleaner.acceptEmptyElement("textarea");
            cleaner.translateTag("i", "em");
            cleaner.translateTag("b", "strong");
            XMLDocumentFilter[] filters = new XMLDocumentFilter[] { remover, cleaner };
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
            parser.parse(new InputSource(new StringReader(html)), frag);
        } catch (Exception e) {
            throw new CMSException(CMSException.PROCESSING_ERROR, e);
        }
    }

    public static Document getDocumentFromStream(InputStream stream, EntityResolver resolver) throws CMSException {
        try {
            Reader reader = Utf8StreamReader.getThreadLocal().setInputStream(stream);
            InputSource source = new InputSource(reader);
            DocumentBuilder builder = newDocumentBuilder(resolver);
            Document doc = builder.parse(source);
            reader.close();
            return doc;
        } catch (Exception e) {
            throw new CMSException(CMSException.PROCESSING_ERROR, e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] getRDFBytes(Resource metadata) {
        return getRDFBytes(metadata.getModel());
    }

    private static byte[] getRDFBytes(Model model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8StreamWriter writer = Utf8StreamWriter.getThreadLocal().setOutputStream(out);
        writeRDF(model, writer);
        try {
            writer.close();
        } catch (IOException e) {
        }
        return out.toByteArray();
    }

    public static char[] getRDFChars(Resource metadata) {
        return (getRDFChars(metadata.getModel()));
    }

    public static char[] getRDFChars(Model model) {
        CharArrayWriter writer = new CharArrayWriter(256);
        writeRDF(model, writer);
        return writer.toCharArray();
    }

    protected static void writeRDF(Model model, Writer writer) {
        RDFWriter rw = (RDFWriter) localWriter.get();
        rw.write(model, writer, "");
    }

    protected static void writeRDF(Model model, OutputStream out) {
        RDFWriter rw = (RDFWriter) localWriter.get();
        rw.write(model, out, "");
    }

    public static Document getRDFDocument(Resource metadata, EntityResolver resolver) throws CMSException {
        return getRDFDocument(metadata.getModel(), resolver);
    }

    public static Document getRDFDocument(Model model, EntityResolver resolver) throws CMSException {
        Document doc = null;
        try {
            CharArrayReader reader = new CharArrayReader(getRDFChars(model));
            InputSource source = new InputSource(reader);
            DocumentBuilder builder = newDocumentBuilder(resolver);
            doc = builder.parse(source);
        } catch (Exception e) {
            throw new CMSException(CMSException.PROCESSING_ERROR, e);
        }
        return doc;
    }

    public static byte[] transform(Templates templates, InputSource source, Resolver resolver, RequestTracker.Errors listener, Map parameters) throws TransformerConfigurationException, CMSException, IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE);
        transform(templates, source, resolver, listener, parameters, os);
        return os.toByteArray();
    }

    public static byte[] transform(Templates templates, SAXParser parser, Resolver resolver, RequestTracker.Errors listener, Map parameters) throws TransformerConfigurationException, IOException, CMSException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE);
        Utf8StreamWriter writer = Utf8StreamWriter.getThreadLocal().setOutputStream(os);
        Result result = new StreamResult(writer);
        transform(templates, parser, resolver, listener, parameters, result);
        writer.close();
        return os.toByteArray();
    }

    public static void transform(Templates templates, InputSource source, Resolver resolver, RequestTracker.Errors listener, Map parameters, OutputStream out) throws TransformerConfigurationException, IOException, CMSException {
        Utf8StreamWriter writer = Utf8StreamWriter.getThreadLocal().setOutputStream(out);
        transform(templates, source, resolver, listener, parameters, writer);
        out.close();
    }

    public static void transform(Templates templates, InputSource source, Resolver resolver, RequestTracker.Errors listener, Map parameters, Writer writer) throws TransformerConfigurationException, CMSException, IOException {
        Result result = new StreamResult(writer);
        transform(templates, source, resolver, listener, parameters, result);
        writer.close();
    }

    public static void transform(Templates templates, final InputSource source, Resolver resolver, RequestTracker.Errors listener, Map parameters, Result result) throws TransformerConfigurationException, CMSException {
        SAXParser parser = new SAXParser() {

            public void parse(ContentHandler conHandler, LexicalHandler lexHandler, ErrorHandler errHandler, Resolver resolver) throws CMSException {
                try {
                    XMLReader reader = getXMLReader(resolver);
                    reader.setErrorHandler(errHandler);
                    reader.setContentHandler(conHandler);
                    reader.setProperty("http://xml.org/sax/properties/lexical-handler", lexHandler);
                    reader.setProperty("http://apache.org/xml/properties/input-buffer-size", new Integer(BUFFER_SIZE));
                    reader.parse(source);
                    recycleXMLReader(reader);
                } catch (Exception e) {
                    throw new CMSException(CMSException.PROCESSING_ERROR, e);
                }
            }
        };
        transform(templates, parser, resolver, listener, parameters, result);
    }

    public static void transform(Templates templates, SAXParser parser, Resolver resolver, RequestTracker.Errors listener, Map parameters, Result result) throws CMSException, TransformerConfigurationException {
        TransformerHandler transformerHandler = ((SAXTransformerFactory) getTransformerFactory(resolver)).newTransformerHandler(templates);
        Transformer trans = transformerHandler.getTransformer();
        trans.setURIResolver(resolver);
        trans.setErrorListener(listener);
        Iterator keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            trans.setParameter(key, parameters.get(key));
        }
        transformerHandler.setResult(result);
        parser.parse(transformerHandler, transformerHandler, listener, resolver);
    }

    public static Templates getTemplates(InputSource source, Resolver resolver) throws TransformerConfigurationException, SAXException, IOException {
        javax.xml.transform.sax.SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) getTransformerFactory(resolver));
        TemplatesHandler templatesHandler = saxTFactory.newTemplatesHandler();
        XMLReader reader = getXMLReader(resolver);
        reader.setContentHandler(templatesHandler);
        reader.parse(source);
        recycleXMLReader(reader);
        return templatesHandler.getTemplates();
    }

    public static Transformer getTransformer(String xslURI, ResourceBundle localization, Resolver resolver) throws ResourceMissingException, IOException, TransformerConfigurationException, CMSException {
        Transformer transformer = null;
        String lookup = new StringBuffer(xslURI).append(localization.getLocale().toString()).toString();
        Templates temp = (Templates) templates.get(lookup);
        if (temp == null) {
            synchronized (templates) {
                Document xsl = getDocumentFromStream(ResourceLoader.getResourceAsStream(DocumentFactory.class, xslURI), resolver);
                addLocalization(xsl, localization);
                Source src = new DOMSource(xsl);
                TransformerFactory tFactory = getTransformerFactory(resolver);
                temp = tFactory.newTemplates(src);
                if (stylesheetRootCacheEnabled) {
                    templates.put(lookup, temp);
                }
            }
        }
        transformer = temp.newTransformer();
        transformer.setURIResolver(resolver);
        return transformer;
    }

    private static void addLocalization(Document xsl, ResourceBundle localization) {
        ArrayList keys = new ArrayList();
        Enumeration en = localization.getKeys();
        while (en.hasMoreElements()) {
            keys.add(en.nextElement());
        }
        Element root = xsl.getDocumentElement();
        Node ft = root.getFirstChild();
        boolean foundFT = false;
        NodeList nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == n.ELEMENT_NODE) {
                Element e = (Element) n;
                if (!foundFT && e.getNamespaceURI().equals("http://www.w3.org/1999/XSL/Transform") && e.getLocalName().equals("template")) {
                    ft = n;
                    foundFT = true;
                }
                if (e.getNamespaceURI().equals("http://www.w3.org/1999/XSL/Transform") && e.getLocalName().equals("variable")) {
                    String name = e.getAttribute("name");
                    if (keys.contains(name)) {
                        e.removeAttribute("select");
                        if (e.hasChildNodes()) {
                            NodeList cl = e.getChildNodes();
                            for (int j = cl.getLength() - 1; j >= 0; j--) {
                                e.removeChild(cl.item(j));
                            }
                        }
                        e.setAttribute("select", "'" + localization.getString(name) + "'");
                        keys.remove(name);
                    }
                }
            }
        }
        for (int z = 0; z < keys.size(); z++) {
            String k = (String) keys.get(z);
            String v = localization.getString(k);
            Element e = xsl.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:variable");
            e.setAttribute("name", k);
            e.setAttribute("select", "'" + v + "'");
            root.insertBefore(e, ft);
        }
    }

    public static ErrorListener getErrorListener() {
        return ehandler;
    }

    public static ErrorHandler getErrorHandler() {
        return ehandler;
    }

    public static LexicalHandler getLexicalHandler() {
        return lhandler;
    }

    private static synchronized DocumentFactory instance() {
        if (_instance == null) {
            _instance = new DocumentFactory();
        }
        return _instance;
    }

    protected static class EHandler implements ErrorHandler, ErrorListener {

        public void warning(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void warning(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void error(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            throw exception;
        }
    }

    protected static class LHandler implements LexicalHandler {

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
        }

        public void endDTD() throws SAXException {
        }

        public void startEntity(String name) throws SAXException {
        }

        public void endEntity(String name) throws SAXException {
        }

        public void startCDATA() throws SAXException {
        }

        public void endCDATA() throws SAXException {
        }

        public void comment(char ch[], int start, int length) throws SAXException {
        }
    }

    protected static class HTMLCleaner extends DefaultFilter implements XMLDocumentFilter {

        ArrayList acceptEmpty = new ArrayList();

        HashMap translateTag = new HashMap();

        boolean stripComments = true;

        boolean stripPI = true;

        boolean ignoreWhiteSpace = false;

        public void acceptEmptyElement(String tagname) {
            acceptEmpty.add(tagname.toLowerCase());
        }

        public void translateTag(String fromtag, String totag) {
            translateTag.put(fromtag.toLowerCase(), totag);
        }

        void translateQName(QName element) {
            String newTag = (String) translateTag.get(element.localpart.toLowerCase());
            if (newTag != null) {
                element.setValues(null, newTag, newTag, null);
            }
        }

        boolean emptyAccepted(QName element) {
            return acceptEmpty.contains(element.localpart.toLowerCase());
        }

        public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
            translateQName(element);
            if (emptyAccepted(element)) {
                super.emptyElement(element, attributes, augs);
            }
        }

        public void characters(XMLString text, Augmentations augs) {
            if (!ignoreWhiteSpace || text.toString().trim().length() > 0) {
                super.characters(text, augs);
            }
        }

        public void ignorableWhitespace(XMLString text, Augmentations augs) {
            if (!ignoreWhiteSpace) {
                super.ignorableWhitespace(text, augs);
            }
        }

        public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
            translateQName(element);
            super.startElement(element, attributes, augs);
        }

        public void endElement(QName element, Augmentations augs) throws XNIException {
            translateQName(element);
            super.endElement(element, augs);
        }

        public void comment(XMLString text, Augmentations augs) {
            if (!stripComments) {
                super.comment(text, augs);
            }
        }

        public void processingInstruction(java.lang.String target, XMLString data, Augmentations augs) {
            if (!stripPI) {
                super.processingInstruction(target, data, augs);
            }
        }
    }
}
