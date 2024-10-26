package org.spice.servlet.dispatcher.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 * @author Karthik Rajkumar
 * @since (included from RED-CHILLIES Version)
 */
public class ClassPathScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathScanner.class);

    /** Pseudo URL prefix for loading from the class path: "classpath:" */
    private static final String CLASSPATH_URL_PREFIX = "classpath*:";

    /** URL protocol for an entry from a jar file: "jar" */
    private static final String URL_PROTOCOL_JAR = "jar";

    /** URL protocol for an entry from a zip file: "zip" */
    private static final String URL_PROTOCOL_ZIP = "zip";

    /** URL protocol for an entry from a WebSphere jar file: "wsjar" */
    private static final String URL_PROTOCOL_WSJAR = "wsjar";

    /** URL protocol for an entry from an OC4J jar file: "code-source" */
    private static final String URL_PROTOCOL_CODE_SOURCE = "code-source";

    /** Separator between JAR URL and file path within the JAR */
    private static final String JAR_URL_SEPARATOR = "!/";

    /**
     * The file returns the String (based on the classpath configuration)
     *
     * @param location
     * @return
     */
    private String getResources(final String location) {
        final int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && (isPattern(location.substring(prefixEnd, rootDirEnd)))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }

    private boolean isPattern(final String path) {
        return (path.indexOf('*') != -1 || path.indexOf('?') != -1);
    }

    /**
     * The method reads the given path and converts based on the JAR file or a directory and
     * looks for any file that ends with the .class file.
     *
     *
     * @param location
     * @param rootDir
     * @return
     * @throws IOException
     */
    private List<String> findResource(final String location, String rootDir) throws IOException {
        final List<String> listOfClasses = new ArrayList<String>();
        final List<String> resources = new ArrayList<String>();
        String path = location;
        if (path.startsWith("/")) path = path.substring(1);
        try {
            Enumeration<?> resourceUrls = ClassPathScanner.class.getClassLoader().getResources(path);
            while (resourceUrls.hasMoreElements()) {
                URL url = (URL) resourceUrls.nextElement();
                if (url.getProtocol().equals(URL_PROTOCOL_JAR) || url.getProtocol().equals(URL_PROTOCOL_ZIP) || url.getProtocol().equals(URL_PROTOCOL_WSJAR) || url.getProtocol().equals(URL_PROTOCOL_CODE_SOURCE) && url.getPath().contains(JAR_URL_SEPARATOR)) {
                    final JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
                    final JarFile jarFile = urlConnection.getJarFile();
                    final Enumeration<?> jarEnum = jarFile.entries();
                    while (jarEnum.hasMoreElements()) {
                        final JarEntry entry = (JarEntry) jarEnum.nextElement();
                        String entryName = entry.getName();
                        if (entryName.endsWith(".class") && entryName.startsWith(location)) {
                            entryName = entryName.replaceAll("/", ".").substring(0, entryName.length() - 6);
                            resources.add(entryName);
                        }
                    }
                } else {
                    final File file = new File(url.getFile());
                    for (String clazz : exploreFolders(file, new File(url.getFile()).getAbsolutePath().length(), path, listOfClasses)) {
                        resources.add(clazz);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("Unable to parse the configuration file as the file does not exist");
        }
        return resources;
    }

    private List<String> exploreFolders(final File file, int rootDir, String path, List<String> listOfClasses) {
        final File[] listOfFiles = file.listFiles();
        for (final File singleFile : listOfFiles) {
            if (singleFile.isDirectory()) {
                exploreFolders(singleFile, rootDir, path, listOfClasses);
                continue;
            }
            final String individualFile = singleFile.getAbsolutePath();
            final String aFileName = individualFile.substring(rootDir + 1);
            final boolean aIsClassFile = aFileName.endsWith(".class");
            if (!aIsClassFile) continue;
            final String aSimpleName = aFileName.substring(0, aFileName.length() - 6).replace('/', '.').replace('\\', '.');
            final String location = path;
            final String packageWork = location.replaceAll("/", ".");
            final String aClassName = packageWork + aSimpleName;
            listOfClasses.add(aClassName);
        }
        return listOfClasses;
    }

    public List<String> readResources(final String location) throws IOException {
        final String rootDir = getResources(location);
        if (LOGGER.isInfoEnabled()) LOGGER.info("The root directory are " + rootDir);
        final String subPattern = location.substring(rootDir.length());
        if (LOGGER.isInfoEnabled()) LOGGER.info("The sub pattern are " + subPattern);
        return findResource(rootDir.substring(CLASSPATH_URL_PREFIX.length()), rootDir);
    }
}
