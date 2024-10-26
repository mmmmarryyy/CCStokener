package net.simpleframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import javax.xml.parsers.SAXParserFactory;
import net.simpleframework.util.IConstants;
import net.simpleframework.util.IoUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import com.sun.org.apache.xerces.internal.impl.Constants;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class AbstractXmlDocument extends ALoggerAware {

    protected Document document;

    protected AbstractXmlDocument() {
    }

    protected AbstractXmlDocument(final URL url) throws IOException {
        this(url.openStream());
    }

    protected AbstractXmlDocument(final InputStream inputStream) {
        createXmlDocument(inputStream);
    }

    protected AbstractXmlDocument(final Reader reader) {
        createXmlDocument(reader);
    }

    protected void createXmlDocument(final Reader reader) {
        try {
            this.document = getSAXReader().read(new InputSource(reader));
            init();
        } catch (final Throwable e) {
            throw XMLParseException.wrapException(e);
        }
    }

    protected void createXmlDocument(final InputStream inputStream) {
        try {
            this.document = getSAXReader().read(new InputSource(inputStream));
            init();
        } catch (final DocumentException e) {
            try {
                final String xString = stripNonValidXMLCharacters(IoUtils.getStringFromInputStream(inputStream, IConstants.UTF8));
                createXmlDocument(new StringReader(xString));
            } catch (final Exception e0) {
                throw XMLParseException.wrapException(e0);
            }
        } catch (final Throwable e) {
            throw XMLParseException.wrapException(e);
        }
    }

    private SAXReader getSAXReader() throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(isValidating());
        factory.setNamespaceAware(isNamespaceAware());
        final XMLReader xmlReader = factory.newSAXParser().getXMLReader();
        final EntityResolver resolver = getEntityResolver();
        if (resolver != null) {
            xmlReader.setEntityResolver(resolver);
        }
        return setFeature(new SAXReader(xmlReader));
    }

    public Document getDocument() {
        return document;
    }

    protected Element getRoot() {
        return document.getRootElement();
    }

    protected Element getElement(final String name) {
        return getElement(null, name);
    }

    protected Element getElement(Element parent, final String name) {
        Element element = (parent == null ? (parent = getRoot()) : parent).element(name);
        if (element == null) {
            element = parent.addElement(name);
        }
        return element;
    }

    protected boolean isValidating() {
        return false;
    }

    protected boolean isNamespaceAware() {
        return false;
    }

    protected boolean isLoadExternalDTDFeature() {
        return false;
    }

    protected SAXReader setFeature(final SAXReader reader) throws SAXException {
        reader.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE, isLoadExternalDTDFeature());
        return reader;
    }

    protected EntityResolver getEntityResolver() {
        return null;
    }

    protected void init() throws Exception {
    }

    public String stripNonValidXMLCharacters(final String in) {
        final StringBuilder out = new StringBuilder();
        char current;
        if (in == null || ("".equals(in))) {
            return "";
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i);
            if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF)) || ((current >= 0xE000) && (current <= 0xFFFD)) || ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
        }
        return out.toString();
    }

    @Override
    public String toString() {
        return document.asXML();
    }

    public static class XMLParseException extends SimpleException {

        private static final long serialVersionUID = 3959123626087714493L;

        public XMLParseException(final String msg) {
            super(msg);
        }

        public XMLParseException(final String msg, final Throwable cause) {
            super(msg, cause);
        }

        public static RuntimeException wrapException(final Throwable throwable) {
            return wrapException(XMLParseException.class, null, throwable);
        }
    }
}
