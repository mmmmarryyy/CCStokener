package com;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.cfdrc.sbmlforge.util.IOUtils;

/**
 * A utility class for locating resources.  
 *
 * All requests for resources should be fully qualified, like:
 * "com/cfdrc/files/myfile.xml"
 *
 * @author John Siegel
 */
public class ResourceLocator {

    /** file separator string */
    public final String SEP = "/";

    /** file separator character */
    public final char SEPC = '/';

    /** file name */
    private String fileName;

    /** absolute path in package tree with NO leading file separator */
    private String relToRootPath;

    /**
     * Locate a resource which is specified fully qualified like: com/cfdrc/docs/preferences.xml
     *
     * @param resourceName The resource name, fully qualified like: com/cfdrc/docs/preferences.xml
     * @return the location of the resource or null if not found
     * @throws FileNotFoundException Thrown if resource cannot be found in local file system
     */
    public static URL locateURL(String resourceName) throws FileNotFoundException {
        ResourceLocator locator = new ResourceLocator(resourceName);
        URL url = locator.getClass().getResource(locator.getResourceNameRelativeToLocator());
        if (url != null) {
            return url;
        } else {
            throw new FileNotFoundException("Unable to locate resource: '" + resourceName + "'");
        }
    }

    /**
     * Does a given resource exist?
     * @param resourceName Resource name to locate
     * @return True if resource can be found in local file system
     */
    public static boolean exists(String resourceName) {
        try {
            return locateURL(resourceName) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Load a resource as a string.  If the resource is not resolved,
     * null will be returned
     * @param resourceName
     * @return the resource or null if not found
     * @throws FileNotFoundException Thrown if resource cannot be found in local file system
     */
    public static String locate(String resourceName) throws FileNotFoundException {
        URL resource = locateURL(resourceName);
        return resource == null ? null : resource.toString();
    }

    /**
     * Get an input stream, given the resource path
     * @param resourceName		Resource to load 
     * @return 					Text stream from given resource
     * @throws IOException 		Thrown if problem reading from resource
     *
     */
    public static InputStream locateAndOpenStream(String resourceName) throws IOException {
        URL url = locateURL(resourceName);
        if (url != null) {
            return url.openStream();
        } else {
            throw new RuntimeException("Error locating resource: " + resourceName);
        }
    }

    /**
     * Locate resource and load as string return fallback if not found
     * @param resourceName 	Resource to load
     * @param fallback		Returned if problem reading from resource 
     * @return Text content of resource
     */
    public static String locateAndLoadAsString(String resourceName, String fallback) {
        try {
            return IOUtils.getAsString(locateAndOpenStream(resourceName));
        } catch (IOException ioe) {
            return fallback;
        }
    }

    /**
     * Construct a resource locator using the
     * full specification of the resource
     * @param resourceName
     */
    ResourceLocator(String resourceName) {
        if (resourceName.startsWith(SEP)) {
            resourceName = resourceName.substring(1);
        }
        if (resourceName.lastIndexOf(SEPC) != -1) {
            fileName = resourceName.substring(resourceName.lastIndexOf(SEPC) + 1);
            relToRootPath = resourceName.substring(0, resourceName.lastIndexOf(SEPC));
        } else {
            fileName = resourceName;
            relToRootPath = "";
        }
    }

    String getBasename() {
        return fileName;
    }

    String getDirname() {
        return relToRootPath;
    }

    /**
     * Get the path relative to this resource locator class
     * @return relative path
     **/
    String getResourceNameRelativeToLocator() {
        String pkPath = this.getClass().getPackage().getName().replace('.', '/');
        String regexp = "[/\\\\]";
        String[] relToRootPathElems = this.relToRootPath.split(regexp);
        String[] pkPathElems = pkPath.split(regexp);
        int identicalLevels = 0;
        for (int i = 0; i < relToRootPathElems.length && i < pkPathElems.length; i++) {
            if (relToRootPathElems[i].equals(pkPathElems[i])) {
                identicalLevels++;
            } else {
                break;
            }
        }
        StringBuffer relPath = new StringBuffer();
        for (int i = identicalLevels; i < pkPathElems.length; i++) {
            relPath.append("..").append(SEP);
        }
        for (int i = identicalLevels; i < relToRootPathElems.length; i++) {
            if (relToRootPathElems[i].equals("")) {
                continue;
            }
            relPath.append(relToRootPathElems[i]).append(SEP);
        }
        relPath.append(this.fileName);
        return relPath.toString();
    }
}
