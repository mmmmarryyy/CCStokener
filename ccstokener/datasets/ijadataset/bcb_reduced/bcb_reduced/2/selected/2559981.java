package org.allcolor.xml.parser;

import org.allcolor.dtd.parser.CDTDParser;
import org.allcolor.dtd.parser.CDocType;
import org.allcolor.xml.parser.dom.CAttr;
import org.allcolor.xml.parser.dom.CCDATASection;
import org.allcolor.xml.parser.dom.CComment;
import org.allcolor.xml.parser.dom.ADocument;
import org.allcolor.xml.parser.dom.CElement;
import org.allcolor.xml.parser.dom.CEntityCoDec;
import org.allcolor.xml.parser.dom.CEntityReference;
import org.allcolor.xml.parser.dom.CNamedNodeMap;
import org.allcolor.xml.parser.dom.CNodeList;
import org.allcolor.xml.parser.dom.CProcessingInstruction;
import org.allcolor.xml.parser.dom.CText;
import org.allcolor.xml.parser.dom.ANode.CNamespace;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DOCUMENT ME!
 * 
 * @author Quentin Anciaux
 * @author Jarle H. Næss- Patch quoted in code
 */
public class CDomHandler implements IParseHandler {

    /** DOCUMENT ME! */
    public static final String CASE_INSENSITIVE = "http://www.allcolor.org/xml/caseinsensitive/";

    /** DOCUMENT ME! */
    public static final String STRICT_MODE = "http://www.allcolor.org/xml/strictmode/";

    /** DOCUMENT ME! */
    public static final String VALIDATE = "http://www.allcolor.org/xml/validate/";

    /** DOCUMENT ME! */
    ADocument document = null;

    /** DOCUMENT ME! */
    private CXmlParser parser = null;

    /** DOCUMENT ME! */
    private final DOMImplementation impl;

    /** DOCUMENT ME! */
    private CElement currentElement = null;

    /** DOCUMENT ME! */
    private String documentURI = null;

    /** DOCUMENT ME! */
    private ThreadLocal localParser = new ThreadLocal();

    /** DOCUMENT ME! */
    private boolean XIncludeAware = false;

    /** DOCUMENT ME! */
    private boolean coalescing = false;

    /** DOCUMENT ME! */
    private boolean expandEntity = true;

    /** DOCUMENT ME! */
    private boolean firstTag = true;

    /** DOCUMENT ME! */
    private boolean ignoringComments = false;

    /** DOCUMENT ME! */
    private boolean ignoringElementContentWhitespace = false;

    /** DOCUMENT ME! */
    private int inXInclude = 0;

    /** DOCUMENT ME! */
    private int xiFallbackFlag = 0;

    /** DOCUMENT ME! */
    private boolean autodoctype = false;

    private boolean ignoreDTD = false;

    /**
         * Creates a new CDomHandler object.
         * 
         * @param features
         *                DOCUMENT ME!
         */
    public CDomHandler(final Map features) {
        Map implFeatures = new HashMap(0);
        if (features.containsKey(CDomImplementation.PARSER_HTML)) implFeatures.put(CDomImplementation.PARSER_HTML, "" + true);
        this.impl = new CDomImplementation(implFeatures);
        boolean caseInsensitive = (features != null) ? (features.get(CASE_INSENSITIVE) != null) : false;
        parser = new CXmlParser(caseInsensitive, this);
    }

