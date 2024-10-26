package edu.indiana.extreme.xbaya.jython.runner;

import edu.indiana.extreme.xbaya.XBayaRuntimeException;
import edu.indiana.extreme.xbaya.XBayaVersion;
import edu.indiana.extreme.xbaya.jython.lib.NotificationSender;
import edu.indiana.extreme.xbaya.util.IOUtil;
import org.python.util.PythonInterpreter;
import xsul5.MLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class loader loads jython related classes without counting on parent
 * class loader. This is because jython related classes use a lot of static
 * fields and cannot be used to invoke Jython scripts multiple times.
 * 
 * @author Satoshi Shirasuna
 */
public class JythonClassLoader extends SecureClassLoader {

    private static final MLogger logger = MLogger.getLogger();

    private ClassLoader parent;

    private Map<String, Class> classes = new HashMap<String, Class>();

    private URL jythonURL;

    private URL xbayaURL;

    private JarFile jythonJarFile;

    private JarFile xbayaJarFile;

    private File tmpJarDirectory;

    /**
     * Constructs a JythonClassLoader.
     * 
     * @param parent
     *            the parent class loader.
     * 
     * This has to be explicitly passed because WebStart applications use
     * user-level class loader. The default system loader cannot load classes in
     * the downloaded jar files.
     */
    public JythonClassLoader(ClassLoader parent) {
        super(parent);
        this.parent = parent;
        this.jythonURL = getBaseURL(PythonInterpreter.class);
        this.xbayaURL = getBaseURL(XBayaVersion.class);
    }

    /**
     * @return XBaya jar file.
     */
    public JarFile getXBayaJarFile() {
        if (this.xbayaJarFile == null) this.xbayaJarFile = maybeGetJarFile(this.xbayaURL);
        return this.xbayaJarFile;
    }

