package net.sf.jpackit.pkg.classloader.url;

import net.sf.jpackit.pkg.classloader.JPackitClassLoaderContext;
import java.net.URLStreamHandler;

/**
 * JPackitURLStreamHandler is implementation of java.net.URLStreamHandler for 
 * handling URLConnection to internal jar representation.
 * @author Kamil K. Shamgunov 
 */
public class JPackitURLStreamHandler extends URLStreamHandler {

    private JPackitClassLoaderContext context;

    /** Creates a new instance of JPackitURLStreamHandler */
    public JPackitURLStreamHandler(JPackitClassLoaderContext context) {
        this.context = context;
    }

    protected java.net.URLConnection openConnection(java.net.URL url) throws java.io.IOException {
        return new JPackitURLConnection(context, url);
    }
}
