package net.plugg.server.config.xml;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Entity Resolver for the configuration files
 * <p>
 * Created on Mar 20, 2008
 * @author cracelr
 */
public class ConfigurationEntityResolver implements EntityResolver {

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        URL url = new URL(System.getenv("plugg_home") + "/" + systemId);
        System.out.println("SystemId = " + systemId);
        return new InputSource(url.openStream());
    }
}
