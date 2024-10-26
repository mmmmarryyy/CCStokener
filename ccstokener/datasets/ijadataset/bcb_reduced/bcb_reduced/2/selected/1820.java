package sun.misc;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.jar.JarFile;
import sun.net.www.ParseUtil;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.AccessControlException;
import java.security.Permission;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;

/**
 * This class is used to maintain a search path of URLs for loading classes
 * and resources from both JAR files and directories.
 *
 * @author  David Connelly
 * @version 1.62, 03/09/00
 */
public class URLClassPath {

    static final String USER_AGENT_JAVA_VERSION = "UA-Java-Version";

    static final String JAVA_VERSION;

    static {
        JAVA_VERSION = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("java.version"));
    }

    private ArrayList path = new ArrayList();

    private Stack urls = new Stack();

    private ArrayList loaders = new ArrayList();

    private HashMap lmap = new HashMap();

    private URLStreamHandler jarHandler;

    /**
     * Creates a new URLClassPath for the given URLs. The URLs will be
     * searched in the order specified for classes and resources. A URL
     * ending with a '/' is assumed to refer to a directory. Otherwise,
     * the URL is assumed to refer to a JAR file.
     *
     * @param urls the directory and JAR file URLs to search for classes
     *        and resources
     * @param factory the URLStreamHandlerFactory to use when creating new URLs
     */
    public URLClassPath(URL[] urls, URLStreamHandlerFactory factory) {
        for (int i = 0; i < urls.length; i++) {
            path.add(urls[i]);
        }
        push(urls);
        if (factory != null) {
            jarHandler = factory.createURLStreamHandler("jar");
        }
    }

    public URLClassPath(URL[] urls) {
        this(urls, null);
    }

    /**
     * Appends the specified URL to the search path of directory and JAR
     * file URLs from which to load classes and resources.
     */
    public void addURL(URL url) {
        synchronized (urls) {
            urls.add(0, url);
            path.add(url);
        }
    }

    /**
     * Returns the original search path of URLs.
     */
    public URL[] getURLs() {
        synchronized (urls) {
            return (URL[]) path.toArray(new URL[path.size()]);
        }
    }

    /**
     * Finds the resource with the specified name on the URL search path
     * or null if not found or security check fails.
     *
     * @param name 	the name of the resource
     * @param check     whether to perform a security check
     * @return a <code>URL</code> for the resource, or <code>null</code>
     * if the resource could not be found.
     */
    public URL findResource(String name, boolean check) {
        Loader loader;
        for (int i = 0; (loader = getLoader(i)) != null; i++) {
            URL url = loader.findResource(name, check);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * Finds the first Resource on the URL search path which has the specified
     * name. Returns null if no Resource could be found.
     *
     * @param name the name of the Resource
     * @param check 	whether to perform a security check
     * @return the Resource, or null if not found
     */
    public Resource getResource(String name, boolean check) {
        Loader loader;
        for (int i = 0; (loader = getLoader(i)) != null; i++) {
            Resource res = loader.getResource(name, check);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    /**
     * Finds all resources on the URL search path with the given name.
     * Returns an enumeration of the URL objects.
     *
     * @param name the resource name
     * @return an Enumeration of all the urls having the specified name
     */
    public Enumeration findResources(final String name, final boolean check) {
        return new Enumeration() {

            private int index = 0;

            private URL url = null;

            private boolean next() {
                if (url != null) {
                    return true;
                } else {
                    Loader loader;
                    while ((loader = getLoader(index++)) != null) {
                        url = loader.findResource(name, check);
                        if (url != null) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            public boolean hasMoreElements() {
                return next();
            }

            public Object nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                URL u = url;
                url = null;
                return u;
            }
        };
    }

    public Resource getResource(String name) {
        return getResource(name, true);
    }

    /**
     * Finds all resources on the URL search path with the given name.
     * Returns an enumeration of the Resource objects.
     *
     * @param name the resource name
     * @return an Enumeration of all the resources having the specified name
     */
    public Enumeration getResources(final String name, final boolean check) {
        return new Enumeration() {

            private int index = 0;

            private Resource res = null;

            private boolean next() {
                if (res != null) {
                    return true;
                } else {
                    Loader loader;
                    while ((loader = getLoader(index++)) != null) {
                        res = loader.getResource(name, check);
                        if (res != null) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            public boolean hasMoreElements() {
                return next();
            }

            public Object nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                Resource r = res;
                res = null;
                return r;
            }
        };
    }

    public Enumeration getResources(final String name) {
        return getResources(name, true);
    }

    private synchronized Loader getLoader(int index) {
        while (loaders.size() < index + 1) {
            URL url;
            synchronized (urls) {
                if (urls.empty()) {
                    return null;
                } else {
                    url = (URL) urls.pop();
                }
            }
            if (lmap.containsKey(url)) {
                continue;
            }
            Loader loader;
            try {
                loader = getLoader(url);
                URL[] urls = loader.getClassPath();
                if (urls != null) {
                    push(urls);
                }
            } catch (IOException e) {
                continue;
            }
            loaders.add(loader);
            lmap.put(url, loader);
        }
        return (Loader) loaders.get(index);
    }

    private Loader getLoader(final URL url) throws IOException {
        try {
            return (Loader) java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {

                public Object run() throws IOException {
                    String file = url.getFile();
                    if (file != null && file.endsWith("/")) {
                        if ("file".equals(url.getProtocol())) {
                            return new FileLoader(url);
                        } else {
                            return new Loader(url);
                        }
                    } else {
                        return new JarLoader(url, jarHandler, lmap);
                    }
                }
            });
        } catch (java.security.PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
    }

    private void push(URL[] us) {
        synchronized (urls) {
            for (int i = us.length - 1; i >= 0; --i) {
                urls.push(us[i]);
            }
        }
    }

    /**
     * Convert class path specification into an array of file URLs.
     *
     * The path of the file is encoded before conversion into URL
     * form so that reserved characters can safely appear in the path.
     */
    public static URL[] pathToURLs(String path) {
        StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
        URL[] urls = new URL[st.countTokens()];
        int count = 0;
        while (st.hasMoreTokens()) {
            File f = new File(st.nextToken());
            try {
                f = new File(f.getCanonicalPath());
            } catch (IOException x) {
            }
            try {
                urls[count++] = ParseUtil.fileToEncodedURL(f);
            } catch (IOException x) {
            }
        }
        if (urls.length != count) {
            URL[] tmp = new URL[count];
            System.arraycopy(urls, 0, tmp, 0, count);
            urls = tmp;
        }
        return urls;
    }

    public URL checkURL(URL url) {
        try {
            check(url);
        } catch (Exception e) {
            return null;
        }
        return url;
    }

    static void check(URL url) throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            URLConnection urlConnection = url.openConnection();
            Permission perm = urlConnection.getPermission();
            if (perm != null) {
                try {
                    security.checkPermission(perm);
                } catch (SecurityException se) {
                    if ((perm instanceof java.io.FilePermission) && perm.getActions().indexOf("read") != -1) {
                        security.checkRead(perm.getName());
                    } else if ((perm instanceof java.net.SocketPermission) && perm.getActions().indexOf("connect") != -1) {
                        URL locUrl = url;
                        if (urlConnection instanceof JarURLConnection) {
                            locUrl = ((JarURLConnection) urlConnection).getJarFileURL();
                        }
                        security.checkConnect(locUrl.getHost(), locUrl.getPort());
                    } else {
                        throw se;
                    }
                }
            }
        }
    }

    /**
     * Inner class used to represent a loader of resources and classes
     * from a base URL.
     */
    private static class Loader {

        private final URL base;

        Loader(URL url) {
            base = url;
        }

        URL getBaseURL() {
            return base;
        }

        URL findResource(final String name, boolean check) {
            URL url;
            try {
                url = new URL(base, name);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("name");
            }
            try {
                if (check) {
                    URLClassPath.check(url);
                }
                InputStream is = url.openStream();
                is.close();
                return url;
            } catch (Exception e) {
                return null;
            }
        }

        Resource getResource(final String name, boolean check) {
            final URL url;
            try {
                url = new URL(base, name);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("name");
            }
            final URLConnection uc;
            try {
                if (check) {
                    URLClassPath.check(url);
                }
                uc = url.openConnection();
                InputStream in = uc.getInputStream();
            } catch (Exception e) {
                return null;
            }
            return new Resource() {

                public String getName() {
                    return name;
                }

                public URL getURL() {
                    return url;
                }

                public URL getCodeSourceURL() {
                    return base;
                }

                public InputStream getInputStream() throws IOException {
                    return uc.getInputStream();
                }

                public int getContentLength() throws IOException {
                    return uc.getContentLength();
                }
            };
        }

        Resource getResource(final String name) {
            return getResource(name, true);
        }

        URL[] getClassPath() throws IOException {
            return null;
        }
    }

    private static class JarLoader extends Loader {

        private JarFile jar;

        private URL csu;

        private JarIndex index;

        private URLStreamHandler handler;

        private HashMap lmap;

        JarLoader(URL url, URLStreamHandler jarHandler, HashMap loaderMap) throws IOException {
            super(new URL("jar", "", -1, url + "!/", jarHandler));
            jar = getJarFile(url);
            index = JarIndex.getJarIndex(jar);
            csu = url;
            handler = jarHandler;
            lmap = loaderMap;
            if (index != null) {
                String[] jarfiles = index.getJarFiles();
                for (int i = 0; i < jarfiles.length; i++) {
                    try {
                        URL jarURL = new URL(csu, jarfiles[i]);
                        if (!lmap.containsKey(jarURL)) {
                            lmap.put(jarURL, null);
                        }
                    } catch (MalformedURLException e) {
                        continue;
                    }
                }
            }
        }

        private JarFile getJarFile(URL url) throws IOException {
            if ("file".equals(url.getProtocol())) {
                FileURLMapper p = new FileURLMapper(url);
                if (!p.exists()) {
                    throw new FileNotFoundException(p.getPath());
                }
                return new JarFile(p.getPath());
            }
            URLConnection uc = getBaseURL().openConnection();
            uc.setRequestProperty(USER_AGENT_JAVA_VERSION, JAVA_VERSION);
            return ((JarURLConnection) uc).getJarFile();
        }

        JarIndex getIndex() {
            return index;
        }

        Resource checkResource(final String name, boolean check, final JarEntry entry) {
            final URL url;
            try {
                url = new URL(getBaseURL(), name);
                if (check) {
                    URLClassPath.check(url);
                }
            } catch (MalformedURLException e) {
                return null;
            } catch (IOException e) {
                return null;
            } catch (AccessControlException e) {
                return null;
            }
            return new Resource() {

                public String getName() {
                    return name;
                }

                public URL getURL() {
                    return url;
                }

                public URL getCodeSourceURL() {
                    return csu;
                }

                public InputStream getInputStream() throws IOException {
                    return jar.getInputStream(entry);
                }

                public int getContentLength() {
                    return (int) entry.getSize();
                }

                public Manifest getManifest() throws IOException {
                    return jar.getManifest();
                }

                ;

                public Certificate[] getCertificates() {
                    return entry.getCertificates();
                }

                ;
            };
        }

        boolean validIndex(final String name) {
            String packageName = name;
            int pos;
            if ((pos = name.lastIndexOf("/")) != -1) {
                packageName = name.substring(0, pos);
            }
            String entryName;
            ZipEntry entry;
            Enumeration enum_ = jar.entries();
            while (enum_.hasMoreElements()) {
                entry = (ZipEntry) enum_.nextElement();
                entryName = entry.getName();
                if ((pos = entryName.lastIndexOf("/")) != -1) entryName = entryName.substring(0, pos);
                if (entryName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        URL findResource(final String name, boolean check) {
            Resource rsc = getResource(name, check);
            if (rsc != null) {
                return rsc.getURL();
            }
            return null;
        }

        Resource getResource(final String name, boolean check) {
            final JarEntry entry = jar.getJarEntry(name);
            if (entry != null) return checkResource(name, check, entry);
            if (index == null) return null;
            HashSet visited = new HashSet();
            return getResource(name, check, visited);
        }

        Resource getResource(final String name, boolean check, Set visited) {
            Resource res;
            Object[] jarFiles;
            int count = 0;
            LinkedList jarFilesList = null;
            if ((jarFilesList = index.get(name)) == null) return null;
            do {
                jarFiles = jarFilesList.toArray();
                int size = jarFilesList.size();
                while (count < size) {
                    String jarName = (String) jarFiles[count++];
                    JarLoader newLoader;
                    final URL url;
                    try {
                        url = new URL(csu, jarName);
                        if ((newLoader = (JarLoader) lmap.get(url)) == null) {
                            newLoader = (JarLoader) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                                public Object run() throws IOException {
                                    return new JarLoader(url, handler, lmap);
                                }
                            });
                            JarIndex newIndex = ((JarLoader) newLoader).getIndex();
                            if (newIndex != null) {
                                int pos = jarName.lastIndexOf("/");
                                newIndex.merge(this.index, (pos == -1 ? null : jarName.substring(0, pos + 1)));
                            }
                            lmap.put(url, newLoader);
                        }
                    } catch (java.security.PrivilegedActionException pae) {
                        continue;
                    } catch (MalformedURLException e) {
                        continue;
                    }
                    boolean visitedURL = !visited.add(url);
                    if (!visitedURL) {
                        final JarEntry entry = newLoader.jar.getJarEntry(name);
                        if (entry != null) {
                            return newLoader.checkResource(name, check, entry);
                        }
                        if (!newLoader.validIndex(name)) {
                            throw new InvalidJarIndexException("Invalid index");
                        }
                    }
                    if (visitedURL || newLoader == this || newLoader.getIndex() == null) {
                        continue;
                    }
                    if ((res = newLoader.getResource(name, check, visited)) != null) {
                        return res;
                    }
                }
                jarFilesList = index.get(name);
            } while (count < jarFilesList.size());
            return null;
        }

        URL[] getClassPath() throws IOException {
            if (index != null) {
                return null;
            }
            parseExtensionsDependencies();
            Manifest man = jar.getManifest();
            if (man != null) {
                Attributes attr = man.getMainAttributes();
                if (attr != null) {
                    String value = attr.getValue(Name.CLASS_PATH);
                    if (value != null) {
                        return parseClassPath(csu, value);
                    }
                }
            }
            return null;
        }

        private void parseExtensionsDependencies() throws IOException {
            ExtensionDependency.checkExtensionsDependencies(jar);
        }

        private URL[] parseClassPath(URL base, String value) throws MalformedURLException {
            StringTokenizer st = new StringTokenizer(value);
            URL[] urls = new URL[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                String path = st.nextToken();
                urls[i] = new URL(base, path);
                i++;
            }
            return urls;
        }
    }

    private static class FileLoader extends Loader {

        private File dir;

        FileLoader(URL url) throws IOException {
            super(url);
            if (!"file".equals(url.getProtocol())) {
                throw new IllegalArgumentException("url");
            }
            String path = url.getFile().replace('/', File.separatorChar);
            path = ParseUtil.decode(path);
            dir = new File(path);
        }

        URL findResource(final String name, boolean check) {
            Resource rsc = getResource(name, check);
            if (rsc != null) {
                return rsc.getURL();
            }
            return null;
        }

        Resource getResource(final String name, boolean check) {
            final URL url;
            try {
                URL normalizedBase = new URL(getBaseURL(), ".");
                url = new URL(getBaseURL(), name);
                if (url.getFile().startsWith(normalizedBase.getFile()) == false) {
                    return null;
                }
                if (check) URLClassPath.check(url);
                final File file = new File(dir, name.replace('/', File.separatorChar));
                if (file.exists()) {
                    return new Resource() {

                        public String getName() {
                            return name;
                        }

                        ;

                        public URL getURL() {
                            return url;
                        }

                        ;

                        public URL getCodeSourceURL() {
                            return getBaseURL();
                        }

                        ;

                        public InputStream getInputStream() throws IOException {
                            return new FileInputStream(file);
                        }

                        ;

                        public int getContentLength() throws IOException {
                            return (int) file.length();
                        }

                        ;
                    };
                }
            } catch (Exception e) {
                return null;
            }
            return null;
        }
    }
}
