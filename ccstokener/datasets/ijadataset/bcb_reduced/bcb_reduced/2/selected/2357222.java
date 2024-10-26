package jaxlib.management.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.loading.MLet;
import javax.management.loading.MLetMBean;
import jaxlib.io.IO;
import jaxlib.io.file.Files;
import jaxlib.io.file.monitor.DefaultFileAlterationMonitor;
import jaxlib.io.file.monitor.FileAlterationEvent;
import jaxlib.io.file.monitor.FileAlterationListener;
import jaxlib.io.file.monitor.FileAlterationMonitor;
import jaxlib.io.file.monitor.FileAlterationMonitorType;
import jaxlib.io.stream.BufferedXInputStream;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.io.stream.RandomAccessFileMode;
import jaxlib.io.stream.XRandomAccessFile;
import jaxlib.lang.Objects;
import jaxlib.management.AnnotatedSingleRegistrationMBean;
import jaxlib.management.LocalMBeanReference;
import jaxlib.management.MBean;
import jaxlib.management.MBeanAttribute;
import jaxlib.management.MBeanOperation;
import jaxlib.management.MBeanSynchronization;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * @deprecated This class will be replaced by a more general (and less buggy) deployment mechanism which
 * will be independent of JMX.
 *
 * A deployment scanner periodicaly scans a single directory for new or modified Jar archives containing
 * MBeans.
 * <b>TODO: Redeployment is currently not working under MS-Windows.</b>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: DeploymentDirectoryMonitor.java 2730 2009-04-21 01:12:29Z joerg_wassmer $
 */
@Deprecated
@MBean
public class DeploymentDirectoryMonitor extends AnnotatedSingleRegistrationMBean implements DeploymentDirectoryMonitorMBean {

    private static final int ZIP_MAGIC = 0x504b0304;

    private static final String MLET_JAR_PATH = "META-INF/services.mlet";

    private static final String MLET_FILE_PATH = "META-INF" + File.separator + "services.mlet";

    private static final HashSet<String> fileNameExtensionsToIgnore = new HashSet<String>();

    static {
        fileNameExtensionsToIgnore.add(".bak");
        fileNameExtensionsToIgnore.add(".log");
        fileNameExtensionsToIgnore.add(".old");
        fileNameExtensionsToIgnore.add(".temp");
        fileNameExtensionsToIgnore.add(".tmp");
    }

    private final boolean delegateToClassLoaderRepository;

    private ClassLoader parentClassLoader;

    private final ObjectName parentClassLoaderName;

    private AccessControlContext accessControlContext;

    private final DeploymentDirectoryMonitor.FileAlterationHandler fileAlterationHandler = new FileAlterationHandler();

    private DefaultFileAlterationMonitor fileMonitor;

    private volatile File deployDir;

    private LinkedHashMap<String, DeploymentDirectoryMonitor.Deployment> deployments = new LinkedHashMap<String, Deployment>();

    private volatile long fileLockTimeoutMillis = 1000;

    private volatile long scanDelay = 10000;

    private volatile File workDir;

    /**
   * MLets will not start services if this field is true.
   */
    private volatile boolean initialDeploy = true;

    public DeploymentDirectoryMonitor() {
        this((ClassLoader) null, true, null);
    }

    public DeploymentDirectoryMonitor(ObjectName parentClassLoaderName, boolean delegateToClassLoaderRepository) {
        this(parentClassLoaderName, delegateToClassLoaderRepository, null);
    }

    public DeploymentDirectoryMonitor(ObjectName parentClassLoaderName, boolean delegateToClassLoaderRepository, AccessControlContext context) {
        super(MBeanSynchronization.SETTERS_AND_OPERATIONS);
        this.accessControlContext = context;
        this.delegateToClassLoaderRepository = delegateToClassLoaderRepository;
        this.parentClassLoaderName = parentClassLoaderName;
    }

    public DeploymentDirectoryMonitor(ClassLoader parentClassLoader, boolean delegateToClassLoaderRepository, AccessControlContext context) {
        super(MBeanSynchronization.SETTERS_AND_OPERATIONS);
        this.accessControlContext = context;
        this.delegateToClassLoaderRepository = delegateToClassLoaderRepository;
        this.parentClassLoader = parentClassLoader;
        this.parentClassLoaderName = null;
    }

