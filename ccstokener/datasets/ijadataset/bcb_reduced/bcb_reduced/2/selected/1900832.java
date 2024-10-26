package org.formaria.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.formaria.aria.build.BuildProperties;
import org.formaria.debug.DebugLogger;
import java.net.URLConnection;
import org.formaria.aria.Project;

/**
 * Used to locate project files. It searches for files within the paths
 * specified in the paths array.
 * The paths/URIs can be specified in the startup file as follows:<br>
 * NumRemoteClassLoaderPaths=2<br>
 * RemoteClassLoaderPath1=http://localhost:8080/<br>
 * RemoteClassLoaderPath2=http://localhost:8080/myresources/<br>
 * <p>Copyright: Copyright (c) Formaria Ltd., 2008</p>
 * $Revision: 2.3 $
 */
public class RemoteClassLoader extends ClassLoader implements IResourceLoader {

    /**
   * the URIs that are searched
   */
    protected URI[] uri;

    /**
   * Create a new class loader. Initially no search path is set.
   * @param the owner project
   */
    public RemoteClassLoader(Project project) {
        super();
        try {
            Project currentProject = project;
            int numClassLoadersPaths = new Integer(currentProject.getStartupParam("NumRemoteClassLoaderPaths")).intValue();
            String paths[] = new String[numClassLoadersPaths];
            for (int i = 0; i < numClassLoadersPaths; i++) paths[i] = currentProject.getStartupParam("RemoteClassLoaderPath" + (i + 1));
            setSources(paths);
        } catch (Exception e) {
            if (BuildProperties.DEBUG) DebugLogger.logError("APPLICATION", "Unable to load class loader: " + e.getMessage()); else System.err.println(e.getMessage());
        }
    }

    /**
   * Set the URIs for loading files
   * @param projectUris The String array of URLs for inclusion in the search.
   * @return true on success, false if there were problems.
   */
    public boolean setSources(String[] projectUris) {
        try {
            uri = new URI[projectUris.length];
            for (int i = 0; i < projectUris.length; i++) {
                uri[i] = new URI(projectUris[i]);
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
   * Overrides the Classloader method and searches for the resource file
   * specifies by the parameter fileName.
   * @param fileName The name of the file to be found.
   * @return An InputStream created by from the filename if found or null if not
   * found.
   */
    public InputStream getResourceAsStream(String fileName) {
        try {
            for (int i = 0; i < uri.length; i++) {
                URL url = new URL(uri[i].toURL(), fileName);
                URLConnection conn = url.openConnection();
                conn.setDefaultUseCaches(false);
                conn.setDoOutput(true);
                conn.setIfModifiedSince(0);
                InputStream is = conn.getInputStream();
                if (is != null) return is;
            }
        } catch (IOException ex) {
        }
        return null;
    }

    /**
   * Overrides the Classloader method and searches for the resource file
   * specifies by the parameter fileName.
   * @param fileName The name of the file to be found.
   * @return A URL from the filename if found or null if not
   * found.
   */
    public URL findResource(String fileName) {
        try {
            for (int i = 0; i < uri.length; i++) {
                String prefix = (fileName.indexOf("\\") > -1 || fileName.indexOf("/") > -1) ? "" : uri[i].toString() + File.separatorChar;
                File f = new File(prefix + fileName);
                if (f.exists()) {
                    return f.toURL();
                }
            }
        } catch (MalformedURLException ex) {
        } catch (NullPointerException ex) {
        }
        return null;
    }

    /**
   * Finds the specified class. This method should be overridden
   * by class loader implementations that follow the new delegation model
   * for loading classes, and will be called by the <code>loadClass</code>
   * method after checking the parent class loader for the requested class.
   * The default implementation throws <code>ClassNotFoundException</code>.
   * @return the resulting <code>Class</code> object
   * @param className the class to locate
   * @exception ClassNotFoundException if the class could not be found
   */
    public Class findClass(String className) throws ClassNotFoundException {
        return (findClass(className, true));
    }

    /**
   * Overrides the method from ClassLoader. This method is called auotmatically
   * if the system classloader cannot find the class it is looking for. We then
   * have the chance to find, load and return the class.
   * @param className The name of the class we are being asked to find
   * @param resolveIt Simply forward the value passed in
   * @return The newly created Class
   * @throws ClassNotFoundException pronlems...
   */
    public synchronized Class findClass(String className, boolean resolveIt) throws ClassNotFoundException {
        Class result;
        byte[] classBytes;
        try {
            result = super.findSystemClass(className);
            return result;
        } catch (ClassNotFoundException e) {
        }
        classBytes = loadClassBytes(className);
        if (classBytes == null) {
            throw new ClassNotFoundException();
        }
        result = defineClass(classBytes, 0, classBytes.length);
        if (result == null) {
            throw new ClassFormatError();
        }
        if (resolveIt) resolveClass(result);
        return result;
    }

    /**
   * Opens the class file specified by the className. If found it opens the file
   * and loads it into a byte array. The byte array is then returned.
   * @param className The name of the class we are being asked to load
   * @return A byte array created from the class file.
   */
    private byte[] loadClassBytes(String className) {
        byte[] classBytes = new byte[0];
        InputStream is = getResourceAsStream(className + ".class");
        try {
            byte[] b = new byte[1000];
            int i = is.read(b);
            while (i != -1) {
                byte[] tempBytes = classBytes;
                classBytes = new byte[classBytes.length + i];
                System.arraycopy(b, 0, classBytes, tempBytes.length, i);
                b = new byte[1000];
                i = is.read(b);
            }
        } catch (IOException ex) {
        }
        return classBytes;
    }
}
