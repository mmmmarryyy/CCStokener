package org.owasp.jxt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * URLEntityResolver
 *
 * @author Jeffrey Ichnowski
 * @version $Revision: 8 $
 */
class URLEntityResolver implements EntityResolver {

    private URL _root;

    public void setRoot(URL root) {
        _root = root;
    }

    public InputSource resolveEntity(String publicId, String systemId) throws IOException {
        URL url = new URL(_root, systemId.replaceFirst("^/+", ""));
        InputStream in = url.openStream();
        InputSource source = new InputSource(in);
        source.setPublicId(publicId);
        source.setSystemId(url.toString());
        return source;
    }
}
