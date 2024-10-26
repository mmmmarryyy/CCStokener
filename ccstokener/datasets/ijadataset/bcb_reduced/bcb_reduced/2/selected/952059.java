package org.personalsmartspace.ipojo.xml.parser;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Entity Resolver loading embedded XML Schemas.
 * This resolver avoid using a network connection to get schemas as they
 * are loaded from the manipulator jar file.
* @author <a href="mailto:patx.cheevers@intel.com">Persist Project Team</a>

 */
public class PSSSchemaResolver implements EntityResolver {

    /**
     * Directory where embedded schemas are copied.
     */
    public static final String XSD_PATH = "xsd";

    /**
     * Resolves systemIds to use embedded schemas. The schemas are loaded from
     * the {@link PSSSchemaResolver#XSD_PATH} directory with the current classloader.
     * @param publicId the publicId
     * @param systemId the systemId (Schema URL)
     * @return the InputSource to load the schemas or <code>null</code> if the schema
     * cannot be loaded (not embedded)
     * @throws SAXException cannot happen 
     * @throws IOException when the embedded resource cannot be read correctly
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        URL url = getURL(systemId);
        if (url == null) {
            return null;
        } else {
            return new InputSource(url.openStream());
        }
    }

    /**
     * Computes the local URL of the given system Id.
     * This URL is computed by trying to load the resource from
     * the current classloader. First, the last fragment (file name) of the system id
     * url is extracted and the file is loaded from the {@link PSSSchemaResolver#XSD_PATH}
     * directory ('xsd/extracted') 
     * @param id the systemId to load
     * @return the URL to the resources or <code>null</code> if the resource cannot be found.
     */
    private URL getURL(String id) {
        int index = id.lastIndexOf('/');
        String fragment = id.substring(index);
        return this.getClass().getClassLoader().getResource(XSD_PATH + fragment);
    }
}
