package gnu.xml.validation.relaxng;

import java.io.IOException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Schema factory for RELAX NG grammars.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class RELAXNGSchemaFactory extends SchemaFactory {

    LSResourceResolver resourceResolver;

    ErrorHandler errorHandler;

    public LSResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(LSResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public boolean isSchemaLanguageSupported(String schemaLanguage) {
        return XMLConstants.RELAXNG_NS_URI.equals(schemaLanguage);
    }

    public Schema newSchema() throws SAXException {
        throw new UnsupportedOperationException();
    }

    public Schema newSchema(Source[] schemata) throws SAXException {
        if (schemata == null || schemata.length != 1) throw new IllegalArgumentException("must specify one source");
        try {
            Document doc = getDocument(schemata[0]);
            FullSyntaxBuilder builder = new FullSyntaxBuilder();
            return builder.parse(doc);
        } catch (IOException e) {
            SAXException e2 = new SAXException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
    }

    private static Document getDocument(Source source) throws SAXException, IOException {
        if (source instanceof DOMSource) {
            Node node = ((DOMSource) source).getNode();
            if (node != null && node instanceof Document) return (Document) node;
        }
        String url = source.getSystemId();
        try {
            InputSource input = new InputSource(url);
            if (source instanceof StreamSource) {
                StreamSource streamSource = (StreamSource) source;
                input.setByteStream(streamSource.getInputStream());
                input.setCharacterStream(streamSource.getReader());
            }
            if (input.getByteStream() == null && input.getCharacterStream() == null && url != null) input.setByteStream(new URL(url).openStream());
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setCoalescing(true);
            f.setExpandEntityReferences(true);
            f.setIgnoringComments(true);
            f.setIgnoringElementContentWhitespace(true);
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(input);
        } catch (ParserConfigurationException e) {
            SAXException e2 = new SAXException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
    }
}
