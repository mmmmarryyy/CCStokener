package net.sf.istcontract.wsimport.wsdl.parser;

import net.sf.istcontract.wsimport.api.streaming.XMLStreamReaderFactory;
import net.sf.istcontract.wsimport.api.wsdl.parser.XMLEntityResolver;
import net.sf.istcontract.wsimport.streaming.TidyXMLStreamReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Wraps {@link EntityResolver} into {@link net.sf.istcontract.wsimport.api.wsdl.parser.XMLEntityResolver}.
 *
 * @author Kohsuke Kawaguchi
 */
final class EntityResolverWrapper implements XMLEntityResolver {

    private final EntityResolver core;

    public EntityResolverWrapper(EntityResolver core) {
        this.core = core;
    }

    public Parser resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource source = core.resolveEntity(publicId, systemId);
        if (source == null) return null;
        if (source.getSystemId() != null) systemId = source.getSystemId();
        URL url = new URL(systemId);
        InputStream stream = url.openStream();
        return new Parser(url, new TidyXMLStreamReader(XMLStreamReaderFactory.create(url.toExternalForm(), stream, true), stream));
    }
}
