package org.jtools.util.props;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

/**
 * TODO type-description
 * @author <a href="mailto:rainer.noack@jtools.org">Rainer Noack</a>
 */
public class SimpleProperties extends Properties {

    private static final class ArrayEnumerator implements Enumeration<Object> {

        private final Object[] array;

        private int nextIndex = 0;

        public ArrayEnumerator(Object[] array) {
            this.array = array;
        }

        public boolean hasMoreElements() {
            if (array == null) return false;
            if (nextIndex >= array.length) return false;
            return true;
        }

        public Object nextElement() {
            if (!hasMoreElements()) throw new NoSuchElementException();
            return array[nextIndex++];
        }
    }

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256719572286713904L;

    private transient String filename = null;

    private transient boolean inSortedStore = false;

    private transient String loadPath = null;

    private final SimplePropertyStyle style = new SimplePropertyStyle();

    public SimpleProperties() {
        this(null, SystemPropertyMode.IGNORE);
    }

    public SimpleProperties(Properties defaults) {
        this(defaults, SystemPropertyMode.IGNORE);
    }

    public SimpleProperties(Properties defaults, SystemPropertyMode systemPropertyMode) {
        this(defaults, systemPropertyMode, null);
    }

    public SimpleProperties(Properties defaults, SystemPropertyMode systemPropertyMode, String systemPropertyPrefix) {
        super(defaults);
        if (SystemPropertyMode.LOAD == systemPropertyMode) putAll(System.getProperties(), true, systemPropertyPrefix); else if (SystemPropertyMode.SET == systemPropertyMode) {
            putAll(System.getProperties(), true, null);
            System.setProperties(this);
        }
    }

    protected SimpleProperties(SimpleProperties props) {
        this((props == null) ? (Properties) null : props.getDefaults());
        if (props != null) {
            setStyle(props.getInternalStyle());
            loadPath = props.getLoadPath();
            filename = props.getFilename();
            putAll(props);
        }
    }

    public SimpleProperties(String systemPropertyPrefix) {
        this(null, (systemPropertyPrefix == null) ? SystemPropertyMode.IGNORE : SystemPropertyMode.LOAD, systemPropertyPrefix);
    }

    public SimpleProperties(SystemPropertyMode systemPropertyMode) {
        this(null, systemPropertyMode, null);
    }

    @Override()
    public Object clone() {
        return new SimpleProperties(this);
    }

    protected final URL getAsUrl(String fileOrUrl) throws IOException {
        fileOrUrl = fileOrUrl.trim().replace('\\', '/');
        if ("".equals(fileOrUrl)) return null;
        URL url = null;
        try {
            url = new URL(fileOrUrl);
        } catch (MalformedURLException e) {
            url = new File(fileOrUrl).getCanonicalFile().toURI().toURL();
        }
        return url;
    }

    public final Properties getDefaults() {
        return defaults;
    }

    public final String getFilename() {
        return filename;
    }

    public final String getFilenameProperty() {
        return style.getFilenameProperty();
    }

    protected final SimplePropertyStyle getInternalStyle() {
        return style;
    }

    public final String getLoadPath() {
        return loadPath;
    }

    public final PropertyStyle getStyle() {
        return (PropertyStyle) style.clone();
    }

    protected final void initFromParent(Properties parent) {
        if ((parent != null) && (parent instanceof SimpleProperties)) setStyle(((SimpleProperties) parent).getInternalStyle());
    }

    @Override()
    public synchronized Enumeration<Object> keys() {
        if (this.inSortedStore) {
            Set<Object> set = keySet();
            Object[] array = set.toArray(new Object[set.size()]);
            Arrays.sort(array);
            return new ArrayEnumerator(array);
        }
        return super.keys();
    }

    @Override
    public final void list(PrintStream out) {
        list(out, null, null);
    }

    public final void list(PrintStream out, String prefix, String filter) {
        if (prefix == null) prefix = "";
        Object[] keyArray = keySet().toArray();
        Arrays.sort(keyArray, 0, keyArray.length);
        for (int i = 0; i < keyArray.length; i++) {
            if ((filter == null) || ((String) keyArray[i]).startsWith(filter)) out.println(prefix + (String) keyArray[i] + "='" + getProperty((String) keyArray[i]) + "'");
        }
    }

    @Override
    public final void list(PrintWriter out) {
        list(out, null, null);
    }

    /**
     * list Method
     */
    public final void list(PrintWriter out, String prefix, String filter) {
        if (prefix == null) prefix = "";
        Object[] a = keySet().toArray();
        Arrays.sort(a, 0, a.length);
        for (int i = 0; i < a.length; i++) {
            if ((filter == null) || ((String) a[i]).startsWith(filter)) out.println(prefix + (String) a[i] + "='" + getProperty((String) a[i]) + "'");
        }
    }

    public final LoadReturnCode load(File file) throws IOException {
        return load((Properties) null, file);
    }

    @Override
    public final void load(InputStream inStream) throws IOException {
        load((Properties) null, inStream);
    }

