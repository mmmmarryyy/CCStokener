package org.gdi3d.xnavi.appletlauncher;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class loader able to load classes and DLLs with a higher priority from a given set of JARs.
 * Its bytecode is Java 1.1 compatible to be loadable by old JVMs.
 * @author Emmanuel Puybaret, Arne Schilling
 */
public class ApplicationClassLoader extends ClassLoader {

    private final ProtectionDomain protectionDomain;

    private HashSet<String> applicationPackageSet;

    private JarFile[] extensionJars = null;

    private Map<String, String> nativeLibMap;

    /**
	 * Creates a class loader. It will consider JARs and DLLs of <code>extensionJarsAndDlls</code>
	 * as classpath and libclasspath elements with a higher priority than the ones of default classpath,
	 * and will load itself all the classes belonging to packages of <code>applicationPackages</code>.
	 * @param jnlpAppletLauncher
	 * @throws InterruptedException
	 */
    public ApplicationClassLoader(ClassLoader parent, ProtectionDomain protectionDomain, JarFile[] extensionJars, Map<String, String> nativeLibMap) throws InterruptedException {
        super(parent);
        this.protectionDomain = protectionDomain;
        this.extensionJars = extensionJars;
        this.nativeLibMap = nativeLibMap;
        applicationPackageSet = new HashSet<String>();
        if (extensionJars != null && extensionJars.length > 0) {
            parseExtensionPackages();
        }
    }

    protected void parseExtensionPackages() {
        if (this.extensionJars != null) {
            for (int i = 0; i < this.extensionJars.length; i++) {
                JarFile extensionJar = this.extensionJars[i];
                Enumeration<JarEntry> entries = extensionJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String entryName = jarEntry.getName();
                    if (!entryName.endsWith("/")) {
                        String applicationClassPackageBase = "";
                        StringTokenizer tok = new StringTokenizer(entryName, "/");
                        int numTokens = tok.countTokens();
                        int c = 0;
                        while (tok.hasMoreTokens() && c < (numTokens - 1)) {
                            String part = tok.nextToken();
                            if (c > 0) applicationClassPackageBase += ".";
                            applicationClassPackageBase += part;
                            c++;
                        }
                        if (!applicationPackageSet.contains(applicationClassPackageBase)) {
                            applicationPackageSet.add(applicationClassPackageBase);
                        }
                    }
                }
            }
        }
    }

    /**
	 * Finds and defines the given class among the extension JARs
	 * given in constructor, then among resources.
	 */
    protected Class findClass(String name) throws ClassNotFoundException {
        String classFile = name.replace('.', '/') + ".class";
        InputStream classInputStream = null;
        if (this.extensionJars != null) {
            for (int i = 0; i < this.extensionJars.length; i++) {
                JarFile extensionJar = this.extensionJars[i];
                JarEntry jarEntry = extensionJar.getJarEntry(classFile);
                if (jarEntry != null) {
                    try {
                        classInputStream = extensionJar.getInputStream(jarEntry);
                        if (classInputStream != null) {
                            break;
                        }
                    } catch (IOException ex) {
                        throw new ClassNotFoundException("Couldn't read class " + name, ex);
                    }
                }
            }
        }
        if (classInputStream == null) {
            URL url = getResource(classFile);
            if (url == null) {
                throw new ClassNotFoundException("Class " + name);
            }
            try {
                classInputStream = url.openStream();
            } catch (IOException ex) {
                throw new ClassNotFoundException("Couldn't read class " + name, ex);
            }
        } else {
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BufferedInputStream in = new BufferedInputStream(classInputStream);
            byte[] buffer = new byte[8192];
            int size;
            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
            }
            in.close();
            return defineClass(name, out.toByteArray(), 0, out.size(), this.protectionDomain);
        } catch (IOException ex) {
            throw new ClassNotFoundException("Class " + name, ex);
        }
    }

    /**
	 * Returns the library path of an extension DLL.
	 */
    protected String findLibrary(String libname) {
        return (String) this.nativeLibMap.get(libname);
    }

    /**
	 * Returns the URL of the given resource searching first if it exists among
	 * the extension JARs given in constructor.
	 */
    protected URL findResource(String name) {
        if (this.extensionJars != null) {
            for (int i = 0; i < this.extensionJars.length; i++) {
                JarFile extensionJar = this.extensionJars[i];
                JarEntry jarEntry = extensionJar.getJarEntry(name);
                if (jarEntry != null) {
                    try {
                        String urls = "jar:file:///" + extensionJar.getName() + "!/" + jarEntry.getName();
                        return new URL(urls);
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return super.findResource(name);
    }

    /**
	 * Loads a class with this class loader if its package belongs to <code>applicationPackages</code>
	 * given in constructor.
	 */
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (this.extensionJars == null) {
            return super.loadClass(name, resolve);
        }
        Class loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                String classPackage = "";
                StringTokenizer tok = new StringTokenizer(name, ".");
                int numTokens = tok.countTokens();
                if (numTokens > 1) {
                    int c = 0;
                    while (tok.hasMoreTokens() && c < (numTokens - 1)) {
                        String part = tok.nextToken();
                        if (c > 0) classPackage += ".";
                        classPackage += part;
                        c++;
                    }
                }
                if (applicationPackageSet.contains(classPackage)) {
                    loadedClass = findClass(name);
                    if (loadedClass == null) {
                        System.out.println("ERROR: " + name + " not found by ExtensionsClassLoader. Using deafault class loader");
                    }
                }
            } catch (ClassNotFoundException ex) {
            }
            if (loadedClass == null) {
                loadedClass = super.loadClass(name, resolve);
            }
        } else {
        }
        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }

    public static void loadLibrary(String libraryName) {
        System.out.println("ExtensionsClassLoader.loadLibrary b������������������������������h!");
    }
}
