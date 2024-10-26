package com.aptana.ide.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import com.aptana.ide.internal.core.CoreNatives;

/**
 * FileUtils
 */
public class FileUtils {

    /**
	 * The newline separator character
	 */
    public static String NEW_LINE = System.getProperty("line.separator");

    /**
	 * The file separator character.
	 */
    private static final char ALT_SEPARATOR_CHAR = File.separatorChar == '/' ? '\\' : '/';

    /**
	 * The file directory hashtable
	 */
    private static Hashtable fileDirectoryStatus = new Hashtable();

    /**
	 * The temp directory of the system;
	 */
    public static String systemTempDir = getTempDir();

    /**
	 * Protected constructor for utility class.
	 */
    protected FileUtils() {
    }

    /**
	 * copies a file or a directory from one directory to another
	 * 
	 * @param from
	 *            directory from where to copy
	 * @param to
	 *            directory where to copy
	 * @param what
	 *            what to copy (file or directory, recursively)
	 * @return true if successful <br>
	 *         <br>
	 *         <b>Example</b>:
	 *         <li><code>copy("c:\\home\\vlad\\dev", "c:\\home\\vlad\\rtm", "contents.xml")</code></li>
	 */
    public static boolean copy(String from, String to, String what) {
        return copy(new File(from, what), new File(to, what));
    }

    /**
	 * copy copies a file or a directory from one directory to another
	 * 
	 * @param from
	 *            directory from where to copy
	 * @param to
	 *            directory where to copy
	 * @param what
	 *            what to copy (file or directory, recursively)
	 * @return true if successful <br>
	 *         <br>
	 *         <b>Example</b>:
	 *         <li><code>copy(new File(myHomeDir, "dev"), new File(myHomeDir, "rtm"), "contents.xml")</code></li>
	 */
    public static boolean copy(File from, File to, String what) {
        return copy(new File(from, what), new File(to, what));
    }

    /**
	 * In a "creative" way checks whether a string or a container is empty. <br>
	 * Accepts a <code>Collection</code>, a <code>Map</code>, an array, a <code>String</code>.
	 * 
	 * @param data
	 *            a Collection or a Map or an array or a string to check
	 * @return true if data is empty <br>
	 *         <br>
	 *         <b>Examples</b>:
	 *         <li><code>isEmpty(StringUtils.EMPTY), isEmpty(null), isEmpty(new HashMap())</code> all return
	 *         <b>true</b>;</li>
	 *         <li><code>isEmpty(" "), isEmpty(new int[] {1})</code> returns <b>false</b>.</li>
	 */
    public static final boolean isEmpty(Object data) {
        if (data == null) {
            return true;
        }
        if (data instanceof Collection) {
            return ((Collection) data).isEmpty();
        }
        if (data instanceof Map) {
            return ((Map) data).isEmpty();
        }
        if (data instanceof Object[]) {
            return ((Object[]) data).length == 0;
        }
        return (data.toString().length() == 0) || "null".equals(data.toString());
    }

    /**
	 * copy copies a file or a directory to another.
	 * 
	 * @param from
	 *            the source path
	 * @param to
	 *            the destination path
	 * @return true if successful <br>
	 *         <br>
	 *         <b>Example</b>:
	 *         <li><code>copy("c:\\home\\vlad\\dev\\contents.xml", "c:\\home\\vlad\\rtm\\contents.rss")</code></li>
	 */
    public static boolean copy(String from, String to) {
        return copy(new File(from), new File(to));
    }

    /**
	 * copy copies a file or a directory to another.
	 * 
	 * @param from
	 *            the source path
	 * @param to
	 *            the destination path
	 * @return true if successful <br>
	 *         <br>
	 *         <b>Example</b>:
	 *         <li><code>copy(new File(myHomeDir, "contents.xml"), new File(mySite, "contents.rss")</code></li>
	 */
    public static boolean copy(File from, File to) {
        if (from.isDirectory()) {
            String[] contents = from.list();
            for (int i = 0; contents != null && i < contents.length; i++) {
                copy(from, to, contents[i]);
            }
        } else {
            try {
                OutputStream os = makeFile(to);
                InputStream is = new FileInputStream(from);
                pipe(is, os, false);
                is.close();
                os.close();
            } catch (IOException ex) {
                return false;
            }
        }
        long time = from.lastModified();
        if (!to.setLastModified(time)) {
            return false;
        }
        long newtime = to.lastModified();
        return time == newtime;
    }

