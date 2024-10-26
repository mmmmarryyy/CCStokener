package net.sourceforge.javautil.classloader.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.sourceforge.javautil.classloader.impl.ClassInfo;
import net.sourceforge.javautil.classloader.impl.ClassSearchInfo;
import net.sourceforge.javautil.classloader.impl.PackageSearchInfo;
import net.sourceforge.javautil.common.ClassNameUtil;
import net.sourceforge.javautil.common.FileUtil;
import net.sourceforge.javautil.common.IOUtil;
import net.sourceforge.javautil.common.Refreshable;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualArtifact;
import net.sourceforge.javautil.common.io.impl.SystemFile;

/**
 * A class source wrapping a zipped/jarred archive.
 * 
 * @author ponder
 * @author $Author: ponderator $
 * @version $Id: ZipClassSource.java 2678 2010-12-24 04:18:29Z ponderator $
 */
public class ZipClassSource extends ClassSource implements Refreshable {

    /**
	 * If the url points is of protocol type jar:/ or file:/ it will be translated into a {@link File}
	 * instance, otherwise a temporary file will be created using the input stream of the URL and a
	 * reference to the temporary file will be returned.
	 * 
	 * @param url A url to translate or transmit to a file
	 * @return
	 */
    public static File toFile(URL url) {
        try {
            if (url.getProtocol().equals("jar") || url.getProtocol().equals("file")) {
                String path = url.getPath();
                if (path.startsWith("file://")) path = path.substring(7);
                path = URLDecoder.decode(path, "UTF-8");
                return new File(path);
            } else {
                File tmp = File.createTempFile("tmpZip", ".zip");
                IOUtil.transfer(url.openStream(), new FileOutputStream(tmp));
                return tmp;
            }
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    protected boolean searchedAll = false;

    protected int searched = 0;

    protected ZipFile zipFile;

    protected URL url;

    protected File file;

    protected boolean hasDirectories = false;

    protected List<ZipEntry> entries = null;

    protected List<String> packages = new ArrayList<String>();

    protected long loaded;

    protected ZipStreamHandler handler = new ZipStreamHandler();

    /**
	 * @param url Uses {@link #toFile(URL)} in order to pass the file onto {@link #ZipClassSource(File)}
	 */
    public ZipClassSource(URL url) {
        this(toFile(url));
    }

    /**
	 * @param file The java archive
	 */
    public ZipClassSource(File file) {
        super(file.getPath());
        try {
            this.file = file;
            this.url = file.toURI().toURL();
            this.zipFile = new ZipFile(file);
            this.loaded = System.currentTimeMillis();
        } catch (ZipException e) {
            throw ThrowableManagerRegistry.caught(new RuntimeException(url.toExternalForm(), e));
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(new RuntimeException(url.toExternalForm(), e));
        }
    }

    @Override
    public IVirtualArtifact getVirtualArtifact() {
        return new SystemFile(file);
    }

    @Override
    public ClassInfo getClassInfo(ClassSearchInfo info) throws ClassNotFoundException {
        if (this.hasClass(info)) return new ClassInfo(info, this);
        throw new ClassNotFoundException(info.getFullClassName());
    }

    @Override
    public boolean hasClass(ClassSearchInfo info) {
        return this.getEntry(info.getClassPath()) != null;
    }

    @Override
    public synchronized boolean hasPackage(PackageSearchInfo info) {
        String path = info.getPackagePath();
        if (this.hasDirectories) return this.getEntry(path) != null; else if (!packages.contains(info.getPackageName()) && !searchedAll) {
            int count = 0;
            List<ZipEntry> entries = this.getEntries();
            int size = entries.size();
            for (int e = 0; e < size; e++) {
                ZipEntry entry = entries.get(e);
                if (++count <= searched) continue;
                if (!this.hasDirectories && entry.isDirectory()) this.hasDirectories = true;
                if (this.hasDirectories && entry.isDirectory()) {
                    packages.add(ClassNameUtil.toPackageName(entry.getName()));
                    if (entry.getName().equals(path)) {
                        searched = -1;
                        break;
                    }
                } else if (ClassNameUtil.isClassSource(entry.getName())) {
                    String pkg = ClassNameUtil.getPackageNameFromPath(entry.getName(), '/');
                    packages.add(pkg);
                    if (info.getPackageName().equals(pkg)) {
                        searched = -1;
                        break;
                    }
                }
            }
            if (searched == -1) searched = count; else searchedAll = true;
        }
        return packages.contains(info.getPackageName());
    }

    @Override
    public synchronized boolean hasParentPackage(String packageName) {
        return this.hasPackage(new PackageSearchInfo(packageName));
    }

    @Override
    public Collection<String> getPackages() {
        return packages;
    }

    @Override
    public URL getResource(String resourceName) {
        ZipEntry entry = this.getEntry(resourceName);
        if (this.hasResource(resourceName)) {
            String path = entry.getName();
            if (!path.startsWith("/")) path = "/" + path;
            try {
                return new URL("jar", null, 0, url + "!" + path, handler);
            } catch (MalformedURLException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        } else return null;
    }

    @Override
    public InputStream getResourceAsStream(String resourceName) {
        return this.getInputStream(resourceName);
    }

    @Override
    public boolean hasDirectories() {
        return this.hasDirectories;
    }

    @Override
    public boolean hasSearchedAll() {
        return false;
    }

    @Override
    public boolean hasResource(String resourceName) {
        return this.getEntry(resourceName) != null;
    }

    @Override
    public boolean hasParentResource(String parentResource) {
        List<ZipEntry> entries = this.getEntries();
        int size = entries.size();
        for (int e = 0; e < size; e++) if (entries.get(e).getName().startsWith(parentResource)) return true;
        return false;
    }

    @Override
    public synchronized byte[] loadInternal(ClassSearchInfo info) throws ClassNotFoundException {
        if (!this.hasClass(info)) throw new ClassNotFoundException(info.getFullClassName());
        return IOUtil.read(this.getInputStream(info.getClassPath()), this.getBuffer());
    }

    public void refresh() {
        try {
            this.searched = 0;
            this.searchedAll = false;
            this.packages.clear();
            this.entries = null;
            this.hasDirectories = false;
            this.zipFile.close();
            this.zipFile = new ZipFile(this.zipFile.getName());
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * @param resourceName The name of the resource that points to an internal archive
	 * @return A zip class source wrapping the internal archive, otherwise null if the resource does not exist
	 */
    public ZipClassSource getInternalArchive(String resourceName) {
        if (this.hasResource(resourceName)) return new InternalZipClassSource(resourceName, this.getInputStream(resourceName));
        return null;
    }

    /**
	 * @param path The path to get an input stream for
	 * @return An input stream to the zip entry
	 */
    protected InputStream getInputStream(String path) {
        try {
            return zipFile.getInputStream(this.getEntry(path));
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * @param path The name/path of an entry in the archive
	 * @return The named entry, otherwise null if it does not exist
	 */
    protected ZipEntry getEntry(String path) {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);
        List<ZipEntry> entries = this.getEntries();
        for (int e = 0; e < entries.size(); e++) {
            if (this.entries.get(e).getName().equals(path)) return this.entries.get(e);
        }
        return null;
    }

    /**
	 * This will initialize the internal entries cache on the first call (or if the cache was cleared), after which
	 * it will only return the cache.
	 * 
	 * @return A list of zip entries pertaining to this archive
	 */
    protected List<ZipEntry> getEntries() {
        if (this.entries == null) {
            this.entries = new ArrayList<ZipEntry>();
            synchronized (entries) {
                Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
                while (entries.hasMoreElements()) this.entries.add(entries.nextElement());
            }
        }
        return this.entries;
    }

    @Override
    public ClassSource clone() throws CloneNotSupportedException {
        return new ZipClassSource(this.file);
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public List<String> getClassNamesForPackage(PackageSearchInfo info) {
        List<String> classNames = new ArrayList<String>();
        String pp = info.getPackagePath();
        List<ZipEntry> entries = this.getEntries();
        int size = entries.size();
        for (int e = 0; e < size; e++) {
            ZipEntry entry = entries.get(e);
            if (entry.getName().startsWith(pp) && ClassNameUtil.isClassSource(entry.getName())) {
                classNames.add(ClassNameUtil.toClassName(entry.getName()));
            }
        }
        return classNames;
    }

    @Override
    public Collection<String> getResourceNames() {
        List<String> names = new ArrayList<String>();
        List<ZipEntry> entries = this.getEntries();
        int size = entries.size();
        for (int e = 0; e < size; e++) {
            ZipEntry entry = entries.get(e);
            if (!ClassNameUtil.isClassSource(entry.getName())) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    @Override
    public Collection<String> getClassNames() {
        List<String> names = new ArrayList<String>();
        List<ZipEntry> entries = this.getEntries();
        int size = entries.size();
        for (int e = 0; e < size; e++) {
            ZipEntry entry = entries.get(e);
            if (ClassNameUtil.isClassSource(entry.getName())) {
                names.add(ClassNameUtil.toClassName(entry.getName()));
            }
        }
        return names;
    }

    @Override
    public long getLastModified() {
        return this.file.lastModified();
    }

    @Override
    public long getLastModifiedClass() {
        return this.getLastModified();
    }

    @Override
    public boolean isHasBeenModified() {
        return this.loaded < this.file.lastModified();
    }

    @Override
    public boolean isHasClassesBeenModified() {
        return this.isHasBeenModified();
    }

    @Override
    public void reload() {
        this.refresh();
        this.entries = null;
        this.loaded = this.file.lastModified();
    }

    @Override
    public void cleanup() {
        try {
            this.zipFile.close();
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
        super.cleanup();
    }

    /**
	 * The handler, a single instance of this is available for
	 * each instance of {@link ZipClassSource}.
	 * 
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ZipClassSource.java 2678 2010-12-24 04:18:29Z ponderator $
	 */
    protected class ZipStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new ZipConnection(u);
        }
    }

    /**
	 * A single zip connection to this library.
	 * 
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ZipClassSource.java 2678 2010-12-24 04:18:29Z ponderator $
	 */
    protected class ZipConnection extends URLConnection {

        protected final String path;

        public ZipConnection(URL url) {
            super(url);
            this.path = url.getFile().split("!")[1];
        }

        @Override
        public void connect() throws IOException {
            this.connected = true;
        }

        @Override
        public long getLastModified() {
            return file.lastModified();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return zipFile.getInputStream(getEntry(path));
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
