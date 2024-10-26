package org.indi.nativelib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loader for the rxtx native libraries.
 * 
 * @author Richard van Nieuwenhoven
 */
public class NativeLibraryLoader {

    /**
     * the logger for errors and warnings.
     */
    private static final Log LOG = LogFactory.getLog(NativeLibraryLoader.class);

    /**
     * The standard java library path property to change.
     */
    private static final String JAVA_LIBRARY_PATH = "java.library.path";

    /**
     * Directory to save the library locally.
     */
    private static final String LOCAL_DIRECTORY_PATH = System.getProperty("user.home") + File.separator + ".indi4java" + File.separator;

    /**
     * Property file stored in jars that identify the libraries.
     */
    public static final String NATIVE_LIBRARY_PROPERTY_FILE = "META-INF/libraries.properties";

    /**
     * OS and architecture identification of the current runtime system.
     */
    public static final String OS_ARCH;

    static {
        String os_name = System.getProperty("os.name").toLowerCase().replace(' ', '_');
        String os_arch = System.getProperty("os.arch").toLowerCase().replace(' ', '_');
        if (os_name.startsWith("Windows ")) {
            os_name = "win32";
        }
        if (os_arch.indexOf("86") >= 0) {
            if (os_arch.indexOf("64") >= 0) {
                os_arch = "x86_64";
            } else {
                os_arch = "x86";
            }
        }
        OS_ARCH = os_name + "-" + os_arch;
    }

    /**
     * Load the named native library into the system and extend the library
     * paths.
     * 
     * @param classloader
     *            the classloader to use.
     * @param libName
     *            the name of the library
     * @return true if the loading was successful.
     */
    public static final synchronized boolean loadLibrary(ClassLoader classloader, String libName) {
        try {
            HashMap<String, String> libMap = collectLibrariesInClasspath(classloader);
            String libraryInClasspath = libMap.get(libName + "." + OS_ARCH);
            if (libraryInClasspath != null) {
                String fileLibPath = copyResourceToLocalFilesystem(classloader, libraryInClasspath);
                System.load(fileLibPath);
                return true;
            }
        } catch (Exception e) {
            LOG.error("Could not load library deu to exception", e);
        }
        return false;
    }

    /**
     * Copy the resource file from the classpath to the local file system.
     * 
     * @param classloader
     *            the classloader to use
     * @param resourcePath
     *            the resource to copy
     * @return the copy of the resource in the local file system.
     * @throws IOException
     *             when the copy could not be made.
     */
    private static final synchronized String copyResourceToLocalFilesystem(ClassLoader classloader, String resourcePath) throws IOException {
        File resourceFile = new File(LOCAL_DIRECTORY_PATH + resourcePath);
        File parentFile = resourceFile.getParentFile();
        if (parentFile == null) {
            return null;
        }
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        resourceFile.delete();
        URL url = classloader.getResource(resourcePath);
        if (url == null) {
            return null;
        }
        InputStream classpathInputStream = url.openStream();
        OutputStream fileOutputStream = new FileOutputStream(resourceFile);
        byte[] buffer = new byte[1024];
        int readByteCount;
        while ((readByteCount = classpathInputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, readByteCount);
        }
        classpathInputStream.close();
        fileOutputStream.flush();
        fileOutputStream.close();
        extendLibPath(parentFile.toString());
        return resourceFile.toString();
    }

    /**
     * @param classloader
     *            the class loader to scan for native libraries.
     * @return A HashMap mapping library names to paths for every os and arch.
     */
    private static final HashMap<String, String> collectLibrariesInClasspath(ClassLoader classloader) throws IOException {
        HashMap<String, String> libMap = new HashMap<String, String>();
        Enumeration<URL> nativeLibPropertyFiles = classloader.getResources(NATIVE_LIBRARY_PROPERTY_FILE);
        while (nativeLibPropertyFiles.hasMoreElements()) {
            Properties p = new Properties();
            p.load(nativeLibPropertyFiles.nextElement().openStream());
            for (Object key : p.keySet()) {
                libMap.put((String) key, (String) p.get(key));
            }
        }
        return libMap;
    }

    /**
     * Extend the system library path of java with the specified path. This is a
     * hack but but necessary.
     * 
     * @param path
     *            the path to add to the library path.
     */
    private static void extendLibPath(String path) {
        String javaLibraryPath = System.getProperty(JAVA_LIBRARY_PATH);
        String pathExtention = File.pathSeparator + path;
        if (javaLibraryPath.indexOf(pathExtention) < 0) {
            try {
                Class<ClassLoader> clazz = ClassLoader.class;
                Field field = clazz.getDeclaredField("sys_paths");
                boolean accessible = field.isAccessible();
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(clazz, null);
                System.setProperty(JAVA_LIBRARY_PATH, javaLibraryPath + pathExtention);
                field.setAccessible(accessible);
            } catch (Exception e) {
                LOG.error("could not expand lib path");
            }
        }
    }
}
