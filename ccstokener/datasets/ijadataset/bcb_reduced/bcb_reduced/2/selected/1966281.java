package de.ios.framework.basic;

import java.awt.Frame;
import java.awt.FileDialog;
import java.net.*;
import java.io.*;

/**
 * Collection of usefull methods to reading and writing files.
 */
public final class IOTools {

    /** */
    protected static Class thisClass = new IOTools().getClass();

    /**
   * Constructor. No instance of this class is needed.
   */
    protected IOTools() {
    }

    /**
   * Write data to a file.
   */
    public static void writeFile(String file, byte data[]) throws IOException {
        try {
            FileOutputStream os = new FileOutputStream(file);
            writeData(os, data);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
   * Write data to a outputstream.
   * After the data is written, the stream is closed.
   */
    public static void writeData(OutputStream os, byte data[]) throws IOException {
        os.write(data);
        os.close();
    }

    /**
   * OPens a stream to a file from Resource (CLASSPATH).
   * The file can be contained in a zip or jar-file specified in the CLASSPATH!
   * If the file is not found or can't be read, null is returned.
   */
    public static InputStream openResourceInputStream(String name) throws IOException {
        ClassLoader cl = thisClass.getClassLoader();
        InputStream is = ClassLoader.getSystemResourceAsStream(name);
        if ((is == null) && (cl != null)) is = cl.getResourceAsStream(name);
        if (is == null) {
            if ((File.separatorChar != '/') && (name.indexOf(File.separatorChar) >= 0)) {
                Debug.println(Debug.INFO, "IOTools", "Load from resource failed for '" + name + "'. Trying to use '/' as file-separator.");
                return openResourceInputStream(name.replace(File.separatorChar, '/'));
            }
            return null;
        }
        return is;
    }

    /**
   * Reads a file from Resource (CLASSPATH).
   * The file can be contained in a zip or jar-file specified in the CLASSPATH!
   * If the fiel is not found or can't be read, null is returned.
   */
    public static byte[] readFromResource(String name) throws IOException {
        InputStream is = openResourceInputStream(name);
        if (is == null) return null;
        return readData(is);
    }

    /**
   * Reads a file.
   * Read the complete contents of the file.
   * @param file Name of the file.
   */
    public static byte[] readFile(File file) throws IOException {
        return readFile(file.getAbsolutePath());
    }

    /**
   * Reads a file.
   * Read the complete contents of the file.
   * @param file Name of the file.
   */
    public static byte[] readFile(String file) throws IOException {
        try {
            return readData(new FileInputStream(file));
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            throw new IOException(t.getMessage());
        }
    }

    /**
   * Get the path (without the terminating "/").
   */
    public static String getPath(String file) {
        if (File.separatorChar != '/') file = file.replace('/', File.separatorChar);
        file = new File(new File(file).getAbsolutePath()).getParent();
        return file;
    }

    /**
   * Expand a path (allows rel. pathes including ~[user] as home-dir of users).
   */
    public static String getFilePath(String f) {
        return getFilePath(f, null);
    }

    /**
   * Expand a path (allows rel. pathes including ~[user] as home-dir of users).
   * This Method was moved here from the Class 'Parameters' and is used by Parameters.get[Optional]Filename(...).
   */
    public static String getFilePath(String f, String basepath) {
        if (f != null) {
            String h = Parameters.getParameter("user.home");
            String d = Parameters.getParameter("user.dir");
            String s = File.separator;
            char sep;
            int idx;
            if (basepath != null) if (basepath.length() > 0) d = basepath;
            if (s == null) s = "\\";
            if (s.length() == 1) {
                sep = s.charAt(0);
                f = f.replace('/', sep).replace('\\', sep);
                if (d != null) d = d.replace('/', sep).replace('\\', sep);
            }
            if ((d != null) && !(f.startsWith("~") || f.startsWith(s) || (f.indexOf(':') >= 0))) f = d + (d.endsWith(s) ? "" : (s)) + f;
            if (f.startsWith("~")) if (f.length() == 1) f = h; else if (f.startsWith("~" + s)) f = ((h == null) ? "" : h) + f.substring(1); else f = ((h == null) ? "" : (h + s + ".." + s)) + f.substring(1);
        }
        return f;
    }

    /**
   * Creates a path (if it it not already exists) with all needed subdirs.
   * '/'-file-seperators are converted if nesessary.
   */
    public static void createPath(String path) throws IOException {
        if (path == null) return;
        path = path.trim();
        if (path.length() == 0) return;
        if (File.separatorChar != '/') path = path.replace('/', File.separatorChar);
        if (path.charAt(path.length() - 1) == File.separatorChar) path = path.substring(path.length() - 1);
        File file = new File(path);
        if (!file.exists()) if (!file.mkdirs()) throw new IOException("Can't create directory '" + path + "'");
    }

    /**
   * Opens a inputstream from an url.
   */
    public static InputStream openURLInputStream(String urlName) throws IOException {
        URL url;
        try {
            url = new URL(urlName);
            return url.openStream();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable e2) {
            throw new IOException(e2.getMessage());
        }
    }

    /**
   * Reads a file from URL.
   * Read the complete contents of the file.
   * @param urlName Url to file.
   */
    public static byte[] readURLFile(String urlName) throws IOException {
        byte[] idata = null;
        byte[] buffer = new byte[10240];
        if (urlName == null) return null;
        urlName = urlName.trim();
        if (urlName.length() > 0) {
            try {
                return readData(openURLInputStream(urlName));
            } catch (IOException ioe) {
                throw ioe;
            }
        } else return null;
    }

    /**
   * Reads all available data from inputstream.
   * After all data is read, the stream is closed.
   */
    public static byte[] readData(InputStream is) throws IOException {
        byte[] buffer = new byte[10240];
        ByteArrayOutputStream bs = new ByteArrayOutputStream(10240);
        int len;
        try {
            while ((len = is.read(buffer, 0, buffer.length)) >= 0) bs.write(buffer, 0, len);
            is.close();
            return bs.toByteArray();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable e2) {
            throw new IOException(e2.getMessage());
        }
    }

    /** The last selected file-path.*/
    protected static String browseDir = "";

    /** The last selected file.*/
    protected static String lastFile = "";

    /**
   * Sets the default-file for selecting files.
   * This is automatically reseted after each selection.
   */
    public static final void setDefaultFile(String file) {
        lastFile = file;
    }

    /**
   * Sets the default-path for selecting files.
   */
    public static final void setDefaultBrowsePath(String path) {
        browseDir = path;
    }

    /**
   * Sets the default-path for selecting files.
   */
    public static final String getDefaultBrowsePath() {
        return browseDir;
    }

    /**
   * Show the fileselector with the last selected path.
   * @param   parentFrame Frame needed to display the file-selector.
   * @param   title       Title for the dialog. If null, nothing is displayed.
   * @return  The filename (will full path) of an empty String if the user aborted the selection.
   */
    public static String fileSelect(Frame parentFrame, String title) {
        return fileSelect(parentFrame, title, null);
    }

    /**
   * Show the fileselector with the last selected path.
   * @param   parentFrame Frame needed to display the file-selector.
   * @param   title       Title for the dialog. If null, nothing is displayed.
   * @param   pattern     Filepattern for selecting files. RegularExpressions can be used.
   * @return  The filename (will full path) of an empty String if the user aborted the selection.
   * @see de.ios.framework.basic.RegularExpression
   */
    public static String fileSelect(Frame parentFrame, String title, String pattern) {
        String file;
        FileDialog fileB = new FileDialog(parentFrame, title == null ? "" : title, FileDialog.LOAD);
        fileB.setDirectory(browseDir);
        fileB.setFile(lastFile);
        if (pattern != null) {
            FilenameFilter flt = new RegularFilenameFilter(pattern);
            fileB.setFilenameFilter(flt);
        }
        lastFile = "";
        fileB.show();
        file = fileB.getFile();
        if (file == null) file = ""; else file = file.trim();
        browseDir = fileB.getDirectory();
        if (file.length() > 0) file = browseDir + file;
        return file;
    }

    /**
   * Main - for Testing.
   */
    public static void main(String[] args) {
        Parameters.defineArguments(args);
        System.out.println("getFilePath: \"a/b\": " + getFilePath("a/b"));
        System.out.println("getFilePath: \"c:\\a\\b\": " + getFilePath("c:\\a\\b"));
        System.out.println("getFilePath: \"~a/b\": " + getFilePath("~a/b"));
        System.out.println("getFilePath: \"~/a/b\": " + getFilePath("~/a/b"));
        System.out.println("getFilePath: \"a/b\",\"c\": " + getFilePath("a/b", "c"));
        System.out.println("getFilePath: \"a/b\",\"c\\\": " + getFilePath("a/b", "c\\"));
        System.out.println("new File(\"aaa/bbb\").getParent(): " + (new File("aaa/bbb").getParent()));
        System.out.println("new File(\"aaa\").getParent(): " + (new File("aaa").getParent()));
    }
}
