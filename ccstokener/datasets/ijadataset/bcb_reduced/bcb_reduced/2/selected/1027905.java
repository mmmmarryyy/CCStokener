package org.extwind.osgi.launch.deploy.impl;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author donf.yang
 * 
 */
public class DeployEntityResolver implements EntityResolver {

    protected static final String SCHEMA_URI = "http://www.extwind.org/schema/deploy.xsd";

    protected static final String SCHEMA_CLASSPATH = "org/extwind/osgi/schema/deploy.xsd";

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (SCHEMA_URI.equals(systemId)) {
            URL url = this.getClass().getClassLoader().getResource(SCHEMA_CLASSPATH);
            if (url != null) {
                return new InputSource(url.openStream());
            }
        }
        return null;
    }
}
