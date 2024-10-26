package org.formaria.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.formaria.aria.Project;

/**
 * Used to locate project files. This classloader is used in the Project and is initialised when
 * a project is created or opened. It searches for files within the paths
 * specified in the paths array.
 * <p>Copyright: Copyright (c) Formaria Ltd., 2008</p>
 * $Revision: 1.6 $
 */
public class ProjectClassLoader extends ClassLoader implements IResourceLoader {

    protected URL[] urls;

    protected String[] paths;

    protected String basePath;

    protected Project currentProject;

    /**
   * Create a new class loader. Initially no search path is set.
   * @param classLoader the super ClassLoader
   */
    public ProjectClassLoader(Project project, ClassLoader classLoader) {
        super(classLoader);
        currentProject = project;
    }

    /**
   * Create a new class loader. Initially no search path is set.
   * @param classLoader the super ClassLoader
   * @param path the basic path prefix to search
   */
    public ProjectClassLoader(ClassLoader classLoader, String path) {
        super(classLoader);
        basePath = path;
        int pos = basePath.indexOf("!/");
        if (pos > 0) basePath = basePath.substring(pos + 2);
    }

    /**
   * Set the paths for loading files
   * @param projectPaths The String array of paths for inclusion in the search.
   * @return true to indicate that the sources are setup, false to indicate some failure
   */
    public boolean setSources(String[] projectPaths) {
        paths = projectPaths;
        return true;
    }

    /**
   * Set the urls for loading files
   * @param projectURLs The URL array of paths for inclusion in the search.
   * @return true to indicate that the sources are setup, false to indicate some failure
   */
    public boolean setUrls(URL[] projectURLs) {
        urls = projectURLs;
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
        ClassLoader parentClassLoader = getParent();
        try {
            int pos = -1;
            if ((basePath == null) && ((paths != null) || (urls != null))) {
                if (paths != null) {
                    for (int i = 0; i < paths.length; i++) {
                        String prefix = ((fileName.indexOf("\\") == 0) || (fileName.indexOf("/") == 0)) ? "" : paths[i] + File.separatorChar;
                        File f = new File(prefix + fileName);
                        if (f.exists()) {
                            FileInputStream is = new FileInputStream(f);
                            if (is != null) return is;
                        } else if ((pos = fileName.indexOf("!/")) > 0) {
                            String fn = fileName.substring(pos + 2);
                            InputStream is = parentClassLoader.getResourceAsStream(fn);
                            if (is != null) return is;
                        }
                    }
                } else {
                    for (int i = 0; i < urls.length; i++) {
                        try {
                            URL url = new URL(urls[i], fileName);
                            InputStream is = url.openStream();
                            if (is != null) return is;
                        } catch (MalformedURLException ex) {
                            ex.printStackTrace();
                        } catch (IOException ex) {
                        }
                    }
                }
            } else {
                pos = fileName.indexOf("!/");
                if (pos > 0) fileName = fileName.substring(pos + 2);
                if (paths != null) {
                    String strippedFileName = fileName;
                    pos = strippedFileName.lastIndexOf("\\");
                    if (pos > 0) strippedFileName = strippedFileName.substring(pos + 1);
                    pos = strippedFileName.lastIndexOf("/");
                    if (pos > 0) strippedFileName = strippedFileName.substring(pos + 1);
                    for (int i = 0; i < paths.length; i++) {
                        String fileStr = basePath + ((fileName.charAt(0) == '/') ? "" : "/");
                        String subPath = paths[i];
                        if (((pos = subPath.indexOf(basePath)) >= 0) && (subPath.length() >= basePath.length())) subPath = subPath.substring(pos + basePath.length());
                        if (subPath.length() > 0) {
                            char c0 = subPath.charAt(0);
                            if ((c0 == '\\') || (c0 == '/')) subPath = subPath.substring(1);
                            fileStr += subPath + "/";
                        }
                        fileStr += strippedFileName;
                        InputStream is = parentClassLoader.getResourceAsStream(fileStr);
                        if (is != null) return is;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        if (parentClassLoader != null) {
            if (currentProject.canAccessDefaultPackage(parentClassLoader, fileName)) {
                return parentClassLoader.getResourceAsStream(fileName);
            }
        }
        return null;
    }

    public final URL getResource(final String fileName) {
        URL url = findResource(fileName);
        return url;
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
            if (paths != null) {
                for (int i = 0; i < paths.length; i++) {
                    String prefix = (fileName.indexOf("\\") > -1 || fileName.indexOf("/") > -1) ? "" : paths[i] + File.separatorChar;
                    File f = new File(prefix + fileName);
                    if (f.exists()) {
                        return f.toURL();
                    }
                }
            }
            if (urls != null) {
                for (int i = 0; i < urls.length; i++) {
                    URL url = new URL(urls[i], fileName);
                    try {
                        InputStream is = url.openStream();
                        if (is != null) {
                            is.close();
                            return url;
                        }
                    } catch (IOException ex) {
                    }
                }
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return null;
    }

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
   * @throws ClassNotFoundException
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
        result = defineClass(null, classBytes, 0, classBytes.length);
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
        InputStream is = getResourceAsStream(className.replace('.', '/') + ".class");
        try {
            byte[] b = new byte[1000];
            int i = is.read(b);
            while (i != -1) {
                byte[] tempBytes = classBytes;
                classBytes = new byte[classBytes.length + i];
                System.arraycopy(tempBytes, 0, classBytes, 0, tempBytes.length);
                System.arraycopy(b, 0, classBytes, tempBytes.length, i);
                i = is.read(b);
            }
        } catch (IOException ex) {
        }
        return classBytes;
    }
}
