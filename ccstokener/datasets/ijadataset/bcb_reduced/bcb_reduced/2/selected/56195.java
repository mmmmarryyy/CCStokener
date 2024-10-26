package org.allcolor.alc.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.allcolor.alc.config.ConfigMerger;
import org.allcolor.alc.config.ConfigMergerImpl;
import org.allcolor.alc.filesystem.Directory;
import org.allcolor.alc.filesystem.File;
import org.allcolor.alc.filesystem.FileSystem;
import org.allcolor.alc.filesystem.FileSystemElement;
import org.allcolor.alc.filesystem.FileSystemType;
import org.allcolor.alc.filesystem.classloader.FileSystemClassLoader;
import org.allcolor.alc.reflect.Dynamic;
import org.allcolor.alc.utils.io.ReaderUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * DOCUMENT ME!
 * 
 * @author Quentin Anciaux
 * @version $Revision$
 */
public class ALCWebFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(ALCWebFilter.class);

    /**
	 * DOCUMENT ME!
	 * 
	 * @author Quentin Anciaux
	 * @version $Revision$
	 */
    public static class ALCManifest {

        /**
		 * DOCUMENT ME!
		 */
        private final List<ALCManifest> dependencies = new ArrayList<ALCManifest>();

        /** DOCUMENT ME! */
        private final FileSystemElement file;

        private List<ALCManifest> fullDependencies = null;

        /** DOCUMENT ME! */
        private final YPackage main;

        private final Manifest manifest;

        /**
		 * @param file
		 * @param fullDependencies
		 * @param manifest
		 * @param main
		 */
        public ALCManifest(final FileSystemElement file, final Manifest manifest, final YPackage main) {
            super();
            this.file = file;
            this.manifest = manifest;
            this.main = main;
        }

        /**
		 * DOCUMENT ME!
		 * 
		 * @param p
		 *            DOCUMENT ME!
		 */
        private void addDependency(final ALCManifest p) {
            this.dependencies.add(p);
        }

        private List<ALCManifest> constructDependenciesList(final ALCManifest manifest) {
            final List<ALCManifest> list = new ArrayList<ALCManifest>();
            this.constructDependenciesList(manifest, list);
            return list;
        }

        private void constructDependenciesList(final ALCManifest manifest, final List<ALCManifest> list) {
            for (final ALCManifest mf : manifest.getPartialDependenciesList()) {
                if (!ALCWebFilter.containsALCManifest(list, mf, false)) {
                    list.add(mf);
                    this.constructDependenciesList(mf, list);
                }
            }
        }

        public List<ALCManifest> getDependenciesList() {
            if (this.fullDependencies == null) {
                this.fullDependencies = Collections.unmodifiableList(this.constructDependenciesList(this));
            }
            return this.fullDependencies;
        }

        public FileSystemElement getJarFile() {
            return this.file;
        }

        public Manifest getManifest() {
            return this.manifest;
        }

        public YPackage getPackage() {
            return this.main;
        }

        private List<ALCManifest> getPartialDependenciesList() {
            return Collections.unmodifiableList(this.dependencies);
        }
    }

    /**
	 * @author Quentin Anciaux
	 * @version $Revision$
	 */
    private static interface MainFilter {

        /**
		 * DOCUMENT ME!
		 */
        public abstract void destroy();

        /**
		 * DOCUMENT ME!
		 * 
		 * @param request
		 *            DOCUMENT ME!
		 * @param response
		 *            DOCUMENT ME!
		 */
        public abstract void doFilter(final Object request, final Object response);

        /**
		 * DOCUMENT ME!
		 * 
		 * @param unionWebappFS
		 *            DOCUMENT ME!
		 * @param contextName
		 *            DOCUMENT ME!
		 * 
		 * @throws ServletException
		 *             DOCUMENT ME!
		 */
        public abstract void init(final FileSystem unionWebappFS, final String contextName) throws ServletException;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @author Quentin Anciaux
	 * @version $Revision$
	 */
    public static class YPackage {

        private final String depends;

        private final String description;

        private final String license;

        private final String licenseText;

        /** DOCUMENT ME! */
        private final String packageName;

        private final String title;

        private final String vendor;

        /** DOCUMENT ME! */
        private final String version;

        /**
		 * @param packageName
		 * @param versionMajor
		 * @param versionMinor
		 * @param versionPatch
		 * @param description
		 * @param license
		 * @param licenseText
		 * @param vendor
		 * @param title
		 */
        private YPackage(final String packageName, final String version, final String depends, final String description, final String license, final String licenseText, final String vendor, final String title) {
            super();
            this.packageName = packageName;
            this.version = version;
            this.depends = depends;
            this.description = description;
            this.license = license;
            this.licenseText = licenseText;
            this.vendor = vendor;
            this.title = title;
        }

        public String getDepends() {
            return this.depends;
        }

        public String getDescription() {
            return this.description;
        }

        public String getLicense() {
            return this.license;
        }

        public String getLicenseText() {
            return this.licenseText;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public String getTitle() {
            return this.title;
        }

        public String getVendor() {
            return this.vendor;
        }

        public String getVersion() {
            return this.version;
        }

        /**
		 * DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
        @Override
        public String toString() {
            return this.packageName + " - version: " + this.version;
        }
    }

    /** DOCUMENT ME! */
    private static FileSystemClassLoader fsClassLoader = null;

    private static ALCWebFilter handle = null;

    /**
	 * DOCUMENT ME!
	 * 
	 * @param list
	 *            DOCUMENT ME!
	 * @param dx
	 *            DOCUMENT ME!
	 * @param noVersionCheck
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    private static boolean containsALCManifest(final List<ALCManifest> list, final ALCManifest dx, final boolean noVersionCheck) {
        for (final ALCManifest dxm : list) {
            if ((noVersionCheck && dxm.main.packageName.equals(dx.main.packageName)) || ((dxm.main.version.equals(dx.main.version)) && dxm.main.packageName.equals(dx.main.packageName))) {
                return true;
            }
        }
        return false;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public static final FileSystemClassLoader getFsClassLoader() {
        return ALCWebFilter.fsClassLoader;
    }

    public static ALCWebFilter getHandle() {
        return ALCWebFilter.handle;
    }

    /** DOCUMENT ME! */
    private volatile boolean init = false;

    /** DOCUMENT ME! */
    private volatile Thread initThread = null;

    private List<ALCManifest> lALCManifest = new ArrayList<ALCManifest>(0);

    private List<ALCManifest> lBundlesALCManifest = new ArrayList<ALCManifest>(0);

    /** DOCUMENT ME! */
    private final List<FileSystem> listMountedFs = new ArrayList<FileSystem>();

    /** DOCUMENT ME! */
    private MainFilter mainFilter = null;

    private volatile long lastLoadLog4j = -1;

    private ServletContext context = null;

    /**
	 * DOCUMENT ME!
	 * 
	 * @param config
	 *            DOCUMENT ME!
	 * 
	 * @throws ServletException
	 *             DOCUMENT ME!
	 */
    private void _init(final FilterConfig config) throws ServletException {
        if (this.init) {
            return;
        }
        ALCWebFilter.handle = this;
        try {
            try {
                URL url = config.getServletContext().getResource("/WEB-INF/classes/log4j.properties");
                URLConnection uc = url.openConnection();
                lastLoadLog4j = uc.getLastModified();
                try {
                    uc.getInputStream().close();
                } catch (Exception ignore) {
                }
                PropertyConfigurator.configure(url);
            } catch (final Exception e) {
            }
            this.registerConfigMergers();
            final String contextName = this.getContextName(context);
            final StringBuilder bufferUnionAlrFs = new StringBuilder();
            final StringBuilder bufferUnionFs = new StringBuilder();
            final StringBuilder bufferUnionLibsFs = new StringBuilder();
            final FileSystem contextfs = FileSystem.mount(contextName + "_CONTEXT", FileSystemType.SERVLETCONTEXT, context);
            this.listMountedFs.add(contextfs);
            LOG.info("Mounted servlet context filesystem " + contextfs.getLabel());
            try {
                this.lBundlesALCManifest = this.getPackageToDeploy(contextfs.directory("/WEB-INF/bundle-archives/"), context);
            } catch (Exception ignore) {
            }
            LOG.info("lBundlesALCManifest : " + lBundlesALCManifest.size());
            for (final ALCManifest bundle : this.lBundlesALCManifest) {
                LOG.info("mounting : " + bundle.getJarFile().getPath());
                final FileSystem zipfs = FileSystem.mount(contextName + "_ALR_" + bundle.getJarFile().getName(), FileSystemType.ZIP, bundle.getJarFile());
                LOG.info("mounted : " + bundle.getJarFile().getPath());
                bufferUnionAlrFs.append(zipfs.getLabel());
                bufferUnionAlrFs.append(":");
                this.listMountedFs.add(zipfs);
            }
            bufferUnionAlrFs.append(contextfs.getLabel());
            bufferUnionAlrFs.append(":");
            LOG.info("Mounting unionALRFS filesystem " + bufferUnionAlrFs.toString());
            final FileSystem unionALRFS = FileSystem.mount(contextName, FileSystemType.UNION, bufferUnionAlrFs.toString());
            LOG.info("Mounted unionALRFS filesystem " + unionALRFS.getLabel() + " - " + bufferUnionAlrFs.toString());
            final FileSystem memoryfs = FileSystem.mount(unionALRFS.getLabel() + "_MEMORY", FileSystemType.MEMORY, null);
            this.listMountedFs.add(memoryfs);
            LOG.info("Mounted MEMORY filesystem " + memoryfs.getLabel());
            bufferUnionFs.append(unionALRFS.getLabel());
            bufferUnionFs.append(":");
            bufferUnionFs.append(memoryfs.getLabel());
            final Directory archives = unionALRFS.directory("/WEB-INF/archives/");
            this.lALCManifest = this.getPackageToDeploy(archives, context);
            for (int i = this.lALCManifest.size() - 1; i >= 0; i--) {
                final FileSystemElement file = this.lALCManifest.get(i).getJarFile();
                FileSystem zipfs = null;
                try {
                    zipfs = file.isFile() ? FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName(), FileSystemType.ZIP, file) : FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName(), FileSystemType.CHROOT, file);
                } catch (final Exception ignore) {
                    zipfs = FileSystem.getFileSystem(unionALRFS.getLabel() + "_" + file.getName());
                    if (zipfs != null) {
                        zipfs.umount();
                        zipfs = file.isFile() ? FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName(), FileSystemType.ZIP, file) : FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName(), FileSystemType.CHROOT, file);
                    } else {
                        continue;
                    }
                }
                this.listMountedFs.add(zipfs);
                try {
                    FileSystem libs = null;
                    try {
                        libs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_libs", FileSystemType.CHROOT, unionALRFS.getLabel() + "_" + file.getName() + ":/lib/");
                    } catch (final Exception ignore) {
                        libs = FileSystem.getFileSystem(unionALRFS.getLabel() + "_" + file.getName() + "_libs");
                        if (libs != null) {
                            libs.umount();
                            libs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_libs", FileSystemType.CHROOT, unionALRFS.getLabel() + "_" + file.getName() + ":/lib/");
                        }
                    }
                    if (libs != null) {
                        this.listMountedFs.add(libs);
                        LOG.info("Mounted libs filesystem " + unionALRFS.getLabel() + "_" + file.getName() + ":/lib/");
                        if (bufferUnionLibsFs.length() > 0) {
                            bufferUnionLibsFs.append(":");
                        }
                        bufferUnionLibsFs.append(unionALRFS.getLabel());
                        bufferUnionLibsFs.append("_");
                        bufferUnionLibsFs.append(file.getName());
                        bufferUnionLibsFs.append("_libs");
                        for (final File lib : libs.getRoot().getFiles()) {
                            if (lib.getName().endsWith(".jar")) {
                                FileSystem fs = null;
                                try {
                                    fs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_lib_" + lib.getName(), FileSystemType.ZIP, lib);
                                } catch (final Exception ignore) {
                                    fs = FileSystem.getFileSystem(unionALRFS.getLabel() + "_" + file.getName() + "_lib_" + lib.getName());
                                    if (fs != null) {
                                        fs.umount();
                                        fs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_lib_" + lib.getName(), FileSystemType.ZIP, lib);
                                    }
                                }
                                if (fs != null) {
                                    this.listMountedFs.add(fs);
                                    if (bufferUnionLibsFs.length() > 0) {
                                        bufferUnionLibsFs.append(":");
                                    }
                                    bufferUnionLibsFs.append(fs.getLabel());
                                    LOG.info("Mounted lib filesystem " + fs.getLabel());
                                }
                            }
                        }
                    }
                } catch (final IOException ignore) {
                    ;
                }
                try {
                    FileSystem fs = null;
                    try {
                        fs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_webapp", FileSystemType.CHROOT, unionALRFS.getLabel() + "_" + file.getName() + ":/content/");
                    } catch (final Exception ignore) {
                        fs = FileSystem.getFileSystem(unionALRFS.getLabel() + "_" + file.getName() + "_webapp");
                        if (fs != null) {
                            fs.umount();
                            fs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_webapp", FileSystemType.CHROOT, unionALRFS.getLabel() + "_" + file.getName() + ":/content/");
                        }
                    }
                    if (fs != null) {
                        this.listMountedFs.add(fs);
                        LOG.info("Mounted content filesystem " + fs.getLabel() + "_" + file.getName() + ":/content/");
                    }
                    bufferUnionFs.append(":");
                    bufferUnionFs.append(unionALRFS.getLabel());
                    bufferUnionFs.append("_");
                    bufferUnionFs.append(file.getName());
                    bufferUnionFs.append("_webapp");
                } catch (final IOException ignore) {
                    ;
                }
            }
            for (int i = 0; i < this.lALCManifest.size(); i++) {
                final FileSystemElement file = this.lALCManifest.get(i).getJarFile();
                try {
                    FileSystem fs = null;
                    try {
                        fs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_config", FileSystemType.CHROOT, unionALRFS.getLabel() + "_" + file.getName() + ":/config/");
                    } catch (final Exception ignore) {
                        fs = FileSystem.getFileSystem(unionALRFS.getLabel() + "_" + file.getName() + "_config");
                        if (fs != null) {
                            fs.umount();
                            fs = FileSystem.mount(unionALRFS.getLabel() + "_" + file.getName() + "_config", FileSystemType.CHROOT, unionALRFS.getLabel() + "_" + file.getName() + ":/config/");
                        }
                    }
                    if (fs != null) {
                        this.listMountedFs.add(fs);
                        for (final File f : fs.getRoot().getFiles()) {
                            try {
                                ConfigMergerImpl.getInstance().mergeConfig(f, memoryfs, file.getName());
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                        LOG.info("Mounted config filesystem " + fs.getLabel() + "_" + file.getName() + ":/config/");
                    }
                } catch (final IOException ignore) {
                    ;
                }
            }
            FileSystem unionWebappFS = null;
            try {
                unionWebappFS = FileSystem.mount(unionALRFS.getLabel() + "_UNION_webapp", FileSystemType.UNION, bufferUnionFs.toString());
            } catch (final Exception ignore) {
                unionWebappFS = FileSystem.getFileSystem(unionALRFS.getLabel() + "_UNION_webapp");
                if (unionWebappFS != null) {
                    unionWebappFS.umount();
                    unionWebappFS = FileSystem.mount(unionALRFS.getLabel() + "_UNION_webapp", FileSystemType.UNION, bufferUnionFs.toString());
                }
            }
            if (unionWebappFS != null) {
                this.listMountedFs.add(unionWebappFS);
            }
            LOG.info("Mounted lib unionWebappFS " + bufferUnionFs.toString());
            FileSystem unionLibsFS = null;
            try {
                unionLibsFS = FileSystem.mount(unionALRFS.getLabel() + "_UNION_libs", FileSystemType.UNION, bufferUnionLibsFs.toString());
            } catch (final Exception ignore) {
                unionLibsFS = FileSystem.getFileSystem(unionALRFS.getLabel() + "_UNION_libs");
                if (unionLibsFS != null) {
                    unionLibsFS.umount();
                    unionLibsFS = FileSystem.mount(unionALRFS.getLabel() + "_UNION_libs", FileSystemType.UNION, bufferUnionLibsFs.toString());
                }
            }
            if (unionLibsFS != null) {
                this.listMountedFs.add(unionLibsFS);
            }
            LOG.info("Mounted lib unionLibsFS " + bufferUnionLibsFs.toString());
            ALCWebFilter.fsClassLoader = new FileSystemClassLoader(unionLibsFS.getLabel(), bufferUnionLibsFs.toString(), null, this.getClass().getClassLoader());
            LOG.info("Classloader initialized : " + ALCWebFilter.fsClassLoader);
            try {
                this.mainFilter = Dynamic._.Proxy(ALCWebFilter.fsClassLoader.loadClass("org.allcolor.ywt.filter.CMainFilter").newInstance(), MainFilter.class);
                this.mainFilter.init(unionWebappFS, unionALRFS.getLabel());
                LOG.info("MainFilter initialized : " + this.mainFilter);
            } catch (final IllegalAccessException e) {
                throw e;
            } catch (final InstantiationException e) {
                throw e;
            } catch (final ClassNotFoundException e) {
                throw e;
            }
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    public void destroy() {
        if ((this.initThread != null) && this.initThread.isAlive()) {
            try {
                Thread.class.getMethod("stop", (Class<?>[]) null).invoke(this.initThread, (Object[]) null);
            } catch (final Exception ignore) {
                ;
            }
        }
        this.initThread = null;
        ALCWebFilter.fsClassLoader = null;
        if (this.mainFilter != null) {
            this.mainFilter.destroy();
            this.mainFilter = null;
        }
        for (final FileSystem fs : this.listMountedFs) {
            try {
                fs.umount();
            } catch (final Throwable ignore) {
                final Throwable t = ignore;
                if (t.getClass() == ThreadDeath.class) {
                    throw (ThreadDeath) t;
                }
                Throwable cause = ignore.getCause();
                while (cause != null) {
                    if (cause.getClass() == ThreadDeath.class) {
                        throw (ThreadDeath) cause;
                    }
                    cause = cause.getCause();
                }
            }
        }
        this.listMountedFs.clear();
        this.lALCManifest.clear();
        this.context = null;
        ALCWebFilter.handle = null;
        this.init = false;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param arg0
	 *            DOCUMENT ME!
	 * @param arg1
	 *            DOCUMENT ME!
	 * @param arg2
	 *            DOCUMENT ME!
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 * @throws ServletException
	 *             DOCUMENT ME!
	 */
    public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain arg2) throws IOException, ServletException {
        if (!this.init) {
            final HttpServletResponse response = Dynamic._.Cast(arg1);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Mainfilter not initialized.");
            return;
        }
        if (this.mainFilter != null) {
            try {
                URL url = this.context.getResource("/WEB-INF/classes/log4j.properties");
                URLConnection uc = url.openConnection();
                if (uc.getLastModified() != lastLoadLog4j) {
                    lastLoadLog4j = uc.getLastModified();
                    try {
                        uc.getInputStream().close();
                    } catch (Exception ignore) {
                    }
                    PropertyConfigurator.configure(url);
                } else {
                    try {
                        uc.getInputStream().close();
                    } catch (Exception ignore) {
                    }
                }
            } catch (final Exception e) {
            }
            this.mainFilter.doFilter(arg0, arg1);
        } else {
            final HttpServletResponse response = Dynamic._.Cast(arg1);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Mainfilter bad setup.");
        }
    }

    /**
	 * 
	 * @param toCheck
	 * @param pck
	 * @param pckVersion
	 * @return
	 */
    private ALCManifest findDependency(final List<ALCManifest> toCheck, final String pck, final String pckVersion, final String operator) throws ServletException {
        ALCManifest toRet = null;
        if ("<".equals(operator) || "!!<".equals(operator)) {
            for (final ALCManifest dx : toCheck) {
                if (dx.main.packageName.equals(pck)) {
                    if (dx.main.version.compareTo(pckVersion) >= 0) {
                        continue;
                    }
                    toRet = dx;
                    break;
                }
            }
        } else if ("<=".equals(operator) || "!!<=".equals(operator)) {
            for (final ALCManifest dx : toCheck) {
                if (dx.main.packageName.equals(pck)) {
                    if (dx.main.version.compareTo(pckVersion) > 0) {
                        continue;
                    }
                    toRet = dx;
                    break;
                }
            }
        } else if ("==".equals(operator) || "!!==".equals(operator)) {
            for (final ALCManifest dx : toCheck) {
                if (dx.main.packageName.equals(pck)) {
                    if (dx.main.version.compareTo(pckVersion) != 0) {
                        continue;
                    }
                    toRet = dx;
                    break;
                }
            }
        } else if (">=".equals(operator) || "!!>=".equals(operator)) {
            for (final ALCManifest dx : toCheck) {
                if (dx.main.packageName.equals(pck)) {
                    if (dx.main.version.compareTo(pckVersion) < 0) {
                        continue;
                    }
                    toRet = dx;
                    break;
                }
            }
        } else if (">".equals(operator) || "!!>".equals(operator)) {
            for (final ALCManifest dx : toCheck) {
                if (dx.main.packageName.equals(pck)) {
                    if (dx.main.version.compareTo(pckVersion) <= 0) {
                        continue;
                    }
                    toRet = dx;
                    break;
                }
            }
        }
        if ((operator != null) && operator.startsWith("!!")) {
            if (toRet != null) {
                throw new ServletException("Conflict found !! " + pck + " " + toRet.main.version + " " + operator + " " + pckVersion + " and version " + pckVersion + " found.");
            }
        }
        if (toRet != null) {
            LOG.info("Found package : " + toRet.getPackage().packageName + "/" + toRet.getPackage().version + " - " + toRet.getJarFile().getPath());
        }
        return toRet;
    }

    public List<ALCManifest> getALCManifestList() {
        return Collections.unmodifiableList(this.lALCManifest);
    }

    public List<ALCManifest> getBundlesALCManifestList() {
        return Collections.unmodifiableList(this.lBundlesALCManifest);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param context
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    private final String getContextName(final ServletContext context) {
        try {
            String url = context.getResource("/").toExternalForm();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            if (url.lastIndexOf('/') != -1) {
                url = url.substring(url.lastIndexOf('/') + 1).trim();
            }
            if (url.length() == 0) {
                return "ROOT";
            }
            return url;
        } catch (final Exception e) {
            final String result = context.getServletContextName();
            if (result == null) {
                return "unknown";
            }
            return result;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param files
	 *            DOCUMENT ME!
	 * @param context
	 *            DOCUMENT ME!
	 * 
	 * @throws ServletException
	 *             DOCUMENT ME!
	 */
    private List<ALCManifest> getPackageToDeploy(final Directory archives, final ServletContext context) throws ServletException, IOException {
        LOG.info("Dependencies check started.");
        final List<ALCManifest> lALCManifest = new ArrayList<ALCManifest>();
        for (final Directory dir : archives.getDirectories()) {
            if (dir.getName().endsWith(".jar")) {
                FileSystem fs = null;
                try {
                    fs = FileSystem.mount(UUID.randomUUID() + "_" + dir.getName(), FileSystemType.CHROOT, dir);
                    final File alcManifest = fs.file("/META-INF/MANIFEST.MF");
                    if ((alcManifest != null) && alcManifest.exists()) {
                        final ALCManifest current = this.parseManifest(alcManifest, dir, fs);
                        if (current != null) {
                            if (ALCWebFilter.containsALCManifest(lALCManifest, current, false)) {
                                continue;
                            }
                            lALCManifest.add(current);
                        }
                    }
                } catch (final IOException e) {
                    continue;
                } finally {
                    try {
                        fs.umount();
                    } catch (final Exception ignore) {
                        ;
                    }
                }
            }
        }
        for (final File file : archives.getFiles()) {
            if (file.getName().endsWith(".jar")) {
                FileSystem fs = null;
                try {
                    fs = FileSystem.mount(UUID.randomUUID() + "_" + file.getName(), FileSystemType.ZIP, file);
                    final File alcManifest = fs.file("/META-INF/MANIFEST.MF");
                    if ((alcManifest != null) && alcManifest.exists()) {
                        final ALCManifest current = this.parseManifest(alcManifest, file, fs);
                        if (current != null) {
                            if (ALCWebFilter.containsALCManifest(lALCManifest, current, false)) {
                                continue;
                            }
                            lALCManifest.add(current);
                        }
                    }
                } catch (final IOException e) {
                    continue;
                } finally {
                    try {
                        fs.umount();
                    } catch (final Exception ignore) {
                        ;
                    }
                }
            }
        }
        LOG.info("end looking for file !");
        for (int i = 0; i < lALCManifest.size(); i++) {
            final ALCManifest dx = lALCManifest.get(i);
            if (dx.main.getDepends() != null && dx.main.getDepends().trim().length() > 0) {
                LOG.info("dependancies for " + dx.main.packageName + " : " + dx.main.getDepends());
                final StringTokenizer tkDep = new StringTokenizer(dx.main.getDepends(), ",\n\r\\/", false);
                while (tkDep.hasMoreTokens()) {
                    final String token = tkDep.nextToken().trim();
                    final StringTokenizer tkPackage = new StringTokenizer(token, " \\/,", false);
                    if (tkPackage.countTokens() == 3) {
                        final String sPckpackageName = tkPackage.nextToken();
                        final String sOperator = tkPackage.nextToken();
                        final String sPckversion = tkPackage.nextToken();
                        if (sPckversion == null) {
                            throw new ServletException("Dependency " + sPckpackageName + " - " + sPckversion + " not found.");
                        }
                        final ALCManifest dependency = this.findDependency(lALCManifest, sPckpackageName, sPckversion, sOperator);
                        if (dependency != null) {
                            dx.addDependency(dependency);
                        } else {
                            throw new ServletException("Dependency " + sPckpackageName + " - " + sPckversion + " not found.");
                        }
                    }
                }
            }
        }
        Collections.sort(lALCManifest, new Comparator<ALCManifest>() {

            public int compare(final ALCManifest o1, final ALCManifest o2) {
                if (ALCWebFilter.containsALCManifest(o1.getDependenciesList(), o2, true)) {
                    return 1;
                }
                if (ALCWebFilter.containsALCManifest(o2.getDependenciesList(), o1, true)) {
                    return -1;
                }
                return 0;
            }
        });
        LOG.info("Packages found:");
        for (final ALCManifest alcMf : lALCManifest) {
            LOG.info(alcMf.getPackage().getPackageName() + " - " + alcMf.getPackage().getVersion() + " - (" + alcMf.getPackage().title + ") " + alcMf.getJarFile().getPath());
        }
        LOG.info("Dependencies check finished.");
        return lALCManifest;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param config
	 *            DOCUMENT ME!
	 * 
	 * @throws ServletException
	 *             DOCUMENT ME!
	 */
    public void init(final FilterConfig config) throws ServletException {
        final ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        this.context = config.getServletContext();
        this.initThread = new Thread() {

            {
                this.setContextClassLoader(currentCL);
                this.setDaemon(true);
                this.start();
            }

            @Override
            public void run() {
                try {
                    ALCWebFilter.this._init(config);
                    ALCWebFilter.this.init = true;
                } catch (final ServletException e) {
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
                    }
                    if (e.getRootCause() != null) {
                        e.getRootCause().printStackTrace();
                    }
                    ALCWebFilter.this.init = false;
                }
            }
        };
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param manifest
	 *            DOCUMENT ME!
	 * @param yar
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws RuntimeException
	 *             DOCUMENT ME!
	 */
    private ALCManifest parseManifest(final File manifest, final FileSystemElement file, final FileSystem fs) {
        InputStream in = null;
        try {
            in = manifest.toURL().openStream();
            LOG.info("parsing : " + manifest.getPath() + " - " + file.getPath());
            final Manifest mf = new Manifest(in);
            final Attributes alcAttributes = mf.getAttributes("org/allcolor/alc/");
            if (alcAttributes != null) {
                final String spackageName = alcAttributes.getValue("Extension-name") == null ? "" : alcAttributes.getValue("Extension-name").trim();
                if ("".equals(spackageName)) {
                    return null;
                }
                final String sversion = alcAttributes.getValue("Specification-Version") == null ? "" : alcAttributes.getValue("Specification-Version").trim();
                if ("".equals(sversion)) {
                    return null;
                }
                final String sdepends = alcAttributes.getValue("Depends") == null ? "" : alcAttributes.getValue("Depends").trim();
                final String slicenses = alcAttributes.getValue("License") == null ? "" : alcAttributes.getValue("License").trim();
                final StringBuilder licensesText = new StringBuilder();
                final StringTokenizer tkLic = new StringTokenizer(slicenses, ",", false);
                while (tkLic.hasMoreTokens()) {
                    final String token = tkLic.nextToken().trim();
                    try {
                        File licenseFile = null;
                        if (token.endsWith("+")) {
                            licenseFile = fs.file("/META-INF/" + token.substring(0, token.length() - 1) + ".license");
                        } else {
                            licenseFile = fs.file("/META-INF/" + token + ".license");
                        }
                        if ((licenseFile != null) && licenseFile.exists()) {
                            ReaderUtils.forEachLine(licenseFile, new ReaderUtils.LineListener() {

                                public void exception(final IOException ioe) {
                                }

                                public boolean line(final String line) throws IOException {
                                    licensesText.append(line);
                                    licensesText.append('\n');
                                    return true;
                                }
                            });
                        }
                    } catch (final Exception ignore) {
                    }
                }
                final String sdescription = alcAttributes.getValue("Description") == null ? "" : alcAttributes.getValue("Description");
                final String svendor = alcAttributes.getValue("Specification-Vendor") == null ? "" : alcAttributes.getValue("Specification-Vendor");
                final String stitle = alcAttributes.getValue("Specification-Title") == null ? "" : alcAttributes.getValue("Specification-Title");
                return new ALCManifest(file, mf, new YPackage(spackageName, sversion, sdepends, sdescription, slicenses, licensesText.toString(), svendor, stitle));
            }
            return null;
        } catch (final RuntimeException ignore) {
            throw ignore;
        } catch (final Exception ignore) {
            throw new RuntimeException(ignore);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final Exception ignore) {
            }
        }
    }

    private void registerConfigMergers() throws IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (final Enumeration<URL> it = cl.getResources("META-INF/config-merger.properties"); it.hasMoreElements(); ) {
            InputStream in = null;
            try {
                in = it.nextElement().openStream();
                final Properties props = new Properties();
                props.load(in);
                for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                    try {
                        final ConfigMerger cm = (ConfigMerger) cl.loadClass((String) entry.getValue()).newInstance();
                        ConfigMergerImpl.getInstance().registerConfigMerger((String) entry.getKey(), cm);
                    } catch (final Exception ignore) {
                    }
                }
            } catch (final Exception ignore) {
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (final Exception ignore) {
                }
            }
        }
    }
}