    private void clearWorkDirectory() {
        File workDir = this.workDir;
        if (workDir != null) {
            File[] files = workDir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    files[i] = null;
                    try {
                        if (!Files.delete(f)) getLogger().warning("Unable to delete file in work directory: " + f);
                    } catch (final Exception ex) {
                        getLogger().log(logLevelFor(ex), "Exception while deleteting file in work directory: " + f, ex);
                    }
                }
            }
        }
    }

    private void extract(File srcFile) throws IOException {
        File tmpFile = Files.createTempFile(srcFile.getParentFile());
        File destDir = srcFile;
        Files.move(srcFile, tmpFile, Files.OVERWRITE);
        JarInputStream in = new JarInputStream(new BufferedXInputStream(new FileInputStream(srcFile)), true);
        try {
            Files.extractAll(in, destDir, Files.OVERWRITE, true);
            in.close();
            in = null;
        } finally {
            if (in != null) {
                IO.tryClose(in);
                in = null;
                try {
                    Files.delete(destDir);
                } catch (final Throwable ex) {
                }
            }
            try {
                Files.delete(tmpFile);
            } catch (final Throwable ex) {
                getLogger().log(logLevelFor(ex), "Unable to delete file: " + tmpFile, ex);
            }
        }
    }

    private static void extract(JarFile jarFile, JarEntry jarEntry, File destFile) throws IOException {
        long uncompressedSize = jarEntry.getSize();
        if (uncompressedSize == 0) {
            destFile.createNewFile();
        } else {
            int bufferSize = (uncompressedSize < 0) ? 8192 : Math.max(32, (int) Math.min(8192, uncompressedSize));
            BufferedXInputStream in = null;
            BufferedXOutputStream out = null;
            try {
                FileOutputStream fileOut = new FileOutputStream(destFile, false);
                FileChannel outChannel = fileOut.getChannel();
                outChannel.lock();
                out = new BufferedXOutputStream(fileOut, bufferSize);
                in = new BufferedXInputStream(jarFile.getInputStream(jarEntry), bufferSize);
                in.transferTo(out, -1);
                in.close();
                in = null;
                outChannel.truncate(outChannel.position());
                out.close();
                out = null;
            } finally {
                IO.tryClose(in);
                IO.tryClose(out);
            }
        }
    }

    private static boolean isZipFile(RandomAccessFile f) throws IOException {
        if (f.length() <= 100) {
            return false;
        } else {
            f.seek(0);
            return f.readInt() == ZIP_MAGIC;
        }
    }

    private static boolean isZipFile(File f) throws IOException {
        RandomAccessFile in = new RandomAccessFile(f, "r");
        try {
            boolean result = isZipFile(in);
            in.close();
            in = null;
            return result;
        } finally {
            IO.tryClose(in);
        }
    }

    static Level logLevelFor(Throwable ex) {
        return (ex instanceof IOException) ? Level.WARNING : Level.SEVERE;
    }

    protected boolean acceptFile(File f) {
        return (!f.getPath().endsWith("~") && !fileNameExtensionsToIgnore.contains(Files.getSuffix(f).toLowerCase()) && f.isFile() && f.canRead());
    }

    private void redeploy(File deployPath) {
        if (!acceptFile(deployPath)) {
            Logger logger = getLogger();
            if (logger.isLoggable(Level.FINER)) logger.log(Level.FINER, "Ignoring file: ".concat(deployPath.getPath()));
            return;
        }
        String fileName = deployPath.getName();
        fileName.hashCode();
        File workPath = new File(this.workDir, fileName).getAbsoluteFile();
        XRandomAccessFile deployFile = null;
        XRandomAccessFile workFile = null;
        long timestamp = -1;
        synchronized (this) {
            MBeanServer mbeanServer = getMBeanServerUnsafe();
            if (mbeanServer == null) return;
            try {
                deployFile = new XRandomAccessFile(deployPath, RandomAccessFileMode.READ_ONLY, this.fileLockTimeoutMillis, TimeUnit.MILLISECONDS);
                Deployment d = this.deployments.get(fileName);
                if (d != null) {
                    workPath = d.workFile;
                    timestamp = deployPath.lastModified();
                    if (timestamp == d.modTime) {
                        deployFile.close();
                        return;
                    } else {
                        d.unregister();
                        this.deployments.remove(fileName);
                        d = null;
                    }
                }
                if (workPath == null) workPath = new File(this.workDir, fileName).getAbsoluteFile();
                if (deployPath.isFile()) {
                    Logger logger = getLogger();
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Deploying: " + deployPath);
                    if (workPath.isDirectory() && !Files.delete(workPath)) throw new IOException("Unable to delete directory: " + workPath.getPath());
                    workFile = new XRandomAccessFile(workPath, RandomAccessFileMode.READ_WRITE, this.fileLockTimeoutMillis, TimeUnit.MILLISECONDS);
                    long length = deployFile.length();
                    workFile.setLength(length);
                    deployFile.copy(0, workFile, 0, length);
                    if (timestamp < 0) timestamp = deployPath.lastModified();
                    deployFile.close();
                } else {
                    if (!Files.delete(workPath)) throw new IOException("Unable to delete file: " + workPath.getPath());
                    return;
                }
            } catch (final Throwable ex) {
                IO.tryClose(deployFile);
                IO.tryClose(workFile);
                getLogger().log(Level.WARNING, Strings.concat("Exception!" + "\n  deployFile = ", deployPath.getPath(), "\n  workFile   = ", workPath.getPath()), ex);
                return;
            }
            deployFile = null;
            boolean isZipFile;
            try {
                workPath.setLastModified(timestamp);
                isZipFile = isZipFile(workFile);
                workFile.close();
                workFile = null;
                if (isZipFile) {
                    Logger logger = getLogger();
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Installing ZIP file: " + deployPath);
                    URL url = new URL(Strings.concat("jar:file:", workPath.getPath(), "!/"));
                    Deployment d = new Deployment(this.parentClassLoader, url, deployPath, workPath, timestamp);
                    this.deployments.put(fileName, d);
                    try {
                        installZipFile(mbeanServer, d, workPath);
                        mbeanServer.registerMBean(d, d.mbeanName);
                        d.startMBeans();
                    } catch (final Throwable ex) {
                        try {
                            if (!mbeanServer.isRegistered(d.mbeanName)) this.deployments.remove(fileName);
                        } finally {
                            throw ex;
                        }
                    }
                } else if (fileName.endsWith(".mlet")) {
                    Logger logger = getLogger();
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Installing standalone MLet file: " + deployPath);
                    URL url = workPath.toURL();
                    Deployment d = new Deployment(this.parentClassLoader, url, deployPath, workPath, timestamp);
                    this.deployments.put(fileName, d);
                    d.addMLetURL(url);
                    try {
                        mbeanServer.registerMBean(d, d.mbeanName);
                        d.startMBeans();
                    } catch (final Throwable ex) {
                        try {
                            if (!mbeanServer.isRegistered(d.mbeanName)) this.deployments.remove(fileName);
                        } finally {
                            throw ex;
                        }
                    }
                } else {
                    Logger logger = getLogger();
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Not a ZIP nor a MLet file, just copying to work dir: " + deployPath);
                }
            } catch (final Throwable ex) {
                IO.tryClose(workFile);
                try {
                    workPath.delete();
                } finally {
                    getLogger().log(Level.WARNING, "Exception while working with file: " + workPath.getPath(), ex);
                    return;
                }
            }
        }
    }

    protected void fileCreated(File f) {
        redeploy(f);
    }

    protected void fileDeleted(File f) {
        if (f.exists() && !acceptFile(f)) return;
        String fileName = f.getName();
        fileName.hashCode();
        Throwable ex = null;
        Deployment d;
        synchronized (this) {
            d = this.deployments.get(fileName);
            if (d != null) {
                try {
                    d.unregister();
                } catch (final Throwable t) {
                    ex = t;
                }
            }
        }
        if (d != null) {
            if (ex == null) {
                try {
                    d.undeploy(false);
                } catch (final Throwable t) {
                    getLogger().log(Level.WARNING, "I/O exception while undeploying file: " + d.deployFile, t);
                }
            } else {
                getLogger().log(Level.WARNING, "File not undeployed because of exception: " + d.deployFile, ex);
            }
        }
    }

    protected void fileModified(File f) {
        redeploy(f);
    }

    protected synchronized void installZipFile(MBeanServer mbeanServer, Deployment mlet, File workFile) throws Exception {
        if (workFile.getName().endsWith(".ear")) installEAR(mbeanServer, mlet, workFile); else installJAR(mbeanServer, mlet, workFile);
    }

    private void installEAR(MBeanServer mbeanServer, Deployment mlet, File workFile) throws Exception {
        extract(workFile);
        File[] files = workFile.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (isZipFile(f)) {
                    files[i] = f = f.getAbsoluteFile();
                    mlet.addURL(f.toURL());
                } else {
                    files[i] = null;
                }
            }
            File f = new File(workFile, MLET_FILE_PATH);
            if (f.isFile()) {
                mlet.addMLetURL(f.toURL());
            }
            for (int i = 0; i < files.length; i++) {
                f = files[i];
                if (f != null) installZipFile(mbeanServer, mlet, f);
            }
        }
    }

    private void installJAR(MBeanServer mbeanServer, Deployment mlet, File workFile) throws Exception {
        JarFile jarFile = new JarFile(workFile);
        try {
            JarEntry jarEntry = jarFile.getJarEntry(MLET_JAR_PATH);
            boolean result;
            if (jarEntry == null) {
                result = false;
            } else {
                File extractedMLetPath = new File(workFile.getParentFile(), workFile.getName() + ".mlet");
                extract(jarFile, jarEntry, extractedMLetPath);
                mlet.addMLetURL(extractedMLetPath.toURL());
                result = true;
            }
            jarFile.close();
            jarFile = null;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (final Throwable ex) {
                }
            }
        }
    }

    @MBeanOperation
    public void forceFileAlterationScan() throws IOException {
        FileAlterationMonitor monitor = this.fileMonitor;
        if (monitor != null) monitor.forceFileAlterationScan();
    }

    @MBeanAttribute(persistent = true, string = true)
    public File getDeploymentDirectory() {
        return this.deployDir;
    }

    @MBeanAttribute(persistent = true)
    public long getFileLockTimeoutMillis() {
        return this.fileLockTimeoutMillis;
    }

    @MBeanAttribute
    public ObjectName getParentClassLoaderName() {
        return this.parentClassLoaderName;
    }

    @MBeanAttribute(persistent = true)
    public long getScanDelayMillis() {
        return this.scanDelay;
    }

    @MBeanAttribute(persistent = true, string = true)
    public File getWorkDirectory() {
        return this.workDir;
    }

    @MBeanOperation()
    public String info() {
        Deployment[] a;
        synchronized (this) {
            a = this.deployments.values().toArray(new Deployment[this.deployments.size()]);
        }
        StringBuilder sb = new StringBuilder(a.length * 48);
        sb.append(a.length).append(" files").append("\n\n");
        for (Deployment d : a) {
            sb.append("File        : ").append(d.deployFile.getPath()).append('\n');
            sb.append("LastModified: ").append(d.modTime).append('\n');
            sb.append("MLet        : ").append(d.mbeanName).append('\n');
            sb.append('\n');
        }
        return sb.toString();
    }

    @MBeanAttribute
    public boolean isDelegatingToClassLoaderRepository() {
        return this.delegateToClassLoaderRepository;
    }

    @Override
    protected synchronized void postDeregister(LocalMBeanReference mbeanRef, int countRegistrations) throws Exception {
        super.postDeregister(mbeanRef, countRegistrations);
        if (countRegistrations != 0) return;
        DefaultFileAlterationMonitor fileMonitor = this.fileMonitor;
        if (fileMonitor != null) {
            this.fileMonitor = null;
            fileMonitor.removeFileAlterationListener(this.fileAlterationHandler);
        }
        String[] fileNames = this.deployments.keySet().toArray(new String[this.deployments.size()]);
        boolean haveFailed = false;
        for (int i = fileNames.length; --i >= 0; ) {
            String fileName = fileNames[i];
            fileNames[i] = null;
            Deployment d = this.deployments.get(fileName);
            if (d != null) {
                try {
                    d.unregister();
                    this.deployments.remove(fileName);
                } catch (final Throwable ex) {
                    haveFailed = true;
                    getLogger().warning("Unable to unregister MLet: " + d.mbeanName);
                    continue;
                }
            }
        }
        fileNames = null;
        if (haveFailed) {
            fileMonitor = null;
            getLogger().warning("Keeping files in work directory because was unable to stop all MBeans: " + this.workDir);
        } else {
            this.deployments = new LinkedHashMap<String, Deployment>(0);
            if (fileMonitor != null) {
                fileMonitor = null;
                clearWorkDirectory();
            }
        }
        this.accessControlContext = null;
        this.parentClassLoader = null;
    }

    @Override
    protected synchronized void postRegister(LocalMBeanReference mbeanRef, int countRegistrations) throws Exception {
        super.postRegister(mbeanRef, countRegistrations);
        if (countRegistrations != 1) return;
        File deployDir = this.deployDir;
        if (deployDir.isDirectory()) {
            File[] files = deployDir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    files[i] = null;
                    f = f.getAbsoluteFile();
                    fileCreated(f);
                }
            }
        }
        AccessControlContext context = this.accessControlContext;
        if (context == null) context = AccessController.getContext(); else this.accessControlContext = null;
        this.fileMonitor = new DefaultFileAlterationMonitor(getScanDelayMillis(), TimeUnit.MILLISECONDS);
        this.fileMonitor.addFileAlterationListener(this.fileAlterationHandler, this.deployDir, FileAlterationMonitorType.DIRECTORY);
        this.initialDeploy = false;
        for (Deployment d : this.deployments.values()) d.startMBeans();
    }

    @Override
    public synchronized ObjectName preRegister(MBeanServer mbeanServer, ObjectName name) throws Exception {
        boolean initial = getMBeanServerUnsafe() == null;
        name = super.preRegister(mbeanServer, name);
        if (initial) {
            if (this.parentClassLoaderName != null) this.parentClassLoader = getMBeanServerUnsafe().getClassLoader(this.parentClassLoaderName);
            if (this.accessControlContext == null) {
                preRegister0();
            } else {
                this.accessControlContext = AccessController.getContext();
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction() {

                        public Object run() throws IOException {
                            preRegister0();
                            return null;
                        }
                    });
                } catch (final PrivilegedActionException ex) {
                    throw ex.getException();
                }
            }
        }
        return name;
    }

    private void preRegister0() throws IOException {
        File deployDir = this.deployDir;
        File workDir = this.workDir;
        if (this.deployDir == null) throw new IllegalStateException("Deployment directory is not set.");
        if (this.workDir == null) throw new IllegalStateException("Work directory is not set.");
        if (workDir.isDirectory()) {
            clearWorkDirectory();
        } else {
            if (!workDir.mkdirs()) throw new IOException("Unable to create work directory: " + workDir);
        }
    }

    @MBeanAttribute
    public void setDeploymentDirectory(File dir) {
        CheckArg.notNull(dir, "dir");
        synchronized (this) {
            if (!Objects.equals(this.deployDir, dir)) {
                if ((this.workDir != null) && dir.getAbsoluteFile().equals(this.workDir.getAbsoluteFile())) throw new IllegalArgumentException("Deployment directory equal to working directory: " + dir);
                if (this.fileMonitor != null) throw new IllegalStateException("Can not change directory while running");
                this.deployDir = dir;
            }
        }
    }

    @MBeanAttribute()
    public void setFileLockTimeoutMillis(long timeout) {
        CheckArg.notNegative(timeout, "timeout");
        synchronized (this) {
            this.fileLockTimeoutMillis = timeout;
        }
    }

    @MBeanAttribute()
    public void setScanDelayMillis(long scanPeriod) {
        CheckArg.notNegative(scanPeriod, "scanPeriod");
        synchronized (this) {
            if (scanPeriod != this.scanDelay) {
                DefaultFileAlterationMonitor fileMonitor = this.fileMonitor;
                if (fileMonitor != null) fileMonitor.setScanDelay(scanPeriod, TimeUnit.MILLISECONDS);
                this.scanDelay = scanPeriod;
            }
        }
    }

    @MBeanAttribute()
    public void setWorkDirectory(File dir) {
        CheckArg.notNull(dir, "dir");
        synchronized (this) {
            if (!Objects.equals(this.workDir, dir)) {
                if ((this.deployDir != null) && dir.getAbsoluteFile().equals(this.deployDir.getAbsoluteFile())) throw new IllegalArgumentException("Deployment directory equal to working directory: " + dir);
                if (getMBeanServerUnsafe() != null) throw new IllegalStateException("Can not change directory while running");
                if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("Unable to create directory: " + dir);
                this.workDir = dir;
            }
        }
    }

    @MBeanOperation()
    public boolean undeploy(String fileName) throws IOException, MBeanRegistrationException {
        CheckArg.notNull(fileName, "fileName");
        synchronized (this) {
            Deployment d = this.deployments.get(fileName);
            if (d == null) {
                return false;
            } else {
                d.unregister();
                try {
                    d.undeploy(true);
                    this.deployments.remove(fileName);
                    return true;
                } catch (final IOException ex) {
                    this.deployments.remove(fileName);
                    getLogger().log(Level.WARNING, "I/O exception while undeploying file: " + d.deployFile, ex);
                    return true;
                }
            }
        }
    }

    /**
   * A {@code MBean} representing a file deployed by a {@code DeploymentDirectoryMonitor}.
   *
   * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
   * @since   JaXLib 1.0
   * @version 1.0
   */
    private final class Deployment extends MLet implements DeploymentMBean {

        final LinkedHashSet<URL> mletFileURLs = new LinkedHashSet<URL>();

        final File deployFile;

        final File workFile;

        final long modTime;

        final ObjectName mbeanName;

        private boolean installed;

        private MBeanServer mbeanServer;

        private ObjectName[] childObjectNames;

        Deployment(ClassLoader parentLoader, URL url, File deployFile, File workFile, long timestamp) throws MalformedObjectNameException {
            super(new URL[] { url }, parentLoader);
            this.mbeanName = ObjectName.getInstance(DeploymentDirectoryMonitor.this.getMBeanName().toString() + ",filename=" + ObjectName.quote(workFile.getName()));
            this.deployFile = deployFile;
            this.workFile = workFile;
            this.modTime = timestamp;
        }

        private void unregisterChildMBeans() {
            ObjectName[] childObjectNames = this.childObjectNames;
            if (childObjectNames != null) {
                this.childObjectNames = null;
                MBeanServer mbeanServer = this.mbeanServer;
                if (mbeanServer != null) {
                    for (int i = childObjectNames.length; --i >= 0; ) {
                        ObjectName childObjectName = childObjectNames[i];
                        childObjectNames[i] = null;
                        try {
                            mbeanServer.unregisterMBean(childObjectName);
                        } catch (final InstanceNotFoundException ex) {
                        } catch (final Throwable ex) {
                            DeploymentDirectoryMonitor.this.getLogger().log(Level.SEVERE, "Exception while unregistering MBean:" + "\n  ObjectName      = " + childObjectName + "\n  Deployment file = " + this.deployFile, ex);
                        }
                    }
                }
            }
        }

        void undeploy(boolean deleteDeployFile) throws IOException {
            HashSet<URL> closedJarURLs = new HashSet<URL>();
            for (URL url : getURLs()) {
                if ("jar".equals(url.getProtocol())) {
                    try {
                        URLConnection connection = url.openConnection();
                        if (connection instanceof JarURLConnection) {
                            JarURLConnection jarConnection = (JarURLConnection) connection;
                            if (closedJarURLs.add(jarConnection.getJarFileURL())) {
                                JarFile jarFile = jarConnection.getJarFile();
                                if (jarFile != null) jarFile.close();
                            }
                        }
                    } catch (final Exception ex) {
                        DeploymentDirectoryMonitor.this.getLogger().log(Level.SEVERE, "Exception while closing deployment file:" + "\n  Deployment file = " + this.deployFile + "\n  Work       file = " + this.workFile, ex);
                    }
                }
            }
            boolean deletedWorkFile = Files.delete(this.workFile);
            boolean deletedDeployFile = !deleteDeployFile || (deletedWorkFile && Files.delete(this.deployFile));
            if (!deletedWorkFile) throw new IOException("Unable to delete work file: " + this.workFile);
            if (!deletedDeployFile) throw new IOException("Unable to delete deployment file: " + this.deployFile);
        }

        void unregister() throws MBeanRegistrationException {
            MBeanServer mbeanServer = this.mbeanServer;
            if (mbeanServer != null) {
                try {
                    mbeanServer.unregisterMBean(this.mbeanName);
                    this.mbeanServer = null;
                } catch (final InstanceNotFoundException ex) {
                    unregisterChildMBeans();
                    this.mbeanServer = null;
                } catch (final MBeanRegistrationException ex) {
                    unregisterChildMBeans();
                    if (mbeanServer.isRegistered(this.mbeanName)) {
                        throw ex;
                    } else {
                        this.mbeanServer = null;
                    }
                } finally {
                    if (!mbeanServer.isRegistered(this.mbeanName)) {
                        synchronized (DeploymentDirectoryMonitor.this) {
                            DeploymentDirectoryMonitor.this.deployments.remove(this.deployFile);
                        }
                    }
                }
            }
        }

        synchronized void addMLetURL(URL url) {
            CheckArg.notNull(url, "url");
            if (this.mletFileURLs.add(url)) {
                Logger logger = DeploymentDirectoryMonitor.this.getLogger();
                if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "MLet found: " + url);
            }
        }

        void startMBeans() {
            if (DeploymentDirectoryMonitor.this.initialDeploy) return;
            synchronized (this) {
                if (this.mbeanServer == null) return;
                URL[] urls = this.mletFileURLs.toArray(new URL[this.mletFileURLs.size()]);
                this.mletFileURLs.clear();
                ArrayList<ObjectName> childObjectNames = new ArrayList<ObjectName>();
                for (int i = 0; i < urls.length; i++) {
                    URL url = urls[i];
                    urls[i] = null;
                    try {
                        Set set = getMBeansFromURL(url);
                        for (Object e : set) {
                            if (e instanceof Throwable) {
                                Logger logger = DeploymentDirectoryMonitor.this.getLogger();
                                logger.log(Level.WARNING, "Exception creating MBean, mlet url=" + url, (Throwable) e);
                            } else {
                                ObjectInstance objectInstance = (ObjectInstance) e;
                                childObjectNames.add(objectInstance.getObjectName());
                            }
                        }
                    } catch (final Throwable ex) {
                        Logger logger = DeploymentDirectoryMonitor.this.getLogger();
                        logger.log(logLevelFor(ex), "Exception while installing MBeans from: " + url, ex);
                    }
                }
                this.childObjectNames = childObjectNames.toArray(new ObjectName[childObjectNames.size()]);
            }
        }

        @Override
        public synchronized void postDeregister() {
            super.postDeregister();
            unregisterChildMBeans();
            String name = this.workFile.getName();
            name.hashCode();
            synchronized (DeploymentDirectoryMonitor.this) {
                if (DeploymentDirectoryMonitor.this.deployments.get(name) == this) {
                    DeploymentDirectoryMonitor.this.deployments.remove(name);
                    try {
                        if (!Files.delete(this.workFile)) {
                            DeploymentDirectoryMonitor.this.getLogger().warning("Unable to delete work path: " + this.workFile);
                        }
                    } catch (final Throwable ex) {
                        DeploymentDirectoryMonitor.this.getLogger().severe("Exception while deleting work path: " + this.workFile);
                    }
                }
                this.mbeanServer = null;
            }
        }

        @Override
        public synchronized void postRegister(Boolean registrationDone) {
            if (this.mbeanServer == null) throw new IllegalStateException("Not registered.");
            if (this.installed) return;
            super.postRegister(registrationDone);
        }

        @Override
        public synchronized void preDeregister() throws Exception {
            super.preDeregister();
            unregisterChildMBeans();
            undeploy(false);
        }

        @Override
        public synchronized ObjectName preRegister(MBeanServer mbeanServer, ObjectName objectName) throws Exception {
            if (mbeanServer == null) throw new NullPointerException("mbeanServer");
            if (this.mbeanServer != null) throw new IllegalStateException("Already registered");
            if (!this.mbeanName.equals(objectName)) throw new IllegalArgumentException("ObjectName must be " + this.mbeanName);
            super.preRegister(mbeanServer, this.mbeanName);
            this.mbeanServer = mbeanServer;
            return objectName;
        }
    }

    /**
   * A {@code MBean} representing a file deployed by a {@code DeploymentDirectoryMonitor}.
   *
   * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
   * @since   JaXLib 1.0
   * @version 1.0
   */
    public static interface DeploymentMBean extends MLetMBean {
    }

    private final class FileAlterationHandler extends Object implements FileAlterationListener {

        FileAlterationHandler() {
            super();
        }

        public void fileAltered(FileAlterationEvent e) {
            File f = e.getFile().getAbsoluteFile();
            switch(e.getFileAlterationType()) {
                case CREATED:
                    DeploymentDirectoryMonitor.this.fileCreated(f);
                    break;
                case DELETED:
                    DeploymentDirectoryMonitor.this.fileDeleted(f);
                    break;
                case MODIFIED:
                    DeploymentDirectoryMonitor.this.fileModified(f);
                    break;
                default:
                    throw new AssertionError(e.getFileAlterationType());
            }
        }
    }
}
