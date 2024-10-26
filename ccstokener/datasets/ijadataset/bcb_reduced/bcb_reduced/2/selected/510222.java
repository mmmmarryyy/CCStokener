package com.sun.java.help.search;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * A factory for files that can be accessed as Random Access file from a URL. 
 * RAFFiles can either be an acutall RAF file or a memory version of the
 * RAF file. When a URL is it is opened as a RAF file, otherwise it is moved
 * to a temporary file if possible or into a memory resident version.
 * 
 *
 * @author Roger D. Brinkley
 * @version	1.27	10/30/06
 */
class RAFFileFactory {

    /**
     * Cannot create these, need to go through the factory method
     */
    private RAFFileFactory() {
    }

    static RAFFileFactory theFactory;

    public static synchronized RAFFileFactory create() {
        if (theFactory == null) {
            theFactory = new RAFFileFactory();
        }
        return theFactory;
    }

    private int memoryCacheLimit = 10000;

    private boolean isFileURL(URL url) {
        return url.getProtocol().equalsIgnoreCase("file");
    }

    public int getMemoryCacheLimit() {
        return memoryCacheLimit;
    }

    public void setMemoryCacheLimit(int limit) {
        this.memoryCacheLimit = limit;
    }

    public synchronized RAFFile get(URL url, boolean update) throws IOException {
        RAFFile result = null;
        if (isFileURL(url)) {
            try {
                String f = url.getFile();
                f = Utilities.URLDecoder(f);
                result = new RAFFile(f, update);
                debug("Opened Dict file with file protocol:" + f);
            } catch (SecurityException e) {
            }
        }
        if (result == null) {
            result = createLocalRAFFile(url);
        }
        if (result == null) {
            throw new FileNotFoundException(url.toString());
        }
        return result;
    }

    /** 
     * Given a URL, retrieves a DICT file and creates a cached DICT
     * file object. If the file is larger than the size limit,
     * and if temp files are supported by the Java virtual machine,
     * the DICT file is it is cached to disk. Otherwise the DICT file 
     * is cached in memory.
     */
    private static RAFFile createLocalRAFFile(URL url) throws IOException {
        RAFFile result = null;
        URLConnection connection = url.openConnection();
        try {
            Class types[] = {};
            Method m = URLConnection.class.getMethod("getPermission", types);
            result = RAFFileFactoryOn12.get(connection);
        } catch (NoSuchMethodError ex) {
        } catch (NoSuchMethodException ex) {
        }
        if (result == null) {
            result = new MemoryRAFFile(connection);
            debug("Opening a Dict file in Memory");
        }
        return result;
    }

    /**
     * Debug code
     */
    private static final boolean debug = false;

    private static void debug(String msg) {
        if (debug) {
            System.err.println("RAFFileFactory: " + msg);
        }
    }
}
