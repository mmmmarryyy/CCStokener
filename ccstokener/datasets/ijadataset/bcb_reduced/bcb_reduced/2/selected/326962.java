package org.wings.style;

import java.io.*;
import java.net.*;
import java.util.Set;

/**
 * TODO: documentation
 *
 * @author <a href="mailto:engels@mercatis.de">Holger Engels</a>
 * @version $Revision: 163 $
 */
public class URLStyleSheet implements StyleSheet {

    URL url = null;

    String name;

    /**
     * TODO: documentation
     *
     * @param url
     */
    public URLStyleSheet(URL url) {
        this.url = url;
        if (url != null) name = url.toString();
    }

    /**
     * TODO: documentation
     *
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (url != null) return url.openStream();
        return null;
    }

    public boolean isStable() {
        return false;
    }

    /**
     * TODO: documentation
     *
     * @return
     */
    public Set styleSet() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO: documentation
     *
     * @return
     */
    public String toString() {
        return name;
    }
}
