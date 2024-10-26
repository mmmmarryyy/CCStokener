package org.akrogen.core.impl.resources;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import org.akrogen.core.resources.IResourceLocator;

/**
 * Http resources locator implementation.
 * 
 * @version 1.0.0
 * @author <a href="mailto:angelo.zerr@gmail.com">Angelo ZERR</a>
 * 
 */
public class HttpResourcesLocatorImpl implements IResourceLocator {

    public String resolve(String uri) {
        if (uri.startsWith("http")) return uri;
        return null;
    }

    public InputStream getInputStream(String uri) throws Exception {
        URL url = new java.net.URL((new File("./")).toURL(), uri);
        return url.openStream();
    }

    public Reader getReader(String uri) throws Exception {
        return null;
    }
}
