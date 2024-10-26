package org.silabsoft.rs.web.toolbar.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.silabsoft.rs.web.toolbar.net.WebResource;

/**
 *
 * @author Silabsoft
 */
public class HTTPXMLResource implements WebResource {

    protected URL url;

    public void setUrl(URL url) {
        this.url = url;
    }

    public HTTPXMLResource(URL url) {
        this.url = url;
    }

    public HTTPXMLResource(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public InputStream getResource() {
        try {
            return url.openStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
