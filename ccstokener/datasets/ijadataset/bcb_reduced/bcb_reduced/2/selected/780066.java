package gumbo.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * Utilities related to the Java language and its standard packages, in general.
 * Specialized groups of Java related utilities may be in separate utility classes.
 * @author jonb
 * @see StringUtils
 * @see TimeUtils
 * @see TypeUtils
 */
public class JavaUtils {

    private JavaUtils() {
    }

    /**
	 * Returns true if two object references test equal.
	 * @param objA Temp input object. Possibly null.
	 * @param objB Temp input object. Possibly null.
	 * @return True if objA and objB are null, or if they test
	 * as equal (objA.equals(objB)).
	 */
    public static boolean equals(Object objA, Object objB) {
        if (objA == objB) return true;
        if (objA == null || objB == null) return false;
        return objA.equals(objB);
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(char valA, char valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(byte valA, byte valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(short valA, short valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(int valA, int valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(long valA, long valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(float valA, float valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 */
    public static int compare(double valA, double valB) {
        if (valA < valB) return -1;
        if (valA > valB) return 1;
        return 0;
    }

    /**
	 * Compares the two values, returning -1, 0, or 1 depending on whether
	 * valA is <, =, or > valB.
	 * @return The result.
	 * @throws IllegalArgumentException if valA or valB is null or not Comparable.
	 */
    public static <T extends Comparable<T>> int compare(T valA, T valB) {
        AssertUtils.assertNonNullArg(valA);
        AssertUtils.assertNonNullArg(valB);
        return valA.compareTo(valB);
    }

    /**
	 * Returns true if an object is assignable to a reference of the specified
	 * type (the object is null or an instance of the type).
	 * @param obj Temp input object. Possibly null.
	 * @param type The type. Never null.
	 * @return True if obj is null, or if type.isInstance() is true.
	 */
    public static boolean assignableTo(Object obj, Class<?> type) {
        if (obj == null) return true;
        return type.isInstance(obj);
    }

    /**
	 * Returns a string with the "identity" hash code of an object. The format
	 * is "@???", where ??? is the hash code in fixed-width hex. Safe to use
	 * even if the target is null or disposed.
	 * @param The object. If null, returns "(null)".
	 * @return The result. Never null.
	 */
    public static String toHashCode(Object obj) {
        String string;
        if (obj == null) {
            string = "(null)";
        } else {
            int code = System.identityHashCode(obj);
            string = String.format("@%08x", code);
        }
        return string;
    }

    /**
	 * Returns a string with the "reference identity" of an object, consisting
	 * of the fully qualified type and the "identity" hash code (the same as
	 * Object.toString() but without any object override of toString() or
	 * hashCode()). The format is "type@code". Returns "(null)" if the object is
	 * null. Safe to use even if the target is null or disposed.
	 * @param obj The object. If null, returns "(null)".
	 * @return The result. Never null.
	 */
    public static String toString(Object obj) {
        if (obj == null) return "(null)";
        return obj.getClass().getName() + toHashCode(obj);
    }

    /**
	 * Similar to toString() but the class name is unqualified (returns the
	 * remainder of toString() after the last "."). Returns "(null)" if the
	 * object is null. Safe to use even if the target is null or disposed.
	 * @param obj The object. If null, returns "(null)".
	 * @return The result. Never null.
	 * @see #toString(Object)
	 */
    public static String toShortString(Object obj) {
        if (obj == null) return "(null)";
        String string = toString(obj);
        int index = string.lastIndexOf('.');
        return string.substring(index + 1);
    }

    /**
	 * Returns the fully qualified class name of an object. Returns "(null)" if
	 * the object is null. Safe to use even if the target is null or disposed.
	 * @param obj The object. If null, returns "(null)".
	 * @return The result. Never null.
	 */
    public static String toType(Object obj) {
        if (obj == null) return "(null)";
        return obj.getClass().getName();
    }

    /**
	 * Similar to toType() but the class name is unqualified (returns the
	 * remainder of toType() after the last "."). Returns "(null)" if obj is
	 * null. Safe to use even if the target is null or disposed.
	 * @param obj Target object. If null, returns "(null)".
	 * @return The string. Never null.
	 */
    public static String toShortType(Object obj) {
        if (obj == null) return "(null)";
        String string = toType(obj);
        int index = string.lastIndexOf('.');
        return string.substring(index + 1);
    }

    /**
	 * Returns the fully qualified class name of a class. Returns "(null)" if
	 * the class is null. Safe to use even if the target is null or disposed.
	 * @param type The type. If null, returns "(null)".
	 * @return The result. Never null.
	 */
    public static String toClass(Class<?> type) {
        if (type == null) return "(null)";
        return type.getName();
    }

    /**
	 * Similar to toClass() but the class name is unqualified (returns the
	 * remainder of toClass() after the last "."). Returns "(null)" if class is
	 * null. Safe to use even if the target is null or disposed.
	 * @param obj Target object. If null, returns "(null)".
	 * @return The string. Never null.
	 */
    public static String toShortClass(Class<?> type) {
        if (type == null) return "(null)";
        String string = toClass(type);
        int index = string.lastIndexOf('.');
        return string.substring(index + 1);
    }

    /**
	 * Sets the logging level for all loggers accessible via
	 * LogManager.getLogManager(). By default, the logging level is set to
	 * report all messages. Typically, the level should be set to Level.SEVERE.
	 */
    public static void setLoggingLevel(Level level) {
        Enumeration<String> loggers = LogManager.getLogManager().getLoggerNames();
        while (loggers.hasMoreElements()) {
            String name = loggers.nextElement();
            Logger logger = LogManager.getLogManager().getLogger(name);
            logger.setLevel(level);
        }
    }

    /**
	 * Makes the current thread sleep for a specified number of milliseconds. If
	 * an interrupt exception occurs sleep is terminated, the stack trace is is
	 * printed to stderr, but nothing else happens.
	 * @param msec Duration of sleep (msec). If non-positive does nothing.
	 */
    public static void sleep(int msec) {
        if (msec <= 0) return;
        try {
            Thread.sleep(msec);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Makes the current thread sleep, with messages to stdout at the beginning
	 * and end of the sleep period. If an interrupt exception occurs its stack
	 * trace is is printed to stderr.
	 * @param msec Duration of sleep (msec). If non-positive does nothing.
	 * @param host The host object, which is incorporated into the sleep
	 * messages. None if null.
	 */
    public static void sleep(int msec, Object host) {
        if (msec <= 0) return;
        try {
            if (host != null) System.out.println(CoreUtils.string(host, "Going to sleep..."));
            Thread.sleep(msec);
            if (host != null) System.out.println(CoreUtils.string(host, "...waking up!"));
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Finds all classes inheriting or implementing a given base class in the
	 * currently loaded packages.
	 * <p>
	 * Derived from
	 * http://www.javaworld.com/javaworld/javatips/jw-javatip113.html
	 * @param baseClassName The base class name. Never null.
	 * @return Set of base class subclasses. Never null.
	 */
    public static Collection<Class<?>> findSubClasses(String baseClassName) {
        AssertUtils.assertNonNullArg(baseClassName);
        try {
            Class<?> baseClass = Class.forName(baseClassName);
            Collection<Class<?>> subClasses = new ArrayList<Class<?>>();
            Package[] pkgs = Package.getPackages();
            for (Package pkg : pkgs) {
                subClasses.addAll(findSubClasses(pkg.getName(), baseClass));
            }
            return subClasses;
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Can't find base class. base=" + baseClassName);
        }
    }

    /**
	 * Finds all classes inheriting or implementing a given base
	 * class in a given package in the classpath (file system or jar file).
	 * <p>
	 * Derived from
	 * http://www.javaworld.com/javaworld/javatips/jw-javatip113.html
	 * @param pkgName The fully qualified name of the package. Never null.
	 * @param baseClassName The base class name. Never null.
	 * @return Set of base class subclasses. Never null.
	 */
    public static Collection<Class<?>> findSubClasses(String pkgName, String baseClassName) {
        AssertUtils.assertNonNullArg(pkgName);
        AssertUtils.assertNonNullArg(baseClassName);
        try {
            Class<?> baseClass = Class.forName(baseClassName);
            return findSubClasses(pkgName, baseClass);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Can't find base class. class=" + baseClassName);
        }
    }

    /**
	 * Finds all classes inheriting or implementing a given base
	 * class in a given package in the classpath (file system or jar file).
	 * <p>
	 * Derived from
	 * http://www.javaworld.com/javaworld/javatips/jw-javatip113.html
	 * @param pkgName The fully qualified name of the package. Never null.
	 * @param baseClass The base class. Never null.
	 * @return Set of base class subclasses. Never null.
	 */
    public static Collection<Class<?>> findSubClasses(String pkgName, Class<?> baseClass) {
        AssertUtils.assertNonNullArg(pkgName);
        AssertUtils.assertNonNullArg(baseClass);
        String pkgPath = new String(pkgName);
        if (!pkgPath.startsWith("/")) {
            pkgPath = "/" + pkgPath;
        }
        pkgPath = pkgPath.replace('.', '/');
        URL url = JavaUtils.class.getResource(pkgPath);
        if (url == null) {
            throw new IllegalStateException("Can't find package path." + " It may be in a jar with missing ancestor packages. path=" + pkgPath);
        }
        File directory = new File(url.getFile());
        Collection<Class<?>> subClasses = new ArrayList<Class<?>>();
        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    String className = files[i].substring(0, files[i].length() - 6);
                    String subClassName = pkgName + "." + className;
                    try {
                        Class<?> subClass = Class.forName(subClassName);
                        if (baseClass.isInstance(subClass)) {
                            subClasses.add(subClass);
                        }
                    } catch (ClassNotFoundException cnfex) {
                        throw new IllegalStateException("Can't find sub class file. class=" + subClassName);
                    }
                }
            }
        } else {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                String starts = conn.getEntryName();
                JarFile jfile = conn.getJarFile();
                Enumeration<JarEntry> e = jfile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String entryname = entry.getName();
                    if (entryname.startsWith(starts) && (entryname.lastIndexOf('/') <= starts.length()) && entryname.endsWith(".class")) {
                        String subClassName = entryname.substring(0, entryname.length() - 6);
                        if (subClassName.startsWith("/")) subClassName = subClassName.substring(1);
                        subClassName = subClassName.replace('/', '.');
                        try {
                            Class<?> subClass = Class.forName(subClassName);
                            if (baseClass.isInstance(subClass)) {
                                subClasses.add(subClass);
                            }
                        } catch (ClassNotFoundException cnfex) {
                            throw new IllegalStateException("Can't find sub class jar entry. class=" + subClassName);
                        }
                    }
                }
            } catch (IOException ioex) {
                throw new IllegalStateException("Can't find jar file. url=" + url);
            }
        }
        return subClasses;
    }

    /**
	 * Prints a prompt to standard out, and then blocks until the user enters
	 * input and hits the enter key via standard in. If the user hits ctrl+Z
	 * returns null, which typically indicates that the user wants to quit the
	 * application.
	 * <p>
	 * Note that, once called, stdin will block until the enter key or ctrl+Z is
	 * hit. There is NO way to reliably interrupt or close stdin
	 * programmatically except by exiting the app.
	 * @param prompt Prompt string. None if null.
	 * @return The input string. Null if Ctrl+Z.
	 * @throws IllegalStateException if there is any problem.
	 */
    public static String getUserInput(String prompt) {
        if (prompt != null) {
            System.out.print(prompt);
        }
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String input = stdin.readLine();
            if (input == null) {
                return null;
            } else {
                return input;
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
