package net.sourceforge.jaulp.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import net.sourceforge.jaulp.file.FileUtils;
import net.sourceforge.jaulp.file.filter.MultiplyExtensionsFileFilter;

/**
 * Utility class for the use Resource object.
 *
 * @version 1.0
 * @author Asterios Raptis
 */
public class ResourceUtils {

    /** The Constant PROPERTIES_SUFFIX. */
    public static final String PROPERTIES_SUFFIX = ".properties";

    /**
     * Gets the all annotated classes that belongs from the given package path and the given annotation class.
     *
     * @param packagePath the package path
     * @param annotationClass the annotation class
     * @return the all classes
     * @throws ClassNotFoundException the class not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Set<Class<?>> getAllAnnotatedClasses(String packagePath, Class<? extends Annotation> annotationClass) throws ClassNotFoundException, IOException {
        List<File> directories = ResourceUtils.getDirectoriesFromResources(packagePath, true);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (File directory : directories) {
            classes.addAll(scanForAnnotatedClasses(directory, packagePath, annotationClass));
        }
        return classes;
    }

    /**
     * Gets the all annotated classes that belongs from the given package path and the given list with annotation classes.
     *
     * @param packagePath the package path
     * @param annotationClasses the list with the annotation classes
     * @return the all classes
     * @throws ClassNotFoundException the class not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Set<Class<?>> getAllAnnotatedClassesFromSet(String packagePath, Set<Class<? extends Annotation>> annotationClasses) throws ClassNotFoundException, IOException {
        List<File> directories = ResourceUtils.getDirectoriesFromResources(packagePath, true);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (File directory : directories) {
            classes.addAll(scanForAnnotatedClassesFromSet(directory, packagePath, annotationClasses));
        }
        return classes;
    }

    /**
     * Gets all the classes from the class loader that belongs to the given package path.
     *
     * @param packagePath the package path
     *
     * @return the all classes
     *
     * @throws ClassNotFoundException the class not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Set<Class<?>> getAllClasses(String packagePath) throws ClassNotFoundException, IOException {
        return getAllAnnotatedClasses(packagePath, null);
    }

    /**
     * Gets all the classes from the class loader that belongs to the given package path.
     *
     * @param packagePath the package path
     * @param annotationClasses the annotation classes
     * @return the all classes
     * @throws ClassNotFoundException the class not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Set<Class<?>> getAllClasses(String packagePath, Set<Class<? extends Annotation>> annotationClasses) throws ClassNotFoundException, IOException {
        return getAllAnnotatedClassesFromSet(packagePath, annotationClasses);
    }

    /**
     * Gets the current class loader.
     *
     * @return 's the current class loader
     */
    public static ClassLoader getClassLoader() {
        return ResourceUtils.getClassLoader(null);
    }

