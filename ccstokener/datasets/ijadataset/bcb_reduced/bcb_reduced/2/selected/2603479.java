package jbuzzer.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import jbuzzer.util.INameFilter;
import jbuzzer.util.StringUtil;
import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * <p>
 * A way to fool the {@link javax.swing.JFileChooser}. 
 * Never use it somewhere else, as a {@link File} is 
 * really different. 
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
 *
 */
public abstract class URLFile extends File {

    protected URL url;

    protected File parent;

    /**
     * Little factory method: 
     * For local url's (protocol "file") a real file is 
     * returned. For others an instance of this class.
     * 
     * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
     */
    public static File createFile(URL url) {
        File ret = null;
        String protocol = url.getProtocol().toLowerCase();
        if (protocol.equals("file")) {
            ret = new File(url.getPath());
        } else if (protocol.equals("http")) {
            ret = new HTTPFile(url, new FileFilterExtensions(new String[] { "wav", "mp3" }), new FileFilterExtensions(new String[] { "html", "htm", "jsp", "asp" }));
        } else {
            if (Debug.debug) {
                Debug.getInstance().error(URLFile.class.getName() + ".createFile(URL): unhandeled protocol: \"" + protocol + "\"");
            }
        }
        return ret;
    }

    /**
     * @param uri
     */
    private URLFile(URI uri) throws MalformedURLException {
        this(uri.toURL());
    }

    private URLFile(URL url) {
        super(url.getFile());
        this.url = url;
    }

    public boolean canRead() {
        boolean ret = false;
        try {
            this.url.openStream();
            ret = true;
        } catch (Exception ex) {
        }
        return ret;
    }

    public boolean canWrite() {
        return false;
    }

