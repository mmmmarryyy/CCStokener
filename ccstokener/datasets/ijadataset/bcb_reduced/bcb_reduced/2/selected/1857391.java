package org.doit.muffin;

import java.io.*;
import java.net.*;

public class URLFile implements UserFile {

    private URL url;

    URLFile(URL url) {
        this.url = url;
    }

    public String getName() {
        return url.toString();
    }

    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Can't output to " + url);
    }
}
