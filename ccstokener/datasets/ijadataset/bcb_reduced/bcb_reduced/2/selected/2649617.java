package rachel.loader;

import java.util.jar.*;
import java.io.*;
import java.net.*;
import caramel.util.*;
import rachel.*;

public class JarResourceLoader implements ResourceLoader {

    private ResourceLoader _delegate;

    public JarResourceLoader(URL url) throws IOException {
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        File file = File.createTempFile("rachel-", null);
        FileUtils.saveStreamToFile(in, file);
        JarFile jarFile = new JarFile(file, false);
        init(file, jarFile);
    }

    public JarResourceLoader(File file) throws IOException {
        JarFile jarFile = new JarFile(file, false);
        init(file, jarFile);
    }

    private void init(File file, JarFile jarFile) {
        _delegate = new JarResourceLoaderHelper(file, jarFile);
    }

    public InputStream getResourceAsStream(String name) {
        return _delegate.getResourceAsStream(name);
    }

    public URL getResourceAsUrl(String name) {
        return _delegate.getResourceAsUrl(name);
    }
}
