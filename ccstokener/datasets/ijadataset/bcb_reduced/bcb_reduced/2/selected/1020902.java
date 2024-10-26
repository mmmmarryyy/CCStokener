package genj.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An origin describes where a resource came from. This is normally
 * a URL that points to that resource. Other resources can be 
 * loaded relative to the initial origin. Supported origins are
 * file-system (file://) and remote http resource (http://).
 * A resource can be comprised of an archive (jar or zip) - the
 * URL then has to include an anchor that identifies the file
 * in the archive serving as the origin. 
 * Other resources can be opened relative to an origin - that's 
 * either relative to the same 'directory' of the original or
 * pulled from the same archive.
 */
public abstract class Origin {

    private static final Logger LOG = Logger.getLogger("genj.util");

    /** chars we need */
    private static final char BSLASH = '\\', FSLASH = '/';

    /** the url that the origin is based on */
    protected URL url;

    protected long lastModified;

    /**
   * Constructor
   */
    protected Origin(URL url) {
        this.url = url;
    }

    /**
   * Factory method to create an instance for given location string
   * @param s url as string
   */
    public static Origin create(String s) throws MalformedURLException {
        return create(new URL(s));
    }

    public static Origin create(String s, URLStreamHandler handler) throws MalformedURLException {
        return create(new URL(null, s, handler));
    }

    /**
   * Factory method to create an instance for given url
   * @param url either http://host/dir/file or ftp://dir/file or
   *   protocol://[host/]dir/file.zip#file 
   */
    public static Origin create(URL url) {
        if (url.getFile().endsWith(".zip")) {
            return new ZipOrigin(url);
        } else {
            return new DefaultOrigin(url);
        }
    }

    /**
   * Open this origin for input
   * @return stream to read from
   */
    public abstract InputStream open() throws IOException;

    /**
   * Open resource relative to this origin
   * @param name the name of resource to open - either relative "./somethingelse"
   *  or absolute "file://dir/somethingelse"
   */
    public final InputStream open(String name) throws IOException {
        name = back2forwardslash(name);
        if (ABSOLUTE.matcher(name).matches()) {
            LOG.fine("Trying to open " + name + " as absolute path (origin is " + this + ")");
            URLConnection uc;
            try {
                uc = new URL(name).openConnection();
                lastModified = uc.getLastModified();
            } catch (MalformedURLException e1) {
                try {
                    uc = new URL("file:" + name).openConnection();
                    lastModified = uc.getLastModified();
                } catch (MalformedURLException e2) {
                    return null;
                }
            }
            return new InputStreamImpl(uc.getInputStream(), uc.getContentLength());
        }
        LOG.fine("Trying to open " + name + " as relative path (origin is " + this + ")");
        return openImpl(name);
    }

    /**
   * Open file relative to origin
   */
    protected abstract InputStream openImpl(String name) throws IOException;

    /**
   * String representation
   */
    public String toString() {
        return url.toString();
    }

    public long getLastModified() {
        return lastModified;
    }

    /**
   * Tries to calculate a relative path for given file
   * @param file the file that might be relative to this origin
   * @return relative path or null if not applicable
   */
    private static final Pattern ABSOLUTE = Pattern.compile("([a-z]:).*|([A-Z]:).*|\\/.*|\\\\.*");

    public String calcRelativeLocation(String file) {
        String here = url.toString();
        if (here.startsWith("file://")) here = here.substring("file:/".length()); else if (here.startsWith("file:")) here = here.substring("file:".length());
        if (!ABSOLUTE.matcher(file).matches()) return null;
        try {
            here = back2forwardslash(new File(here.substring(0, here.lastIndexOf(FSLASH))).getCanonicalPath()) + "/";
            file = back2forwardslash(new File(file).getCanonicalPath());
            boolean startsWith = file.startsWith(here);
            LOG.fine("File " + file + " is " + (startsWith ? "" : "not ") + "relative to " + here);
            if (startsWith) return file.substring(here.length());
        } catch (Throwable t) {
        }
        return null;
    }

    /**
   * Lists the files available at this origin if that information is available
   */
    public abstract String[] list() throws IOException;

    /**
   * Returns the Origin as a File or null if no local gedcom file can be named
   */
    public abstract File getFile();

    /**
   * Returns an absolute file representation of a resource relative 
   * to this origin
   * @exception IllegalArgumentException if not applicable (e.g. for origin http://host/dir/file)
   */
    public abstract File getFile(String name);

    /**
   * Returns the Origin's filename. For example
   * <pre>
   *  file://d:/gedcom/[example.ged]
   *  http://host/dir/[example.ged]
   *  http://host/dir/archive.zip#[example.ged]
   * </pre>
   */
    public String getFileName() {
        return getName();
    }

    /**
   * Returns the origin's distinctive name. For example
   * <pre>
   *  file://d:/gedcom/[example.ged]
   *  http://host/dir/[example.ged]
   *  http://host/dir/[archive.zip#example.ged]
   * </pre>
   */
    public String getName() {
        String path = back2forwardslash(url.toString());
        if (path.endsWith("" + FSLASH)) path = path.substring(0, path.length() - 1);
        return path.substring(path.lastIndexOf(FSLASH) + 1);
    }

    /**
   * Object Comparison
   */
    public boolean equals(Object other) {
        return other instanceof Origin && ((Origin) other).url.toString().equals(url.toString());
    }

    /**
   * Object hash
   */
    public int hashCode() {
        return url.toString().hashCode();
    }

    /**
   * Returns a cleaned up string with forward instead
   * of backwards slash(e)s
   */
    protected String back2forwardslash(String s) {
        return s.toString().replace(BSLASH, FSLASH);
    }

    /**
   * A default origin 
   */
    private static class DefaultOrigin extends Origin {

        /**
     * Constructor
     */
        protected DefaultOrigin(URL url) {
            super(url);
        }

        /**
     * @see genj.util.Origin#open()
     */
        public InputStream open() throws IOException {
            URLConnection uc = url.openConnection();
            lastModified = uc.getLastModified();
            return new InputStreamImpl(uc.getInputStream(), uc.getContentLength());
        }

        /**
     * @see genj.util.Origin#openImpl(java.lang.String)
     */
        protected InputStream openImpl(String name) throws IOException {
            String path = back2forwardslash(url.toString());
            path = path.substring(0, path.lastIndexOf(FSLASH) + 1) + name;
            try {
                URLConnection uc = new URL(path).openConnection();
                lastModified = uc.getLastModified();
                return new InputStreamImpl(uc.getInputStream(), uc.getContentLength());
            } catch (MalformedURLException e) {
                throw new IOException(e.getMessage());
            }
        }

        /**
     * list directory of origin if file
     */
        public String[] list() {
            File dir = getFile();
            if (dir == null) throw new IllegalArgumentException("list() not supported by url protocol");
            if (!dir.isDirectory()) dir = dir.getParentFile();
            return dir.list();
        }

        /**
     * @see genj.util.Origin#getFile()
     */
        public File getFile() {
            return "file".equals(url.getProtocol()) ? new File(url.getFile()) : null;
        }

        /**
     * @see genj.util.Origin#getFile(java.lang.String)
     */
        public File getFile(String file) {
            if (file.length() < 1) return null;
            if (ABSOLUTE.matcher(file).matches()) return new File(file);
            return new File(getFile().getParent(), file);
        }
    }

    /**
   * Class which stands for an origin of a resource - this Origin
   * is pointing to a ZIP file so all relative files are read
   * from the same archive
   */
    private static class ZipOrigin extends Origin {

        /** cached bytes */
        private byte[] cachedBits;

        /**
     * Constructor
     */
        protected ZipOrigin(URL url) {
            super(url);
        }

        /**
     * list directory of origin if file
     */
        public String[] list() throws IOException {
            ArrayList<String> result = new ArrayList<String>();
            ZipInputStream in = openImpl();
            while (true) {
                ZipEntry entry = in.getNextEntry();
                if (entry == null) break;
                result.add(entry.getName());
            }
            in.close();
            return (String[]) result.toArray(new String[result.size()]);
        }

        /**
     * @see genj.util.Origin#open()
     */
        public InputStream open() throws IOException {
            String anchor = url.getRef();
            if ((anchor == null) || (anchor.length() == 0)) {
                throw new IOException("ZipOrigin needs anchor for open()");
            }
            return openImpl(anchor);
        }

        /**
     * open the zip input stream
     */
        private ZipInputStream openImpl() throws IOException {
            if (cachedBits == null) try {
                URLConnection uc = url.openConnection();
                lastModified = uc.getLastModified();
                cachedBits = new ByteArray(uc.getInputStream(), true).getBytes();
            } catch (InterruptedException e) {
                throw new IOException("interrupted while opening " + getName());
            }
            return new ZipInputStream(new ByteArrayInputStream(cachedBits));
        }

        /**
     * @see genj.util.Origin#openImpl(java.lang.String)
     */
        protected InputStream openImpl(String file) throws IOException {
            ZipInputStream zin = openImpl();
            for (ZipEntry zentry = zin.getNextEntry(); zentry != null; zentry = zin.getNextEntry()) {
                if (zentry.getName().equals(file)) return new InputStreamImpl(zin, (int) zentry.getSize());
            }
            throw new IOException("Couldn't find resource " + file + " in ZIP-file");
        }

        /**
     * @see genj.util.Origin#getFile()
     */
        public File getFile() {
            return null;
        }

        /**
     * Returns the Origin's Filename file://d:/gedcom/example.zip#[example.ged]
     * @see genj.util.Origin#getFileName()
     */
        public String getFileName() {
            return url.getRef();
        }

        /**
     * File not available
     * @see genj.util.Origin#getFile(java.lang.String)
     */
        public File getFile(String name) {
            return null;
        }
    }

    /**
   * An InputStream returned from Origin
   */
    private static class InputStreamImpl extends InputStream {

        /** wrapped input stream */
        private InputStream in;

        /** length of data */
        private int len;

        /**
     * Constructor
     */
        protected InputStreamImpl(InputStream in, int len) {
            this.in = in;
            this.len = len;
        }

        /**
     * @see java.io.InputStream#read()
     */
        public int read() throws IOException {
            return in.read();
        }

        /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
        public int read(byte[] b, int off, int len) throws IOException {
            return in.read(b, off, len);
        }

        /**
     * @see java.io.InputStream#available()
     */
        public int available() throws IOException {
            return len;
        }

        /**
     * 20040220 have to delegate close() to 'in' to make
     * sure the input is closed right (file open problems)
     * @see java.io.InputStream#close()
     */
        public void close() throws IOException {
            in.close();
        }
    }
}
