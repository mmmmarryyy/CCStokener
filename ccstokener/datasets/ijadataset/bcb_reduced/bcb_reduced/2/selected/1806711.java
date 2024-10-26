package org.eclipse.help.internal.protocols;

import java.io.*;
import java.net.*;

public class HelpURLStreamHandler extends URLStreamHandler {

    private static HelpURLStreamHandler instance;

    /**
	 * Constructor for URLHandler
	 */
    public HelpURLStreamHandler() {
        super();
    }

    /**
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
    protected URLConnection openConnection(URL url) throws IOException {
        String protocol = url.getProtocol();
        if (protocol.equals("help")) {
            return new HelpURLConnection(url);
        }
        return null;
    }

    public static URLStreamHandler getDefault() {
        if (instance == null) {
            instance = new HelpURLStreamHandler();
        }
        return instance;
    }
}
