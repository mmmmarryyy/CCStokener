package org.ikasan.common.xml.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.ikasan.common.util.ResourceUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class implements interface <code>org.xml.sax.EntityResolver</code>
 * to resolve external entities.
 *
 * <pre>
 *    Usage Example:
 *
 *    import javax.xml.parsers.SAXParserFactory;
 *    import javax.xml.parsers.SAXParser;
 *    import org.xml.sax.InputSource;
 *    import org.xml.sax.XMLReader;
 *
 *    try
 *    {
 *       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
 *       factory.setNamespaceAware(true);
 *       factory.setValidating(true);
 *       DocumentBuilder builder = this.factory.newDocumentBuilder();
 *       builder.setErrorHandler(new DefaultErrorHandler());
 *       builder.setEntityResolver(new DefaultEntityResolver);
 *       builder.parse(InputSource("example.xml"));
 *    }
 *    catch (Exception e)
 *    {
 *        e.printStackTrace();
 *    }
 *
 * </pre>
 *
 * author Jun Suetake
 *
 */
public class DefaultEntityResolver implements EntityResolver {

    /**
     * The logger instance.
     */
    private static Logger logger = Logger.getLogger(DefaultEntityResolver.class);

    /**
     * Create a new instance of <code>DefaultEntityResolver</code>
     * with the default trace level.
     */
    public DefaultEntityResolver() {
    }

    /**
     * This method is invoked if the default entityResolver is registered
     * on the parser instance.
     * 
     * It will try to resolve the resource identified via the systemId from
     * the classpath.
     * 
     * If the resource is not found return null to allow the default URL
     * resolver to handle it.
     *
     * @param publicId
     * @param systemId
     * @return InputSource
     * @throws SAXException
     * @throws IOException
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        URI uri = null;
        String resource = null;
        InputStream is = null;
        try {
            uri = new URI(systemId);
            resource = uri.getSchemeSpecificPart();
            is = this.getInputStream(resource);
            if (is == null) is = this.getInputStream(resource.substring(resource.lastIndexOf('/') + 1));
            if (is != null) {
                InputSource src = new InputSource(is);
                src.setSystemId(systemId);
                return src;
            }
        } catch (URISyntaxException e) {
            logger.debug("systemId [" + systemId + "] is not a valid URI", e);
        }
        return null;
    }

    /**
     * Utility method for loading the resource and returning as an input stream
     * @param resource
     * @return InputStream
     * @throws IOException
     */
    private InputStream getInputStream(final String resource) throws IOException {
        URL url = ResourceUtils.getAsUrl(resource);
        if (url != null) return url.openStream();
        return null;
    }
}
