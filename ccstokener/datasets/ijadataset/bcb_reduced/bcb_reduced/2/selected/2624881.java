package xbrlcore.taxonomy;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class is used to be extended in order to load files from other location than
 * the "Instance directory".
 *
 * @author Nicolas Georges, S�bastien Kirche
 * @author Marvin Froehlich (INFOLOG GmbH)
 */
public class FileLoader {

    private final Proxy proxy;

    public FileLoader() {
        this(null);
    }

    public FileLoader(Proxy proxy) {
        this.proxy = proxy;
    }

    public final Proxy getProxy() {
        return proxy;
    }

    public URL getFile(String basePath, String namespace, String schemaLocation) throws MalformedURLException {
        if ((schemaLocation.indexOf("://") > 0) || schemaLocation.startsWith("file:")) return new URL(schemaLocation);
        if (schemaLocation.startsWith("../")) return new File(getCanonicalFile(basePath, schemaLocation)).toURI().toURL();
        File f = new File(schemaLocation);
        if (f.isAbsolute()) return f.toURI().toURL();
        if (namespace != null) return getURL(namespace, schemaLocation);
        return new File(basePath, schemaLocation).toURI().toURL();
    }

    public static String getCanonicalFile(String path, String file) {
        path = path.replace("\\", "/");
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        while (file.startsWith("../")) {
            int p = path.lastIndexOf('/');
            if (p < 0) return path + "/" + file;
            path = path.substring(0, p);
            file = file.substring(3);
        }
        return path + "/" + file;
    }

    public static final URL getURL(String namespace, String schemaLocation) throws MalformedURLException {
        int p = namespace.lastIndexOf('/');
        if (p > 0) namespace = namespace.substring(0, p);
        return new URL(namespace + "/" + schemaLocation);
    }

    public URLConnection openConnection(URL url) throws IOException {
        if (getProxy() == null) return url.openConnection();
        return url.openConnection(getProxy());
    }
}
