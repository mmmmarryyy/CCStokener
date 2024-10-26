package org.extwind.osgi.tapestry.internal.proxy;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.parser.ComponentTemplate;
import org.apache.tapestry5.internal.services.StaxTemplateParser;
import org.apache.tapestry5.internal.services.TemplateParser;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.codehaus.stax2.XMLInputFactory2;

/**
 * @author Donf Yang
 * 
 */
public class ProxyTemplateParser implements TemplateParser, XMLResolver {

    private final Map<String, URL> configuration;

    private final boolean defaultCompressWhitespace;

    private final XMLInputFactory2 inputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();

    public ProxyTemplateParser(Map<String, URL> configuration, @Symbol(SymbolConstants.COMPRESS_WHITESPACE) boolean defaultCompressWhitespace) {
        this.configuration = configuration;
        this.defaultCompressWhitespace = defaultCompressWhitespace;
        inputFactory.configureForSpeed();
        inputFactory.setXMLResolver(this);
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    }

    public ComponentTemplate parseTemplate(Resource templateResource) {
        if (!templateResource.exists()) throw new RuntimeException(ProxyServicesMessages.missingTemplateResource(templateResource));
        ProxyStaxTemplateParser parser;
        try {
            parser = new ProxyStaxTemplateParser(templateResource, inputFactory);
        } catch (Exception ex) {
            throw new RuntimeException(ProxyServicesMessages.newParserError(templateResource, ex), ex);
        }
        return parser.parse(defaultCompressWhitespace);
    }

    public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
        URL url = configuration.get(publicID);
        try {
            if (url != null) return url.openStream();
        } catch (IOException ex) {
            throw new XMLStreamException(String.format("Unable to open stream for resource %s: %s", url, InternalUtils.toMessage(ex)), ex);
        }
        return null;
    }
}
