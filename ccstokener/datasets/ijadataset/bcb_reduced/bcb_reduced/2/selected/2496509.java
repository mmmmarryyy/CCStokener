package com.apelon.dts.util;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class is responsbile for loading modular classifier properties
 * 
 * 
 * @author  Apelon Inc
 * @since DTS 3.3
 */
public class MCServerConnection {

    private static Map propertyMap;

    private MCServerConnection() {
    }

    public static synchronized Map getConnectionMap(String fileName) {
        if (propertyMap != null) {
            return propertyMap;
        }
        InputStream is = null;
        try {
            Properties props = new Properties();
            URL url = getRelativePath(fileName);
            props = new Properties();
            is = url.openStream();
            props.load(is);
            Enumeration keys = props.keys();
            propertyMap = new HashMap();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                Object value = props.get(key);
                propertyMap.put(key.toLowerCase(), value);
            }
            return propertyMap;
        } catch (FileNotFoundException e) {
            IllegalArgumentException ille = new IllegalArgumentException("cannot load: " + fileName + " " + e.getMessage());
            ille.setStackTrace(e.getStackTrace());
            throw ille;
        } catch (Exception e) {
            RuntimeException rte = new RuntimeException("cannot load: " + fileName + " " + e.getMessage());
            rte.setStackTrace(e.getStackTrace());
            throw rte;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    throw new RuntimeException("cannot close: " + fileName + e.getMessage());
                }
            }
        }
    }

    private static URL getRelativePath(String fileName) {
        String absPath = null;
        String sep = null;
        URL url = null;
        try {
            StringBuffer urlSbf = new StringBuffer();
            File wd = new File(System.getProperty("user.dir"));
            absPath = wd.getCanonicalPath();
            sep = System.getProperty("file.separator");
            File parent = new File(absPath);
            if (fileName != null && fileName.trim().length() > 0) {
                urlSbf.append("file:" + sep + sep + sep + parent + sep + fileName);
                url = new URL(urlSbf.toString());
            } else {
                throw new RuntimeException(fileName + " not found.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return url;
    }
}
