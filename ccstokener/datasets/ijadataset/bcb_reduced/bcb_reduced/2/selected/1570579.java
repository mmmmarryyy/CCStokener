package org.gjt.sp.jedit.proto.jeditresource;

import java.io.IOException;
import java.net.*;

/**
 *  
 *
 * One somewhat unconventional requirement of URLStreamHandler classes 
 * is that the class name and even the package name have certain restrictions. 
 * You must name the handler class Handler, as in the previous example. 
 * The package name must include the protocol name as the last dot-separated token.
 * This way, the Handler is automatically created in a lazy-fashion by the default
 * URLStreamHandlerFactory.
 *
 * see http://java.sun.com/developer/onlineTraining/protocolhandlers/
 * 
 * You should never need to create an instance of this class directly. 
 *
 */
public class Handler extends URLStreamHandler {

    public URLConnection openConnection(URL url) throws IOException {
        PluginResURLConnection c = new PluginResURLConnection(url);
        c.connect();
        return c;
    }
}
