package jaxlib.net.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import jaxlib.lang.ExternalAssertionException;
import jaxlib.net.URLConnectionFactory;
import jaxlib.net.URLs;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: DefaultURLRepository.java 1386 2005-04-22 06:48:32Z joerg_wassmer $
 */
public class DefaultURLRepository extends Object implements URLRepository, Serializable {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    private static final DummyURLConnection DUMMY_URL_CONNECTION = new DummyURLConnection();

    private static boolean shouldSkipResource(URL url) {
        if ("jar".equals(url.getProtocol())) {
            String s = url.getFile();
            if (s.startsWith("file:")) {
                int i = s.lastIndexOf("!/");
                s = s.substring("file:".length(), (i < 0) ? s.length() : i);
                return !new File(s).isFile();
            }
        }
        return false;
    }

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private final URL url;

    /**
   * @since JaXLib 1.0
   */
    private transient URLConnectionFactory urlConnectionFactory;

    private transient File file;

    private transient volatile SoftReference<JarURLConnection> jarFileRef;

    public DefaultURLRepository(URL url) {
        this(url, null);
    }

    public DefaultURLRepository(URL url, URLConnectionFactory connectionFactory) {
        super();
        CheckArg.notNull(url);
        this.url = url;
        this.urlConnectionFactory = connectionFactory;
        this.file = URLs.toFile(url);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.urlConnectionFactory = (URLConnectionFactory) in.readObject();
        this.file = URLs.toFile(this.url);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.urlConnectionFactory);
    }

    private URLConnection createConnection(URL url) throws IOException {
        URLConnectionFactory factory = getURLConnectionFactory();
        return (factory == null) ? url.openConnection() : factory.createURLConnection(url);
    }

    protected URLConnectionFactory getURLConnectionFactory() {
        return this.urlConnectionFactory;
    }

    protected URLConnection openConnection(String resourceName, URL resourceURL, boolean testOnly) throws IOException {
        if (this.file != null) {
            return openFileConnection(resourceName, resourceURL, testOnly);
        } else if ("jar".equalsIgnoreCase(this.url.getProtocol())) {
            return openJarConnection(resourceName, resourceURL, testOnly, false);
        } else {
            URLConnection co = createConnection(resourceURL);
            if (co instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) co;
                http.setRequestMethod(testOnly ? "HEAD" : "GET");
                if (http.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) return null;
            } else {
                if ("ftp".equalsIgnoreCase(resourceURL.getProtocol())) {
                    boolean directory = resourceName.endsWith("/");
                    co.setRequestProperty("type", directory ? "d" : "i");
                }
                co.connect();
                if (co.getContentLength() < 0) return null;
            }
            return testOnly ? DUMMY_URL_CONNECTION : co;
        }
    }

    private URLConnection openFileConnection(String resourceName, URL resourceURL, boolean testOnly) throws IOException {
        File f = new File(this.file, resourceName);
        boolean directory = resourceName.endsWith("/");
        if (directory ? !f.isDirectory() : !f.isFile()) return null; else if (testOnly) return DUMMY_URL_CONNECTION; else return createConnection(resourceURL);
    }

    private URLConnection openJarConnection(String resourceName, URL resourceURL, boolean testOnly, boolean isSecondTry) throws IOException {
        SoftReference<JarURLConnection> jarFileRef = this.jarFileRef;
        JarURLConnection jarFileCo = (jarFileRef == null) ? null : jarFileRef.get();
        if (jarFileCo == null) {
            URLConnection co = createConnection(this.url);
            if (co instanceof JarURLConnection) {
                jarFileCo = (JarURLConnection) co;
                this.jarFileRef = jarFileRef = new SoftReference<JarURLConnection>(jarFileCo);
            } else {
                co = createConnection(resourceURL);
                co.connect();
                if (co.getContentLength() < 0) return null; else return co;
            }
        }
        String repositoryName = jarFileCo.getEntryName();
        String entryName;
        if ((repositoryName == null) || (repositoryName.length() == 0) || repositoryName.equals("/")) {
            if ((resourceName.length() > 1) && resourceName.startsWith("/")) resourceName = resourceName.substring(1);
            entryName = resourceName;
        } else {
            entryName = repositoryName.endsWith("/") || resourceName.startsWith("/") ? Strings.concat(repositoryName, resourceName) : Strings.concat(repositoryName, "/", resourceName);
        }
        JarFile jarFile = jarFileCo.getJarFile();
        JarEntry jarEntry;
        try {
            jarEntry = jarFile.getJarEntry(entryName);
        } catch (IllegalStateException ex) {
            this.jarFileRef = null;
            jarFileRef.clear();
            jarFile = null;
            jarFileCo = null;
            if (isSecondTry) throw ex; else return openJarConnection(resourceName, resourceURL, testOnly, true);
        }
        if ((resourceName.length() == 0) || resourceName.equals("/")) {
            return testOnly ? DUMMY_URL_CONNECTION : createConnection(this.url);
        } else {
            boolean directory = resourceName.endsWith("/");
            if (directory) {
                if ((jarEntry != null) && !jarEntry.isDirectory()) return null;
            } else {
                if ((jarEntry == null) || jarEntry.isDirectory()) return null;
            }
            if ((resourceName.length() > 1) && resourceName.startsWith("/")) resourceName = resourceName.substring(1);
            return testOnly ? DUMMY_URL_CONNECTION : createConnection(new URL(this.url, resourceName));
        }
    }

    public URLResource getResource(String name) throws URLResourceException {
        URL resourceURL = null;
        try {
            resourceURL = new URL(this.url, name);
            URLConnection co = openConnection(name, resourceURL, true);
            return (co == null) ? null : new URLResource(name, this.url, resourceURL);
        } catch (IOException ex) {
            throw new URLResourceException(new URLResource(name, this.url, resourceURL), ex);
        }
    }

    /**
   * Returns the local file of the URL, or {@code null} if the protocol of the URL is not "file".
   */
    public final File getRepositoryFile() {
        return this.file;
    }

    public final URL getRepositoryURL() {
        return this.url;
    }

    public Iterator<URLResource> iterateAllLocationsOf(final String name) {
        final class IterateAllLocationsOf extends Object implements Iterator<URLResource> {

            private URLResourceException ex;

            private URLResource res;

            private boolean scanned = false;

            public boolean hasNext() {
                if (this.scanned) {
                    return (this.ex != null) || (this.res != null);
                } else {
                    this.scanned = true;
                    try {
                        this.res = getResource(name);
                        return this.res != null;
                    } catch (URLResourceException ex) {
                        this.ex = ex;
                        return true;
                    }
                }
            }

            public URLResource next() {
                if (!hasNext()) throw new NoSuchElementException();
                URLResourceException ex = this.ex;
                URLResource res = this.res;
                this.ex = null;
                this.res = null;
                if (ex == null) return res; else throw new IllegalStateException(ex);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        ;
        return new IterateAllLocationsOf();
    }

    public URLResourceConnection openResourceConnection(String name) throws URLResourceException {
        URL resourceURL = null;
        try {
            resourceURL = new URL(this.url, name);
            URLConnection co = openConnection(name, resourceURL, false);
            if (co == null) return null; else return new URLResourceConnection(new URLResource(name, this.url, resourceURL), co);
        } catch (IOException ex) {
            throw new URLResourceException(new URLResource(name, this.url, resourceURL), ex);
        }
    }

    private static final class DummyURLConnection extends URLConnection {

        private static URL createDummyURL() {
            try {
                return new URL("file:/dummy");
            } catch (MalformedURLException ex) {
                throw new AssertionError(ex);
            }
        }

        DummyURLConnection() {
            super(createDummyURL());
        }

        @Override
        public void connect() throws IOException {
            throw new ExternalAssertionException("this method should never be called - implementation error in URLRepositoryClassLoader or subclass");
        }
    }
}