    /**
	 * pipes data from input stream to output stream, possibly pumping them through the filter (if
	 * any)
	 * 
	 * @param in
	 *            InputStream the source of data
	 * @param out
	 *            OutputStream where the output goes, filtered if filter is present, or unfiltered
	 *            otherwise
	 * @param isBlocking
	 *            boolean whether input is blocking (in this case the maximum amount is read in one
	 *            operation; for nonblocking in.available() determines how many bytes can be read)
	 * @param filter
	 *            ByteFilter the filter that applies to data; can be null
	 * @throws IOException
	 *             when input or output fails see the test for examples
	 */
    public static void pipe(InputStream in, OutputStream out, boolean isBlocking, ByteFilter filter) throws IOException {
        byte[] buf = new byte[50000];
        int nread;
        int navailable;
        int total = 0;
        synchronized (in) {
            navailable = isBlocking ? buf.length : in.available();
            nread = in.read(buf, 0, Math.min(buf.length, navailable));
            while (navailable > 0 && nread >= 0) {
                if (filter == null) {
                    out.write(buf, 0, nread);
                } else {
                    byte[] filtered = filter.filter(buf, nread);
                    out.write(filtered);
                }
                total += nread;
                navailable = isBlocking ? buf.length : in.available();
                nread = in.read(buf, 0, Math.min(buf.length, navailable));
            }
        }
        out.flush();
        buf = null;
    }

    /**
	 * <p>
	 * Description: The interface is used to define filters for filtering data in pipes. Filters,
	 * similar to those in JSPs, can modify the bytes going from one end of the pipe to another, or
	 * just sniff them and act based on results - e.g. count bytes, calculate crc, you name it.
	 * </p>
	 */
    public interface ByteFilter {

        /**
		 * filters data coming from input
		 * 
		 * @param input
		 *            byte[] input data
		 * @param length
		 *            int number of meaningful bytes
		 * @return byte[] result of filtering
		 */
        byte[] filter(byte[] input, int length);
    }

    /**
	 * pipes data from input stream to output stream
	 * 
	 * @param in
	 *            InputStream the source of data
	 * @param out
	 *            OutputStream where the output goes, filtered if filter is present, or unfiltered
	 *            otherwise
	 * @param isBlocking
	 *            boolean whether input is blocking (in this case the maximum amount is read in one
	 *            operation; for nonblocking in.available() determines how many bytes can be read)
	 * @throws IOException
	 *             when input or output fails see the test for examples
	 */
    public static void pipe(InputStream in, OutputStream out, boolean isBlocking) throws IOException {
        pipe(in, out, isBlocking, null);
    }

