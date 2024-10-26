package org.javatb.util;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.apache.commons.logging.*;
import org.javatb.search.*;

/**
 * This class provides a set of utility methods for reading, writing and manipulating files.
 * @author Laurent Cohen
 */
public final class FileUtils {

    /**
	 * Logger for this class.
	 */
    private static Log log = LogFactory.getLog(FileUtils.class);

    /**
	 * Fast way to determine whether debug level is enabled.
	 */
    private static boolean debugEnabled = log.isDebugEnabled();

    /**
	 * Maximum buffer size for reading class files.
	 */
    public static final int BUFFER_SIZE = 32 * 1024;

    /**
	 * Set of compatible archive file extensions.
	 */
    public static final Set<String> ZIP_EXT = Collections.unmodifiableSet(SearchEngine.asSet(Configuration.getArrayProperty("find4j.zip.extensions")));

    /**
	 * Instantiation of this class is not permitted.
	 */
    private FileUtils() {
    }

    /**
	 * Read the content of a specified reader into a string.
	 * @param aReader the reader to read the content from.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
    public static String readTextFile(Reader aReader) throws IOException {
        LineNumberReader reader = new LineNumberReader(aReader);
        StringBuilder sb = new StringBuilder();
        String s = "";
        while (s != null) {
            s = reader.readLine();
            if (s != null) sb.append(s).append("\n");
        }
        return sb.toString();
    }

    /**
	 * Read the content of a specified file into a string.
	 * @param filename the location of the file to read.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
    public static String readTextFile(String filename) throws IOException {
        return readTextFile(getFileReader(filename));
    }

    /**
	 * Write the content of a string into a specified file.
	 * @param filename the location of the file to write to.
	 * @param content the content to wwrite into the file.
	 * @throws IOException if the file can't be found or read.
	 */
    public static void writeTextFile(String filename, String content) throws IOException {
        LineNumberReader reader = new LineNumberReader(new StringReader(content));
        Writer writer = new BufferedWriter(new FileWriter(filename));
        String s = "";
        while (s != null) {
            s = reader.readLine();
            if (s != null) {
                writer.write(s);
                writer.write("\n");
            }
        }
        writer.flush();
        writer.close();
    }

    /**
	 * Read the content of a specified file into a string.
	 * @param file the location of the file to read.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
    public static String readTextFile(File file) throws IOException {
        return readTextFile(getFileReader(file));
    }

    /**
	 * Get an input stream given a file path.
	 * @param path the path to the file to lookup.
	 * @return a <code>InputStream</code> instance, or null if the file could not be found.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
    public static InputStream getFileInputStream(String path) throws IOException {
        InputStream is = null;
        File file = new File(path);
        if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
        if (is == null) {
            URL url = FileUtils.class.getClassLoader().getResource(path);
            is = (url == null) ? null : url.openStream();
        }
        return is;
    }

    /**
	 * Get a Reader from the specified path.
	 * @param path the path to the file to load.
	 * @return a <code>Reader</code> instance, or null if the file could not be found.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
    public static Reader getFileReader(String path) throws IOException {
        return getFileReader(new File(path));
    }

    /**
	 * Load a file from the specified path.
	 * @param path the path to the file to load.
	 * @return a <code>Reader</code> instance, or null if the file could not be found.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
    public static Reader getFileReader(File path) throws IOException {
        if (path.exists()) return new BufferedReader(new FileReader(path));
        return null;
    }

    /**
	 * Get the extension of a file.
	 * @param filePath the file from which to get the extension.
	 * @return the file extension, or null if it si not a file or does not have an extension.
	 */
    public static String getFileExtension(String filePath) {
        int idx = filePath.lastIndexOf(".");
        if (idx >= 0) {
            String s = filePath.substring(idx + 1);
            if ((s.indexOf('/') < 0) && (s.indexOf('\\') < 0)) return s;
        }
        return null;
    }

    /**
	 * Get the extension of a file.
	 * @param file the file from which to get the extension.
	 * @return the file extension, or null if it si not a file or does not have an extension.
	 */
    public static String getFileExtension(File file) {
        if ((file == null) || !file.exists() || !file.isFile()) return null;
        return getFileExtension(file.getName());
    }

