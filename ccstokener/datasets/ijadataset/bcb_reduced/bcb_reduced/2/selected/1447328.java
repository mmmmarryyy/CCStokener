package tuner3d;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This bootstrap class loads Sweet Home 3D application classes from standard
 * jars in classpath or from jars stored in the jar file containing j3dcore.jar.
 * 
 * @author Emmanuel Puybaret
 */
public class ApplicationBootstrap {

    public static void main(String[] args) throws Exception {
        Class applicationBootstrapClass = ApplicationBootstrap.class;
        String[] applicationPackages = { "tuner3d", "com.sun.opengl", "com.sun.gluegen.runtime", "javax.media.opengl" };
        ClassLoader java3DClassLoader = new ApplicationClassLoader(applicationBootstrapClass.getClassLoader(), applicationBootstrapClass.getProtectionDomain(), applicationPackages);
        String applicationClassName = "tuner3d.Main";
        Class applicationClass = java3DClassLoader.loadClass(applicationClassName);
        Method applicationClassMain = applicationClass.getMethod("main", Array.newInstance(String.class, 0).getClass());
        applicationClassMain.invoke(null, new Object[] { args });
    }

    /**
	 * Class loader used by this bootstrap class to load all the other
	 * application classes.
	 */
    private static class ApplicationClassLoader extends ClassLoader {

        private final ProtectionDomain protectionDomain;

        private final String[] applicationPackages;

        private final Map<String, String> java3DDLLs = new HashMap<String, String>();

        private JarFile[] java3DJars = null;

        private ApplicationClassLoader(ClassLoader parent, ProtectionDomain protectionDomain, String[] applicationPackages) {
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
            URL java3DCoreUrl = getResource("jogl.jar");
            if (java3DCoreUrl != null && (java3DCoreUrl.toString().startsWith("jar:file") || java3DCoreUrl.toString().startsWith("jar:http"))) {
                try {
                    String java3DFile;
                    String urlString = java3DCoreUrl.toString();
                    if (urlString.startsWith("jar:file:")) {
                        java3DFile = urlString.substring("jar:file:".length(), urlString.indexOf('!'));
                    } else {
                        URL java3DURL = new URL(urlString.substring("jar:".length(), urlString.indexOf('!')));
                        java3DFile = copyInputStreamToTmpFile(java3DURL.openStream(), "jar");
                    }
                    JarFile jarFile = new JarFile(java3DFile, false);
                    ArrayList<JarFile> java3DJars = new ArrayList<JarFile>();
                    for (Enumeration<JarEntry> jarEntryEnum = jarFile.entries(); jarEntryEnum.hasMoreElements(); ) {
                        JarEntry jarEntry = jarEntryEnum.nextElement();
                        String jarEntryName = jarEntry.getName();
                        boolean jarEntryIsAJar = jarEntryName.endsWith(".jar");
                        if (jarEntryIsAJar) {
                            String java3DJar = copyInputStreamToTmpFile(jarFile.getInputStream(jarEntry), ".jar");
                            java3DJars.add(new JarFile(java3DJar, false));
                        } else if (jarEntryName.endsWith(dllSuffix)) {
                            String java3DDLL = copyInputStreamToTmpFile(jarFile.getInputStream(jarEntry), dllSuffix);
                            java3DDLLs.put(jarEntryName.substring(dllPrefix.length(), jarEntryName.indexOf(dllSuffix)), java3DDLL);
                        }
                    }
                    this.java3DJars = java3DJars.toArray(new JarFile[java3DJars.size()]);
                } catch (IOException ex) {
                    throw new RuntimeException("Couldn't extract Java 3D jars", ex);
                }
            }
        }

        /**
		 * Returns the file name of a temporary copy of <code>input</code>
		 * content.
		 */
        private String copyInputStreamToTmpFile(InputStream input, String suffix) throws IOException {
            File java3DJar = File.createTempFile("Java3D", suffix);
            java3DJar.deleteOnExit();
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(java3DJar));
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
            return java3DJar.toString();
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String classFile = name.replace('.', '/') + ".class";
            InputStream classInputStream = null;
            for (JarFile java3DJar : this.java3DJars) {
                JarEntry jarEntry = java3DJar.getJarEntry(classFile);
                if (jarEntry != null) {
                    try {
                        classInputStream = java3DJar.getInputStream(jarEntry);
                    } catch (IOException ex) {
                        throw new ClassNotFoundException("Couldn't read class " + name, ex);
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

        @Override
        protected String findLibrary(String libname) {
            return this.java3DDLLs.get(libname);
        }

        @Override
        protected URL findResource(String name) {
            if (this.java3DJars != null) {
                for (JarFile java3DJar : this.java3DJars) {
                    JarEntry jarEntry = java3DJar.getJarEntry(name);
                    if (jarEntry != null) {
                        try {
                            return new URL("jar:file:" + java3DJar.getName() + ":" + jarEntry.getName());
                        } catch (MalformedURLException ex) {
                        }
                    }
                }
            }
            return super.findResource(name);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (this.java3DJars == null) {
                return super.loadClass(name, resolve);
            }
            Class loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                try {
                    for (String applicationPackage : applicationPackages) {
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
}
