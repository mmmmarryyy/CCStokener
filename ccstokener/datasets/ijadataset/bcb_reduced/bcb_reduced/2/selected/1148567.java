package net.sf.jarclassloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Arne Limburg
 */
public class InnerJarStreamHandler extends URLStreamHandler {

    /**
	 * Returns an {@link net.sf.jarclassloader.InnerJarUrlConnection},
	 * if the specified url is an inner-jar-url,
	 * returns {@link java.net.URL#openConnection()} otherwise.
	 * @param url the url to open the connection to
	 * @return the connection
	 */
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (url.toExternalForm().startsWith(JarBrowser.INNER_JAR_PROTOCOL + JarBrowser.PROTOCOL_DELIMITER)) {
            return new InnerJarUrlConnection(url);
        } else {
            return url.openConnection();
        }
    }
}
