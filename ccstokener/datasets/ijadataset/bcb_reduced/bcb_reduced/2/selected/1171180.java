package org.wikiup.core.imp.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.wikiup.core.inf.Resource;
import org.wikiup.util.Assert;

public class URLResource implements Resource {

    private URL url;

    public URLResource(URL url) {
        this.url = url;
    }

    public InputStream open() {
        try {
            return url.openStream();
        } catch (IOException ex) {
            Assert.fail(ex);
        }
        return null;
    }

    public String getURI() {
        return url.getPath();
    }

    public boolean exists() {
        return true;
    }

    public String getHost() {
        return url.getHost();
    }
}