    private boolean isValidChild(final String elemName, final CElement elem) {
        try {
            IHtmlValidChild parent = (IHtmlValidChild) elem;
            return parent.isValidChild(elemName);
        } catch (final Throwable ignore) {
            final Throwable t = ignore;
            if (t.getClass() == ThreadDeath.class) {
                throw (ThreadDeath) t;
            }
            Throwable cause = ignore.getCause();
            while (cause != null) {
                if (cause.getClass() == ThreadDeath.class) {
                    throw (ThreadDeath) cause;
                }
                cause = cause.getCause();
            }
        }
        return true;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param coalescing
         *                DOCUMENT ME!
         */
    public final void setCoalescing(final boolean coalescing) {
        this.coalescing = coalescing;
    }

    /**
         * DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public DOMImplementation getDOMImplementation() {
        return impl;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param in
         *                DOCUMENT ME!
         * @param documentURI
         *                DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public Document getDocument(final Reader in, String documentURI) {
        try {
            if (documentURI != null) {
                if (documentURI.lastIndexOf("/") != -1) {
                    documentURI = documentURI.substring(0, documentURI.lastIndexOf("/") + 1);
                }
                this.documentURI = documentURI;
            } else {
                this.documentURI = null;
            }
            parser.parse(in);
            if (isIgnoringElementContentWhitespace()) document.normalize();
            document.setBuildStageDone();
            return document;
        } finally {
            try {
                in.close();
            } catch (final Exception ignore) {
            }
            localParser.set(null);
            currentElement = null;
        }
    }

    /**
         * DOCUMENT ME!
         * 
         * @param handler
         *                DOCUMENT ME!
         */
    public void documentEnd() {
        currentElement = document;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param handler
         *                DOCUMENT ME!
         */
    public void documentStart() {
        document = (ADocument) impl.createDocument(null, null, null);
        document.setBuildStage();
        document.setDocumentURI(this.documentURI);
        document.setInputEncoding(encoding);
        currentElement = document;
        firstTag = true;
    }

    /**
         * DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public final boolean isCoalescing() {
        return coalescing;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param expandEntity
         *                DOCUMENT ME!
         */
    public final void setExpandEntity(final boolean expandEntity) {
        this.expandEntity = expandEntity;
    }

    /**
         * DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public final boolean isExpandEntity() {
        return expandEntity;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param ignoringComments
         *                DOCUMENT ME!
         */
    public final void setIgnoringComments(final boolean ignoringComments) {
        this.ignoringComments = ignoringComments;
    }

    /**
         * DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public final boolean isIgnoringComments() {
        return ignoringComments;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param ignoringElementContentWhitespace
         *                DOCUMENT ME!
         */
    public final void setIgnoringElementContentWhitespace(final boolean ignoringElementContentWhitespace) {
        this.ignoringElementContentWhitespace = ignoringElementContentWhitespace;
    }

    /**
         * DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public final boolean isIgnoringElementContentWhitespace() {
        return ignoringElementContentWhitespace;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param includeAware
         *                DOCUMENT ME!
         */
    public final void setXIncludeAware(final boolean includeAware) {
        XIncludeAware = includeAware;
    }

    /**
         * DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
    public final boolean isXIncludeAware() {
        return XIncludeAware;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param content
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseCDATA(String content) {
        if (XIncludeAware) {
            if (((inXInclude != xiFallbackFlag) || (xiFallbackFlag == 0)) && (inXInclude > 0)) {
                return;
            }
        }
        if (coalescing) {
            Node n = currentElement.getLastChild();
            if (n.getNodeType() == Node.TEXT_NODE) {
                content = n.getNodeValue() + content;
                currentElement.removeChild(n);
            }
            content = CEntityCoDec.encode(content);
            parseText(content, false);
        } else {
            CCDATASection cdata = new CCDATASection(content, document);
            if (currentElement == document) {
                document.appendChildInternal(cdata);
            } else {
                cdata.parentNode = currentElement;
                currentElement.listChild.addItem(cdata);
            }
        }
    }

    /**
         * DOCUMENT ME!
         * 
         * @param content
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseComment(final String content) {
        if (isIgnoringComments()) {
            return;
        }
        if (XIncludeAware) {
            if (((inXInclude != xiFallbackFlag) || (xiFallbackFlag == 0)) && (inXInclude > 0)) {
                return;
            }
        }
        CComment c = new CComment(content, document);
        if (currentElement == document) {
            document.appendChildInternal(c);
        } else {
            c.parentNode = currentElement;
            if (currentElement.listChild == null) {
                currentElement.listChild = new CNodeList(false);
            }
            currentElement.listChild.addItem(c);
        }
    }

    /**
         * DOCUMENT ME!
         * 
         * @param content
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseDoctype(final String content) {
        try {
            if (isIgnoreDTD()) return;
            CDocType docType = CDTDParser.parseDoctype(content, documentURI, document);
            docType.setOwnerDocument(document);
            docType.setNodeValue(content);
            document.appendChildInternal(docType);
            document.setDocumentType(docType);
            document.setEntityCodec(new CEntityCoDec(docType.getKnownEntities()));
            if ((docType.getPublicId() != null) && (docType.getPublicId().indexOf("HTML") != -1) && (docType.getPublicId().indexOf("//W3C//DTD ") != -1)) {
                document.setHTMLDocument(true);
            }
            return;
        } catch (final Exception ignore) {
        }
    }

    /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * @param attributes
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseEmptyTag(final String name, final CAttr[] attributes, final int count, final int indexSep) {
        if (XIncludeAware) {
            if (name.endsWith("include")) {
                if (inXInclude == xiFallbackFlag) {
                    xiFallbackFlag++;
                }
                inXInclude++;
            }
            if (((inXInclude != xiFallbackFlag) || (xiFallbackFlag == 0)) && (inXInclude > 0)) {
                if (name.endsWith("include")) {
                    inXInclude--;
                }
                return;
            }
            if (name.endsWith("include")) {
                if (inXInclude == xiFallbackFlag) {
                    xiFallbackFlag--;
                }
                inXInclude--;
            }
        }
        CElement elem = null;
        if (parser.htmlDocument) {
            elem = document.createElementInternal(name, indexSep);
            if (currentElement != document && elem instanceof IHtmlValidChild) {
                while (!isValidChild(name, currentElement)) {
                    parseEndTag(currentElement.name, -1);
                    if (currentElement == document) break;
                }
            }
        } else {
            elem = new CElement(name, document, indexSep);
        }
        if (firstTag) {
            if (parser.htmlDocument) {
                document.setHTMLDocument(true);
            }
            firstTag = false;
            document.loadXSD(elem);
            if (isAutodoctype() && document.getDoctype() == null) {
                CDocType dt = (CDocType) CXmlParser.dtTr.cloneNode(true);
                document.appendChildInternal(dt);
                document.setDocumentType(dt);
            }
        }
        if (count > 0) {
            if (elem.listAttributes == null) {
                elem.listAttributes = new CNamedNodeMap(elem);
                elem.listAttributes.list = attributes;
                elem.listAttributes.count = count;
                for (int i = 0; i < count; i++) {
                    CAttr attr = attributes[i];
                    attr.parentNode = elem;
                    attr.ownerDocument = document;
                    if (attr.name == "xmlns" || attr.prefix == "xmlns") {
                        elem.notifyNSChange(attr.localName);
                    }
                }
            } else {
                for (int i = 0; i < count; i++) {
                    CAttr attr = attributes[i];
                    attr.parentNode = elem;
                    attr.ownerDocument = document;
                    if (attr.name == "xmlns" || attr.prefix == "xmlns") {
                        elem.notifyNSChange(attr.localName);
                    }
                    if (elem.listAttributes == null) {
                        elem.listAttributes = new CNamedNodeMap(elem);
                    }
                    elem.listAttributes.setNamedItemForce(attr);
                }
            }
        }
        if (currentElement == document) {
            document.appendChildInternal(elem);
        } else {
            elem.parentNode = currentElement;
            currentElement.listChild.addItem(elem);
        }
        if (XIncludeAware && "http://www.w3.org/2001/XInclude".equals(elem.getNamespaceURI())) {
            handleXInclude(elem);
            if (inXInclude > 0) {
                if (name.endsWith("include")) {
                    inXInclude--;
                }
                if (name.endsWith("fallback") && (xiFallbackFlag == inXInclude)) {
                    xiFallbackFlag--;
                }
            }
        }
    }

    /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseEndTag(final String name, final int indexSep) {
        if (XIncludeAware && (inXInclude > 0)) {
            if (name.endsWith("include")) {
                inXInclude--;
            }
            if (name.endsWith("fallback") && (xiFallbackFlag == inXInclude)) {
                xiFallbackFlag--;
            }
        }
        if (name.equals(currentElement.name)) currentElement = (CElement) currentElement.parentNode;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * @param content
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parsePI(final String name, final String content) {
        if (XIncludeAware) {
            if (((inXInclude != xiFallbackFlag) || (xiFallbackFlag == 0)) && (inXInclude > 0)) {
                return;
            }
        }
        if (!"xml".equals(name)) {
            CProcessingInstruction pi = new CProcessingInstruction(name, content, document);
            if (currentElement == document) {
                document.appendChildInternal(pi);
            } else {
                pi.parentNode = currentElement;
                currentElement.listChild.addItem(pi);
            }
        } else if (firstTag && "xml".equals(name)) {
            CStringTokenizer tokenizer = new CStringTokenizer(content, "=\"' \t\r\n", false);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if ("version".equalsIgnoreCase(token) && tokenizer.hasMoreTokens()) {
                    document.setXmlVersion(tokenizer.nextToken());
                } else if ("standalone".equalsIgnoreCase(token) && tokenizer.hasMoreTokens()) {
                    String standalone = tokenizer.nextToken();
                    if ("yes".equalsIgnoreCase(standalone) || "true".equalsIgnoreCase(standalone)) document.setXmlStandalone(true);
                } else if ("encoding".equalsIgnoreCase(token) && tokenizer.hasMoreTokens()) {
                    String encoding = tokenizer.nextToken();
                    document.setXmlEncoding(encoding);
                }
            }
        }
    }

    private String encoding = null;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * @param attributes
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseStartTag(final String name, final CAttr[] attributes, final int count, final int indexSep) {
        if (XIncludeAware) {
            if (name.endsWith("include")) {
                if (inXInclude == xiFallbackFlag) {
                    xiFallbackFlag++;
                }
                inXInclude++;
            }
            if (((inXInclude != xiFallbackFlag) || (xiFallbackFlag == 0)) && (inXInclude > 0)) {
                return;
            }
            if (name.endsWith("include")) {
                if (inXInclude == xiFallbackFlag) {
                    xiFallbackFlag--;
                }
                inXInclude--;
            }
        }
        CElement elem = null;
        if (parser.htmlDocument) {
            elem = document.createElementInternal(name, indexSep);
            if (currentElement != document && elem instanceof IHtmlValidChild) {
                while (!isValidChild(name, currentElement)) {
                    parseEndTag(currentElement.name, -1);
                    if (currentElement == document) break;
                }
            }
        } else {
            elem = new CElement(name, document, indexSep);
        }
        elem.listChild = new CNodeList(false);
        if (firstTag) {
            if (parser.htmlDocument) {
                document.setHTMLDocument(true);
            }
            firstTag = false;
            document.loadXSD(elem);
            if (isAutodoctype() && document.getDoctype() == null) {
                CDocType dt = (CDocType) CXmlParser.dtTr.cloneNode(true);
                document.appendChildInternal(dt);
                document.setDocumentType(dt);
            }
        }
        if (count > 0) {
            if (elem.listAttributes == null) {
                elem.listAttributes = new CNamedNodeMap(elem);
                elem.listAttributes.list = attributes;
                elem.listAttributes.count = count;
                for (int i = 0; i < count; i++) {
                    CAttr attr = attributes[i];
                    attr.parentNode = elem;
                    attr.ownerDocument = document;
                    if (attr.name == "xmlns" || attr.prefix == "xmlns") {
                        elem.notifyNSChange(attr.localName);
                    }
                }
            } else {
                for (int i = 0; i < count; i++) {
                    CAttr attr = attributes[i];
                    attr.parentNode = elem;
                    attr.ownerDocument = document;
                    if (attr.name == "xmlns" || attr.prefix == "xmlns") {
                        elem.notifyNSChange(attr.localName);
                    }
                    if (elem.listAttributes == null) {
                        elem.listAttributes = new CNamedNodeMap(elem);
                    }
                    elem.listAttributes.setNamedItemForce(attr);
                }
            }
        }
        if (currentElement == document) {
            document.appendChildInternal(elem);
        } else {
            elem.parentNode = currentElement;
            currentElement.listChild.addItem(elem);
        }
        if (XIncludeAware && "http://www.w3.org/2001/XInclude".equals(elem.getNamespaceURI())) {
            handleXInclude(elem);
        } else {
            if (parser.htmlDocument && elem instanceof IHtmlValidChild) {
                if (((IHtmlValidChild) elem).canHaveChild()) {
                    currentElement = elem;
                }
            } else {
                currentElement = elem;
            }
        }
    }

    /**
         * DOCUMENT ME!
         * 
         * @param content
         *                DOCUMENT ME!
         * @param handler
         *                DOCUMENT ME!
         */
    public void parseText(final String content, final boolean decode) {
        if (XIncludeAware) {
            if (((inXInclude != xiFallbackFlag) || (xiFallbackFlag == 0)) && (inXInclude > 0)) {
                return;
            }
        }
        if (content.length() == 0) return;
        if (!decode) {
            CText text = new CText(content, document);
            if (currentElement == document) {
                document.appendChildInternal(text);
            } else {
                text.parentNode = currentElement;
                currentElement.listChild.addItem(text);
            }
            return;
        }
        contentHandler h = new contentHandler(currentElement, document, this);
        CEntityCoDec codec = document.getEntityCodec();
        codec.decodeInternal(content, h, null, null, true);
    }

    /**
         * DOCUMENT ME!
         * 
         * @param elem
         *                DOCUMENT ME!
         */
    private void handleXInclude(final Element elem) {
        currentElement.removeChild(elem);
        if ("include".equals(elem.getLocalName())) {
            inXInclude++;
            String href = elem.getAttribute("href");
            if ((href == null) || "".equals(href.trim())) {
                href = null;
            }
            String parse = elem.getAttribute("parse");
            if ((parse == null) || "".equals(parse.trim())) {
                parse = "xml";
            }
            String xpointer = elem.getAttribute("xpointer");
            if ((xpointer == null) || "".equals(xpointer.trim())) {
                xpointer = null;
            }
            String encoding = elem.getAttribute("encoding");
            if ((encoding == null) || "".equals(encoding.trim())) {
                encoding = null;
            }
            String accept = elem.getAttribute("accept");
            if ((accept == null) || "".equals(accept.trim())) {
                accept = null;
            }
            String accept_language = elem.getAttribute("accept-language");
            if ((accept_language == null) || "".equals(accept_language.trim())) {
                accept_language = null;
            }
            if (href != null) {
                if (href.indexOf(":/") == -1) {
                    if (href.startsWith("/")) {
                        href = href.substring(1);
                    }
                    href = documentURI + href;
                }
                if (localParser.get() == null) {
                    localParser.set(new CShaniDomParser());
                }
                CShaniDomParser p = (CShaniDomParser) localParser.get();
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
                        currentElement.appendChild(doc.getDocumentElement());
                    } else {
                        XPath xpath = new DOMXPath(xpointer);
                        for (Iterator it = doc.getNamespaceList().iterator(); it.hasNext(); ) {
                            CNamespace ns = (CNamespace) it.next();
                            xpath.addNamespace(ns.getPrefix() == null ? "" : ns.getPrefix(), ns.getNamespaceURI());
                        }
                        List result = xpath.selectNodes(doc.getDocumentElement());
                        for (final Iterator it = result.iterator(); it.hasNext(); ) {
                            final Node node = (Node) it.next();
                            currentElement.appendChild(node);
                        }
                    }
                } catch (final Exception e) {
                    xiFallbackFlag++;
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
         * @author $author$
         * @version $Revision: 1.47 $
         */
    private static class contentHandler extends DefaultHandler2 {

        /** DOCUMENT ME! */
        ADocument document;

        /** DOCUMENT ME! */
        CDomHandler dh;

        /** DOCUMENT ME! */
        CElement currentElement;

        /**
         * Creates a new contentHandler object.
         * 
         * @param currentElement
         *                DOCUMENT ME!
         * @param document
         *                DOCUMENT ME!
         * @param dh
         *                DOCUMENT ME!
         */
        public contentHandler(final CElement currentElement, final ADocument document, final CDomHandler dh) {
            this.currentElement = currentElement;
            this.document = document;
            this.dh = dh;
        }

        /**
         * DOCUMENT ME!
         * 
         * @param locator
         *                DOCUMENT ME!
         */
        public void setDocumentLocator(final Locator locator) {
        }

        /**
         * DOCUMENT ME!
         * 
         * @param ch
         *                DOCUMENT ME!
         * @param start
         *                DOCUMENT ME!
         * @param length
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void characters(final char ch[], final int start, final int length) throws SAXException {
            if (currentElement.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
                String value = new String(ch, start, length);
                CText text = new CText(value, document);
                if (currentElement == document) {
                    document.appendChildInternal(text);
                } else {
                    text.parentNode = currentElement;
                    if (currentElement.listChild == null) {
                        currentElement.listChild = new CNodeList(false);
                    }
                    currentElement.listChild.addItem(text);
                }
            }
        }

        /**
         * DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void endDocument() throws SAXException {
        }

        /**
         * DOCUMENT ME!
         * 
         * @param uri
         *                DOCUMENT ME!
         * @param localName
         *                DOCUMENT ME!
         * @param qName
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if (currentElement.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
                currentElement = (CElement) currentElement.parentNode;
            }
        }

        /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void endEntity(final String name) throws SAXException {
            currentElement = currentElement.parentNode;
        }

        /**
         * DOCUMENT ME!
         * 
         * @param prefix
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void endPrefixMapping(final String prefix) throws SAXException {
        }

        /**
         * DOCUMENT ME!
         * 
         * @param ch
         *                DOCUMENT ME!
         * @param start
         *                DOCUMENT ME!
         * @param length
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void ignorableWhitespace(final char ch[], final int start, final int length) throws SAXException {
            if (currentElement.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
                if (dh.isIgnoringElementContentWhitespace()) {
                    return;
                }
                String value = new String(ch, start, length);
                CText text = new CText(value, document);
                if (currentElement == document) {
                    document.appendChildInternal(text);
                } else {
                    text.parentNode = currentElement;
                    if (currentElement.listChild == null) {
                        currentElement.listChild = new CNodeList(false);
                    }
                    currentElement.listChild.addItem(text);
                }
            }
        }

        /**
         * DOCUMENT ME!
         * 
         * @param target
         *                DOCUMENT ME!
         * @param data
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void processingInstruction(final String target, final String data) throws SAXException {
            if (currentElement.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
                CProcessingInstruction pi = new CProcessingInstruction(target, data, document);
                if (currentElement == document) {
                    document.appendChildInternal(pi);
                } else {
                    pi.parentNode = currentElement;
                    if (currentElement.listChild == null) {
                        currentElement.listChild = new CNodeList(false);
                    }
                    currentElement.listChild.addItem(pi);
                }
            }
        }

        /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void skippedEntity(final String name) throws SAXException {
        }

        /**
         * DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void startDocument() throws SAXException {
        }

        /**
         * DOCUMENT ME!
         * 
         * @param uri
         *                DOCUMENT ME!
         * @param localName
         *                DOCUMENT ME!
         * @param qName
         *                DOCUMENT ME!
         * @param atts
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
            if (currentElement.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
                CElement elem = new CElement(qName, document);
                for (int i = 0; i < atts.getLength(); i++) {
                    CAttr attr = new CAttr(atts.getQName(i), atts.getValue(i), document, currentElement, true);
                    if (currentElement.listAttributes == null) {
                        currentElement.listAttributes = new CNamedNodeMap(currentElement);
                    }
                    currentElement.listAttributes.setNamedItemForce(attr);
                }
                if (currentElement == document) {
                    document.appendChildInternal(elem);
                } else {
                    elem.parentNode = currentElement;
                    if (currentElement.listChild == null) {
                        currentElement.listChild = new CNodeList(false);
                    }
                    currentElement.listChild.addItem(elem);
                }
                currentElement = elem;
            }
        }

        /**
         * DOCUMENT ME!
         * 
         * @param name
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void startEntity(final String name) throws SAXException {
            CEntityReference ref = new CEntityReference(name, null, document);
            if (currentElement == document) {
                document.appendChildInternal(ref);
            } else {
                ref.parentNode = currentElement;
                if (currentElement.listChild == null) {
                    currentElement.listChild = new CNodeList(false);
                }
                currentElement.listChild.addItem(ref);
            }
            currentElement = ref;
        }

        /**
         * DOCUMENT ME!
         * 
         * @param prefix
         *                DOCUMENT ME!
         * @param uri
         *                DOCUMENT ME!
         * 
         * @throws SAXException
         *                 DOCUMENT ME!
         */
        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
        }
    }

    public boolean isAutodoctype() {
        return autodoctype;
    }

    public void setAutodoctype(boolean autodoctype) {
        this.autodoctype = autodoctype;
    }

    public boolean isIgnoreDTD() {
        return ignoreDTD;
    }

    public void setIgnoreDTD(boolean ignoreDTD) {
        this.ignoreDTD = ignoreDTD;
    }
}
