package net.sourceforge.liftoff.installer.source;

import java.net.*;
import java.io.IOException;

/**
 * URL-Handlers for resources from a self extraxting zip archive.
 */
public class ResURLHandler extends URLStreamHandler {

    public URLConnection openConnection(URL url) throws IOException {
        ResURLConnection con = new ResURLConnection(url);
        con.connect();
        return con;
    }
}
