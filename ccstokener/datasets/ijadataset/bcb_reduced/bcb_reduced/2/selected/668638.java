package com.noahsloan.nutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import com.noahsloan.nutils.log.Logger;

public class FileUtils {

    private static final Logger LOG = Logger.getLogger(FileUtils.class);

    public static void recursiveDelete(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                recursiveDelete(child);
            }
        }
        f.delete();
    }

    /**
	 * Load a resource from the current working directory.
	 * 
	 * @param resource
	 *            the resource to load.
	 * @return and InputStream to the resource, or null if it could not be
	 *         found.
	 * @throws IOException
	 */
    public static InputStream load(String resource) {
        return load(resource, false);
    }

    /**
	 * Same as {@link #load(String)} but also searches the classpath if
	 * classpath is true.
	 * 
	 * @param resource
	 * @param classpath
	 * @return
	 * @throws IOException
	 */
    public static InputStream load(String resource, boolean classpath) {
        return load(resource, classpath, ".");
    }

    /**
	 * Load a resource, searching the given paths, plus the classpath.
	 * 
	 * @param resource
	 * @param paths
	 * @return
	 * @throws IOException
	 */
    public static InputStream load(String resource, String... paths) {
        return load(resource, true, paths);
    }

    /**
	 * Check paths first, in the order given, then the classpath.
	 * 
	 * @param resource
	 *            the resource to load.
	 * @param classpath
	 *            should the classpath be checked?
	 * @param paths
	 *            the file system and resource loader paths to check. There
	 *            should be no trailing '/' or '\\'
	 * @return
	 * @throws IOException
	 */
    public static InputStream load(String resource, boolean classpath, String... paths) {
        for (String path : paths) {
            File file = new File(path, resource);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    LOG.error(file + " must have been deleted just now.", e);
                }
            }
        }
        if (classpath) {
            InputStream r = FileUtils.class.getClassLoader().getResourceAsStream(resource);
            if (r == null) {
                r = loadResource(resource);
                if (r == null) {
                    for (String path : paths) {
                        r = loadResource(path + '/' + resource);
                        if (r != null) {
                            return r;
                        }
                    }
                }
            }
            return r;
        }
        return null;
    }

    public static InputStream loadResource(String resource) {
        URL url = FileUtils.class.getResource(resource);
        try {
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            return null;
        }
    }
}
