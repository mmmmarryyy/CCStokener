package org.wings.style;

import java.io.*;
import java.net.*;
import java.util.Set;

/**
 * TODO: documentation
 *
 * @author <a href="mailto:engels@mercatis.de">Holger Engels</a>
 * @version $Revision: 6 $
 */
public class ResourceStyleSheet implements StyleSheet {

    File file = null;

    URL url = null;

    String name;

    /**
     * TODO: documentation
     *
     * @param file
     * @throws IOException
     */
    public ResourceStyleSheet(File file) throws IOException {
        this.file = file;
        if (!file.exists() || !file.canRead()) throw new IOException(file.getName() + " is not readable");
        name = file.getName();
    }

    /**
     * TODO: documentation
     *
     * @param url
     */
    public ResourceStyleSheet(URL url) {
        this.url = url;
        name = url.toString();
    }

    /**
     * TODO: documentation
     *
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (file != null) return new FileInputStream(file); else if (url != null) return url.openStream();
        return null;
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
