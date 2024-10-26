package uk.ac.shef.wit.commons;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Several utility methods related to files.
 *
 * @author <a href="mailto:j.iria@dcs.shef.ac.uk">Jos&eacute; Iria</a>
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 * @version $Id: UtilFiles.java 558 2010-05-07 10:02:06Z zqz $
 */
public class UtilFiles {

    private static final Logger log = Logger.getLogger(UtilFiles.class.getName());

    public static final String CLASSPATH_SEPARATOR = System.getProperty("os.name").contains("indows") ? ";" : ":";

    private static final int BUFFER_SIZE = 1 << 13;

    /**
     * <p>Reads content from an URL into a string buffer.</p>
     *
     * @param url the url to get the content from.
     * @return string buffer with the contents of the url.
     * @throws IOException problem reading the url stream.
     */
    public static StringBuilder getContent(final URL url) throws IOException {
        return getContent(url.openStream());
    }

    public static StringBuilder getContent(final InputStream stream) throws IOException {
        return getContent(stream, "UTF-8");
    }

    /**
     * Read input raw text file as a list
     * @param path input file path
     * @param lowercase whether to convert input string to lowercase
     * @return
     * @throws IOException
     */
    public static List<String> readList(final String path, final boolean lowercase) throws IOException {
        List<String> res = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) continue;
            if (lowercase) res.add(line.toLowerCase()); else res.add(line);
        }
        reader.close();
        return res;
    }

    /**
     * Read all new line offsets in a string
     * @param content the string
     * @param baseOffset a base number to be added to all offsets. I.e., all offsets will be incremented by this base
     * @param newlinechar the new line character as string
     * @return a list of offsets sorted ascendingly
     */
    public static List<Integer> readNewlines(final String content, final int baseOffset, final String newlinechar) {
        List<Integer> offsets = new LinkedList<Integer>();
        int from = 0;
        int start;
        while ((start = content.indexOf(newlinechar, from)) != -1) {
            offsets.add(start + baseOffset + newlinechar.length());
            from = start + newlinechar.length();
        }
        if (offsets.get(offsets.size() - 1) != content.length()) offsets.add(content.length());
        return offsets;
    }

    public static StringBuilder getContent(final InputStream stream, final String charsetName) throws IOException {
        final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(stream, charsetName);
            final char[] readBuffer = new char[BUFFER_SIZE];
            int numRead = 0;
            do {
                int offset = 0;
                while (BUFFER_SIZE > offset && 0 <= (numRead = reader.read(readBuffer, offset, BUFFER_SIZE - offset))) offset += numRead;
                buffer.append(readBuffer, 0, offset);
            } while (0 <= numRead);
        } finally {
            if (reader != null) reader.close();
        }
        buffer.trimToSize();
        return buffer;
    }

    /**
     * <p>Adds a separator character to the end of the filename if it does not have one already.</p>
     *
     * @param filename the filename.
     * @return the filename with a separator at the end.
     */
    public static String addSeparator(final String filename) {
        if (filename != null && !filename.endsWith(File.separator)) return filename + File.separator;
        return filename;
    }

    /**
     * <p>Lists files in directories and their subdirectories (recursively).</p>
     *
     * @param paths the directories to list the files from.
     * @return a sorted set of <i>File</i> objects.
     */
    public static Set<File> listFilesRecursive(final String[] paths) {
        final Set<File> files = new HashSet<File>();
        for (final String path : paths) files.addAll(listFilesRecursive(path));
        return files;
    }

    /**
     * <p>Lists files in directory and its subdirectories (recursively).</p>
     *
     * @param path the directory to list the files from.
     * @return a sorted set of <i>File</i> objects.
     */
    public static Set<File> listFilesRecursive(final String path) {
        final Set<File> files = new HashSet<File>();
        listFilesRecursive(files, new File(path));
        return files;
    }

    public static List<File> listFoldersRecursive(File dir) throws FileNotFoundException {
        Set<File> result = new HashSet<File>();
        File[] filesAndDirs = dir.listFiles();
        List filesDirs = Arrays.asList(filesAndDirs);
        Iterator filesIter = filesDirs.iterator();
        File file;
        while (filesIter.hasNext()) {
            file = (File) filesIter.next();
            if (file.isDirectory()) {
                result.add(file);
                List<File> deeperList = listFoldersRecursive(file);
                result.addAll(deeperList);
            }
        }
        result.add(dir);
        List<File> toreturn = new ArrayList<File>(result);
        Collections.sort(toreturn);
        return toreturn;
    }

    /**
     * <p>Lists filenames in directories and their subdirectories (recursively).</p>
     *
     * @param paths the directories to list the filenames from.
     * @return a sorted set of <i>String</i> objects (the filenames).
     */
    public static Set<String> listFilenamesRecursive(final String[] paths) {
        final Set<String> filenames = new HashSet<String>();
        for (final String path : paths) filenames.addAll(listFilenamesRecursive(path));
        return filenames;
    }

    /**
     * <p>Lists filenames in directory and its subdirectories (recursively).</p>
     *
     * @param path the directory to list the filenames from.
     * @return a sorted set of <i>String</i> objects (the filenames).
     */
    public static Set<String> listFilenamesRecursive(final String path) {
        return listFilenamesRecursive(path, null);
    }

    /**
     * <p>Lists filenames in directory and its subdirectories (recursively).</p>
     *
     * @param path the directory to list the filenames from.
     * @return a sorted set of <i>String</i> objects (the filenames).
     */
    public static Set<String> listFilenamesRecursive(final String path, final FilenameFilter filter) {
        final List<File> files = new LinkedList<File>();
        if (filter == null) listFilesRecursive(files, new File(path)); else listFilesRecursive(files, new File(path), filter);
        final Set<String> filenames = new HashSet<String>(files.size());
        for (final File file : files) filenames.add(file.getAbsolutePath());
        return filenames;
    }

    /**
     * <p>Lists uris of files in directories and their subdirectories (recursively).</p>
     *
     * @param paths the directories to list the uris from.
     * @return a sorted set of <i>URI</i> objects.
     */
    public static Set<URI> listURIsRecursive(final String[] paths) {
        final Set<URI> uris = new HashSet<URI>();
        for (final String path : paths) uris.addAll(listURIsRecursive(path));
        return uris;
    }

    /**
     * <p>Lists uris of files in directory and its subdirectories (recursively).</p>
     *
     * @param path the directory to list the uris from.
     * @return a sorted set of <i>URI</i> objects.
     */
    public static Set<URI> listURIsRecursive(final String path) {
        final List<File> files = new LinkedList<File>();
        listFilesRecursive(files, new File(path));
        final Set<URI> uris = new HashSet<URI>(files.size());
        for (final File file : files) uris.add(file.toURI());
        return uris;
    }

    /**
     * <p>Converts filenames to their respective URL forms.</p>
     */
    public static URL[] filenamesToURLs(final String[] filenames) throws MalformedURLException {
        final Collection<URL> urls = new LinkedList<URL>();
        for (final String filename : filenames) urls.add(new File(filename).toURI().toURL());
        return urls.toArray(new URL[urls.size()]);
    }

    /**
     * <p>Deletes files recursively.</p>
     *
     * @param path the file or directory to delete.
     */
    public static void deleteFilesRecursive(final File path) {
        if (path.isDirectory()) for (final File file : path.listFiles()) deleteFilesRecursive(file);
        path.delete();
    }

    /**
     * Copy the entire directory to target directory. If source directory does not exist, returns "false". If source
     * directory is not a directory throws IOException.If destination directory does not exist, they will be created.
     * @param sourceDir must be the source directory
     * @param destDir to which directory files are copied
     * @return true if operation succeeds; false if otherwise, e.g., if source directory does not exist
     * @throws IOException if source is not a directory
     */
    public static boolean copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!sourceDir.exists()) return false;
        if (!sourceDir.isDirectory()) throw new IOException("Required: directory " + sourceDir);
        destDir.mkdirs();
        File[] children = sourceDir.listFiles();
        for (File sourceChild : children) {
            String name = sourceChild.getName();
            File destChild = new File(destDir, name);
            if (sourceChild.isDirectory()) {
                copyDirectory(sourceChild, destChild);
            } else {
                copyFile(sourceChild, destChild);
            }
        }
        return true;
    }

    /**
     * Copy and rename a source file to specified location
     * @param source source file, or directory
     * @param dest destination file
     * @return true if operation succeeds; false otherwise, e.g., if source file does not exist
     * @throws IOException
     */
    public static boolean copyFile(File source, File dest) throws IOException {
        if (!source.exists()) return false;
        if (!dest.exists()) dest.createNewFile();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
            in = null;
            out = null;
        }
        return true;
    }

    /**
     * <p>Writes content from a String into a file.</p>
     *
     * @param content      the string with the data to write
     * @param pathToOutput the path to the output file
     */
    public static void writeToFile(final String content, final String pathToOutput) throws IOException {
        OutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(new File(pathToOutput)));
            stream.write(content.getBytes());
        } finally {
            if (stream != null) stream.close();
        }
    }

    /**
     * <p>Convenience method for serializing an object into a file.</p>
     */
    public static void serialize(final String path, final Object object) throws IOException {
        final ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(path));
        stream.writeObject(object);
        stream.close();
    }

    /**
     * <p>Convenience method for deserializing an object from a file.</p>
     *
     * @return the object obtained from the file.
     */
    public static Object deserialize(final String path) throws ClassNotFoundException, IOException {
        final Object result;
        final FileInputStream inputStream = new FileInputStream(path);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            result = objectInputStream.readObject();
        } catch (IOException e) {
            inputStream.close();
            if (objectInputStream != null) objectInputStream.close();
            throw e;
        } catch (ClassNotFoundException e) {
            if (objectInputStream != null) objectInputStream.close();
            throw e;
        }
        objectInputStream.close();
        return result;
    }

    private static void listFilesRecursive(final Collection<File> files, final File path) {
        if (path.isDirectory()) for (final File file : path.listFiles()) listFilesRecursive(files, file); else files.add(path);
    }

    private static void listFilesRecursive(final Collection<File> files, final File path, final FilenameFilter filter) {
        if (path.isDirectory()) for (final File file : path.listFiles()) listFilesRecursive(files, file, filter); else if (filter.accept(path.getParentFile(), path.getName())) files.add(path);
    }

    private UtilFiles() {
    }
}
