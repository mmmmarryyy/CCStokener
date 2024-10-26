package org.qtitools.qti.rendering.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * Resolver capable of loading java resources.
 * 
 * @author Jonathon Hare
 *
 */
public class PackagedUriResolver implements URIResolver {

    private Class<?> clazz;

    /**
	 * Creates a PackagedUriResolver with a given class. By setting the
	 * class, you can have some control over with ClassLoader will be used.
	 * 
	 * @param clazz Class to use
	 */
    public PackagedUriResolver(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Source resolve(String href, String base) throws TransformerException {
        URL url = clazz.getResource(href);
        if (url != null) try {
            return new StreamSource((InputStream) url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