    /**
     * Cleans up temporary files.
     */
    public void cleanUp() {
        this.jythonJarFile = null;
        this.xbayaJarFile = null;
        if (this.tmpJarDirectory != null) {
            try {
                IOUtil.deleteDirectory(this.tmpJarDirectory);
            } catch (RuntimeException e) {
                logger.caught(e);
            }
        }
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return (loadClass(className, false));
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    @Override
    public synchronized Class<?> loadClass(String name, boolean resolveIt) throws ClassNotFoundException {
        Class klass = null;
        try {
            klass = findClass(name);
        } catch (ClassNotFoundException e) {
            try {
                klass = super.loadClass(name, false);
            } catch (ClassNotFoundException e2) {
                klass = this.parent.loadClass(name);
                logger.finest("found from parent, klass: " + klass);
            }
        }
        if (resolveIt) {
            resolveClass(klass);
        }
        return klass;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (this.jythonJarFile == null) this.jythonJarFile = maybeGetJarFile(this.jythonURL);
        if (this.jythonJarFile == null) this.xbayaJarFile = maybeGetJarFile(this.xbayaURL);
        Class klass;
        klass = this.classes.get(name);
        if (klass != null) {
            return klass;
        }
        if (name.startsWith("org.python.")) {
            klass = findClassFromURL(name, this.jythonURL, this.jythonJarFile);
        } else if (name.startsWith(NotificationSender.class.getPackage().getName()) || name.startsWith(JythonOneTimeRunnerImpl.class.getName())) {
            klass = findClassFromURL(name, this.xbayaURL, this.xbayaJarFile);
        }
        if (klass != null) {
            this.classes.put(name, klass);
            return klass;
        } else {
            throw new ClassNotFoundException();
        }
    }

    /**
     * @see java.security.SecureClassLoader#getPermissions(java.security.CodeSource)
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        logger.entering(new Object[] { codesource });
        Permissions permissions = new Permissions();
        AllPermission permission = new AllPermission();
        permissions.add(permission);
        return permissions;
    }

    private URL getBaseURL(Class klass) {
        String path = klass.getName().replace('.', '/').concat(".class");
        URL classURL = this.parent.getResource(path);
        String jarURLString;
        if ("jar".equals(classURL.getProtocol())) {
            String file = classURL.getFile();
            logger.finest("file: " + file);
            jarURLString = file.substring(0, file.lastIndexOf('!'));
        } else {
            String file = classURL.getFile();
            int index = file.lastIndexOf(path);
            jarURLString = "file:" + file.substring(0, index);
        }
        try {
            URL jarURL = new URL(jarURLString);
            logger.exiting(jarURL);
            return jarURL;
        } catch (MalformedURLException e) {
            throw new XBayaRuntimeException(e);
        }
    }

    private JarFile maybeGetJarFile(URL url) {
        String path;
        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new XBayaRuntimeException(e);
        }
        logger.finest("path: " + path);
        if (path.endsWith("/")) {
            return null;
        } else if ("file".equals(url.getProtocol())) {
            try {
                JarFile jarFile = new JarFile(path);
                return jarFile;
            } catch (IOException e) {
                throw new XBayaRuntimeException(e);
            }
        } else {
            try {
                if (this.tmpJarDirectory == null) {
                    Date date = new Date();
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss-S");
                    String time = format.format(date);
                    String fileName = ".xbaya-jars-" + time;
                    String tmpdir = System.getProperty("java.io.tmpdir");
                    this.tmpJarDirectory = new File(tmpdir, fileName);
                    this.tmpJarDirectory.mkdir();
                }
                int i = path.lastIndexOf('/');
                File file = new File(this.tmpJarDirectory, path.substring(i + 1));
                logger.finest("file: " + file);
                InputStream stream = url.openStream();
                IOUtil.writeToFile(stream, file);
                JarFile jarFile = new JarFile(file);
                return jarFile;
            } catch (IOException e) {
                throw new XBayaRuntimeException(e);
            }
        }
    }

    private Class findClassFromURL(String name, URL url, JarFile jarFile) throws ClassNotFoundException {
        String classPath = name.replace('.', '/').concat(".class");
        try {
            byte[] classBytes;
            CodeSource codeSource = null;
            if (jarFile == null) {
                String dirPath = URLDecoder.decode(url.getPath(), "UTF-8");
                File classFile = new File(dirPath, classPath);
                classBytes = IOUtil.readToByteArray(classFile);
            } else {
                JarEntry jarEntry = jarFile.getJarEntry(classPath);
                CodeSigner[] codeSigners = jarEntry.getCodeSigners();
                if (codeSigners != null) {
                    for (CodeSigner signer : codeSigners) {
                        logger.finest("signer: " + signer);
                    }
                }
                codeSource = new CodeSource(this.xbayaURL, codeSigners);
                InputStream classInputStream = jarFile.getInputStream(jarEntry);
                classBytes = IOUtil.readToByteArray(classInputStream);
            }
            Class<?> klass = defineClass(name, classBytes, 0, classBytes.length, codeSource);
            this.classes.put(name, klass);
            return klass;
        } catch (IOException e) {
            logger.caught(e);
            throw new ClassNotFoundException();
        }
    }

    /**
     * @see java.lang.ClassLoader#clearAssertionStatus()
     */
    @Override
    public synchronized void clearAssertionStatus() {
        logger.entering();
        super.clearAssertionStatus();
    }

    /**
     * @see java.lang.ClassLoader#definePackage(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, java.net.URL)
     */
    @Override
    protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
        logger.entering();
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    /**
     * @see java.lang.ClassLoader#findLibrary(java.lang.String)
     */
    @Override
    protected String findLibrary(String libname) {
        logger.entering();
        return super.findLibrary(libname);
    }

    /**
     * @see java.lang.ClassLoader#findResource(java.lang.String)
     */
    @Override
    protected URL findResource(String name) {
        logger.entering();
        return super.findResource(name);
    }

    /**
     * @see java.lang.ClassLoader#findResources(java.lang.String)
     */
    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        logger.entering();
        return super.findResources(name);
    }

    /**
     * @see java.lang.ClassLoader#getPackage(java.lang.String)
     */
    @Override
    protected Package getPackage(String name) {
        logger.entering();
        return super.getPackage(name);
    }

    /**
     * @see java.lang.ClassLoader#getPackages()
     */
    @Override
    protected Package[] getPackages() {
        logger.entering();
        return super.getPackages();
    }

    /**
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String name) {
        logger.entering();
        return super.getResource(name);
    }

    /**
     * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        logger.entering();
        return super.getResourceAsStream(name);
    }

    /**
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        logger.entering();
        return super.getResources(name);
    }

    /**
     * @see java.lang.ClassLoader#setClassAssertionStatus(java.lang.String,
     *      boolean)
     */
    @Override
    public synchronized void setClassAssertionStatus(String className, boolean enabled) {
        logger.entering();
        super.setClassAssertionStatus(className, enabled);
    }

    /**
     * @see java.lang.ClassLoader#setDefaultAssertionStatus(boolean)
     */
    @Override
    public synchronized void setDefaultAssertionStatus(boolean enabled) {
        logger.entering();
        super.setDefaultAssertionStatus(enabled);
    }

    /**
     * @see java.lang.ClassLoader#setPackageAssertionStatus(java.lang.String,
     *      boolean)
     */
    @Override
    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled) {
        logger.entering();
        super.setPackageAssertionStatus(packageName, enabled);
    }
}