    /**
	 * Get name of a file, without the path.
	 * @param filePath the file from which to get the name.
	 * @return the file name without the path.
	 */
    public static String getFileShortName(String filePath) {
        int idx = filePath.lastIndexOf('/');
        int idx2 = filePath.lastIndexOf('\\');
        if ((idx < 0) && (idx2 < 0)) return filePath;
        return filePath.substring(Math.max(idx, idx2) + 1);
    }

    /**
	 * Get an input stream from the current entry in a specified zip input stream.
	 * @param zis the current zip input stream.
	 * @return an array of bytes for the current entry in the current zip input stream.
	 * @throws Exception if an error is raised while reading the zip entry.
	 */
    public static byte[] zipEntryAsBytes(ZipInputStream zis) throws Exception {
        int size = BUFFER_SIZE;
        byte[] buffer = new byte[size];
        boolean end = false;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (!end) {
            int n = zis.read(buffer, 0, size);
            if (n < 0) break;
            if (n > 0) baos.write(buffer, 0, n);
        }
        baos.flush();
        baos.close();
        return baos.toByteArray();
    }

    /**
	 * Open a search element with its OS-associated application.
	 * @param elt the element to open.
	 */
    public static void openSearchElement(SearchElement elt) {
        performAction(elt, Desktop.Action.OPEN);
    }

    /**
	 * Edit a search element with its OS-associated application.
	 * @param elt the element to edit.
	 */
    public static void editSearchElement(SearchElement elt) {
        performAction(elt, Desktop.Action.EDIT);
    }

    /**
	 * Open a search element with its OS-associated application for the specified action.
	 * @param elt the element to process.
	 * @action the action to perform, one of the {@link java.awt.Desktop.Action Desktop.Action} enum values.
	 */
    private static void performAction(SearchElement elt, Desktop.Action action) {
        try {
            if (!Desktop.isDesktopSupported()) return;
            File file = null;
            if (elt.isFile()) {
                file = new File(elt.toString());
            } else if (elt.isArchiveEntry()) {
                List<SearchElement> parents = elt.getPath();
                InputStream rootIs = new BufferedInputStream(new FileInputStream(parents.get(0).getName()));
                ZipInputStream currentZis = new ZipInputStream(rootIs);
                for (int i = 1; i < parents.size(); i++) {
                    SearchElement p = parents.get(i);
                    byte[] bytes = getEntryAsBytes(currentZis, p.getName());
                    if (bytes == null) return;
                    currentZis = new ZipInputStream(new ByteArrayInputStream(bytes));
                }
                byte[] bytes = getEntryAsBytes(currentZis, elt.getName());
                if (bytes == null) return;
                String name = getFileShortName(elt.getName());
                String tmpDir = System.getProperty("java.io.tmpdir");
                file = new File(tmpDir + File.separatorChar + name);
                file.deleteOnExit();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                os.write(bytes);
                os.flush();
                os.close();
            }
            if ((file != null) && file.exists()) {
                switch(action) {
                    case OPEN:
                        Desktop.getDesktop().open(file);
                        break;
                    case EDIT:
                        Desktop.getDesktop().edit(file);
                        break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
	 * Get the content of an archive entry with the specified name.
	 * @param zis the archive input stream.
	 * @param name the name of the entry to find.
	 * @return the content of the entry as an array of bytes, or null if the entry could not be found.
	 * @throws Exception if an error occurs while looking up or reading the entry
	 */
    private static byte[] getEntryAsBytes(ZipInputStream zis, String name) throws Exception {
        try {
            ZipEntry entry = zis.getNextEntry();
            while ((entry != null) && (!entry.getName().equals(name))) {
                entry = zis.getNextEntry();
            }
            if (entry != null) {
                byte[] b = zipEntryAsBytes(zis);
                return b;
            }
        } finally {
            zis.close();
        }
        return null;
    }

    public static void deleteRecursive(File file) {
        try {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                for (int i = 0; i < children.length; i++) deleteRecursive(children[i]);
            }
            file.delete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
