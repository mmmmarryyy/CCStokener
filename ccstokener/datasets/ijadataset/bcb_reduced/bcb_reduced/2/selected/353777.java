package jolie.jap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 *
 * @author Fabrizio Montesi
 */
public class JapURLStreamHandler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws MalformedURLException, IOException {
        return new JapURLConnection(url);
    }
}
