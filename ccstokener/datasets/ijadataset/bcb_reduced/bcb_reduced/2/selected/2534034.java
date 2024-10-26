package net.sf.buildbox.util;

import net.sf.buildbox.util.exec.SimpleExec;
import org.codehaus.plexus.util.*;
import org.codehaus.plexus.util.cli.CommandLineException;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

public class BbxSystemUtils {

    /**
     * Same as {@link org.codehaus.plexus.util.FileUtils#byteCountToDisplaySize(int)} but with param of type long.
     * @param size -
     * @return human readable expression of string
     */
    public static String byteCountToDisplaySize(long size) {
        if (size / org.codehaus.plexus.util.FileUtils.ONE_GB > 0) {
            return String.valueOf(size / org.codehaus.plexus.util.FileUtils.ONE_GB) + " GB";
        }
        if (size / org.codehaus.plexus.util.FileUtils.ONE_MB > 0) {
            return String.valueOf(size / org.codehaus.plexus.util.FileUtils.ONE_MB) + " MB";
        }
        if (size / org.codehaus.plexus.util.FileUtils.ONE_KB > 0) {
            return String.valueOf(size / org.codehaus.plexus.util.FileUtils.ONE_KB) + " KB";
        }
        return String.valueOf(size) + " bytes";
    }

    public static final FileFilter UNIQUE_AND_PRESENT_FILTER = new FileFilter() {

        private final Set<String> used = new HashSet<String>();

        public boolean accept(File f) {
            return f.exists() && used.add(f.getAbsolutePath());
        }
    };

    /**
     * Filters classpath using standard {@link FileFilter}.
     * @param path  the classpath to be filtered
     * @param filter  the filter to be evaluated
     * @return filtered classpath
     */
    public static String filterPath(String path, FileFilter filter) {
        final StringBuilder sb = new StringBuilder();
        for (StringTokenizer tok = new StringTokenizer(path, File.pathSeparator); tok.hasMoreTokens(); ) {
            final String cpe = tok.nextToken();
            if (!filter.accept(new File(cpe))) continue;
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(cpe);
        }
        return sb.toString();
    }

    public static String replacePathElement(String classPath, File newPathElement, final String... matchingSubpaths) {
        final StringBuilder sb = new StringBuilder(newPathElement.getAbsolutePath());
        final String reducedPath = filterPath(classPath, new FileFilter() {

            public boolean accept(File cpe) {
                if (!UNIQUE_AND_PRESENT_FILTER.accept(cpe)) return false;
                for (String subpath : matchingSubpaths) {
                    final File f = new File(cpe, subpath);
                    if (f.exists()) return false;
                }
                return true;
            }
        });
        if (!reducedPath.equals("")) {
            sb.append(File.pathSeparatorChar);
            sb.append(newPathElement);
        }
        return sb.toString();
    }

    public static String replaceJavaHome(String path, File newJavaHome) {
        return replacePathElement(path, newJavaHome, "bin/java", "bin\\java.exe");
    }

    public static String createClasspathString(List<File> classpath) {
        final Set<String> alreadyAdded = new HashSet<String>();
        final StringBuilder sb = new StringBuilder();
        for (File classPathElement : classpath) {
            final String cpeStr = classPathElement.getAbsolutePath();
            if (alreadyAdded.contains(cpeStr)) continue;
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(cpeStr);
            alreadyAdded.add(cpeStr);
        }
        return sb.toString();
    }

    /**
     * @deprecated no replacement available
     */
    @Deprecated
    public static void printStackTrace(SystemUtils.OutputEater eater, Throwable e) {
        if (eater != null) {
            eater.stderr(e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                eater.stderr(stackTraceElement.toString());
            }
        } else {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Lists all service implementations registered in META-INF/services/${serviceType} resources.
     *
     * @param serviceType  interface/abstract type representing the service
     * @param classLoader  the classloader to use for lookup and instantiation
     * @return list of service implementation classes
     * @throws java.io.IOException -
     * @throws ClassNotFoundException -
     */
    @SuppressWarnings("unchecked")
    public static <T extends Class> Collection<T> listServices(T serviceType, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        final Collection<T> result = new LinkedHashSet<T>();
        final Enumeration<URL> resouces = classLoader.getResources("META-INF/services/" + serviceType.getName());
        while (resouces.hasMoreElements()) {
            final URL url = resouces.nextElement();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            try {
                String line = reader.readLine();
                while (line != null) {
                    if (line.startsWith("#")) {
                    } else if ("".equals(line.trim())) {
                    } else {
                        final T implClass = (T) Class.forName(line, true, classLoader);
                        if (!serviceType.isAssignableFrom(implClass)) {
                            throw new IllegalStateException(String.format("%s: class %s does not implement required interfafce %s", url, implClass, serviceType));
                        }
                        result.add(implClass);
                    }
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
        }
        return result;
    }

    /**
     * Creates services represented by given class collection
     * @param serviceClasses  classes as returned by {@link net.sf.buildbox.util.BbxSystemUtils#listServices(Class, ClassLoader)}
     * @param argClasses  constructor argument types
     * @param args constructor argument values, must match argClasses
     * @return created instances
     * @throws NoSuchMethodException -
     * @throws java.lang.reflect.InvocationTargetException -
     * @throws IllegalAccessException -
     * @throws InstantiationException -
     */
    public static <T> Collection<T> createServices(Collection<Class<T>> serviceClasses, Class[] argClasses, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final Collection<T> services = new ArrayList<T>(serviceClasses.size());
        for (Class<T> serviceClass : serviceClasses) {
            final Constructor<T> con = serviceClass.getConstructor(argClasses);
            final T service = con.newInstance(args);
            services.add(service);
        }
        return services;
    }

    public static <T> Collection<T> createServices(Collection<Class<T>> serviceClasses) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return createServices(serviceClasses, new Class[0], new Object[0]);
    }

    public static void symlink(String reference, File file) throws CommandLineException {
        final BbxCommandline cl = new BbxCommandline();
        cl.setWorkingDirectory(file.getParentFile());
        cl.setExecutable("ln");
        cl.addArguments(new String[] { "-s", reference, file.getAbsolutePath() });
        final SimpleExec exec = new SimpleExec(cl);
        System.out.println(String.format("ln -s %s %s", reference, file.getAbsolutePath()));
        exec.call();
    }

    public static void storeProperties(Properties properties, File propertyFileName, String comment) throws IOException {
        final OutputStream os = new FileOutputStream(propertyFileName);
        try {
            properties.storeToXML(os, comment);
        } finally {
            os.close();
        }
    }

    public static String relpath(String absPath, File refDirectory) {
        final String ref = refDirectory.getAbsolutePath();
        if (absPath.startsWith(ref)) {
            return absPath.substring(ref.length() + 1);
        }
        return absPath;
    }

    public static void deletePathAndItsEmptyParents(File path) {
        while (path.delete()) {
            path = path.getParentFile();
        }
    }
}
