package org.dwgsoftware.raistlin.repository.util;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Enumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import org.dwgsoftware.raistlin.repository.Artifact;
import org.dwgsoftware.raistlin.repository.RepositoryException;

/**
 * Various static utility methods used throughout repository related programing 
 * interfaces.
 * 
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $
 */
public class RepositoryUtils {

    /** meta extension tag for meta-data containing artifacts */
    public static final String META = "meta";

    /**
     * Transforms a Properties into a Attributes using a simple enumeration 
     * convention for property names which appends a numeric enumeration name
     * component to the dotted property key.  Note that changes to the 
     * Attributes object do not have any effect on the Properties object and 
     * vice versa.  All values are copied.
     * 
     * @param properties the properties to be transformed
     * @return the Attributes representing the properties
     */
    public static Attributes getAsAttributes(Properties properties) {
        if (null == properties) throw new NullPointerException("properties");
        Attributes l_attrs = new BasicAttributes(false);
        Enumeration l_list = properties.propertyNames();
        while (l_list.hasMoreElements()) {
            String l_key = (String) l_list.nextElement();
            if (isEnumerated(l_key)) {
                String l_keyBase = getEnumeratedBase(l_key);
                Attribute l_attr = l_attrs.get(l_keyBase);
                if (null == l_attr) {
                    l_attr = new BasicAttribute(l_keyBase, false);
                }
                l_attr.add(properties.getProperty(l_key));
                l_attrs.put(l_attr);
            } else {
                l_attrs.put(l_key, properties.getProperty(l_key));
            }
        }
        return l_attrs;
    }

    /**
     * Gets the Attribues in a remote artifact.
     * 
     * @param repositories the reprositories to search against
     * @param artifact the artifact to load meta data from
     * @return the meta data as attributes
     * @throws RepositoryException if there is execution failure
     */
    public static Attributes getAttributes(String[] repositories, Artifact artifact) throws RepositoryException {
        return getAsAttributes(getProperties(repositories, artifact));
    }

    /**
     * Gets the Attribues from the cache.
     * 
     * @param cache the reprository cache
     * @param artifact the artifact to load meta data from
     * @return the meta data as attributes
     * @throws RepositoryException if there is execution failure
     */
    public static Attributes getAttributes(File cache, Artifact artifact) throws RepositoryException {
        return getAsAttributes(getProperties(cache, artifact));
    }

    /**
     * Gets the Properties in the local cache.
     * 
     * @param cache the local cache
     * @param artifact the artifact to load meta data from
     * @return the loaded properties 
     * @throws RepositoryException if there is any problem loading the 
     *    properties
     */
    public static Properties getProperties(File cache, Artifact artifact) throws RepositoryException {
        File local = new File(cache, artifact.getPath() + "." + META);
        if (!local.exists()) {
            final String error = "Cannot load metadata due to missing resurce.";
            Throwable cause = new FileNotFoundException(local.toString());
            throw new RepositoryException(error, cause);
        }
        try {
            Properties properties = new Properties();
            InputStream input = new FileInputStream(local);
            properties.load(input);
            return properties;
        } catch (Throwable e) {
            final String error = "Unexpected error while attempting to load properties from local meta: " + local.toString();
            throw new RepositoryException(error, e);
        }
    }

    /**
     * Gets the Properties in a remote properties file.
     * 
     * @param repositories the reprositories to search against
     * @param artifact the artifact to load meta data from
     * @return the loaded properties 
     * @throws RepositoryException if there is any problem loading the 
     *    properties
     */
    public static Properties getProperties(String[] repositories, Artifact artifact) throws RepositoryException {
        if (null == repositories) throw new NullPointerException("repositories");
        if (null == artifact) throw new NullPointerException("artifact");
        Throwable l_throwable = null;
        Properties l_props = null;
        for (int ii = 0; ii < repositories.length; ii++) {
            StringBuffer l_buf = new StringBuffer();
            l_buf.append(artifact.getURL(repositories[ii]));
            l_buf.append(".");
            l_buf.append(META);
            try {
                URL l_url = new URL(l_buf.toString());
                l_props = getProperties(l_url);
                return l_props;
            } catch (Throwable e) {
                l_throwable = e;
            }
        }
        StringBuffer l_repos = new StringBuffer();
        for (int ii = 0; ii < repositories.length; ii++) {
            l_repos.append(repositories[ii]).append(',');
        }
        throw new RepositoryException("None of the repositories [" + l_repos.toString() + "] contained the metadata properties for " + artifact, l_throwable);
    }