    /**
	 * pipes data from input stream to output stream
	 * 
	 * @param in
	 *            Reader the source of data
	 * @param out
	 *            Writer where the output goes, filtered if filter is present, or unfiltered
	 *            otherwise
	 * @return boolean true if successful, false otherwise see the test for examples
	 */
    public static boolean pipe(Reader in, Writer out) {
        if (in == null) {
            return false;
        }
        if (out == null) {
            return false;
        }
        try {
            int c;
            synchronized (in) {
                c = in.read();
                while (in.ready() && c > 0) {
                    out.write(c);
                    c = in.read();
                }
            }
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
	 * <code>append</code> determines whether to open in <i>append</i> mode
	 * 
	 * @param dirname
	 *            file location
	 * @param filename
	 *            the name of the file
	 * @param append
	 *            <b>true</b> if open in <i>append</i> mode
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(String dirname, String filename, boolean append) throws IOException {
        if (!isEmpty(dirname)) {
            File dir = new File(dirname);
            if (!dir.isDirectory()) {
                if (dir.exists()) {
                    dir.delete();
                }
                dir.mkdirs();
            }
        }
        return new FileOutputStream(new File(dirname, filename), append);
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten.
	 * 
	 * @param dir
	 *            file location
	 * @param filename
	 *            the name of the file
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(String dir, String filename) throws IOException {
        return makeFile(dir, filename, false);
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
	 * <code>append</code> determines whether to open in <i>append</i> mode
	 * 
	 * @param path
	 *            [0] is directory name, [1] is file name
	 * @param append
	 *            <b>true</b> if open in <i>append</i> mode
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(String[] path, boolean append) throws IOException {
        return makeFile(path[0], path[1], append);
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten.
	 * 
	 * @param path
	 *            [0] is directory name, [1] is file name
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(String[] path) throws IOException {
        return makeFile(path[0], path[1]);
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
	 * <code>append</code> determines whether to open in <i>append</i> mode
	 * 
	 * @param path
	 *            file path
	 * @param append
	 *            <b>true</b> if open in <i>append</i> mode
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(String path, boolean append) throws IOException {
        return makeFile(splitPath(path), append);
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten.
	 * 
	 * @param path
	 *            file path
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(String path) throws IOException {
        return makeFile(splitPath(path));
    }

    /**
	 * Splits a path into directory name and file name.
	 * 
	 * @param path
	 *            the path to split
	 * @return String array consisting of two elements <br>
	 *         <br>
	 *         <b>Examples</b>:
	 *         <li><code>splitPath("/home/zaphod/jbuilder8/samples/welcome")</code> returns
	 *         {"/home/zaphod/jbuilder8/samples", "welcome"};</li>
	 *         <li><code>splitPath("src.java")</code> returns {".", "src.java"};</li>
	 *         <li><code>splitPath("MyHome\\dev")</code> returns {"MyHome", "dev"}.</li>
	 */
    public static String[] splitPath(String path) {
        return new String[] { dirname(path), new File(path).getName() };
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten, unless <code>append</code> is <b>true</b>.
	 * <code>append</code> determines whether to open in <i>append</i> mode
	 * 
	 * @param file
	 *            the file to open
	 * @param append
	 *            <b>true</b> if open in <i>append</i> mode
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(File file, boolean append) throws IOException {
        return makeFile(file.getCanonicalPath(), append);
    }

    /**
	 * Calculates directory path for a file (like in Perl).
	 * 
	 * @param file
	 *            the file to calculate from
	 * @return directory path Unlike java.io.File.getParent(), never returns null (see example 2
	 *         below). <br>
	 *         <br>
	 *         <b>Examples</b>:
	 *         <li><code>dirname(new File("/home/zaphod/jbuilder11/samples/welcome"))</code>
	 *         returns "/home/zaphod/jbuilder8/samples";</li>
	 *         <li><code>dirname(new File("src.java"))</code> returns ".";</li>
	 *         <li><code>dirname(new File("MyHome\\dev"))</code> returns "MyHome".</li>
	 */
    public static String dirname(File file) {
        String parent = file.getParent();
        if (parent == null) {
            parent = ".";
        }
        if (file.getPath().indexOf(File.separatorChar) < 0 && file.getPath().indexOf(ALT_SEPARATOR_CHAR) >= 0 && parent.indexOf(File.separatorChar) >= 0) {
            parent = parent.replace(File.separatorChar, ALT_SEPARATOR_CHAR);
        }
        return parent;
    }

    /**
	 * Calculates directory path by file path (like in Perl)
	 * 
	 * @param path
	 *            the path to use
	 * @return directory path Unlike java.io.File.getParent(), never returns null (see example 2
	 *         below). <br>
	 *         <br>
	 *         <b>Examples</b>:
	 *         <li><code>dirname("/home/zaphod/jbuilder11/samples/welcome")</code> returns
	 *         "/home/zaphod/jbuilder8/samples";</li>
	 *         <li><code>dirname("src.java")</code> returns ".";</li>
	 *         <li><code>dirname("MyHome\\dev")</code> returns "MyHome".</li>
	 */
    public static String dirname(String path) {
        String dirname = dirname(new File(path));
        if (path.indexOf(ALT_SEPARATOR_CHAR) >= 0 && path.indexOf(File.separatorChar) < 0) {
            return dirname.replace(File.separatorChar, ALT_SEPARATOR_CHAR);
        }
        return dirname;
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten.
	 * 
	 * @param file
	 *            the file to open
	 * @return file output stream
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static FileOutputStream makeFile(File file) throws IOException {
        return makeFile(file.getCanonicalPath());
    }

    /**
	 * Creates or opens a file for output. If subdirectories in the path do not exist, they are
	 * created too. If the file exists, it is overwritten.
	 * 
	 * @param path
	 *            the file to open
	 * @param encoding
	 *            the encoding to use
	 * @return output stream writer
	 * @throws IOException
	 *             if unable to write to the file
	 */
    public static final OutputStreamWriter makeFileWriter(String path, String encoding) throws IOException {
        return new OutputStreamWriter(makeFile(path), encoding);
    }

    /**
	 * Returns all files in the current directory.
	 * 
	 * @param file
	 *            The file to grab files in reference to
	 * @return A list of files, or a empty directory
	 */
    public static File[] getFilesInDirectory(File file) {
        Path path = new Path(file.toString());
        String lastSegment = path.lastSegment();
        File[] files = new File[0];
        if (file.isDirectory()) {
            files = file.listFiles();
        } else {
            File parent = file.getParentFile();
            files = parent.listFiles();
        }
        if (lastSegment != null && lastSegment.indexOf('*') >= 0) {
            return matchFiles(lastSegment, files);
        } else {
            return files;
        }
    }

    /**
	 * Given a list of files and a regular expression pattern, return a list of files that match the
	 * pattern
	 * 
	 * @param pattern
	 *            The pattern to check. Currently only really works with patterns like *.js
	 * @param files
	 *            The list of files
	 * @return The filtered list
	 */
    public static File[] matchFiles(String pattern, File[] files) {
        String newPattern = StringUtils.replace(pattern, "\\", "\\\\");
        newPattern = StringUtils.replace(newPattern, ".", "\\.");
        newPattern = StringUtils.replace(newPattern, "*", ".*");
        ArrayList al = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            File fileTest = files[i];
            if (fileTest.toString().matches(newPattern)) {
                al.add(fileTest);
            }
        }
        return (File[]) al.toArray(new File[0]);
    }

    /**
	 * Returns a relative path for the second file compared to the first
	 * 
	 * @param fileA
	 *            The "reference" file path
	 * @param fileB
	 *            The file to make relative
	 * @return String
	 */
    public static String makeFilePathRelative(File fileA, File fileB) {
        String separator = System.getProperty("file.separator");
        String a = fileA.toString();
        if (!fileA.isDirectory()) {
            a = fileA.getParent().toString() + separator;
        }
        String b = fileB.toString();
        if (fileB.isDirectory()) {
            b = b + separator;
        }
        String r = StringUtils.replace(b, a, StringUtils.EMPTY);
        if (r.endsWith(separator)) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }

    /**
	 * Get the extension.
	 * 
	 * @param fileName
	 *            File name
	 * @return the extension
	 */
    public static String getExtension(String fileName) {
        if (fileName == null || StringUtils.EMPTY.equals(fileName)) {
            return fileName;
        }
        int index = fileName.indexOf('.');
        if (index == -1) {
            return StringUtils.EMPTY;
        }
        if (index == fileName.length()) {
            return StringUtils.EMPTY;
        }
        return fileName.substring(index + 1, fileName.length());
    }

    /**
	 * Remove the extension.
	 * 
	 * @param fileName
	 *            File name
	 * @return the extension
	 */
    public static String stripExtension(String fileName) {
        if (fileName == null || StringUtils.EMPTY.equals(fileName)) {
            return fileName;
        }
        int index = fileName.lastIndexOf('.');
        if (index == -1) {
            return fileName;
        }
        if (index == fileName.length()) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    /**
	 * Is the current file a directory?
	 * 
	 * @param f
	 *            The file to test
	 * @return True if yes, false if no
	 */
    public static boolean isDirectory(File f) {
        if (System.getProperty("os.name").startsWith("Mac OS")) {
            return f.isDirectory();
        } else {
            String filePath = f.getAbsolutePath();
            if (fileDirectoryStatus.containsKey(filePath)) {
                return fileDirectoryStatus.get(filePath).equals(Boolean.TRUE);
            } else {
                File fShell = FileTricks.attemptReplaceWithShellFolder(f);
                boolean isDirectory = fShell.isDirectory();
                if (isDirectory) {
                    fileDirectoryStatus.put(filePath, Boolean.TRUE);
                } else {
                    fileDirectoryStatus.put(filePath, Boolean.FALSE);
                }
                return isDirectory;
            }
        }
    }

    private static String getTempDir() {
        if (systemTempDir == null) {
            PrivilegedAction pa = new java.security.PrivilegedAction() {

                public Object run() {
                    return System.getProperty("java.io.tmpdir");
                }
            };
            systemTempDir = ((String) AccessController.doPrivileged(pa));
        }
        return systemTempDir;
    }

    /**
	 * Ensures that the string passed in starts with a "."
	 * 
	 * @param extension
	 * @return String
	 */
    public static String ensureExtension(String extension) {
        if (extension == null || StringUtils.EMPTY.equals(extension)) {
            return extension;
        } else {
            if (extension.startsWith(".")) {
                return extension;
            } else {
                return "." + extension;
            }
        }
    }

    /**
	 * removes the leading period from an extension
	 * 
	 * @param extension
	 * @return String
	 */
    public static String stripExtensionPeriod(String extension) {
        if (extension == null || StringUtils.EMPTY.equals(extension)) {
            return extension;
        } else {
            if (extension.startsWith(".")) {
                return extension.substring(1);
            } else {
                return extension;
            }
        }
    }

    /**
	 * Removes the "middle" from a path to make it short enough to fit within the specified length,
	 * i.e. c:/Documents and Settings/username/My Documents/workspace/whatever.txt would become
	 * c:/Documents and Settings/.../workspace/whatever.txt.
	 * 
	 * @param path
	 *            the path to compress
	 * @param pathLength
	 *            the length to shorten it to. This is more of a guideline
	 * @return a compressed path
	 */
    public static String compressPath(String path, int pathLength) {
        path = path.replace('\\', '/');
        if (path.length() > pathLength) {
            int firstSlash = path.indexOf('/', 1);
            int endSearch = path.length() - pathLength - firstSlash;
            if (endSearch < 0) {
                return path;
            } else {
                int lastSlash = path.indexOf('/', endSearch);
                if (lastSlash > firstSlash) {
                    return path.substring(0, firstSlash) + "/..." + path.substring(lastSlash);
                } else {
                    lastSlash = path.lastIndexOf('/', path.length() - 2);
                    return path.substring(0, firstSlash) + "/..." + path.substring(lastSlash);
                }
            }
        } else {
            return path;
        }
    }

    /**
	 * Creates a file name with a random integer number inserted between the prefix and suffix
	 * @param prefix the name of the file (sans extension)
	 * @param suffix the extension of the file (including the '.')
	 * @return a new file name like test12534.txt
	 */
    public static String getRandomFileName(String prefix, String suffix) {
        if (suffix == null) {
            return prefix + (long) (Integer.MAX_VALUE * Math.random());
        } else {
            return prefix + (long) (Integer.MAX_VALUE * Math.random()) + suffix;
        }
    }

    /**
	 * Deletes the directory at the named location
	 * 
	 * @param parentDirectory
	 *            The location of the parent directory
	 * @param directoryName
	 *            The name of the directory to delete.
	 * @return True if the directory was deleted, false if not.
	 */
    public static boolean deleteDirectory(String parentDirectory, String directoryName) {
        File newDirectory = new File(parentDirectory + File.separator + directoryName);
        if (newDirectory.exists()) {
            return deleteDirectory(newDirectory);
        }
        return false;
    }

    /**
	 * Deletes the directory at the named location
	 * 
	 * @param directory
	 *            The location of the parent directory
	 * @return True if the directory was deleted, false if not.
	 */
    public static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDirectory(new File(directory, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return directory.delete();
    }

    /**
	 * setHidden
	 *
	 * @param file
	 * @return boolean
	 */
    public static boolean setHidden(File file) {
        if (Platform.OS_WIN32.equals(Platform.getOS())) {
            try {
                return CoreNatives.SetFileAttributes(file.getAbsolutePath(), CoreNatives.FILE_ATTRIBUTE_HIDDEN, 0);
            } catch (UnsatisfiedLinkError e) {
                IdeLog.logError(AptanaCorePlugin.getDefault(), Messages.FileUtils_CoreLibraryNotFound, e);
            }
        }
        return false;
    }

    /**
	 * readContent
	 * 
	 * @param stream
	 * @param charset
	 * @return String
	 * @throws IOException
	 */
    public static String readContent(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return StreamUtils.readContent(fis, null);
    }

    /**
	 * Is this url a file url?
	 * @param url The url
	 * @return true or false
	 */
    public static boolean isFileURL(URL url) {
        if (url == null) {
            return false;
        }
        String surl = url.toString();
        return surl.startsWith("file:/");
    }

    /**
	 * Converts a file://-based url into a file
	 * @param url The url
	 * @return the File, or null if not a file URL
	 */
    public static File urlToFile(URL url) {
        if (isFileURL(url)) {
            String surl = url.toString();
            surl = StringUtils.replace(surl, "file://", "");
            surl = StringUtils.replace(surl, "file:/", "");
            surl = StringUtils.urlDecodeFilename(surl.toCharArray());
            File f = new File(surl);
            return f;
        } else {
            return null;
        }
    }

    /**
	 * 
	 * @param uri
	 * @return
	 */
    public static File openURL(String uri) {
        URL fileURL = uriToURL(uri);
        File f = urlToFile(fileURL);
        if (f != null) {
            return f;
        } else {
            String text = readContent(fileURL);
            String[] path = fileURL.getFile().split("/");
            String name = path[path.length - 1];
            File temp;
            try {
                temp = File.createTempFile(FileUtils.stripExtension(name), FileUtils.ensureExtension(FileUtils.getExtension(name)));
                BufferedWriter out = new BufferedWriter(new FileWriter(temp));
                out.write(text);
                out.close();
                return temp;
            } catch (IOException e) {
                IdeLog.logError(AptanaCorePlugin.getDefault(), StringUtils.format("Unable to open URL {0} as file", uri), e);
                return null;
            }
        }
    }

    public static URL uriToURL(String uri) {
        URI uri2;
        String encodedUri;
        try {
            encodedUri = URLEncoder.encode(uri, null, null);
            uri2 = new URI(encodedUri).normalize();
            return uri2.toURL();
        } catch (MalformedURLException e) {
            IdeLog.logError(AptanaCorePlugin.getDefault(), StringUtils.format("Unable to convert uri {0} to URL. Malformed", uri), e);
            return null;
        } catch (URISyntaxException e) {
            IdeLog.logError(AptanaCorePlugin.getDefault(), StringUtils.format("Unable to convert uri {0} to URL. Syntax is incorrect", uri), e);
            return null;
        }
    }

    /**
	 * Reads the content from the URL
	 * @param f
	 * @return
	 */
    public static String readContent(URL url) {
        String text = null;
        try {
            if (isFileURL(url)) {
                File file = urlToFile(url);
                return readContent(file);
            } else {
                InputStream is = url.openStream();
                text = StreamUtils.readContent(is, null);
            }
        } catch (IOException e) {
            IdeLog.logError(AptanaCorePlugin.getDefault(), "Unable to read content", e);
        }
        return text;
    }
}