    /**
	 * Gets the ClassLoader from the given object.
	 *
	 * @param obj The object.
	 *
	 * @return the ClassLoader from the given object.
	 */
    public static ClassLoader getClassLoader(final Object obj) {
        ClassLoader classLoader = null;
        if (null != obj) {
            if (isDerivate(Thread.currentThread().getContextClassLoader(), obj.getClass().getClassLoader())) {
                classLoader = obj.getClass().getClassLoader();
            } else {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (isDerivate(classLoader, ClassLoader.getSystemClassLoader())) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        } else {
            if (isDerivate(Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader())) {
                classLoader = ClassLoader.getSystemClassLoader();
            } else {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
        }
        return classLoader;
    }

    /**
     * Gets the classname and concats the suffix ".class" from the object.
     *
     * @param obj The object.
     *
     * @return The classname and concats the suffix ".class".
     */
    public static String getClassnameWithSuffix(final Object obj) {
        String className = obj.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1) + ".class";
        return className;
    }

    /**
     * Gets the directories from the given path.
     *
     * @param path the path
     *
     * @return the directories from resources
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static List<File> getDirectoriesFromResources(String path) throws IOException {
        List<URL> resources = ResourceUtils.getResources(path);
        List<File> dirs = new ArrayList<File>();
        for (URL resource : resources) {
            dirs.add(new File(URLDecoder.decode(resource.getFile(), "UTF-8")));
        }
        return getDirectoriesFromResources(path, false);
    }

    /**
     * Gets the directories from the given path.
     *
     * @param path the path
     * @param isPackage If the Flag is true than the given path is a package.
     *
     * @return the directories from resources
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static List<File> getDirectoriesFromResources(String path, boolean isPackage) throws IOException {
        if (isPackage) {
            path = path.replace('.', '/');
        }
        List<URL> resources = ResourceUtils.getResources(path);
        List<File> dirs = new ArrayList<File>();
        for (URL resource : resources) {
            dirs.add(new File(URLDecoder.decode(resource.getFile(), "UTF-8")));
        }
        return dirs;
    }

    /**
     * Determines the package path from the given class object.
     *
     * @param clazz The class object.
     *
     * @return The package path from the given class object.
     */
    public static String getPackagePath(final Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        final String packagePath = clazz.getPackage().getName().replace('.', '/') + "/";
        return packagePath;
    }

    /**
     * Determines the package path from the given object.
     *
     * @param object The object.
     *
     * @return The package path from the given object.
     */
    public static String getPackagePath(final Object object) {
        if (object == null) {
            return null;
        }
        return getPackagePath(object.getClass());
    }

    /**
     * Determines the package path from the given object and adds a slash at the
     * front.
     *
     * @param clazz the clazz
     * @return The package path from the given object with the added slash at the
     * front.
     */
    public static String getPackagePathWithSlash(final Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        final String packagePath = "/" + getPackagePath(clazz);
        return packagePath;
    }

    /**
     * Determines the package path from the given object and adds a slash at the
     * front.
     *
     * @param object The object.
     *
     * @return The package path from the given object with the added slash at the
     * front.
     */
    public static String getPackagePathWithSlash(final Object object) {
        if (object == null) {
            return null;
        }
        return getPackagePathWithSlash(object.getClass());
    }

    /**
     * Finds the absolute path from the object.
     *
     * @param obj The object.
     *
     * @return The absolute path from the object.
     */
    public static String getPathFromObject(final Object obj) {
        if (obj == null) {
            return null;
        }
        String pathFromObject = obj.getClass().getResource(getClassnameWithSuffix(obj)).getPath();
        return pathFromObject;
    }

    /**
     * Gives the url from the path back.
     *
     * @param clazz The class-object.
     * @param path The path.
     *
     * @return 's the url from the path.
     */
    @SuppressWarnings("unchecked")
    public static URL getResource(final Class clazz, final String path) {
        URL url = clazz.getResource(path);
        if (null == url) {
            url = getClassLoader().getResource(path);
        }
        return url;
    }

    /**
     * Gives the URL from the resource. Wrapes the
     * Class.getResource(String)-method.
     *
     * @param name The name from the resource.
     *
     * @return The resource or null if the resource does not exists.
     */
    public static URL getResource(final String name) {
        URL url = ResourceUtils.class.getResource(name);
        return url;
    }

    /**
     * Gives the URL from the resource. Wrapes the
     * Class.getResource(String)-method.
     *
     * @param <T> the < t>
     * @param name The name from the resource.
     * @param obj The Object.
     * @return The resource or null if the resource does not exists.
     */
    public static <T> URL getResource(final String name, final T obj) {
        final Class<?> clazz = obj.getClass();
        URL url = clazz.getResource(name);
        if (url == null) {
            url = getResource(clazz, name);
        }
        return url;
    }

    /**
     * Gives the resource as a file.
     *
     * @param name The name from the file.
     * @param obj The Object.
     *
     * @return The file or null if the file does not exists.
     */
    public static File getResourceAsFile(final String name, final Object obj) {
        File file = null;
        URL url = ResourceUtils.getResource(name, obj);
        if (null == url) {
            url = getClassLoader(obj).getResource(name);
            if (null != url) {
                try {
                    file = new File(url.toURI());
                } catch (final URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                file = new File(url.toURI());
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * This method call the getResourceAsStream from the ClassLoader. You can
     * use this method to read files from jar-files.
     *
     * @param clazz the clazz
     * @param uri The uri as String.
     * @return The InputStream from the uri.
     */
    public static InputStream getResourceAsStream(final Class<?> clazz, final String uri) {
        InputStream is = clazz.getResourceAsStream(uri);
        if (null == is) {
            is = ResourceUtils.getClassLoader().getResourceAsStream(uri);
        }
        return is;
    }

    /**
     * Gives the Inputstream from the resource. Wrapes the
     * Class.getResourceAsStream(String)-method.
     *
     * @param name The name from the resource.
     *
     * @return The resource or null if the resource does not exists.
     */
    public static InputStream getResourceAsStream(final String name) {
        InputStream inputStream = ResourceUtils.class.getResourceAsStream(name);
        if (null == inputStream) {
            ClassLoader loader = getClassLoader();
            inputStream = loader.getResourceAsStream(name);
        }
        return inputStream;
    }

    /**
     * Gives the Inputstream from the resource. Wrapes the
     * Class.getResourceAsStream(String)-method.
     *
     * @param name The name from the resource.
     * @param obj The Object.
     *
     * @return The resource or null if the resource does not exists.
     */
    public static InputStream getResourceAsStream(final String name, final Object obj) {
        InputStream inputStream = obj.getClass().getResourceAsStream(name);
        if (null == inputStream) {
            ClassLoader loader = getClassLoader(obj);
            inputStream = loader.getResourceAsStream(name);
        }
        return inputStream;
    }

    /**
     * Gets a list with urls from the given path for all resources.
     *
     * @param path The base path.
     *
     * @return The resources.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static List<URL> getResources(String path) throws IOException {
        ClassLoader classLoader = ResourceUtils.getClassLoader();
        List<URL> list = Collections.list(classLoader.getResources(path));
        return list;
    }

    /**
     * Compares the two given ClassLoader objects and returns true if compare is a derivate of source.
     *
     * @param source the source
     * @param compare the compare
     *
     * @return true, if compare is a derivate of source.
     */
    public static boolean isDerivate(final ClassLoader source, ClassLoader compare) {
        if (source == compare) {
            return true;
        }
        if (compare == null) {
            return false;
        }
        if (source == null) {
            return true;
        }
        while (null != compare) {
            compare = compare.getParent();
            if (source == compare) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives a Properties-object from the given packagepath.
     *
     * @param packagePath The package-path and the name from the resource as a String.
     *
     * @return The Properties-object from the given packagepath.
     */
    public static Properties loadProperties(final String packagePath) {
        final Properties properties = new Properties();
        final URL url = ResourceUtils.getResource(packagePath);
        if (null != url) {
            try {
                properties.load(url.openStream());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    /**
     * Load the properties file from the  given class object. The filename from the properties file is the same
     * as the simple name from the class object and it looks at the same path as the given class object. If locale is
     * not null than the language will be added to the filename from the properties file.
     *
     * @param clazz the clazz
     * @param locale the locale
     * @return the properties
     */
    public static Properties loadPropertiesFromClassObject(Class<?> clazz, Locale locale) {
        if (null == clazz) {
            throw new IllegalArgumentException("Class object must not be null!!!");
        }
        StringBuilder propertiesName = new StringBuilder();
        Properties properties = null;
        String language = null;
        String filename = null;
        String pathAndFilename = null;
        File propertiesFile = null;
        String absoluteFilename = null;
        String packagePath = ResourceUtils.getPackagePathWithSlash(clazz);
        if (null != locale) {
            propertiesName.append(clazz.getSimpleName());
            language = locale.getLanguage();
            if (null != language && !language.isEmpty()) {
                propertiesName.append("_" + language);
            }
            String country = locale.getCountry();
            if (null != country && !country.isEmpty()) {
                propertiesName.append("_" + country);
            }
            propertiesName.append(PROPERTIES_SUFFIX);
            filename = propertiesName.toString().trim();
            pathAndFilename = packagePath + filename;
            absoluteFilename = ResourceUtils.getResource(clazz, filename).getFile();
            if (null != absoluteFilename) {
                propertiesFile = new File(absoluteFilename);
            }
            if (null != propertiesFile && propertiesFile.exists()) {
                properties = ResourceUtils.loadProperties(pathAndFilename);
            } else {
                propertiesName = new StringBuilder();
                if (null != locale) {
                    propertiesName.append(clazz.getSimpleName());
                    language = locale.getLanguage();
                    if (null != language && !language.isEmpty()) {
                        propertiesName.append("_" + language);
                    }
                    propertiesName.append(PROPERTIES_SUFFIX);
                    filename = propertiesName.toString().trim();
                    pathAndFilename = packagePath + filename;
                    absoluteFilename = ResourceUtils.getResource(clazz, filename).getFile();
                    if (null != absoluteFilename) {
                        propertiesFile = new File(absoluteFilename);
                    }
                    if (null != propertiesFile && propertiesFile.exists()) {
                        properties = ResourceUtils.loadProperties(pathAndFilename);
                    }
                }
            }
        }
        if (null == properties) {
            propertiesName.append(clazz.getSimpleName() + PROPERTIES_SUFFIX);
            filename = propertiesName.toString().trim();
            pathAndFilename = packagePath + filename;
            absoluteFilename = ResourceUtils.getResource(clazz, filename).getFile();
            propertiesFile = new File(absoluteFilename);
            if (null != absoluteFilename) {
                propertiesFile = new File(absoluteFilename);
            }
            if (null != propertiesFile && propertiesFile.exists()) {
                properties = ResourceUtils.loadProperties(pathAndFilename);
            }
        }
        return properties;
    }

    /**
     * Scan recursive for annotated classes in the given directory.
     *
     * @param directory the directory
     * @param packagePath the package path
     * @param annotationClass the annotation class
     * @return the list
     * @throws ClassNotFoundException the class not found exception
     */
    public static Set<Class<?>> scanForAnnotatedClasses(File directory, String packagePath, Class<? extends Annotation> annotationClass) throws ClassNotFoundException {
        Set<Class<?>> foundClasses = new HashSet<Class<?>>();
        if (!directory.exists()) {
            return foundClasses;
        }
        FileFilter includeFileFilter = new MultiplyExtensionsFileFilter(Arrays.asList(new String[] { ".class" }), true);
        File[] files = directory.listFiles(includeFileFilter);
        for (File file : files) {
            String qualifiedClassname = null;
            if (file.isDirectory()) {
                qualifiedClassname = packagePath + "." + file.getName();
                foundClasses.addAll(scanForAnnotatedClasses(file, qualifiedClassname, annotationClass));
            } else {
                String filename = FileUtils.getFilenameWithoutExtension(file);
                qualifiedClassname = packagePath + '.' + filename;
                Class<?> foundClass = null;
                try {
                    foundClass = Class.forName(qualifiedClassname);
                    if (null != annotationClass) {
                        if (foundClass.isAnnotationPresent(annotationClass)) {
                            foundClasses.add(foundClass);
                        }
                    } else {
                        foundClasses.add(foundClass);
                    }
                } catch (Throwable throwable) {
                    foundClass = Class.forName(qualifiedClassname, false, ResourceUtils.getClassLoader());
                    if (null != annotationClass) {
                        if (foundClass.isAnnotationPresent(annotationClass)) {
                            foundClasses.add(foundClass);
                        }
                    } else {
                        foundClasses.add(foundClass);
                    }
                }
            }
        }
        return foundClasses;
    }

    /**
     * Scan recursive for annotated classes in the given directory.
     *
     * @param directory the directory
     * @param packagePath the package path
     * @param annotationClasses the list with the annotation classes
     * @return the list
     * @throws ClassNotFoundException the class not found exception
     */
    public static Set<Class<?>> scanForAnnotatedClassesFromSet(File directory, String packagePath, Set<Class<? extends Annotation>> annotationClasses) throws ClassNotFoundException {
        Set<Class<?>> foundClasses = new HashSet<Class<?>>();
        if (!directory.exists()) {
            return foundClasses;
        }
        FileFilter includeFileFilter = new MultiplyExtensionsFileFilter(Arrays.asList(new String[] { ".class" }), true);
        File[] files = directory.listFiles(includeFileFilter);
        for (File file : files) {
            String qualifiedClassname = null;
            if (file.isDirectory()) {
                qualifiedClassname = packagePath + "." + file.getName();
                foundClasses.addAll(scanForAnnotatedClassesFromSet(file, qualifiedClassname, annotationClasses));
            } else {
                String filename = FileUtils.getFilenameWithoutExtension(file);
                qualifiedClassname = packagePath + '.' + filename;
                Class<?> foundClass = null;
                try {
                    foundClass = Class.forName(qualifiedClassname);
                    if (null != annotationClasses) {
                        for (Iterator<Class<? extends Annotation>> iterator2 = annotationClasses.iterator(); iterator2.hasNext(); ) {
                            Class<? extends Annotation> annotationClass = iterator2.next();
                            if (foundClass.isAnnotationPresent(annotationClass)) {
                                foundClasses.add(foundClass);
                            }
                        }
                    } else {
                        foundClasses.add(foundClass);
                    }
                } catch (Throwable throwable) {
                    foundClass = Class.forName(qualifiedClassname, false, ResourceUtils.getClassLoader());
                    if (null != annotationClasses) {
                        for (Iterator<Class<? extends Annotation>> iterator2 = annotationClasses.iterator(); iterator2.hasNext(); ) {
                            Class<? extends Annotation> annotationClass = iterator2.next();
                            if (foundClass.isAnnotationPresent(annotationClass)) {
                                foundClasses.add(foundClass);
                            }
                        }
                    } else {
                        foundClasses.add(foundClass);
                    }
                }
            }
        }
        return foundClasses;
    }

    /**
     * Scan recursive for classes in the given directory.
     *
     * @param directory the directory
     * @param packagePath the package path
     * @return the list
     * @throws ClassNotFoundException the class not found exception
     */
    public static Set<Class<?>> scanForClasses(File directory, String packagePath) throws ClassNotFoundException {
        return scanForAnnotatedClasses(directory, packagePath, null);
    }
}
