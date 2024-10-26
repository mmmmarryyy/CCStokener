package org.jxul.swing;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * @author Will Etson
 */
public class DefaultResolver implements URIResolver {

    private URIResolver resolver;

    /**
	 * 
	 */
    public DefaultResolver() {
        this(null);
    }

    /**
	 * @param resolver
	 */
    public DefaultResolver(URIResolver resolver) {
        this.resolver = resolver;
    }

    /**
	 * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
	 */
    public Source resolve(String href, String base) throws TransformerException {
        Source result = null;
        try {
            if (this.resolver != null) {
                result = this.resolver.resolve(href, base);
                if (result != null) {
                    return result;
                }
            }
            URI uri = new URI(href);
            if (!uri.isAbsolute()) {
                System.out.println(base);
                URI baseURI = new URI(base);
                uri = baseURI.resolve(uri);
            }
            if (uri.getScheme().equals("chrome")) {
                result = new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(uri.getPath().substring(1)));
            } else if (uri.getScheme().equals("file")) {
                result = new StreamSource(new FileInputStream(uri.getPath()));
            } else if (uri.getScheme().equals("http")) {
                URL url = new URL(uri.toString());
                result = new StreamSource(url.openStream());
            } else {
                throw new TransformerException("Scheme not recognized: " + uri.getScheme());
            }
            result.setSystemId(uri.toString());
        } catch (Exception e) {
            if (e instanceof TransformerException) {
                throw (TransformerException) e;
            } else {
                throw new TransformerException(e);
            }
        }
        return result;
    }

    /**
	 * @return the contained resolver
	 */
    public URIResolver getResolver() {
        if (resolver == null) {
            return this;
        }
        return resolver;
    }

    /**
	 * @param resolver
	 */
    public void setResolver(URIResolver resolver) {
        this.resolver = resolver;
    }
}
