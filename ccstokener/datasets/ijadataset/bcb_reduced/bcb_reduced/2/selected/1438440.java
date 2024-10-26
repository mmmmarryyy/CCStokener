package com.volantis.testtools.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Class for general IO utilities.
 */
public class IOUtils {

    /**
     * Extract the zip file from a jar file (copy zip file to temporary
     * directory). Note that the returned file provides access to this
     * temporary file, which is itself marked for deletion on JVM exit.
     * <p>
     * NOTE: ResourceTemporaryFileCreator does the same thing, but does not rely
     * on deleteOnExit().
     *
     * @param clazz the class who's <code>getResource</code> method will be
     *              invoked to access the named zip file
     * @param zipFilename The name of the zip file to extract.
     * @param suffix The file suffix for the extracted zip file (e.g. zip).
     * @return the file that points to the copied zip file.
     */
    public static File extractTempZipFromJarFile(Class clazz, String zipFilename, String suffix) throws Exception {
        URL url = clazz.getResource(zipFilename);
        InputStream in = url.openConnection().getInputStream();
        File file = File.createTempFile("testZipFile", "." + suffix, new File(System.getProperty("java.io.tmpdir")));
        FileOutputStream out = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        out.close();
        in.close();
        file.deleteOnExit();
        return file;
    }

    /**
     * Deletes all files in the specified directory and sub-directories
     *
     * @param dir to delete
     * @throws IllegalStateException if one or more file or directory cannot be
     *                               deleted
     */
    public static void deleteDir(File dir) throws IllegalStateException {
        String list[] = dir.list();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                File file = new File(dir, list[i]);
                if (file.isDirectory()) {
                    deleteDir(file);
                } else if (!file.delete()) {
                    throw new IllegalStateException("Could not delete test file: " + file.getAbsolutePath());
                }
            }
            if (!dir.delete()) {
                throw new IllegalStateException("Could not delete test directory: " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Basic utility method to convert a file to a directory.
     * There is a very small chance that an intermittent exception could be
     * thrown - if the file in questions is recreated by someone else between
     * the File#delete and File#mkdirs calls.
     *
     * @param tempDir file to convert to a directory
     * @return a directory that exists
     * @throws IllegalStateException
     */
    public static File createDirectory(File tempDir) throws IllegalStateException {
        if (tempDir.exists()) {
            if (!tempDir.delete()) {
                throw new IllegalStateException("Could not delete temporary test file: " + tempDir);
            }
        }
        if (!tempDir.mkdirs()) {
            throw new IllegalStateException("Could not create temporary test directory: " + tempDir);
        }
        return tempDir;
    }
}
