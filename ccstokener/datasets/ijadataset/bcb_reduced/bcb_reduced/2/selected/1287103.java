package mipt.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import mipt.reflect.MethodCall;

/**
 * Quick utility functions working with files, typical messages, system resources, etc.
 * If Apache Commons IO is in classpath work with files are implemented via it.
 * Is not Singleton because functions do not form some self-consistent behavior
 *  so there is no place of behavior modification typical to Singleton pattern.
 * Depends on UIUtils (in runtime) only on showError (but it's automatically
 *  replaced by showing in console if mipt.gui is not available)
 * @author Evdokimov
 */
public class Utils implements Const {

    public static final ResourceBundle bundle = ResourceBundle.getBundle("mipt.common.Bundle");

    private static Method showError;

    public static class Console {

        public static void showError(String message) {
            System.err.println(message);
        }
    }

    /**
	 * The same as System.getProperty() but works also in applets
	 *  (return Windows-specific values for commonly used properties -
	 *  file, path and line separators)
	 * @see java.lang.System.getProperty(String)
	 */
    public static String getSystemProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (AccessControlException e) {
            if (key.endsWith("separator")) {
                if (key.startsWith("line")) return "\r\n"; else if (key.startsWith("file")) return "\\"; else if (key.startsWith("path")) return ";";
            } else if (key.equals("user.dir")) return "C:"; else if (key.equals("os.name")) return "Windows NT";
            return null;
        }
    }

    /**
	 * Returns resource from file or jar
	 * Is substitute for ClassLoader.getSystemResource(path)
	 *  and clazz.getResource(shortResourceName). Avoids the problem of different
	 *  resource getting in applet's and application's  environments. 
	 * @param path - path from ROOT directory/package but it does not starts with '/';
	 *   separated by '/' as directory/package seperator
	 * @param yourClass  can't be a class from JRE because they often does not have classLoader 
	 */
    public static URL getResource(String path, Class yourClass) {
        URL url = ClassLoader.getSystemResource(path);
        if (url != null) return url;
        return yourClass.getResource(path.startsWith("/") ? path : "/" + path);
    }

    /**
	 * IF YOUR PATH CAN CONTAIN "./" or "../" EXPRESSIONS (any number of "../"),
	 * use this method as argument of {@link #getResource(String, Class)} or of your own call
	 *  to {@link ClassLoader#getSystemResource(String)} .
	 * "./" makes sense for non-class resourcePoints (without "./" path is considered as starting in user.dir).
	 * NOTE: This implementation assumes that "./" or "../" can be in the beginning of path only.
	 * @param path The relative (with respect to resourcePoint) path
	 * @param resourcePoint A {@link Class} or a directory ({@link String}) to load resource with/from.
	 *     If null, "user.dir" is used as the point. May start with "/" or may not; may be absolute path.
	 * @return The normal (relative to ClassLoader only!) path, without ".?/" but starting with "/"
	 *  (if ".?/" was in the path and if resourcePoint is class).
	 */
    public static String getPathNormal(String path, Object resourcePoint) {
        int j = path.indexOf("../");
        if (j > 0) throw new IllegalArgumentException("Utils.getPathNormal() does not support converting ../../ if .. is not the first");
        if (j < 0 && !path.startsWith("./")) return path;
        String point;
        boolean cls = false;
        if (resourcePoint == null) point = userDir; else if (resourcePoint instanceof Class) {
            cls = true;
            point = getPackage((Class) resourcePoint);
        } else point = resourcePoint.toString();
        if (j < 0) {
            if (cls) return '/' + point + path.substring(1);
            point = point.replace('\\', '/');
            point = point.substring(0, point.lastIndexOf('/'));
            return point + path.substring(1);
        }
        String[] points;
        if (cls) points = point.split("\\."); else {
            point = point.replace('\\', '/');
            point = point.substring(0, point.lastIndexOf('/'));
            points = point.split("/");
        }
        int n = points.length;
        j = 0;
        int len = 0;
        while (path.startsWith("../")) {
            j++;
            if (n < j) throw new IllegalArgumentException("The number of levels in your resourse point (" + point + ") is less than number of \"../\" in the relative path (" + path + ")");
            len += (points[n - j].length() + 1);
            path = path.substring(3);
        }
        StringBuffer rootPath = new StringBuffer(len + path.length() + 1);
        n = n - j;
        for (int i = 0; i < n; i++) {
            rootPath.append(points[i]);
            rootPath.append("/");
        }
        if (cls) rootPath.insert(0, "/");
        rootPath.append(path);
        return rootPath.toString();
    }

    /**
	 * Returns the name of the package of the given class (with ending "."!)
	 * Is useful as there is no getPackageName() in java.lang.Class, and getPackage() method
	 *  often returns null!
	 * Is often used to form resources paths; use getPackage(*).replace('.','/') in this case.
	 * If class has no package, returns "" (not null)
	 */
    public static String getPackage(Class cls) {
        String pack = cls.getName();
        int j = pack.lastIndexOf('.');
        if (j < 0) return "";
        return pack.substring(0, j + 1);
    }

    /**
	 * Returns resource stream from file or jar
	 * @see getResource(java.lang.String, java.lang.Class) 
	 * @param path - path from ROOT directory/package but it does not starts with '/';
	 *   separated by '/' as directory/package seperator
	 * @param yourClass  can't be a class from JRE because they often does not have classLoader 
	 */
    public static InputStream getResourceAsStream(String path, Class yourClass) throws IOException {
        URL url = getResource(path, yourClass);
        if (url == null) throw new IOException("Can't find resource: " + path);
        return url.openStream();
    }

    /**
	 * The method is public since somebody may want more advanced util methods than in this Utils.
	 * Use this method if your arguments have the same type as declared in the method.
	 * Use only void FileUtils' methods via this method.
	 * @param className - full class name
	 * @param methodName - name of public STATIC method
	 * @param args : args[i].getClass() must coincide with declared argument type in the method
	 * @return true if succeeded, false if IOException occurred
	 * @throws RuntimeException - if something is wrong with FileUtils (not IOExceptions!)
	 */
    public static boolean callMethod(String className, String methodName, Object[] args) throws RuntimeException {
        try {
            MethodCall.call(Class.forName(className), methodName, args);
            return true;
        } catch (IOException e) {
            return false;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * The same as {@link #callMethod(String, String, Object[])} but for the case when
	 *  types of arguments do not equal to the arg[i].getClass()
	 */
    public static boolean callMethod(String className, String methodName, Class[] argTypes, Object[] args) throws RuntimeException {
        try {
            MethodCall call = new MethodCall(Class.forName(className), methodName, argTypes);
            call.setArgumentValues(args);
            call.call();
            return true;
        } catch (IOException e) {
            return false;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * The same as {@link #callMethod(String, String, Class[], Object[])} but for CONSTRUCTORS
	 *  and returns the instance created (or null if IOException occur).
	 */
    public static Object callConstructor(String className, Class[] argTypes, Object[] args) throws RuntimeException {
        try {
            return Class.forName(className).getConstructor(argTypes).newInstance(args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IOException) return null; else throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * The same as {@link #callMethod(String, String, Object[])} but for NON-STATIC methods
	 *  and returns the result of the method (or null if IOException occur).
	 */
    public static Object callMethod(Object target, String methodName, Object[] args) throws RuntimeException {
        try {
            return MethodCall.call(target, methodName, args);
        } catch (IOException e) {
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * The method is public since somebody may want more advanced util methods than in this Utils.
	 * Use this method if your arguments have the same type as declared in the method.
	 * Use only void FileUtils' methods via this method.
	 * @return true if succeeded, false if IOException occurred
	 * @throws RuntimeException - if something is wrong with FileUtils (not IOExceptions!)
	 */
    public static boolean callApacheFileUtils(String methodName, Object[] args) throws RuntimeException {
        return callMethod("org.apache.commons.io.FileUtils", methodName, args);
    }

    /**
	 * If error occurs, shows a message.
	 * @return boolean - true if succeeded.
	 */
    public static boolean copyDir(File source, File destination) {
        return copyDir(source, destination, true);
    }

    /**
	 * 
	 * Works faster if Apache Commons IO is in classpath. 
	 * @param withMessage - if true, shows a message on error. 
	 * @return boolean - true if succeeded.
	 */
    public static boolean copyDir(File source, File destination, boolean withMessage) {
        try {
            boolean result = callApacheFileUtils("copyDirectory", new Object[] { source, destination });
            if (!result && withMessage) showError(getErrorInFileString(destination.getPath()));
            return result;
        } catch (RuntimeException e) {
        }
        if (!source.exists() || source.isFile()) return false;
        boolean success = true, s;
        if (!destination.exists()) {
            s = destination.mkdir();
            if (withMessage && !s) showError(getCantCreateString(destination.getPath()));
        }
        File files[] = source.listFiles(), file, dest;
        for (int i = 0; i < files.length; i++) {
            file = files[i];
            dest = new File(destination, file.getName());
            if (file.isFile()) s = copyFile(file, dest, withMessage); else s = copyDir(file, dest, withMessage);
            success &= s;
        }
        return success;
    }

    /**
	 * 
	 * @return boolean
	 * @param source java.io.File
	 * @param destination java.io.File
	 */
    public static boolean copyFile(File source, File destination) {
        return copyFile(source, destination, true);
    }

    /**
	 * 
	 * Works faster if Apache Commons IO is in classpath. 
	 * @return boolean
	 * @param source java.io.File
	 * @param destination java.io.File
	 */
    public static boolean copyFile(File source, File destination, boolean withMessage) {
        try {
            boolean result = callApacheFileUtils("copyFile", new Object[] { source, destination });
            if (!result && withMessage) showError(getErrorInFileString(destination.getPath()));
            return result;
        } catch (RuntimeException e) {
        }
        try {
            copy(new FileInputStream(source), new FileOutputStream(destination));
        } catch (FileNotFoundException e) {
            if (withMessage) showError(getFileNotFoundString(source.getPath()));
            return false;
        } catch (IOException e) {
            if (withMessage) showError(getErrorInFileString(destination.getPath()));
            return false;
        }
        return true;
    }

    /**
	 * 
	 * Works faster if Apache Commons IO is in classpath. 
	 * @return boolean
	 * @param dir java.io.File
	 */
    public static boolean deleteDir(File dir) {
        return deleteDir(dir, true);
    }

    /**
	 * 
	 * @return boolean
	 * @param dir java.io.File
	 */
    public static boolean deleteDir(File dir, boolean withMessage) {
        try {
            boolean result = callApacheFileUtils("deleteDirectory", new Object[] { dir });
            if (!result && withMessage) showError(getCantDeleteString(dir.getPath()));
            return result;
        } catch (RuntimeException e) {
        }
        if (dir.isFile()) return false;
        File files[] = dir.listFiles(), file;
        boolean s;
        for (int i = 0; i < files.length; i++) {
            file = files[i];
            if (file.isFile()) {
                s = file.delete();
                if (withMessage && !s) showError(getCantDeleteString(file.getPath()));
            } else s = deleteDir(file, withMessage);
            if (!s) return false;
        }
        s = dir.delete();
        if (withMessage && !s) {
            showError(getCantDeleteString(dir.getPath()));
        }
        return s;
    }

    /**
	 * The method is public since somebody may want more advanced util methods than in this Utils.
	 * Use this method if your arguments have the same type as declared in the method.
	 * Use only void IOUtils' methods via this method.
	 * @param className - full class name
	 * @param methodName - name of public static method
	 * @param argTypes - declared argument types in the method
	 * @param args - actual arguments
	 * @return true if succeeded, false if IOException occurred
	 * @throws RuntimeException - if something is wrong with IOUtils (not IOExceptions!)
	 */
    public static boolean callApacheIOUtils(String methodName, Class[] argTypes, Object[] args) throws RuntimeException {
        return callMethod("org.apache.commons.io.IOUtils", methodName, argTypes, args);
    }

    /**
	 * Writes all bytes from source to destination.
	 * Works faster if Apache Commons IO is in classpath. 
	 */
    public static void copy(InputStream source, OutputStream destination) throws IOException {
        try {
            callApacheIOUtils("copy", new Class[] { InputStream.class, OutputStream.class }, new Object[] { source, destination });
            return;
        } catch (RuntimeException e) {
        }
        int size = 1024;
        byte[] buf = new byte[size];
        BufferedInputStream in = new BufferedInputStream(source, size);
        BufferedOutputStream out = new BufferedOutputStream(destination, size);
        while (true) {
            int len = in.read(buf);
            if (len < 0) break;
            out.write(buf, 0, len);
            if (len < size) break;
        }
        in.close();
        out.close();
    }

    /**
	 * Unzips the given archive file to the given directory.
	 * Supports localized symbols in zipped file names only if Apache Ant is in classpath (org.apache.tools.zip is enough).
	 * Works faster if Apache Commons IO is in classpath. 
	 * @param encoding - if null, localization is not supported.
	 * @return the number of files (not directories) processed or -1 if an exception occurred.
	 */
    public static int unzip(File archive, File destination, String encoding, boolean withMessage) {
        if (!destination.exists()) destination.mkdirs();
        String dest = destination.getPath();
        if (!dest.endsWith("/") && !dest.endsWith("\\")) dest += "/";
        int count = 0;
        try {
            try {
                Object zip = callConstructor("org.apache.tools.zip.ZipFile", new Class[] { File.class, String.class }, new Object[] { archive, encoding });
                if (zip == null) throw new IOException("IOException in opening ZipFile");
                Enumeration entries = (Enumeration) callMethod(zip, "getEntries", null);
                while (entries.hasMoreElements()) {
                    Object entry = entries.nextElement();
                    String name = (String) callMethod(entry, "getName", null);
                    InputStream stream = name.endsWith("/") ? null : (InputStream) callMethod(zip, "getInputStream", new Object[] { entry });
                    unzip(name, stream, dest);
                    if (stream != null) count++;
                }
            } catch (RuntimeException e) {
                ZipFile zip = new ZipFile(archive);
                Enumeration entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    InputStream stream = entry.isDirectory() ? null : zip.getInputStream(entry);
                    unzip(entry.getName(), stream, dest);
                    if (stream != null) count++;
                }
            }
        } catch (FileNotFoundException e) {
            if (withMessage) showError(getFileNotFoundString(archive.getPath()));
            return -1;
        } catch (IOException e) {
            if (withMessage) showError(getErrorInFileString(archive.getPath()));
            return -1;
        }
        return count;
    }

    /**
	 * Unzips one entry from zip InputStream (can be used if {@link #unzip(File, File, String, boolean)}
	 *  is not sufficient for your needs; e.g. if you want to process comments, etc.).
	 * @param name - ZipEntry.getName() 
	 * @param stream - ZipFile.getInputStream(ZipEntry) or null if name is directory
	 * @param dest - destination directory (must end with file separator)
	 * @throws IOException
	 */
    public static void unzip(String name, InputStream stream, String dest) throws IOException {
        File file = new File(dest + name);
        if (stream == null) {
            file.mkdirs();
        } else {
            file.getParentFile().mkdirs();
            copy(stream, new FileOutputStream(file));
        }
    }

    /**
	 * Returns "Can't create "+fileName string
	 * @return java.lang.String
	 */
    public static String getCantCreateString(String fileName) {
        return bundle.getString("CantCreate") + " " + fileName;
    }

    /**
	 * Returns "Can't delete "+fileName string
	 * @return java.lang.String
	 */
    public static String getCantDeleteString(String fileName) {
        return bundle.getString("CantDelete") + " " + fileName;
    }

    /**
	 * Returns "Error in file "+fileName string
	 * @return java.lang.String
	 */
    public static String getErrorInFileString(String fileName) {
        return bundle.getString("Error") + " " + bundle.getString("inFile") + " " + fileName;
    }

    /**
	 * Returns "File "+fileName+" not found" string
	 */
    public static String getFileNotFoundString(String fileName) {
        return bundle.getString("File") + " " + fileName + " " + bundle.getString("notFound");
    }

    /**
	 * Returns "File "+fileName+" not found" string
	 */
    public static String getFileExistsString(String fileName) {
        return bundle.getString("File") + " " + fileName + " " + bundle.getString("exists");
    }

    /**
	 * @param message
	 */
    public static void showError(String message) {
        try {
            getShowError().invoke(null, new Object[] { message });
        } catch (Exception e) {
        }
    }

    protected static Method getShowError() {
        if (showError != null) return showError;
        Class cls;
        try {
            cls = Class.forName("mipt.gui.UIUtils");
        } catch (ClassNotFoundException e) {
            cls = Console.class;
        }
        try {
            showError = cls.getMethod("showError", new Class[] { String.class });
            return showError;
        } catch (Exception e) {
            throw new RuntimeException(cls.getName() + " must have showError() method");
        }
    }
}
