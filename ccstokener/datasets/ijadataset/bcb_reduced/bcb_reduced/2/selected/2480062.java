package seevolution;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class loader able to load classes and DLLs with a higher priority from a given set of JARs. 
 * It's Java 1.1 compatible to be loadable by old JVMs.
 * @author Emmanuel Puybaret
 */
public class ExtensionsClassLoader extends ClassLoader {

    private final ProtectionDomain protectionDomain;

    private final String[] applicationPackages;

    private final Map extensionDlls = new HashMap();

    private JarFile[] extensionJars = null;

    /**
   * Creates a class loader. It will consider JARs and DLLs of <code>extensionJarsAndDlls</code>
   * as classpath and libclasspath elements with a higher priority than the ones of default classpath, 
   * and will load itself all the classes belonging to packages of <code>applicationPackages</code>.
   */
    public ExtensionsClassLoader(ClassLoader parent, ProtectionDomain protectionDomain, String[] extensionJarsAndDlls, String[] applicationPackages) {
        super(parent);
        this.protectionDomain = protectionDomain;
        this.applicationPackages = applicationPackages;
        String dllSuffix;
        String dllPrefix;
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            dllSuffix = ".dll";
            dllPrefix = "";
        } else if (os.startsWith("Mac OS X")) {
            dllSuffix = ".jnilib";
            dllPrefix = "lib";
        } else {
            dllSuffix = ".so";
            dllPrefix = "lib";
        }
        ArrayList extensionJars = new ArrayList();
        for (int i = 0; i < extensionJarsAndDlls.length; i++) {
            String extensionJarOrDll = extensionJarsAndDlls[i];
            try {
                URL extensionJarOrDllUrl = getResource(extensionJarOrDll);
                if (extensionJarOrDllUrl != null) {
                    if (extensionJarOrDll.endsWith(".jar")) {
                        String extensionJar = copyInputStreamToTmpFile(extensionJarOrDllUrl.openStream(), ".jar");
                        extensionJars.add(new JarFile(extensionJar, false));
                    } else if (extensionJarOrDll.endsWith(dllSuffix)) {
                        String extensionDll = copyInputStreamToTmpFile(extensionJarOrDllUrl.openStream(), dllSuffix);
                        this.extensionDlls.put(extensionJarOrDll.substring(dllPrefix.length(), extensionJarOrDll.indexOf(dllSuffix)), extensionDll);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException("Couldn't extract extension jars", ex);
            }
        }
        if (extensionJars.size() > 0) {
            this.extensionJars = (JarFile[]) extensionJars.toArray(new JarFile[extensionJars.size()]);
        }
    }

    /**
   * Returns the file name of a temporary copy of <code>input</code> content.
   */
    private String copyInputStreamToTmpFile(InputStream input, String suffix) throws IOException {
        File tmpFile = File.createTempFile("extension", suffix);
        tmpFile.deleteOnExit();
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(tmpFile));
            byte[] buffer = new byte[8096];
            int size;
            while ((size = input.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
        return tmpFile.toString();
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
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BufferedInputStream in = new BufferedInputStream(classInputStream);
            byte[] buffer = new byte[8096];
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
   * Returns a the library path of an extension DLL.
   */
    protected String findLibrary(String libname) {
        return (String) this.extensionDlls.get(libname);
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
                        return new URL("jar:file:" + extensionJar.getName() + ":" + jarEntry.getName());
                    } catch (MalformedURLException ex) {
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
                for (int i = 0; i < this.applicationPackages.length; i++) {
                    String applicationPackage = this.applicationPackages[i];
                    if (name.startsWith(applicationPackage)) {
                        loadedClass = findClass(name);
                        break;
                    }
                }
            } catch (ClassNotFoundException ex) {
            }
            if (loadedClass == null) {
                loadedClass = super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }
}
