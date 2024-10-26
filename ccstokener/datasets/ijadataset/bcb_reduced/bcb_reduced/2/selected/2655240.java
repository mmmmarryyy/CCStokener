package org.allcolor.xml.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.allcolor.dtd.parser.CDTDParser;
import org.allcolor.dtd.parser.CDocType;
import org.allcolor.dtd.parser.CEntity;
import org.allcolor.xml.parser.dom.ADocument;
import org.allcolor.xml.parser.dom.CAttr;
import org.allcolor.xml.parser.dom.CDom2HTMLDocument;
import org.allcolor.xml.parser.dom.CEntityCoDec;
import org.allcolor.xml.parser.dom.ANode.CNamespace;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributeListImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A fast SAX Parser Implementation. LexicalHandler, EntityResolver and
 * DTDHandler are supported, juste register it with the parser.
 * 
 * @author Quentin Anciaux - Main author
 * @author Tom Finnelly - Patch quoted in code
 * @author Jarle H. Næss- Patch quoted in code
 */
public class CShaniSaxParser extends SAXParser implements XMLReader, Parser, IParseHandler {

    /** DOCUMENT ME! */
    public static final String CASE_INSENSITIVE = "http://www.allcolor.org/xml/caseinsensitive/";

    /** author Tom Finnelly */
    public static final String DECODE_ENTITIES = "http://www.allcolor.org/xml/decodeentities/";

    /** DOCUMENT ME! */
    public static final String STRICT_MODE = "http://www.allcolor.org/xml/strictmode/";

    /** DOCUMENT ME! */
    public static final String VALIDATE = "http://www.allcolor.org/xml/validate/";

