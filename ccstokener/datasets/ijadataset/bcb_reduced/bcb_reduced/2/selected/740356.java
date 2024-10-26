package gnujatella.utils;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Wrapper for Properties class provides convenience
 * functions.
 */
public class AdvancedProperties extends Properties {

    /**
         * Creates a new AdvancedProperties object and
         * loads the properties from the file.
         */
    public AdvancedProperties(URL url) throws IOException {
        super();
        getFromURL(url);
    }

    /**
         * Creates a new AdvancedProperties object and
         * loads the properties from the file.
         */
    public AdvancedProperties(File file) throws IOException {
        super();
        getFromFile(file);
    }

    /**
         * Creates a new AdvancedProperties object and
         * loads the properties from the file denoted
         * by it's path and name.
         */
    public AdvancedProperties(String path, String name) throws IOException {
        super();
        getFromFile(path, name);
    }

    /**
         * Creates a new AdvancedProperties object and
         * loads the properties from the file denoted
         * by it's path and name.
         */
    public AdvancedProperties(File path, String name) throws IOException {
        super();
        getFromFile(path, name);
    }

    /**
         * Loads the data from this url.
         */
    public void getFromURL(URL url) throws IOException {
        load(url.openStream());
    }

    /**
         * Loads the data from the file.
         */
    public void getFromFile(File file) throws IOException {
        load(new FileInputStream(file));
    }

    /**
         * Loads the data from the file.
         */
    public void getFromFile(String str) throws IOException {
        getFromFile(new File(str));
    }

    /**
         * Loads the data from the file denoted by it's
         * path and name.
         */
    public void getFromFile(String path, String name) throws IOException {
        getFromFile(new File(path, name));
    }

    /**
         * Loads the data from the file denoted by it's
         * path and name.
         */
    public void getFromFile(File path, String name) throws IOException {
        getFromFile(path.getPath(), name);
    }

    public void saveToFile(File file, String comment) throws IOException {
        store(new FileOutputStream(file), comment);
    }

    public void saveToFile(String file, String comment) throws IOException {
        saveToFile(new File(file), comment);
    }

    public void saveToFile(String path, String file, String comment) throws IOException {
        saveToFile(new File(path, file), comment);
    }

    public void saveToFile(File path, String file, String comment) throws IOException {
        saveToFile(new File(path.getPath(), file), comment);
    }

    public void setProperty(String key, int i) {
        setProperty(key, "" + i);
    }

    public String getPropertyStr(String key) {
        return getProperty(key);
    }

    public int getPropertyInt(String key) {
        return Integer.valueOf(getProperty(key, "0")).intValue();
    }
}
