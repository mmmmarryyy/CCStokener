package org.ofbiz.base.config;

import java.net.*;
import java.io.*;
import org.ofbiz.base.util.*;

/**
 * Loads resources from the classpath
 *
 */
public class ClasspathLoader extends ResourceLoader implements java.io.Serializable {

    public URL getURL(String location) throws GenericConfigException {
        String fullLocation = fullLocation(location);
        URL url = UtilURL.fromResource(fullLocation);
        if (url == null) {
            throw new GenericConfigException("Classpath Resource not found: " + fullLocation);
        }
        return url;
    }

    public InputStream loadResource(String location) throws GenericConfigException {
        URL url = getURL(location);
        try {
            return url.openStream();
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error opening classpath resource at location [" + url.toExternalForm() + "]", e);
        }
    }
}
