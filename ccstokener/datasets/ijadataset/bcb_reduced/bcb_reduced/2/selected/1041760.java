package org.exist.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class SpecialEntityResolver implements EntityResolver2 {

    private String rootURL;

    public SpecialEntityResolver(String rootURL) {
        this.rootURL = rootURL;
    }

    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        if (baseURI != null) return resolveInputSource(baseURI);
        return null;
    }

    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
        return resolveInputSource(systemId);
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return resolveInputSource(systemId);
    }

    private InputSource resolveInputSource(String path) throws IOException {
        try {
            InputSource inputsource = new InputSource();
            URI url = new URI(path);
            InputStream is;
            if (url.isAbsolute()) is = new URL(path).openStream(); else {
                File file = new File(rootURL + path);
                is = new FileInputStream(file);
            }
            inputsource.setByteStream(is);
            inputsource.setSystemId(path);
            return inputsource;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
