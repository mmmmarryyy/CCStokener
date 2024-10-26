package uk.ac.lkl.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.JarURLConnection;

public class JarUtilities {

    @SuppressWarnings("unused")
    private static ResourceManager getResourceManager(URL url) {
        String protocol = url.getProtocol();
        if (protocol.equals("file")) return ResourceManager.FILE; else if (protocol.equals("jar")) return ResourceManager.JAR;
        throw new UnsupportedOperationException("Unknown protocol: " + protocol);
    }

    public static List<String> newGetResourceListing(Class<?> cl, String path, boolean includeDirectories, boolean stripPath) {
        URL url = cl.getClassLoader().getResource(path);
        System.out.println("URL: " + url);
        if (url == null) {
            String me = cl.getName().replace(".", "/") + ".class";
            url = cl.getClassLoader().getResource(me);
        }
        return new FileResourceManager().getResourceListing(path, includeDirectories, stripPath);
    }

    /**
     * List directory contents for a resource folder. Not recursive. This is
     * basically a brute-force implementation. Works for regular files and also
     * JARs.
     * 
     * This method is based very directly on code written by Gregg Briggs. Used
     * here with permission. See:
     * 
     * http://www.uofr.net/~greg/java/get-resource-listing.html
     * 
     * for the original code.
     * 
     * Modified to use JarURLConnection.
     * 
     * @param cl
     *            Any java class that lives in the same place as the resources
     *            you want.
     * @param path
     *            Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     **/
    public static List<String> getResourceListing(Class<?> cl, String path, boolean includeDirectories, boolean stripPath) {
        try {
            URL dirURL = cl.getClassLoader().getResource(path);
            if (dirURL != null && dirURL.getProtocol().equals("file")) {
                return Arrays.asList(new File(dirURL.toURI()).list());
            }
            if (dirURL == null) {
                String me = cl.getName().replace(".", "/") + ".class";
                dirURL = cl.getClassLoader().getResource(me);
            }
            if (dirURL.getProtocol().equals("jar")) {
                URL urlJar = new URL("jar:" + dirURL.getPath().substring(0, dirURL.getPath().indexOf("!") + 2));
                JarURLConnection conn = (JarURLConnection) urlJar.openConnection();
                JarFile jarFile = conn.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                Set<String> results = new HashSet<String>();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith(path)) {
                        if (stripPath) entry = entry.substring(path.length() + 1);
                        if (entry.endsWith("/")) {
                            if (includeDirectories) {
                                entry = entry.substring(0, entry.length() - 1);
                                results.add(entry);
                            }
                        } else results.add(entry);
                    }
                }
                return new ArrayList<String>(results);
            }
        } catch (URISyntaxException e) {
        } catch (IOException e) {
        }
        return Collections.emptyList();
    }

    public static void preloadClasses(String path) {
        List<String> entries = JarUtilities.getResourceListing(JarUtilities.class, path, true, false);
        for (String entry : entries) {
            entry = entry.replace("/", ".");
            if (!entry.endsWith(".class")) continue;
            try {
                String className = entry.substring(0, entry.length() - 6);
                Class.forName(className);
                System.out.println("Loaded class: " + className);
            } catch (ClassNotFoundException e) {
                System.out.println(entry);
                System.out.println(e);
            } catch (NoClassDefFoundError e) {
                System.out.println(entry);
                System.out.println(e);
            } catch (ExceptionInInitializerError e) {
                System.out.println(entry);
                System.out.println(e);
                System.out.println("Cause: " + e.getCause());
            } catch (Exception e) {
                System.out.println(entry);
                System.out.println(e);
            }
        }
    }
}