    public final LoadReturnCode load(InputStream inStream, String filename) throws IOException {
        return load((Properties) null, inStream, filename);
    }

    public final LoadReturnCode load(Properties overwrites) throws IOException {
        initFromParent(overwrites);
        return loadInternal(overwrites);
    }

    public final LoadReturnCode load(Properties overwrites, File file) throws IOException {
        initFromParent(overwrites);
        if (file == null) return loadInternal(overwrites);
        return loadInternal(overwrites, file.toURI().toURL());
    }

    public final LoadReturnCode load(Properties Overwrites, InputStream inStream) throws IOException {
        return load(Overwrites, inStream, null);
    }

    public final LoadReturnCode load(Properties Overwrites, InputStream inStream, String filename) throws IOException {
        initFromParent(Overwrites);
        if ((inStream == null) && (filename == null)) return loadInternal(Overwrites); else if (inStream == null) return loadInternal(Overwrites, filename); else if (filename == null) return loadInternal(Overwrites, filename); else return loadInternal(Overwrites, inStream, filename);
    }

    public final LoadReturnCode load(Properties overwrites, String fileOrUrl) throws IOException {
        initFromParent(overwrites);
        if (fileOrUrl == null) return loadInternal(overwrites);
        return loadInternal(overwrites, fileOrUrl);
    }

    public final LoadReturnCode load(Properties overwrites, URL url) throws IOException {
        initFromParent(overwrites);
        if (url == null) return loadInternal(overwrites);
        return loadInternal(overwrites, url);
    }

    public final LoadReturnCode load(String fileOrUrl) throws IOException {
        return load((Properties) null, fileOrUrl);
    }

    public final LoadReturnCode load(URL url) throws IOException {
        return load((Properties) null, url);
    }

    protected final LoadReturnCode loadInternal(Properties overwrites) throws IOException {
        String file = null;
        if (overwrites != null) file = overwrites.getProperty(getFilenameProperty(), null);
        if (file == null) return loadInternal(overwrites, null, null);
        return loadInternal(overwrites, file);
    }

    protected final LoadReturnCode loadInternal(Properties overwrites, InputStream inStream, String filename) throws IOException {
        if (filename != null) {
            filename = filename.trim().replace('\\', '/');
            if ("".equals(filename)) filename = null;
        }
        if (filename == null) {
            this.filename = null;
            this.loadPath = null;
        } else {
            this.filename = filename;
            int p = filename.lastIndexOf('/');
            if (p < 0) this.loadPath = null; else this.loadPath = filename.substring(0, p);
        }
        return onLoad(overwrites, inStream);
    }

    protected final LoadReturnCode loadInternal(Properties overwrites, String fileOrUrl) throws IOException {
        URL url = getAsUrl(fileOrUrl);
        if (url == null) return loadInternal(overwrites, null, null);
        return loadInternal(overwrites, url);
    }

    protected final LoadReturnCode loadInternal(Properties overwrites, URL url) throws IOException {
        InputStream inStream = url.openStream();
        try {
            return loadInternal(overwrites, inStream, url.toString());
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                    inStream = null;
                }
            } catch (IOException ioex) {
            }
        }
    }

    /**
     * to overwrite in derived classes.
     */
    protected LoadReturnCode onLoad(Properties overwrites, InputStream inStream) throws IOException {
        if (inStream != null) originalLoad(inStream);
        if (overwrites != null) putAll(overwrites);
        return LoadReturnCode.OK;
    }

    protected final void originalLoad(InputStream inStream) throws IOException {
        super.load(inStream);
    }

    public final void putAll(Properties m, boolean overwrite) {
        if (overwrite) putAll(m); else {
            for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry k = (Map.Entry) i.next();
                if (!containsKey(k.getKey())) put(k.getKey(), k.getValue());
            }
        }
    }

    public final void putAll(Properties m, boolean overwrite, String prefix) {
        if ((prefix == null) || "".equals(prefix)) putAll(m, overwrite);
        if (overwrite) {
            for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry k = (Map.Entry) i.next();
                put(prefix + k.getKey(), k.getValue());
            }
        } else {
            for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry k = (Map.Entry) i.next();
                if (!containsKey(prefix + k.getKey())) put(prefix + k.getKey(), k.getValue());
            }
        }
    }

    public final void putAll(Properties m, String prefix) {
        putAll(m, true, prefix);
    }

    protected final void setFilename(String filename) {
        this.filename = filename;
    }

    public final void setFilenameProperty(String prop) {
        style.setFilenameProperty(prop);
    }

    protected final void setLoadPath(String loadPath) {
        this.loadPath = loadPath;
    }

    public final void setStyle(PropertyStyle style) {
        this.style.updateWith(style);
    }

    public synchronized void storeSorted(OutputStream out, String comments) throws IOException {
        boolean oldSortedStore = inSortedStore;
        inSortedStore = true;
        super.store(out, comments);
        inSortedStore = oldSortedStore;
    }
}
