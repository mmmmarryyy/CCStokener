package gnu.xml.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import gnu.xml.util.Resolver;

/**
 * Filter to process an XPointer-free subset of
 * <a href="http://www.w3.org/TR/xinclude">XInclude</a>, supporting its
 * use as a kind of replacement for parsed general entities.
 * XInclude works much like the <code>#include</code> of C/C++ but
 * works for XML documents as well as unparsed text files.
 * Restrictions from the 17-Sept-2002 CR draft of XInclude are as follows:
 *
 * <ul>
 *
 * <li> URIs must not include fragment identifiers.
 * The CR specifies support for XPointer <em>element()</em> fragment IDs,
 * which is not currently implemented here.
 *
 * <li> <em>xi:fallback</em> handling of resource errors is not
 * currently supported.
 *
 * <li> DTDs are not supported in included files, since the SAX DTD events
 * must have completely preceded any included file. 
 * The CR explicitly allows the DTD related portions of the infoset to
 * grow as an effect of including XML documents.
 *
 * <li> <em>xml:base</em> fixup isn't done.
 *
 * </ul>
 *
 * <p> XML documents that are included will normally be processed using
 * the default SAX namespace rules, meaning that prefix information may
 * be discarded.  This may be changed with {@link #setSavingPrefixes
 * setSavingPrefixes()}.  <em>You are strongly advised to do this.</em>
 *
 * <p> Note that XInclude allows highly incompatible implementations, which
 * are specialized to handle application-specific infoset extensions.  Some
 * such implementations can be implemented by subclassing this one, but
 * they may only be substituted in applications at "user option".
 *
 * <p>TBD: "IURI" handling.
 *
 * @author David Brownell
 */
public class XIncludeFilter extends EventFilter implements Locator {

    private Hashtable extEntities = new Hashtable(5, 5);

    private int ignoreCount;

    private Stack uris = new Stack();

    private Locator locator;

    private Vector inclusions = new Vector(5, 5);

    private boolean savingPrefixes;

    /**
     */
    public XIncludeFilter(EventConsumer next) throws SAXException {
        super(next);
        setContentHandler(this);
        setProperty(DECL_HANDLER, this);
        setProperty(LEXICAL_HANDLER, this);
    }

    private void fatal(SAXParseException e) throws SAXException {
        ErrorHandler eh;
        eh = getErrorHandler();
        if (eh != null) eh.fatalError(e);
        throw e;
    }

    /**
     * Passes "this" down the filter chain as a proxy locator.
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(this);
    }

    /** Used for proxy locator; do not call directly. */
    public String getSystemId() {
        return (locator == null) ? null : locator.getSystemId();
    }

    /** Used for proxy locator; do not call directly. */
    public String getPublicId() {
        return (locator == null) ? null : locator.getPublicId();
    }

    /** Used for proxy locator; do not call directly. */
    public int getLineNumber() {
        return (locator == null) ? -1 : locator.getLineNumber();
    }

    /** Used for proxy locator; do not call directly. */
    public int getColumnNumber() {
        return (locator == null) ? -1 : locator.getColumnNumber();
    }

    /**
     * Assigns the flag controlling the setting of the SAX2
     * <em>namespace-prefixes</em> flag.
     */
    public void setSavingPrefixes(boolean flag) {
        savingPrefixes = flag;
    }

    /**
     * Returns the flag controlling the setting of the SAX2
     * <em>namespace-prefixes</em> flag when parsing included documents.
     * The default value is the SAX2 default (false), which discards
     * information that can be useful.
     */
    public boolean isSavingPrefixes() {
        return savingPrefixes;
    }

    private String addMarker(String uri) throws SAXException {
        if (locator != null && locator.getSystemId() != null) uri = locator.getSystemId();
        if (uri == null) fatal(new SAXParseException("Entity URI is unknown", locator));
        try {
            URL url = new URL(uri);
            uri = url.toString();
            if (inclusions.contains(uri)) fatal(new SAXParseException("XInclude, circular inclusion", locator));
            inclusions.addElement(uri);
            uris.push(url);
        } catch (IOException e) {
            fatal(new SAXParseException("parser bug: relative URI", locator, e));
        }
        return uri;
    }

    private void pop(String uri) {
        inclusions.removeElement(uri);
        uris.pop();
    }

    public void startDocument() throws SAXException {
        ignoreCount = 0;
        addMarker(null);
        super.startDocument();
    }