    /**
     * Gets the Properties in a remote properties file.
     * 
     * @param url the url to the properties file
     * @return the loaded properties for the file
     * @throws IOException indicating a IO error during property loading
     */
    public static Properties getProperties(URL url) throws IOException {
        InputStream l_in = null;
        Properties l_props = new Properties();
        l_in = url.openStream();
        l_props.load(l_in);
        if (l_in != null) {
            l_in.close();
        }
        return l_props;
    }

    /**
     * Detects whether or not a property key is of the multivalued enumeration 
     * kind.  A multivalued key simply enumerates values by appending a '.' and
     * a number after the dot: i.e. artifact.dependency.2 and artifact.alias.23
     * et. cetera.
     * 
     * @param key the property name or key
     * @return true if the property conforms to the enumerated property 
     * convention, false otherwise
     */
    public static boolean isEnumerated(String key) {
        int l_lastDot = key.lastIndexOf('.');
        String l_lastComponent = null;
        if (-1 == l_lastDot) {
            return false;
        }
        l_lastComponent = key.substring(l_lastDot + 1);
        if (key.equals(key.substring(l_lastDot))) {
            return false;
        }
        try {
            Integer.parseInt(l_lastComponent);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Gets the key base of an enumerated property using the multivalued 
     * property key naming convention.
     * 
     * @param key the enumerated key whose last name component is a number
     * @return the base name of the enumerated property
     */
    public static String getEnumeratedBase(String key) {
        if (null == key) {
            return null;
        }
        if (!isEnumerated(key)) {
            return key;
        }
        int l_lastDot = key.lastIndexOf('.');
        String l_base = null;
        if (-1 == l_lastDot) {
            return key;
        }
        return key.substring(0, l_lastDot);
    }

    public static String[] getDelimited(char a_delim, String a_substrate) {
        int l_start = 0, l_end = 0;
        ArrayList l_list = new ArrayList();
        if (null == a_substrate || a_substrate.equals("")) {
            return null;
        }
        while (l_end < a_substrate.length()) {
            l_end = a_substrate.indexOf(',', l_start);
            if (-1 == l_end) {
                l_end = a_substrate.length();
                l_list.add(a_substrate.substring(l_start, l_end));
                break;
            }
            l_list.add(a_substrate.substring(l_start, l_end));
            l_start = l_end + 1;
        }
        return (String[]) l_list.toArray(new String[0]);
    }

    /**
    * Convert a set of host path statements to formal urls.
    * @param hosts the set of host names
    * @return the equivalent URL array
    */
    public static URL[] convertToURLs(String[] hosts) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < hosts.length; i++) {
            URL url = convertToURL(hosts[i]);
            if (url != null) list.add(url);
        }
        return (URL[]) list.toArray(new URL[0]);
    }

    /**
    * Convert a path to a url.
    * @param host the host address
    * @return the equivalent url
    * @exception IllegalArgumentException if the path cannot 
    *   be converted to a url
    */
    public static URL convertToURL(String host) throws IllegalArgumentException {
        try {
            return new URL(host);
        } catch (Throwable e) {
            final String error = "Unable to convert a supplied host spec to a url: " + host;
            throw new IllegalArgumentException(error);
        }
    }

    /**
    * For a given set of host paths, returns an equivalent set 
    * ensuring that the path ends with the "/" character.
    * @param hosts the set of host path names
    * @return the equivalent host path names
    */
    public static String[] getCleanPaths(String[] hosts) {
        String[] paths = new String[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            String path = hosts[i];
            if (!path.endsWith("/")) {
                paths[i] = path + "/";
            } else {
                paths[i] = path;
            }
        }
        return paths;
    }
}