    /**
	 * DOCUMENT ME!
	 * 
	 * @param args
	 *            DOCUMENT ME!
	 * 
	 * @throws Exception
	 *             DOCUMENT ME!
	 */
    public static void main(final String args[]) throws Exception {
        long saxParserTime = 0;
        long integratedParser = 0;
        for (int i = 0; i < 48; i++) {
            if ((i % 2) == 0) {
                saxParserTime += CShaniSaxParser.testShaniSax();
            } else {
                integratedParser += CShaniSaxParser.testIntegSax();
            }
        }
        System.out.println("ShaniSaxParser Time = " + (saxParserTime / 24));
        System.out.println("integrated parser Time = " + (integratedParser / 24));
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws Exception
	 *             DOCUMENT ME!
	 */
    public static long testIntegSax() throws Exception {
        long saxParserTime = 0;
        {
            SAXParserFactory fact = SAXParserFactory.newInstance();
            fact.setValidating(false);
            fact.setNamespaceAware(true);
            XMLReader parserI = fact.newSAXParser().getXMLReader();
            parserI.setContentHandler(new DefaultHandler());
            System.out.println("Starting integrated parser");
            long beginTime = System.currentTimeMillis();
            for (int i = 0; i < 2500; i++) {
                parserI.parse(new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" + "<!-- this is a comment -->\n" + "<!-- this is another comment -->\n" + "<test xmlns:html=\"http://toto\">\n" + "	<a id=\"toto\">hello</a>\n" + "	<!-- and one more comment -->\n" + "	<html:div>toto</html:div>\n" + "	<b>totot <u>li&amp;li</u></b>\n" + "	<ul><li>lol</li></ul>\n" + "</test>")));
            }
            long endTime = System.currentTimeMillis();
            saxParserTime = endTime - beginTime;
        }
        return saxParserTime;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws Exception
	 *             DOCUMENT ME!
	 */
    public static long testShaniSax() throws Exception {
        long saxParserTime = 0;
        {
            XMLReader parser = new CShaniSaxParser();
            parser.setContentHandler(new DefaultHandler());
            System.out.println("Starting ShaniSaxParser parser");
            long beginTime = System.currentTimeMillis();
            for (int i = 0; i < 2500; i++) {
                parser.parse(new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" + "<!-- this is a comment -->\n" + "<!-- this is another comment -->\n" + "<test xmlns:html=\"http://toto\">\n" + "	<a id=\"toto\">hello</a>\n" + "	<!-- and one more comment -->\n" + "	<html:div>toto</html:div>\n" + "	<b>totot <u>li&amp;li</u></b>\n" + "	<ul><li>lol</li></ul>\n" + "</test>")));
            }
            long endTime = System.currentTimeMillis();
            saxParserTime = endTime - beginTime;
        }
        return saxParserTime;
    }

    /** DOCUMENT ME! */
    private CAttributes2Impl atts = new CAttributes2Impl();

    /** DOCUMENT ME! */
    private AttributeListImpl attsOld = new AttributeListImpl();

    /** DOCUMENT ME! */
    private boolean caseInsensitive = false;

    /** DOCUMENT ME! */
    private ContentHandler cHandler = null;

    /** DOCUMENT ME! */
    private CEntityCoDec codec = new CEntityCoDec(new HashMap());

    /** Decode the entities in Text ? author Tom Fennelly */
    private boolean decodeEntites = true;

    /** DOCUMENT ME! */
    private DocumentHandler dHandler = null;

    /** DOCUMENT ME! */
    private CDocType docType = null;

    /** DOCUMENT ME! */
    private String documentURI = null;

    /** DOCUMENT ME! */
    private DTDHandler dtdHandler = null;

    /** DOCUMENT ME! */
    private EntityResolver entityResolver = null;

    /** DOCUMENT ME! */
    private ErrorHandler errorHandler = null;

    /** DOCUMENT ME! */
    private Map features = new HashMap();

    /** DOCUMENT ME! */
    private boolean firstTag = false;

    /** DOCUMENT ME! */
    private boolean htmlDocument = false;

    /** DOCUMENT ME! */
    private int inXInclude = 0;

    /** DOCUMENT ME! */
    private int level = 0;

    /** DOCUMENT ME! */
    private LexicalHandler lHandler = null;

    /** DOCUMENT ME! */
    private ThreadLocal localParser = new ThreadLocal();

    /** DOCUMENT ME! */
    private int maxLevel = 0;

    /** DOCUMENT ME! */
    private boolean nPrefix = false;

    /** DOCUMENT ME! */
    private Map nsMap = new HashMap();

    /** DOCUMENT ME! */
    private CXmlParser parser = null;

    /** DOCUMENT ME! */
    private Map properties = new HashMap();

    /** DOCUMENT ME! */
    private Stack stack = new Stack();

    /** DOCUMENT ME! */
    private int xiFallbackFlag = 0;

    private CNamespace xml = new CNamespace("xml", "http://www.w3.org/XML/1998/namespace");

    /** DOCUMENT ME! */
    private CNamespace xmlns = new CNamespace("xmlns", "http://www.w3.org/2000/xmlns/");

    /**
	 * Creates a new CShaniSaxParser object.
	 */
    public CShaniSaxParser() {
        this.features.put("namespaces", "" + true);
        this.features.put("use-attributes2", "" + true);
        this.features.put("namespace-prefixes", "" + false);
    }

    /** DOCUMENT ME! */
    public static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    /**
	 * Check the parser features and set appropriate runtime flags.
	 * 
	 * @throws SAXNotRecognizedException
	 * @throws SAXNotSupportedException
	 *             author Tom Fennelly
	 */
    private void checkFeatures() throws SAXNotRecognizedException, SAXNotSupportedException {
        this.nPrefix = this.getFeature("namespace-prefixes");
        this.caseInsensitive = this.getFeature(CShaniSaxParser.CASE_INSENSITIVE);
        this.decodeEntites = this.getFeature(CShaniSaxParser.DECODE_ENTITIES, true);
        this.ignoreDTD = this.getFeature(CShaniSaxParser.DISALLOW_DOCTYPE_DECL, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void documentEnd() {
        try {
            if (this.cHandler != null) {
                this.cHandler.endDocument();
            }
            if (this.dHandler != null) {
                this.dHandler.endDocument();
            }
            this.nsMap.clear();
            this.level = 0;
        } catch (SAXException e) {
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void documentStart() {
        try {
            if (this.cHandler != null) {
                this.cHandler.startDocument();
            }
            if (this.dHandler != null) {
                this.dHandler.startDocument();
            }
            this.level = 0;
            this.maxLevel = 0;
            this.htmlDocument = false;
            this.firstTag = true;
            this.nsMap.clear();
            this.nsMap.put("xmlns", this.xmlns);
            this.nsMap.put("xml", this.xml);
        } catch (SAXException e) {
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public ContentHandler getContentHandler() {
        return this.cHandler;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public DTDHandler getDTDHandler() {
        return this.dtdHandler;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public EntityResolver getEntityResolver() {
        return this.entityResolver;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws SAXNotRecognizedException
	 *             DOCUMENT ME!
	 * @throws SAXNotSupportedException
	 *             DOCUMENT ME!
	 */
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.getFeature(name, false);
    }

    /**
	 * Get the on/off status of the named feature. This implementation allows
	 * specification of a default feature on/off indicator.
	 * 
	 * @param name
	 *            Feature name.
	 * @param defaultVal
	 *            Default feature on/off status.
	 * 
	 * @return The defaultVal if the feature is not set, true if the feature is
	 *         set on, otherwise false. author Tom Fennelly
	 */
    private boolean getFeature(final String name, final boolean defaultVal) {
        String value = (String) this.features.get(name);
        if (value == null) {
            return defaultVal;
        }
        return value.equals("true");
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws SAXException
	 *             DOCUMENT ME!
	 */
    public Parser getParser() throws SAXException {
        return this;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws SAXNotRecognizedException
	 *             DOCUMENT ME!
	 * @throws SAXNotSupportedException
	 *             DOCUMENT ME!
	 */
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.properties.get(name);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws SAXException
	 *             DOCUMENT ME!
	 */
    public XMLReader getXMLReader() throws SAXException {
        return this;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param localName
	 *            DOCUMENT ME!
	 * @param atts
	 *            DOCUMENT ME!
	 */
    private void handleXInclude(final String localName, final Attributes atts) {
        if ("include".equals(localName)) {
            this.inXInclude++;
            String href = atts.getValue("href");
            if ((href == null) || "".equals(href.trim())) {
                href = null;
            }
            String parse = atts.getValue("parse");
            if ((parse == null) || "".equals(parse.trim())) {
                parse = "xml";
            }
            String xpointer = atts.getValue("xpointer");
            if ((xpointer == null) || "".equals(xpointer.trim())) {
                xpointer = null;
            }
            String encoding = atts.getValue("encoding");
            if ((encoding == null) || "".equals(encoding.trim())) {
                encoding = null;
            }
            String accept = atts.getValue("accept");
            if ((accept == null) || "".equals(accept.trim())) {
                accept = null;
            }
            String accept_language = atts.getValue("accept-language");
            if ((accept_language == null) || "".equals(accept_language.trim())) {
                accept_language = null;
            }
            if (href != null) {
                if (href.indexOf(":/") == -1) {
                    if (href.startsWith("/")) {
                        href = href.substring(1);
                    }
                    href = this.documentURI + href;
                }
                if (this.localParser.get() == null) {
                    this.localParser.set(new CShaniDomParser());
                }
                CShaniDomParser p = (CShaniDomParser) this.localParser.get();
                InputStream in = null;
                try {
                    URL url = new URL(href);
                    URLConnection connection = url.openConnection();
                    if (accept != null) {
                        connection.addRequestProperty("Accept", accept);
                    }
                    if (accept_language != null) {
                        connection.addRequestProperty("Accept-Language", accept_language);
                    }
                    in = connection.getInputStream();
                    ADocument doc = null;
                    if (encoding != null) {
                        doc = (ADocument) p.parse(new InputStreamReader(in, encoding));
                    } else {
                        doc = (ADocument) p.parse(in);
                    }
                    if (xpointer == null) {
                        CDOM2SAX converter = new CDOM2SAX(doc.getDocumentElement());
                        converter.setProperty("http://xml.org/sax/properties/lexical-handler", this.lHandler);
                        converter.setContentHandler(this.cHandler);
                        converter.setDocumentHandler(this.dHandler);
                        converter.setDTDHandler(this.dtdHandler);
                        converter.serialize();
                    } else {
                        XPath xpath = new DOMXPath(xpointer);
                        for (Iterator it = doc.getNamespaceList().iterator(); it.hasNext(); ) {
                            CNamespace ns = (CNamespace) it.next();
                            xpath.addNamespace(ns.getPrefix() == null ? "" : ns.getPrefix(), ns.getNamespaceURI());
                        }
                        List result = xpath.selectNodes(doc.getDocumentElement());
                        for (final Iterator it = result.iterator(); it.hasNext(); ) {
                            final Node node = (Node) it.next();
                            CDOM2SAX converter = new CDOM2SAX(node);
                            converter.setProperty("http://xml.org/sax/properties/lexical-handler", this.lHandler);
                            converter.setContentHandler(this.cHandler);
                            converter.setDocumentHandler(this.dHandler);
                            converter.setDTDHandler(this.dtdHandler);
                            converter.serialize();
                        }
                    }
                } catch (final Exception e) {
                    this.xiFallbackFlag++;
                } finally {
                    try {
                        in.close();
                        in = null;
                    } catch (final Exception ignore) {
                    }
                }
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public boolean isNamespaceAware() {
        return true;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public boolean isValidating() {
        return false;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param input
	 *            DOCUMENT ME!
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 * @throws SAXException
	 *             DOCUMENT ME!
	 */
    public void parse(final InputSource input) throws IOException, SAXException {
        this.level = 0;
        this.docType = null;
        Reader in = null;
        try {
            this.stack.clear();
            this.checkFeatures();
            this.documentURI = input.getSystemId();
            if ((this.documentURI != null) && (this.documentURI.indexOf("/") != -1)) {
                this.documentURI = this.documentURI.substring(0, this.documentURI.lastIndexOf("/") + 1);
            }
            this.parser = new CXmlParser(this.caseInsensitive, this);
            in = input.getCharacterStream();
            if (in == null) {
                if (input.getByteStream() != null) {
                    in = CXmlParser.getReader(input.getByteStream());
                } else {
                    try {
                        in = CXmlParser.getReader(new URL(input.getSystemId()).openStream());
                    } catch (final Exception e) {
                        in = null;
                        File f = new File(input.getSystemId());
                        if (f.exists()) {
                            in = CXmlParser.getReader(new FileInputStream(f));
                        }
                    }
                }
            }
            if (in == null) {
                throw new IOException("InputSource is not resolvable. : " + input.getSystemId());
            }
            this.parser.parse(in);
        } finally {
            try {
                in.close();
            } catch (final Exception ignore) {
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param systemId
	 *            DOCUMENT ME!
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 * @throws SAXException
	 *             DOCUMENT ME!
	 */
    public void parse(final String systemId) throws IOException, SAXException {
        this.parse(new InputSource(systemId));
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param content
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseCDATA(final String content) {
        try {
            if (((this.inXInclude != this.xiFallbackFlag) || (this.xiFallbackFlag == 0)) && (this.inXInclude > 0)) {
                return;
            }
            if (this.lHandler != null) {
                this.lHandler.startCDATA();
            }
            char value[] = content.toCharArray();
            if (this.cHandler != null) {
                this.cHandler.characters(value, 0, value.length);
            }
            if (this.dHandler != null) {
                this.dHandler.characters(value, 0, value.length);
            }
            if (this.lHandler != null) {
                this.lHandler.endCDATA();
            }
        } catch (SAXException e) {
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param content
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseComment(final String content) {
        try {
            if (((this.inXInclude != this.xiFallbackFlag) || (this.xiFallbackFlag == 0)) && (this.inXInclude > 0)) {
                return;
            }
            if (this.lHandler != null) {
                char value[] = content.toCharArray();
                this.lHandler.comment(value, 0, value.length);
            }
        } catch (SAXException e) {
        }
    }

    private boolean ignoreDTD = false;

    public boolean isIgnoreDTD() {
        return ignoreDTD;
    }

    public void setIgnoreDTD(boolean ignoreDTD) {
        this.ignoreDTD = ignoreDTD;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param content
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseDoctype(final String content) {
        try {
            if (isIgnoreDTD()) return;
            this.docType = CDTDParser.parseDoctype(content, this.documentURI, new CDom2HTMLDocument());
            Map entities = this.docType.getKnownEntities();
            this.codec = new CEntityCoDec(entities);
            if ((this.cHandler != null) && (this.cHandler instanceof DefaultHandler2)) {
                ((DefaultHandler2) this.cHandler).startDTD(this.docType.getName(), this.docType.getPublicId(), this.docType.getSystemId());
            }
            if (this.dtdHandler != null) {
                for (Iterator it = entities.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    CEntity ent = (CEntity) entry.getValue();
                    this.dtdHandler.notationDecl(ent.getEntityName(), ((ent.getPublicId() == null) && (ent.getSystemId() == null)) ? ent.getValue() : ent.getPublicId(), ent.getSystemId());
                }
            } else {
                if ((this.cHandler != null) && (this.cHandler instanceof DefaultHandler2)) {
                    for (Iterator it = entities.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry entry = (Map.Entry) it.next();
                        CEntity ent = (CEntity) entry.getValue();
                        ((DefaultHandler2) this.cHandler).notationDecl(ent.getEntityName(), ((ent.getPublicId() == null) && (ent.getSystemId() == null)) ? ent.getValue() : ent.getPublicId(), ent.getSystemId());
                    }
                }
            }
            if ((this.cHandler != null) && (this.cHandler instanceof DefaultHandler2)) {
                ((DefaultHandler2) this.cHandler).endDTD();
            }
            return;
        } catch (final Exception ignore) {
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param attributes
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseEmptyTag(final String name, final CAttr[] attributes, final int count, final int indexSep) {
        this.parseStartTag(name, attributes, count, indexSep);
        this.parseEndTag(name, indexSep);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseEndTag(final String name, final int indexSep) {
        if (this.inXInclude > 0) {
            if (name.endsWith("include")) {
                this.inXInclude--;
            }
            if (name.endsWith("fallback") && (this.xiFallbackFlag == this.inXInclude)) {
                this.xiFallbackFlag--;
            }
        }
        String nameToCmp = null;
        if (this.stack.size() > 0) {
            nameToCmp = (String) this.stack.peek();
            if (!(nameToCmp.equals(name))) {
                return;
            }
            this.stack.pop();
        }
        CNamespace ns = null;
        int index2p = name.indexOf(':');
        String prefixTag = null;
        if (index2p != -1) {
            prefixTag = name.substring(0, index2p);
            int i = (this.maxLevel < this.level) ? this.maxLevel : this.level;
            prefixTag = new CStringBuilder().append(prefixTag).append("$").toString();
            while ((i > 0) && (ns == null)) {
                ns = (CNamespace) this.nsMap.get(new CStringBuilder().append(prefixTag).append(i).toString());
                i--;
            }
        } else {
            int i = (this.maxLevel < this.level) ? this.maxLevel : this.level;
            while ((i > 0) && (ns == null)) {
                ns = (CNamespace) this.nsMap.get(new CStringBuilder().append('$').append(i).toString());
                i--;
            }
        }
        try {
            if (ns != null) {
                if ("http://www.w3.org/2001/XInclude".equals(ns.getNamespaceURI())) {
                } else {
                    if (index2p != -1) {
                        if (this.cHandler != null) {
                            this.cHandler.endElement(ns.getNamespaceURI(), name.substring(index2p + 1), name);
                        }
                    } else {
                        if (this.cHandler != null) {
                            if (ns.getPrefix().equals("")) {
                                this.cHandler.endElement(ns.getNamespaceURI(), name, name);
                            } else {
                                this.cHandler.endElement(ns.getNamespaceURI(), name, new CStringBuilder().append(ns.getPrefix()).append(':').append(name).toString());
                            }
                        }
                    }
                }
            } else {
                if (this.cHandler != null) {
                    this.cHandler.endElement("", name, name);
                }
            }
            if (this.dHandler != null) {
                this.dHandler.endElement(name);
            }
            if (this.cHandler != null) {
                String slevel = new CStringBuilder().append('$').append(this.level).toString();
                for (Iterator it = this.nsMap.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    CNamespace nsToTest = (CNamespace) entry.getValue();
                    if (((String) entry.getKey()).endsWith(slevel)) {
                        this.cHandler.endPrefixMapping(nsToTest.getPrefix());
                        it.remove();
                    }
                }
            }
            this.level--;
            if (this.maxLevel > this.level) {
                this.maxLevel = this.level;
            }
        } catch (SAXException e) {
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param content
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parsePI(final String name, final String content) {
        if (((this.inXInclude != this.xiFallbackFlag) || (this.xiFallbackFlag == 0)) && (this.inXInclude > 0)) {
            return;
        }
        if (name.equals("xml")) {
            int index2p = content.indexOf("standalone=");
            if (index2p != -1) {
                String st = content.substring(0, index2p + 12);
                int index = st.indexOf('\"');
                if (index != -1) {
                    st = st.substring(0, index);
                    if (st.trim().equalsIgnoreCase("true")) {
                        this.features.put("is-standalone", "true");
                    }
                } else {
                    index = st.indexOf("'");
                    if (index != -1) {
                        st = st.substring(0, index);
                        if (st.trim().equalsIgnoreCase("true")) {
                            this.features.put("is-standalone", "true");
                        }
                    }
                }
            }
        }
        try {
            if ((this.cHandler != null)) {
                if (!"xml".equals(name)) {
                    this.cHandler.processingInstruction(name, content);
                }
            }
        } catch (SAXException e) {
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param attributes
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseStartTag(final String name, final CAttr[] attributes, final int count, final int indexSep) {
        if (name.endsWith("include")) {
            if (this.inXInclude == this.xiFallbackFlag) {
                this.xiFallbackFlag++;
            }
            this.inXInclude++;
        }
        if (((this.inXInclude != this.xiFallbackFlag) || (this.xiFallbackFlag == 0)) && (this.inXInclude > 0)) {
            return;
        }
        if (name.endsWith("include")) {
            if (this.inXInclude == this.xiFallbackFlag) {
                this.xiFallbackFlag--;
            }
            this.inXInclude--;
        }
        this.level++;
        if (!this.htmlDocument && this.firstTag && name.equalsIgnoreCase("html")) {
            this.htmlDocument = true;
            this.codec = new CEntityCoDec(CXmlParser.dtTr.getKnownEntities());
        }
        if (this.htmlDocument && name.equalsIgnoreCase("frameset")) {
            this.codec = new CEntityCoDec(CXmlParser.dtFr.getKnownEntities());
        }
        this.firstTag = false;
        this.atts.clear();
        this.attsOld.clear();
        String prefixTag = null;
        int indexOf2pN = name.indexOf(':');
        if (indexOf2pN != -1) {
            prefixTag = name.substring(0, indexOf2pN);
        }
        if (attributes != null) {
            String sLevel = new CStringBuilder().append('$').append(this.level).toString();
            for (int i = 0; i < count; i++) {
                CAttr attr = attributes[i];
                String attName = attr.name;
                if ((attName.length() >= 5) && (attName.charAt(0) == 'x') && (attName.charAt(1) == 'm') && (attName.charAt(2) == 'l') && (attName.charAt(3) == 'n') && (attName.charAt(4) == 's')) {
                    String value = attr.deferredValue;
                    String prefix = (attName.length() > 5) ? attName.substring(6) : "";
                    CNamespace ns = new CNamespace(prefix, value);
                    if (this.cHandler != null) {
                        try {
                            this.cHandler.startPrefixMapping(ns.getPrefix(), ns.getNamespaceURI());
                        } catch (SAXException e) {
                        }
                    }
                    if (this.level > this.maxLevel) {
                        this.maxLevel = this.level;
                    }
                    this.nsMap.put(new CStringBuilder().append(ns.getPrefix()).append(sLevel).toString(), ns);
                }
            }
        }
        CNamespace ns = null;
        if (prefixTag != null) {
            int i = (this.maxLevel < this.level) ? this.maxLevel : this.level;
            prefixTag = new CStringBuilder().append(prefixTag).append('$').toString();
            while ((i > 0) && (ns == null)) {
                ns = (CNamespace) this.nsMap.get(new CStringBuilder().append(prefixTag).append(i).toString());
                i--;
            }
        } else {
            int i = (this.maxLevel < this.level) ? this.maxLevel : this.level;
            while ((i > 0) && (ns == null)) {
                ns = (CNamespace) this.nsMap.get(new CStringBuilder().append('$').append(i).toString());
                i--;
            }
        }
        if (attributes != null) {
            for (int li = 0; li < count; li++) {
                CAttr attr = attributes[li];
                String attName = attr.name;
                String value = attr.deferredValue;
                if (value == null) {
                    value = "";
                }
                if (this.decodeEntites && (value.indexOf('&') != -1)) {
                    value = this.codec.decodeInternal(value, null, null, this.dtdHandler, true);
                } else {
                    if (this.dtdHandler != null) {
                        value = this.codec.decodeInternal(value, null, null, this.dtdHandler, false);
                    }
                }
                if ((attName.length() >= 5) && (attName.charAt(0) == 'x') && (attName.charAt(1) == 'm') && (attName.charAt(2) == 'l') && (attName.charAt(3) == 'n') && (attName.charAt(4) == 's') && (!this.nPrefix)) {
                    continue;
                }
                int indexOf2p = attName.indexOf(":");
                CNamespace nsToSet = null;
                String prefix = "";
                String aname = attName;
                if (indexOf2p != -1) {
                    prefix = attName.substring(0, indexOf2p);
                    aname = attName.substring(indexOf2p + 1);
                    int i = (this.maxLevel < this.level) ? this.maxLevel : this.level;
                    prefix = new CStringBuilder().append(prefix).append('$').toString();
                    while ((i > 0) && (nsToSet == null)) {
                        nsToSet = (CNamespace) this.nsMap.get(new CStringBuilder().append(prefix).append(i).toString());
                        i--;
                    }
                } else {
                    int i = (this.maxLevel < this.level) ? this.maxLevel : this.level;
                    while ((i > 0) && (nsToSet == null)) {
                        nsToSet = (CNamespace) this.nsMap.get(new CStringBuilder().append('$').append(i).toString());
                        i--;
                    }
                }
                if (nsToSet != null) {
                    this.atts.addAttribute(nsToSet.getNamespaceURI(), aname, attName, "CDATA", value);
                } else {
                    this.atts.addAttribute("", aname, aname, "CDATA", value);
                }
                this.attsOld.addAttribute(attName, "CDATA", value);
            }
        }
        if (ns != null) {
            if ("http://www.w3.org/2001/XInclude".equals(ns.getNamespaceURI())) {
                if (indexOf2pN != -1) {
                    this.handleXInclude(name.substring(indexOf2pN + 1), this.atts);
                } else {
                    this.handleXInclude(name, this.atts);
                }
            } else {
                try {
                    if (indexOf2pN != -1) {
                        if (this.cHandler != null) {
                            this.cHandler.startElement(ns.getNamespaceURI(), name.substring(indexOf2pN + 1), name, this.atts);
                        }
                    } else {
                        if (this.cHandler != null) {
                            if (ns.getPrefix().equals("")) {
                                this.cHandler.startElement(ns.getNamespaceURI(), name, name, this.atts);
                            } else {
                                this.cHandler.startElement(ns.getNamespaceURI(), name, new CStringBuilder().append(ns.getPrefix()).append(':').append(name).toString(), this.atts);
                            }
                        }
                    }
                } catch (SAXException e) {
                }
            }
        } else {
            try {
                if (this.cHandler != null) {
                    this.cHandler.startElement("", name, name, this.atts);
                }
            } catch (SAXException e) {
            }
        }
        try {
            if (this.dHandler != null) {
                this.dHandler.startElement(name, this.attsOld);
            }
        } catch (SAXException e) {
        }
        this.stack.push(name);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param content
	 *            DOCUMENT ME!
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void parseText(final String content, final boolean decode) {
        if (this.stack.size() == 0) {
            return;
        }
        if (((this.inXInclude != this.xiFallbackFlag) || (this.xiFallbackFlag == 0)) && (this.inXInclude > 0)) {
            return;
        }
        char value[] = null;
        if (this.decodeEntites) {
            if (content.indexOf("&") != -1) {
                this.codec.decodeInternal(content, this.cHandler, this.dHandler, this.dtdHandler, true);
                return;
            } else {
                value = content.toCharArray();
            }
        } else {
            if (this.dtdHandler != null) {
                this.codec.decodeInternal(content, this.cHandler, this.dHandler, this.dtdHandler, false);
                return;
            } else {
                value = content.toCharArray();
            }
        }
        if (this.cHandler != null) {
            try {
                this.cHandler.characters(value, 0, value.length);
            } catch (Exception ignore) {
            }
        }
        if (this.dHandler != null) {
            try {
                this.dHandler.characters(value, 0, value.length);
            } catch (Exception ignore) {
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void setContentHandler(final ContentHandler handler) {
        if (handler != null) {
            this.cHandler = handler;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void setDocumentHandler(final DocumentHandler handler) {
        if (handler != null) {
            this.dHandler = handler;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void setDTDHandler(final DTDHandler handler) {
        if (handler != null) {
            this.dtdHandler = handler;
        }
    }

    public void setEncoding(String encoding) {
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param resolver
	 *            DOCUMENT ME!
	 */
    public void setEntityResolver(final EntityResolver resolver) {
        if (resolver != null) {
            this.entityResolver = resolver;
            CDTDParser.setEntityResolver(resolver);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param handler
	 *            DOCUMENT ME!
	 */
    public void setErrorHandler(final ErrorHandler handler) {
        if (handler != null) {
            this.errorHandler = handler;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param value
	 *            DOCUMENT ME!
	 * 
	 * @throws SAXNotRecognizedException
	 *             DOCUMENT ME!
	 * @throws SAXNotSupportedException
	 *             DOCUMENT ME!
	 */
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        this.features.put(name, "" + value);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param locale
	 *            DOCUMENT ME!
	 * 
	 * @throws SAXException
	 *             DOCUMENT ME!
	 */
    public void setLocale(final Locale locale) throws SAXException {
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 * @param value
	 *            DOCUMENT ME!
	 * 
	 * @throws SAXNotRecognizedException
	 *             DOCUMENT ME!
	 * @throws SAXNotSupportedException
	 *             DOCUMENT ME!
	 */
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        this.properties.put(name, value);
        if ((this.lHandler == null) && name.equals("http://xml.org/sax/properties/lexical-handler")) {
            try {
                this.lHandler = (LexicalHandler) value;
            } catch (final Exception ignore) {
                this.lHandler = null;
            }
        }
    }
}
