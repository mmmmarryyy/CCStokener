package net.n0fx.netserve.classloader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class ClassLoaderUtil {

    private static final FilenameFilter LIB_FILTER = new LibraryFilter();

    public static final String ALLOWED_SEPERATORS = ",|;";

    public static URL[] directoryToUrls(String directory) {
        ArrayList urlList = new ArrayList();
        if (directory == null || directory.length() == 0) {
            throw new IllegalArgumentException("Directory must not be null");
        }
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        String[] libraries = dir.list(LIB_FILTER);
        for (int i = 0; i < libraries.length; i++) {
            URL url = convertToUrl(directory + "/" + libraries[i]);
            if (url != null) {
                urlList.add(url);
            }
        }
        return toUrlArray(urlList);
    }

    /**
	 * Converts the configuration file library String to an URL array.
	 * 
	 * @param library
	 * @return URLs
	 * @throws MalformedURLException
	 */
    public static URL[] convertToUrls(String library) {
        if (library == null || library.length() == 0) {
            return (URL[]) null;
        }
        String[] files = library.split(ALLOWED_SEPERATORS);
        ArrayList urlList = new ArrayList();
        for (int idx = 0; idx < files.length; idx++) {
            URL url = convertToUrl(files[idx]);
            if (url != null && !urlList.contains(url)) {
                urlList.add(url);
            }
        }
        return toUrlArray(urlList);
    }

    private static URL[] toUrlArray(ArrayList urlList) {
        URL[] urls = new URL[urlList.size()];
        urlList.toArray(urls);
        return (URL[]) urlList.toArray(urls);
    }

    public static URL convertToUrl(String fileName) {
        URL url = null;
        File file = null;
        try {
            file = new File(fileName);
            if (file.exists() && file.isFile()) {
                url = file.toURI().toURL();
            }
            return url;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not load library " + file.getAbsolutePath(), e);
        }
    }

    /**
	 * Disables file locking for all URLS.
	 * 
	 * @param urls
	 * @param disableFileLocking
	 */
    public static void disableFileLocking(URL[] urls, boolean disableFileLocking) {
        if (disableFileLocking) {
            for (int idx = 0; idx < urls.length; idx++) {
                disableFileLocking(urls[idx]);
            }
        }
    }

    /**
	 * Disables file locking (only useful under Windows)
	 * 
	 * @param urls
	 */
    public static void disableFileLocking(URL url) {
        try {
            url.openConnection().setDefaultUseCaches(false);
        } catch (IOException e) {
            throw new RuntimeException("Could not disable file locking!", e);
        }
    }

    /**
	 * Returns a new inctance loaded with the specified classloader
	 * @param className
	 * @param classloader
	 * @return
	 */
    public static Object getInstanceForName(String className, ClassLoader classloader) {
        try {
            Class clazz = getClassForName(className, classloader);
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getClassForName(String className, ClassLoader classloader) {
        try {
            return Class.forName(className, true, classloader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