    public void endDocument() throws SAXException {
        inclusions.setSize(0);
        extEntities.clear();
        uris.setSize(0);
        super.endDocument();
    }

    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        if (name.charAt(0) == '%') return;
        try {
            URL url = new URL(locator.getSystemId());
            systemId = new URL(url, systemId).toString();
        } catch (IOException e) {
        }
        extEntities.put(name, systemId);
    }

    public void startEntity(String name) throws SAXException {
        if (ignoreCount != 0) {
            ignoreCount++;
            return;
        }
        String uri = (String) extEntities.get(name);
        if (uri != null) addMarker(uri);
        super.startEntity(name);
    }

    public void endEntity(String name) throws SAXException {
        if (ignoreCount != 0) {
            if (--ignoreCount != 0) return;
        }
        String uri = (String) extEntities.get(name);
        if (uri != null) pop(uri);
        super.endEntity(name);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (ignoreCount != 0) {
            ignoreCount++;
            return;
        }
        URL baseURI = (URL) uris.peek();
        String base;
        base = atts.getValue("http://www.w3.org/XML/1998/namespace", "base");
        if (base == null) uris.push(baseURI); else {
            URL url;
            if (base.indexOf('#') != -1) fatal(new SAXParseException("xml:base with fragment: " + base, locator));
            try {
                baseURI = new URL(baseURI, base);
                uris.push(baseURI);
            } catch (Exception e) {
                fatal(new SAXParseException("xml:base with illegal uri: " + base, locator, e));
            }
        }
        if (!"http://www.w3.org/2001/XInclude".equals(uri)) {
            super.startElement(uri, localName, qName, atts);
            return;
        }
        if ("include".equals(localName)) {
            String href = atts.getValue("href");
            String parse = atts.getValue("parse");
            String encoding = atts.getValue("encoding");
            URL url = (URL) uris.peek();
            SAXParseException x = null;
            if (href == null) fatal(new SAXParseException("XInclude missing href", locator));
            if (href.indexOf('#') != -1) fatal(new SAXParseException("XInclude with fragment: " + href, locator));
            if (parse == null || "xml".equals(parse)) x = xinclude(url, href); else if ("text".equals(parse)) x = readText(url, href, encoding); else fatal(new SAXParseException("unknown XInclude parsing mode: " + parse, locator));
            if (x == null) {
                ignoreCount++;
                return;
            }
            fatal(x);
        } else if ("fallback".equals(localName)) {
            fatal(new SAXParseException("illegal top level XInclude 'fallback' element", locator));
        } else {
            ErrorHandler eh = getErrorHandler();
            if (eh != null) eh.warning(new SAXParseException("unrecognized toplevel XInclude element: " + localName, locator));
            super.startElement(uri, localName, qName, atts);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (ignoreCount != 0) {
            if (--ignoreCount != 0) return;
        }
        uris.pop();
        if (!("http://www.w3.org/2001/XInclude".equals(uri) && "include".equals(localName))) super.endElement(uri, localName, qName);
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (ignoreCount == 0) super.characters(ch, start, length);
    }

    public void processingInstruction(String target, String value) throws SAXException {
        if (ignoreCount == 0) super.processingInstruction(target, value);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (ignoreCount == 0) super.ignorableWhitespace(ch, start, length);
    }

    public void comment(char ch[], int start, int length) throws SAXException {
        if (ignoreCount == 0) super.comment(ch, start, length);
    }

    public void startCDATA() throws SAXException {
        if (ignoreCount == 0) super.startCDATA();
    }

    public void endCDATA() throws SAXException {
        if (ignoreCount == 0) super.endCDATA();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (ignoreCount == 0) super.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (ignoreCount == 0) super.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        if (ignoreCount == 0) super.skippedEntity(name);
    }

    void setLocator(Locator l) {
        locator = l;
    }

    Locator getLocator() {
        return locator;
    }

    private class Scrubber extends EventFilter {

        Scrubber(EventFilter f) throws SAXException {
            super(f);
            super.setContentHandler(this);
            super.setProperty(LEXICAL_HANDLER, this);
            super.setDTDHandler(null);
            super.setProperty(DECL_HANDLER, null);
        }

        public void setDocumentLocator(Locator l) {
            setLocator(l);
        }

        public void startDocument() {
        }

        public void endDocument() {
        }

        private void reject(String message) throws SAXException {
            fatal(new SAXParseException(message, getLocator()));
        }

        public void startDTD(String root, String publicId, String systemId) throws SAXException {
            reject("XIncluded DTD: " + systemId);
        }

        public void endDTD() throws SAXException {
            reject("XIncluded DTD");
        }

        public void skippedEntity(String name) throws SAXException {
            reject("XInclude skipped entity: " + name);
        }
    }

    private SAXParseException xinclude(URL url, String href) throws SAXException {
        XMLReader helper;
        Scrubber scrubber;
        Locator savedLocator = locator;
        helper = XMLReaderFactory.createXMLReader();
        helper.setErrorHandler(getErrorHandler());
        helper.setFeature(FEATURE_URI + "namespace-prefixes", true);
        scrubber = new Scrubber(this);
        locator = null;
        bind(helper, scrubber);
        try {
            url = new URL(url, href);
            href = url.toString();
            if (inclusions.contains(href)) fatal(new SAXParseException("XInclude, circular inclusion", locator));
            inclusions.addElement(href);
            uris.push(url);
            helper.parse(new InputSource(href));
            return null;
        } catch (java.io.IOException e) {
            return new SAXParseException(href, locator, e);
        } finally {
            pop(href);
            locator = savedLocator;
        }
    }

    private SAXParseException readText(URL url, String href, String encoding) throws SAXException {
        InputStream in = null;
        try {
            URLConnection conn;
            InputStreamReader reader;
            char buf[] = new char[4096];
            int count;
            url = new URL(url, href);
            conn = url.openConnection();
            in = conn.getInputStream();
            if (encoding == null) encoding = Resolver.getEncoding(conn.getContentType());
            if (encoding == null) {
                ErrorHandler eh = getErrorHandler();
                if (eh != null) eh.warning(new SAXParseException("guessing text encoding for URL: " + url, locator));
                reader = new InputStreamReader(in);
            } else reader = new InputStreamReader(in, encoding);
            while ((count = reader.read(buf, 0, buf.length)) != -1) super.characters(buf, 0, count);
            in.close();
            return null;
        } catch (IOException e) {
            return new SAXParseException("can't XInclude text", locator, e);
        }
    }
}
