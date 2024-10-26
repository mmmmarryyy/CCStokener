package gnu.xml.dom;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Document builder using the GNU DOM Load &amp; Save implementation.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
class DomDocumentBuilder extends DocumentBuilder {

    final DOMImplementation impl;

    final DOMImplementationLS ls;

    final LSParser parser;

    DomDocumentBuilder(DOMImplementation impl, DOMImplementationLS ls, LSParser parser) {
        this.impl = impl;
        this.ls = ls;
        this.parser = parser;
    }

    public boolean isNamespaceAware() {
        DOMConfiguration config = parser.getDomConfig();
        return ((Boolean) config.getParameter("namespaces")).booleanValue();
    }

    public boolean isValidating() {
        DOMConfiguration config = parser.getDomConfig();
        return ((Boolean) config.getParameter("validating")).booleanValue();
    }

    public boolean isXIncludeAware() {
        DOMConfiguration config = parser.getDomConfig();
        return ((Boolean) config.getParameter("xinclude-aware")).booleanValue();
    }

    public void setEntityResolver(EntityResolver resolver) {
        DOMConfiguration config = parser.getDomConfig();
        config.setParameter("entity-resolver", resolver);
    }

    public void setErrorHandler(ErrorHandler handler) {
        DOMConfiguration config = parser.getDomConfig();
        config.setParameter("error-handler", handler);
    }

    public DOMImplementation getDOMImplementation() {
        return impl;
    }

    public Document newDocument() {
        return impl.createDocument(null, null, null);
    }

    public Document parse(InputStream in) throws SAXException, IOException {
        LSInput input = ls.createLSInput();
        input.setByteStream(in);
        return parser.parse(input);
    }

    public Document parse(InputStream in, String systemId) throws SAXException, IOException {
        LSInput input = ls.createLSInput();
        input.setByteStream(in);
        input.setSystemId(systemId);
        return parser.parse(input);
    }

    public Document parse(String systemId) throws SAXException, IOException {
        return parser.parseURI(systemId);
    }

    public Document parse(InputSource is) throws SAXException, IOException {
        LSInput input = ls.createLSInput();
        String systemId = is.getSystemId();
        InputStream in = is.getByteStream();
        if (in != null) {
            input.setByteStream(in);
        } else {
            Reader reader = is.getCharacterStream();
            if (reader != null) {
                input.setCharacterStream(reader);
            } else {
                URL url = new URL(systemId);
                input.setByteStream(url.openStream());
            }
        }
        input.setPublicId(is.getPublicId());
        input.setSystemId(systemId);
        input.setEncoding(is.getEncoding());
        return parser.parse(input);
    }
}