    public int compareTo(File pathname) {
        return this.url.getPath().compareTo(pathname.getAbsolutePath());
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean createNewFile() throws IOException {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean delete() {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return ;
     * </pre>
     */
    public void deleteOnExit() {
    }

    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof URLFile) {
            result = this.compareTo(((URLFile) obj)) == 0;
        }
        return result;
    }

    public boolean exists() {
        return this.canRead();
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return this;
     * </pre>
     */
    public File getAbsoluteFile() {
        return this;
    }

    public String getAbsolutePath() {
        return this.url.toExternalForm();
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return this;
     * </pre>
     */
    public File getCanonicalFile() throws IOException {
        return this;
    }

    public String getCanonicalPath() throws IOException {
        return this.url.toExternalForm();
    }

    public String getName() {
        return new File(this.url.getFile()).getName();
    }

    public String getParent() {
        return this.getParentFile().getAbsolutePath();
    }

    public File getParentFile() {
        return this.parent;
    }

    public String getPath() {
        return this.url.getPath();
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return 0;
     * </pre>
     */
    int getPrefixLength() {
        return 0;
    }

    public int hashCode() {
        return this.url.hashCode();
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return true;
     * </pre>
     */
    public boolean isAbsolute() {
        return true;
    }

    public boolean isDirectory() {
        return super.isDirectory();
    }

    public boolean isFile() {
        return !this.isDirectory();
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean isHidden() {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return 0;
     * </pre>
     */
    public long lastModified() {
        return 0;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return -1;
     * </pre>
     */
    public long length() {
        return -1;
    }

    public String[] list() {
        return this.list(new FilenameFilter() {

            public boolean accept(File f, String s) {
                return true;
            }
        });
    }

    public String[] list(FilenameFilter filter) {
        File[] files = this.listFiles(filter);
        String[] ret = new String[files.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = files[i].getAbsolutePath();
        }
        return ret;
    }

    public File[] listFiles() {
        return this.listFiles(new FilenameFilter() {

            public boolean accept(File f, String s) {
                return true;
            }
        });
    }

    public File[] listFiles(final FileFilter filter) {
        return this.listFiles(new FilenameFilter() {

            FileFilter fita = filter;

            public boolean accept(File f, String s) {
                return fita.accept(f);
            }
        });
    }

    public File[] listFiles(FilenameFilter filter) {
        throw new IllegalStateException("This method has to be overloaded by subclasses!!!");
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean mkdir() {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean mkdirs() {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean renameTo(File dest) {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean setLastModified(long time) {
        return false;
    }

    /**
     * Don't use. This class is a hack for one purpose. See 
     * above.
     * <pre>
     *  return false;
     * </pre>
     */
    public boolean setReadOnly() {
        return false;
    }

    public String toString() {
        return this.url.toExternalForm();
    }

    public URI toURI() {
        URI ret = null;
        try {
            ret = new URI(this.url.toExternalForm());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public URL toURL() throws MalformedURLException {
        return this.url;
    }

    /**
     * <p>
     * A fake file: Whenever the constructor given 
     * search FileFilter returns true for it's internal URL, 
     * it's method {@link #isFile()} will return false but 
     * the listXXX methods will use it for searching it for URL strings. 
     * </P>
     * <p>
     * Whenever the constructor given isFile FileFilter accepts, 
     * it's method {@link #isFile()} returns true and it's content 
     * will not be parsed to search for URL strings.
     * </p>
     * <p>
     * If both FileFilters do not accept it, the url will be ignored.
     * </p>
     * 
     * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
     *
     */
    private static class HTTPFile extends URLFile {

        private INameFilter fileDetector;

        private INameFilter searchFilter;

        private static Set visited = new TreeSet();

        private List children = new LinkedList();

        protected int hops = 2;

        private HTTPFile(URL url, INameFilter isFile, INameFilter searchFilter) {
            super(url);
            this.fileDetector = isFile;
            this.searchFilter = searchFilter;
            if (this.isDirectory()) {
                if (!this.url.getPath().endsWith("/")) {
                    try {
                        this.url = new URL(this.url.toExternalForm() + '/');
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (this.parent == null) {
                    String root = this.url.toExternalForm();
                    root = root.substring(0, root.lastIndexOf('/') + 1);
                    try {
                        this.parent = createFile(new URL(root));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public boolean isDirectory() {
            boolean ret = false;
            String urlstring = this.url.toExternalForm();
            int lastDot = urlstring.lastIndexOf('.');
            int lastSeparator = urlstring.lastIndexOf('/');
            if (lastSeparator == -1) {
                if (lastDot == -1) {
                    ret = true;
                } else {
                    ret = false;
                }
            } else {
                if (lastDot == -1) {
                    ret = true;
                } else if (lastDot > (lastSeparator + 1)) {
                    ret = false;
                } else {
                    ret = true;
                }
            }
            if (Debug.debug) {
                Debug.getInstance().info(this.getClass().getName() + ".isDirectory(" + urlstring + ") : " + ret);
            }
            return ret;
        }

        public boolean isFile() {
            return !this.isDirectory();
        }

        public File[] listFiles(FilenameFilter filter) {
            this.parseUrl();
            File[] ret = new File[this.children.size()];
            ret = (File[]) this.children.toArray(ret);
            return ret;
        }

        private void parseUrl() {
            URLLexer lexer;
            URLParser parser;
            if (this.parent != null) {
                this.hops = ((HTTPFile) this.parent).hops - 1;
            }
            if (this.searchFilter.accept(this.url.getPath())) {
                if (!visited.contains(this.url.toExternalForm())) {
                    if (this.hops > 0) {
                        try {
                            visited.add(this.url.toExternalForm());
                            InputStream in = this.url.openStream();
                            lexer = new URLLexer(this.url.openStream());
                            parser = new URLParser(lexer);
                            URL[] urls = parser.htmlDocument(this.url);
                            if (Debug.debug) {
                                Debug.getInstance().info(this.getClass().getName() + ".parseUrl(): Found the following URLs in " + this.url.toExternalForm() + " : " + StringUtil.ArrayToString(urls, 10));
                            }
                            for (int i = 0; i < urls.length; i++) {
                                this.addInternal(urls[i]);
                            }
                        } catch (IOException e) {
                            if (Debug.debug) {
                                Debug.getInstance().error(e.getMessage());
                            }
                        } catch (RecognitionException e) {
                            if (Debug.debug) {
                                Debug.getInstance().error("Problems while lexing " + this.url.toExternalForm() + " : " + e.getMessage(), e);
                            }
                        } catch (TokenStreamException e) {
                            if (Debug.debug) {
                                Debug.getInstance().error("Problems while parsing " + this.url.toExternalForm() + " : " + e.getMessage(), e);
                            }
                        }
                    }
                } else {
                    if (Debug.debug) {
                        Debug.getInstance().info(this.getClass().getName() + ".parseUrl(): Skipping URL " + this.url.toExternalForm() + " : Maximum depth reached.");
                    }
                }
            } else {
                if (Debug.debug) {
                    Debug.getInstance().info(this.getClass().getName() + ".parseUrl(): Skipping URL " + this.url.toExternalForm() + " : Already parsed.");
                }
            }
        }

        private boolean addInternal(URL add) {
            if (!this.url.getHost().equalsIgnoreCase(add.getHost())) {
                if (Debug.debug) {
                    Debug.getInstance().info(this.getClass().getName() + ".addInternal(URL): Dropping: " + add.toExternalForm() + ". Foreign host.");
                }
                return false;
            }
            StringTokenizer own = new StringTokenizer(this.url.getFile(), "/");
            StringTokenizer him = new StringTokenizer(add.getFile(), "/");
            int ownTokens = own.countTokens();
            int himTokens = him.countTokens();
            if (himTokens < ownTokens) {
                if (this.parent == null) {
                    if (Debug.debug) {
                        Debug.getInstance().info(this.getClass().getName() + ".addInternal(URL): Dropping: " + add.toExternalForm() + ": Above initial root: " + this.url.toExternalForm());
                    }
                    return false;
                } else {
                    return ((HTTPFile) this.parent).addInternal(add);
                }
            } else if (himTokens >= ownTokens) {
                if (add.equals(this.url)) {
                    if (Debug.debug) {
                        Debug.getInstance().info(this.getClass().getName() + ".addInternal(URL): " + this.url.toExternalForm() + " already contained!");
                    }
                    return false;
                }
                StringBuffer path = new StringBuffer(this.url.getProtocol());
                path.append("://").append(this.url.getHost());
                String ownTokenString, himTokenString;
                while (own.hasMoreTokens()) {
                    ownTokenString = own.nextToken();
                    himTokenString = him.nextToken();
                    path.append('/');
                    if (himTokenString.equalsIgnoreCase(ownTokenString)) {
                        path.append(ownTokenString);
                        continue;
                    } else {
                        if (this.parent == null) {
                            if (Debug.debug) {
                                Debug.getInstance().info(this.getClass().getName() + ".addInternal(URL): Dropping: " + add.toExternalForm() + ": At unreachable level : " + this.url.toExternalForm());
                            }
                        } else {
                            return ((HTTPFile) this.parent).addInternal(add);
                        }
                    }
                }
                if (!own.hasMoreTokens() && him.hasMoreTokens()) {
                    path.append('/').append(him.nextToken());
                    if (him.hasMoreTokens()) {
                        path.append('/');
                    }
                    URL childURL;
                    try {
                        childURL = new URL(path.toString());
                        HTTPFile child = (HTTPFile) createFile(childURL);
                        if (!this.children.contains(child)) {
                            if (Debug.debug) {
                                Debug.getInstance().info("Adding " + path + " as a child of " + this.url.toExternalForm());
                            }
                            this.children.add(child);
                            child.parent = this;
                            child.parseUrl();
                            if (him.hasMoreTokens()) {
                                return child.addInternal(add);
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    } catch (MalformedURLException e) {
                        if (Debug.debug) {
                            Debug.getInstance().error(this.getClass().getName() + ".addInternal(URL): Tried to add: " + path + ": Malformed url.", e);
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        public boolean equals(Object obj) {
            if (obj instanceof URL) {
                return (this.url.toExternalForm().equals(((URL) obj).toExternalForm()));
            }
            if (obj instanceof URLFile) {
                return (this.url.toExternalForm().equals(((URLFile) obj).url.toExternalForm()));
            }
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            URL url = new URL("http://jbuzzer.sourceforge.net/lib/");
            File file = URLFile.createFile(url);
            printListRecursive(file, 0);
        } catch (Throwable f) {
            f.printStackTrace(System.err);
        }
    }

    private static void printListRecursive(File f, int level) {
        StringBuffer spaceBuffer = new StringBuffer();
        for (int i = level; i >= 0; i--) {
            spaceBuffer.append(" ");
        }
        String indent = spaceBuffer.toString();
        System.out.println(indent + f.getAbsolutePath());
        File[] children = f.listFiles();
        if (children != null) {
            for (int j = 0; j < children.length; j++) {
                printListRecursive(children[j], level + 1);
            }
        }
    }
}
