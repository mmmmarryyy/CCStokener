package com.objectwave.customClassLoader;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

/**
 *  Loads class bytes from a file.
 *
 * @author  Jack Harich - 8/30/97
 * @version  $Id: FileClassLoader.java,v 2.0 2001/06/11 15:54:25 dave_hoag Exp $
 */
public class FileClassLoader extends MultiClassLoader {

    HashMap classesToSkip;

    private String filePrefix;

    /**
	 *  Attempts to load from a local file using the relative "filePrefix", ie
	 *  starting at the current directory. For example
	 *
	 * @param  filePrefix could be "webSiteClasses\\site1\\".
	 */
    public FileClassLoader(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    /**
	 * @param  b The new LoadLocalClasses value
	 */
    public void setLoadLocalClasses(boolean b) {
        loadLocalClasses = b;
    }

    /**
	 *  Gets the Content attribute of the FileClassLoader object
	 *
	 * @param  url
	 * @return  The Content value
	 * @exception  java.io.IOException
	 */
    public Object getContent(java.net.URL url) throws java.io.IOException {
        java.net.URL url2 = formatURL(url);
        return url2.getContent();
    }

    /**
	 *  Gets the InputStream attribute of the FileClassLoader object
	 *
	 * @param  url
	 * @return  The InputStream value
	 * @exception  java.io.IOException
	 */
    public java.io.InputStream getInputStream(java.net.URL url) throws java.io.IOException {
        java.net.URL url2 = formatURL(url);
        return url2.openStream();
    }

    /**
	 *  Add the name of a class that should not be loaded by this class loader.
	 *  Usefull for sharing classes.
	 *
	 * @param  className The feature to be added to the ClassToSkip attribute
	 */
    public void addClassToSkip(String className) {
        if (className == null) {
            throw new IllegalArgumentException("Attempted to skip a null class!");
        }
        StringBuffer buffer = new StringBuffer(className);
        buffer = buffer.reverse();
        if (classesToSkip == null) {
            classesToSkip = new HashMap();
        }
        classesToSkip.put(buffer.toString(), "");
    }

    /**
	 * @param  className
	 * @return
	 * @exception  ClassNotFoundException
	 */
    public Class forceLoadClass(String className) throws ClassNotFoundException {
        byte[] classBytes = loadClassBytes(className);
        Class c = defineClass(null, classBytes, 0, classBytes.length);
        if (c == null) {
            c = super.loadClass(className);
        }
        return c;
    }

    /**
	 * @param  className
	 * @return
	 */
    protected boolean useLocalLoader(String className) {
        boolean result = super.useLocalLoader(className) && !className.equals(this.getClass().getName());
        if (result && classesToSkip != null) {
            StringBuffer buffer = new StringBuffer(className);
            buffer = buffer.reverse();
            result = classesToSkip.get(buffer.toString()) == null;
        }
        return result;
    }

    /**
	 * @param  url
	 * @return
	 * @exception  java.io.IOException
	 */
    protected java.net.URL formatURL(java.net.URL url) throws java.io.IOException {
        String resourceName = url.getFile();
        String fileName = filePrefix + resourceName;
        fileName = new java.io.File(fileName).getCanonicalPath();
        java.net.URL url2 = new java.net.URL("file:///" + fileName);
        return url2;
    }

    /**
	 * @param  className
	 * @return
	 */
    protected byte[] loadClassBytes(String className) {
        String formattedClassName = formatClassName(className);
        if (sourceMonitorOn) {
            print(">> from file: " + formattedClassName);
        }
        byte result[];
        String fileName = filePrefix + formattedClassName;
        try {
            java.io.File f = new java.io.File(fileName);
            InputStream inStream;
            if (f.exists()) {
                inStream = new FileInputStream(fileName);
            } else {
                if (sourceMonitorOn) {
                    System.out.println("Finding class on class path");
                }
                inStream = locateClass(className);
            }
            result = new byte[inStream.available()];
            inStream.read(result);
            inStream.close();
            return result;
        } catch (Exception e) {
            print("### File '" + fileName + "' not found.");
            return null;
        }
    }
}
