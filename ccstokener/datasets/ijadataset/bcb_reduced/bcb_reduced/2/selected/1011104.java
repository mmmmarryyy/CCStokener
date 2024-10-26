package org.jumpmind.symmetric.ddl.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An entity resolver that matches the specific database dtds to the one that comes
 * with DdlUtils, and that can handle file url's.
 * 
 * @version $Revision: 481151 $
 */
public class LocalEntityResolver implements EntityResolver {

    /** The default DTD. */
    public static final String DTD_PREFIX = "http://db.apache.org/torque/dtd/database";

    /**
     * {@inheritDoc}
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        InputSource result = null;
        if (systemId.startsWith(DTD_PREFIX)) {
            InputStream input = getClass().getResourceAsStream("/database.dtd");
            if (input != null) {
                result = new InputSource(input);
            }
        } else if (systemId.startsWith("file:")) {
            try {
                URL url = new URL(systemId);
                if ("file".equals(url.getProtocol())) {
                    String path = systemId.substring("file:".length());
                    if (path.startsWith("//")) {
                        path = path.substring(2);
                    }
                    result = new InputSource(new FileInputStream(path));
                } else {
                    result = new InputSource(url.openStream());
                }
            } catch (Exception ex) {
                throw new SAXException(ex);
            }
        }
        return result;
    }
}
