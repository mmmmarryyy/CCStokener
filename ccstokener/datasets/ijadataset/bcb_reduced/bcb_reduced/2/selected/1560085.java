package org.drools.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * SchemaEntityResolver allows shemaLocation to locate schemas in the class
 * path.
 * <p>
 * 
 * @author czhao
 * @since Dec 2, 2005
 * 
 */
public class SchemaEntityResolver implements EntityResolver {

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (!systemId.toLowerCase().endsWith("xsd")) {
            return null;
        }
        try {
            URL url = new URL(systemId);
            return new InputSource(url.openStream());
        } catch (Exception e) {
        }
        String xsd;
        int index = systemId.lastIndexOf("/");
        if (index == -1) {
            index = systemId.lastIndexOf("\\");
        }
        if (index != -1) {
            xsd = systemId.substring(index + 1);
        } else {
            xsd = systemId;
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = RuleSetReader.class.getClassLoader();
        }
        try {
            return new InputSource(cl.getResourceAsStream("META-INF/" + xsd));
        } catch (Exception e) {
        }
        try {
            return new InputSource(cl.getResourceAsStream("/META-INF/" + xsd));
        } catch (Exception e) {
        }
        try {
            return new InputSource(cl.getResourceAsStream("/" + xsd));
        } catch (Exception e) {
        }
        try {
            return new InputSource(new BufferedInputStream(new FileInputStream(xsd)));
        } catch (Exception e) {
        }
        cl = ClassLoader.getSystemClassLoader();
        try {
            return new InputSource(cl.getResourceAsStream("META-INF/" + xsd));
        } catch (Exception e) {
        }
        try {
            return new InputSource(cl.getResourceAsStream("/META-INF/" + xsd));
        } catch (Exception e) {
        }
        try {
            return new InputSource(cl.getResourceAsStream("/" + xsd));
        } catch (Exception e) {
        }
        try {
            return new InputSource(new BufferedInputStream(new FileInputStream(xsd)));
        } catch (Exception e) {
        }
        return null;
    }
}
