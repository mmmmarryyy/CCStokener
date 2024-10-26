package org.apache.batik.dom.svg;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Properties;
import org.apache.batik.dom.util.SAXDocumentFactory;
import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.dom.svg12.SVG12DOMImplementation;
import org.apache.batik.util.MimeTypeConstants;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class contains methods for creating SVGDocument instances
 * from an URI using SAX2.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id: SAXSVGDocumentFactory.java,v 1.1 2005/11/21 09:51:29 dev Exp $
 */
public class SAXSVGDocumentFactory extends SAXDocumentFactory implements SVGDocumentFactory {

    public static final Object LOCK = new Object();

    /**
     * Key used for public identifiers
     */
    public static final String KEY_PUBLIC_IDS = "publicIds";

    /**
     * Key used for public identifiers
     */
    public static final String KEY_SKIPPABLE_PUBLIC_IDS = "skippablePublicIds";

    /**
     * Key used for the skippable DTD substitution
     */
    public static final String KEY_SKIP_DTD = "skipDTD";

    /**
     * Key used for system identifiers
     */
    public static final String KEY_SYSTEM_ID = "systemId.";

    /**
     * The dtd public IDs resource bundle class name.
     */
    protected static final String DTDIDS = "org.apache.batik.dom.svg.resources.dtdids";

    /**
     * Constant for HTTP content type header charset field.
     */
    protected static final String HTTP_CHARSET = "charset";

    /**
     * The accepted DTD public IDs.
     */
    protected static String dtdids;

    /**
     * The DTD public IDs we know we can skip.
     */
    protected static String skippable_dtdids;

    /**
     * The DTD content to use when skipping
     */
    protected static String skip_dtd;

    /**
     * The ResourceBunder for the public and system ids
     */
    protected static Properties dtdProps;

    /**
     * Creates a new SVGDocumentFactory object.
     * @param parser The SAX2 parser classname.
     */
    public SAXSVGDocumentFactory(String parser) {
        super(SVGDOMImplementation.getDOMImplementation(), parser);
    }

    /**
     * Creates a new SVGDocumentFactory object.
     * @param parser The SAX2 parser classname.
     * @param dd Whether a document descriptor must be generated.
     */
    public SAXSVGDocumentFactory(String parser, boolean dd) {
        super(SVGDOMImplementation.getDOMImplementation(), parser, dd);
    }

    public SVGDocument createSVGDocument(String uri) throws IOException {
        return (SVGDocument) createDocument(uri);
    }

    /**
     * Creates a SVG Document instance.
     * @param uri The document URI.
     * @param inp The document input stream.
     * @exception IOException if an error occured while reading the document.
     */
    public SVGDocument createSVGDocument(String uri, InputStream inp) throws IOException {
        return (SVGDocument) createDocument(uri, inp);
    }

    /**
     * Creates a SVG Document instance.
     * @param uri The document URI.
     * @param r The document reader.
     * @exception IOException if an error occured while reading the document.
     */
    public SVGDocument createSVGDocument(String uri, Reader r) throws IOException {
        return (SVGDocument) createDocument(uri, r);
    }

