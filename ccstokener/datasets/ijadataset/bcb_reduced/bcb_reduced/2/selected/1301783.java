package uk.ac.shef.wit.trex.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Several utility methods related to file manipulation.
 *
 * @author Jose' Iria, NLP Group, University of Sheffield
 *         (<a  href="mailto:J.Iria@dcs.shef.ac.uk" >email</a>)
 */
public class FileUtil {

    public static final char[] _readBuffer = new char[16384];

    /**
    * <p>Lists files in directories and their subdirectories (recursively).</p>
    *
    * @param path the directories to list the files from.
    * @return a sorted set of <i>File</i> objects.
    */
    public static Set listFilesRecursive(final String[] path) {
        final Set files = new TreeSet();
        for (int i = 0; i < path.length; ++i) files.addAll(listFilesRecursive(path[i]));
        return files;
    }

    /**
    * <p>Lists files in directory and its subdirectories (recursively).</p>
    *
    * @param path the directory to list the files from.
    * @return a sorted set of <i>File</i> objects.
    */
    public static Set listFilesRecursive(final String path) {
        final Set files = new TreeSet();
        listFilesRecursive(files, new File(path));
        return files;
    }

    /**
    * <p>Lists filenames in directories and their subdirectories (recursively).</p>
    *
    * @param path the directories to list the filenames from.
    * @return a sorted set of <i>String</i> objects (the filenames).
    */
    public static Set listFilenamesRecursive(final String[] path) {
        final Set filenames = new TreeSet();
        for (int i = 0; i < path.length; ++i) filenames.addAll(listFilenamesRecursive(path[i]));
        return filenames;
    }

    /**
    * <p>Lists filenames in directory and its subdirectories (recursively).</p>
    *
    * @param path the directory to list the filenames from.
    * @return a sorted set of <i>String</i> objects (the filenames).
    */
    public static Set listFilenamesRecursive(final String path) {
        final List files = new LinkedList();
        final Set filenames = new TreeSet();
        listFilesRecursive(files, new File(path));
        try {
            for (Iterator it = files.iterator(); it.hasNext(); ) filenames.add(((File) it.next()).getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filenames;
    }

    /**
    * <p>Lists urls of files in directories and their subdirectories (recursively).</p>
    *
    * @param path the directories to list the urls from.
    * @return a sorted set of <i>URL</i> objects.
    */
    public static Set listURLsRecursive(final String[] path) {
        final Set urls = new HashSet();
        for (int i = 0; i < path.length; ++i) urls.addAll(listURLsRecursive(path[i]));
        return urls;
    }

    /**
    * <p>Lists urls of files in directory and its subdirectories (recursively).</p>
    *
    * @param path the directory to list the urls from.
    * @return a sorted set of <i>URL</i> objects.
    */
    public static Set listURLsRecursive(final String path) {
        final List files = new LinkedList();
        final Set urls = new HashSet();
        listFilesRecursive(files, new File(path));
        try {
            for (Iterator it = files.iterator(); it.hasNext(); ) urls.add(((File) it.next()).toURL());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }

    /**
    * <p>Deletes files recursively.</p>
    *
    * @param file the file or directory to delete.
    */
    public static void deleteFilesRecursive(final File file) {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (int i = 0; i < files.length; ++i) deleteFilesRecursive(files[i]);
        }
        file.delete();
    }

    /**
    * <p>Adds a separator character to the end of the filename if it does not have it already.</p>
    *
    * @param filename the filename.
    */
    public static String addSeparator(String filename) {
        if (filename != null && !filename.endsWith(File.separator)) filename += File.separator;
        return filename;
    }

    public static Set replaceBasePath(final Set filenames, final String newBasePath) {
        final Set replaced = new HashSet();
        for (Iterator it = filenames.iterator(); it.hasNext(); ) replaced.add(newBasePath + new File((String) it.next()).getName());
        return replaced;
    }

    /**
    * <p>Replaces the filename extension with another one.</p>
    *
    * @param filename     the filename.
    * @param newExtension the new extension.
    * @return the filename with a new extension.
    */
    public static String replaceExtension(final String filename, final String newExtension) {
        final File file = new File(filename);
        final String name = file.getName();
        final String parent = file.getParent();
        int pos = name.lastIndexOf(".");
        return (pos == -1 ? filename : parent + '/' + name.substring(0, pos)) + newExtension;
    }

    /**
    * <p>Reads content from an URL into a byte array.</p>
    *
    * @param url the url to get the content from.
    * @return byte array with the contents of the file.
    */
    public static StringBuffer getContent(final URL url) throws IOException {
        final StringBuffer buffer = new StringBuffer();
        final InputStreamReader reader = new InputStreamReader(url.openStream());
        for (int numRead = 0; numRead >= 0; ) {
            int offset = 0;
            for (; offset < _readBuffer.length && (numRead = reader.read(_readBuffer, offset, _readBuffer.length - offset)) >= 0; offset += numRead) ;
            buffer.append(_readBuffer, 0, offset);
        }
        reader.close();
        return buffer;
    }

    /**
    * <p>Convenience method for serializing an object into a file.</p>
    */
    public static void serialize(final String path, final Object object) throws IOException {
        new ObjectOutputStream(new FileOutputStream(path)).writeObject(object);
    }

    /**
    * <p>Convenience method for deserializing an object from a file.</p>
    *
    * @return the object obtained from the file.
    */
    public static Object deserialize(final String path) throws ClassNotFoundException, IOException {
        Object result = null;
        FileInputStream inputStream = new FileInputStream(path);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            result = objectInputStream.readObject();
        } catch (IOException e) {
            inputStream.close();
            if (objectInputStream != null) objectInputStream.close();
            throw e;
        } catch (ClassNotFoundException e) {
            objectInputStream.close();
            throw e;
        }
        objectInputStream.close();
        return result;
    }

    public static URL[] filenamesToURLs(String[] filenames) throws MalformedURLException {
        final Collection urls = new LinkedList();
        for (int i = 0; i < filenames.length; ++i) urls.add(new File(filenames[i]).toURL());
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    private static void listFilesRecursive(final Collection fileCollection, final File path) {
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) listFilesRecursive(fileCollection, files[i]);
        } else fileCollection.add(path);
    }
}
