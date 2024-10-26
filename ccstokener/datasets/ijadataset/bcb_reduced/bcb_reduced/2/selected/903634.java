package net.sourceforge.javautil.classloader.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import net.sourceforge.javautil.common.ArchiveUtil;
import net.sourceforge.javautil.common.FileUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualArtifact;
import net.sourceforge.javautil.common.io.VirtualArtifactNotFoundException;
import net.sourceforge.javautil.common.io.IVirtualDirectory;
import net.sourceforge.javautil.common.io.IVirtualFile;
import net.sourceforge.javautil.common.io.impl.SimplePath;
import net.sourceforge.javautil.common.io.impl.SystemFile;
import net.sourceforge.javautil.common.io.impl.ZippedFile;

/**
 * A class source that will be a composite containing all the java archives inside a particular
 * directory after scanning such and possibly its recursive sub directories.
 * 
 * @author ponder
 * @author $Author: ponderator $
 * @version $Id: LibDirectoryClassSource.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class LibDirectoryClassSource extends CompositeClassSource {

    public static final String libext = System.mapLibraryName("someLib").substring(7);

    public static final Logger log = Logger.getLogger(LibDirectoryClassSource.class.getName());

    private final boolean recursive;

    private final IVirtualDirectory libDirectory;

    private final URL url;

    private boolean hasLibraries = false;

    /**
	 * This defaults to non-recursive.
	 * 
	 * @see #LibDirectoryClassSource(File, boolean)
	 */
    public LibDirectoryClassSource(IVirtualDirectory libDirectory) {
        this(libDirectory, false);
    }

    /**
	 * @param libDirectory The directory whose java archives should be loaded into this composite
	 * @param recursive True if the sub directories of the passed directory should also be scanned and sub archives loaded
	 */
    public LibDirectoryClassSource(IVirtualDirectory libDirectory, boolean recursive) {
        super(libDirectory.getPath().toString("/"));
        this.recursive = recursive;
        this.libDirectory = libDirectory;
        this.url = libDirectory.getURL();
        Iterator<IVirtualArtifact> artifacts = this.libDirectory.getArtifacts();
        while (artifacts.hasNext()) {
            IVirtualArtifact artifact = artifacts.next();
            if (artifact instanceof IVirtualFile) {
                if (ArchiveUtil.isArchive((IVirtualFile) artifact)) {
                    if (artifact instanceof SystemFile) {
                        try {
                            this.add(new ZipClassSource(((SystemFile) artifact).getRealArtifact()));
                        } catch (Exception e) {
                            ThrowableManagerRegistry.caught(e);
                            log.warning("Could not load archive as class source: " + artifact);
                        }
                    } else {
                        try {
                            this.add(new InternalZipClassSource(artifact.getName(), ((IVirtualFile) artifact).getInputStream()));
                        } catch (IOException e) {
                            ThrowableManagerRegistry.caught(e);
                            log.warning("Could not load archive as class source: " + artifact);
                        }
                    }
                }
            } else if (this.recursive) {
                this.add(new LibDirectoryClassSource((IVirtualDirectory) artifact, true));
            }
        }
    }

    /**
	 * @return True if this composite has a java native library
	 */
    public boolean hasLibraries() {
        List<IVirtualFile> files = libDirectory.getFiles();
        for (int f = 0; f < files.size(); f++) {
            if (files.get(f).getName().endsWith(libext)) return true;
        }
        return false;
    }

    /**
	 * @return True if this lib directory was scanned recursively, otherwise false
	 */
    public boolean isRecursive() {
        return recursive;
    }

    /**
   * @return The lib directory where this composite searched for archives
   */
    public IVirtualDirectory getLibDirectory() {
        return libDirectory;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public URL getResource(String resourceName) {
        URL url = super.getResource(resourceName);
        if (url == null) {
            try {
                url = libDirectory.getArtifact(new SimplePath(resourceName)).getURL();
            } catch (VirtualArtifactNotFoundException e) {
                return null;
            }
        }
        return url;
    }

    @Override
    public InputStream getResourceAsStream(String resourceName) {
        InputStream is = super.getResourceAsStream(resourceName);
        if (is == null) {
            try {
                URL url = this.getResource(resourceName);
                return url.openStream();
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }
        return is;
    }

    @Override
    public List<URL> getResources(String resourceName) throws MalformedURLException {
        List<URL> urls = super.getResources(resourceName);
        URL url = this.getResource(resourceName);
        if (url != null) urls.add(url);
        return urls;
    }

    @Override
    public boolean hasResource(String resourceName) {
        boolean hr = super.hasResource(resourceName);
        if (!hr) {
            if (this.getResource(resourceName) != null) return true;
        }
        return hr;
    }
}
