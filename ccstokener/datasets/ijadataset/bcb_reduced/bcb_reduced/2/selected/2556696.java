package org.ofbiz.base.config;

import java.net.*;
import java.io.*;

/**
 * Loads resources from a URL
 *
 */
public class UrlLoader extends ResourceLoader implements java.io.Serializable {

    public URL getURL(String location) throws GenericConfigException {
        String fullLocation = fullLocation(location);
        URL url = null;
        try {
            url = new URL(fullLocation);
        } catch (java.net.MalformedURLException e) {
            throw new GenericConfigException("Error with malformed URL while trying to load URL resource at location [" + fullLocation + "]", e);
        }
        if (url == null) {
            throw new GenericConfigException("URL Resource not found: " + fullLocation);
        }
        return url;
    }

    public InputStream loadResource(String location) throws GenericConfigException {
        URL url = getURL(location);
        try {
            return url.openStream();
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error opening URL resource at location [" + url.toExternalForm() + "]", e);
        }
    }
}
