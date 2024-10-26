package org.extwind.osgi.tapestry.xml;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Donf Yang
 * 
 */
public class ComponentXSDEntityResolver implements EntityResolver {

    protected static final String SCHEMA_COMPONENT = "http://www.extwind.org/schema/component.xsd";

    protected static final String SCHEMA_COMPONENT_RESOURCE = "org/extwind/osgi/tapestry/xml/component.xsd";

    protected static final Map<String, String> schemaMapping = new HashMap<String, String>(1);

    static {
        schemaMapping.put(SCHEMA_COMPONENT, SCHEMA_COMPONENT_RESOURCE);
    }

    private Bundle bundle;

    public ComponentXSDEntityResolver(Bundle bundle) {
        this.bundle = bundle;
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        String resource = schemaMapping.get(systemId);
        URL url = bundle.getResource(resource);
        if (url != null) {
            return new InputSource(url.openStream());
        }
        return null;
    }
}
