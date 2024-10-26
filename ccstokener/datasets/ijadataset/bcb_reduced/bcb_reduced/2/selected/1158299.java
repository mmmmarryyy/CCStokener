package common.utilities;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class ClasspathURLStreamHandler extends URLStreamHandler {

    private final ClassLoader classLoader;

    public ClasspathURLStreamHandler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        String path = url.getFile();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        URL fixed = classLoader.getResource(path);
        if (fixed == null) {
            System.err.println("Unable to resolve URL: " + url.toString());
            return null;
        }
        return fixed.openConnection();
    }
}
