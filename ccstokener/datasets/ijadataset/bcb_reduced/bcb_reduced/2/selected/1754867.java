package org.argouml.xml;

import java.io.FileInputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Piotr Kaminski
 */
public class DTDEntityResolver implements EntityResolver {

    public static String DTD_DIR = "/org/argouml/xml/dtd/";

    public static final DTDEntityResolver SINGLETON = new DTDEntityResolver();

    protected DTDEntityResolver() {
        super();
    }

    public InputSource resolveEntity(String publicId, String systemId) throws java.io.IOException, SAXException {
        try {
            java.net.URL url = new java.net.URL(systemId);
            try {
                InputSource source = new InputSource(url.openStream());
                if (publicId != null) source.setPublicId(publicId);
                return source;
            } catch (java.io.IOException e) {
                if (systemId.endsWith(".dtd")) {
                    int i = systemId.lastIndexOf('/');
                    i++;
                    java.io.InputStream is;
                    is = getClass().getResourceAsStream(DTD_DIR + systemId.substring(i));
                    if (is == null) {
                        try {
                            is = new FileInputStream(DTD_DIR.substring(1) + systemId.substring(i));
                        } catch (Exception ex) {
                        }
                    }
                    if (is != null) return new InputSource(is);
                }
                throw e;
            }
        } catch (java.net.MalformedURLException e2) {
        }
        return null;
    }
}