    /**
     * Creates a SVG Document instance.
     * This method supports gzipped sources.
     * @param uri The document URI.
     * @exception IOException if an error occured while reading the document.
     */
    public Document createDocument(String uri) throws IOException {
        ParsedURL purl = new ParsedURL(uri);
        InputStream is = purl.openStream(MimeTypeConstants.MIME_TYPES_SVG);
        InputSource isrc = new InputSource(is);
        String contentType = purl.getContentType();
        int cindex = -1;
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            cindex = contentType.indexOf(HTTP_CHARSET);
        }
        if (cindex != -1) {
            int i = cindex + HTTP_CHARSET.length();
            int eqIdx = contentType.indexOf('=', i);
            if (eqIdx != -1) {
                eqIdx++;
                String charset;
                int idx = contentType.indexOf(',', eqIdx);
                int semiIdx = contentType.indexOf(';', eqIdx);
                if ((semiIdx != -1) && ((semiIdx < idx) || (idx == -1))) idx = semiIdx;
                if (idx != -1) charset = contentType.substring(eqIdx, idx); else charset = contentType.substring(eqIdx);
                isrc.setEncoding(charset.trim());
            }
        }
        isrc.setSystemId(uri);
        Document doc = super.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", uri, isrc);
        try {
            ((SVGOMDocument) doc).setURLObject(new URL(purl.toString()));
        } catch (MalformedURLException mue) {
            throw new IOException("Malformed URL: " + uri);
        }
        return doc;
    }

    /**
     * Creates a SVG Document instance.
     * @param uri The document URI.
     * @param inp The document input stream.
     * @exception IOException if an error occured while reading the document.
     */
    public Document createDocument(String uri, InputStream inp) throws IOException {
        Document doc;
        InputSource is = new InputSource(inp);
        is.setSystemId(uri);
        try {
            doc = super.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", uri, is);
            if (uri != null) {
                ((SVGOMDocument) doc).setURLObject(new URL(uri));
            }
        } catch (MalformedURLException e) {
            throw new IOException(e.getMessage());
        }
        return doc;
    }

    /**
     * Creates a SVG Document instance.
     * @param uri The document URI.
     * @param r The document reader.
     * @exception IOException if an error occured while reading the document.
     */
    public Document createDocument(String uri, Reader r) throws IOException {
        Document doc;
        InputSource is = new InputSource(r);
        is.setSystemId(uri);
        try {
            doc = super.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", uri, is);
            if (uri != null) {
                ((SVGOMDocument) doc).setURLObject(new URL(uri));
            }
        } catch (MalformedURLException e) {
            throw new IOException(e.getMessage());
        }
        return doc;
    }

    /**
     * Creates a Document instance.
     * @param ns The namespace URI of the root element of the document.
     * @param root The name of the root element of the document.
     * @param uri The document URI.
     * @exception IOException if an error occured while reading the document.
     */
    public Document createDocument(String ns, String root, String uri) throws IOException {
        if (!SVGDOMImplementation.SVG_NAMESPACE_URI.equals(ns) || !"svg".equals(root)) {
            throw new RuntimeException("Bad root element");
        }
        return createDocument(uri);
    }

    /**
     * Creates a Document instance.
     * @param ns The namespace URI of the root element of the document.
     * @param root The name of the root element of the document.
     * @param uri The document URI.
     * @param is The document input stream.
     * @exception IOException if an error occured while reading the document.
     */
    public Document createDocument(String ns, String root, String uri, InputStream is) throws IOException {
        if (!SVGDOMImplementation.SVG_NAMESPACE_URI.equals(ns) || !"svg".equals(root)) {
            throw new RuntimeException("Bad root element");
        }
        return createDocument(uri, is);
    }

    /**
     * Creates a Document instance.
     * @param ns The namespace URI of the root element of the document.
     * @param root The name of the root element of the document.
     * @param uri The document URI.
     * @param r The document reader.
     * @exception IOException if an error occured while reading the document.
     */
    public Document createDocument(String ns, String root, String uri, Reader r) throws IOException {
        if (!SVGDOMImplementation.SVG_NAMESPACE_URI.equals(ns) || !"svg".equals(root)) {
            throw new RuntimeException("Bad root element");
        }
        return createDocument(uri, r);
    }

    public DOMImplementation getDOMImplementation(String ver) {
        if ((ver == null) || (ver.length() == 0) || ver.equals("1.0") || ver.equals("1.1")) return SVGDOMImplementation.getDOMImplementation();
        return SVG12DOMImplementation.getDOMImplementation();
    }

    /**
     * <b>SAX</b>: Implements {@link
     * org.xml.sax.ContentHandler#startDocument()}.
     */
    public void startDocument() throws SAXException {
        super.startDocument();
        namespaces.put("", SVGDOMImplementation.SVG_NAMESPACE_URI);
        namespaces.put("xlink", XLinkSupport.XLINK_NAMESPACE_URI);
    }

    /**
     * <b>SAX2</b>: Implements {@link
     * org.xml.sax.EntityResolver#resolveEntity(String,String)}.
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        try {
            synchronized (LOCK) {
                if (dtdProps == null) {
                    dtdProps = new Properties();
                    try {
                        Class cls = SAXSVGDocumentFactory.class;
                        InputStream is = cls.getResourceAsStream("resources/dtdids.properties");
                        dtdProps.load(is);
                    } catch (IOException ioe) {
                        throw new SAXException(ioe);
                    }
                }
                if (dtdids == null) dtdids = dtdProps.getProperty(KEY_PUBLIC_IDS);
                if (skippable_dtdids == null) skippable_dtdids = dtdProps.getProperty(KEY_SKIPPABLE_PUBLIC_IDS);
                if (skip_dtd == null) skip_dtd = dtdProps.getProperty(KEY_SKIP_DTD);
            }
            if (publicId == null) return null;
            if (!isValidating && (skippable_dtdids.indexOf(publicId) != -1)) {
                return new InputSource(new StringReader(skip_dtd));
            }
            if (dtdids.indexOf(publicId) != -1) {
                String localSystemId = dtdProps.getProperty(KEY_SYSTEM_ID + publicId.replace(' ', '_'));
                if (localSystemId != null && !"".equals(localSystemId)) {
                    return new InputSource(getClass().getResource(localSystemId).toString());
                }
            }
        } catch (MissingResourceException e) {
            throw new SAXException(e);
        }
        return null;
    }
}
