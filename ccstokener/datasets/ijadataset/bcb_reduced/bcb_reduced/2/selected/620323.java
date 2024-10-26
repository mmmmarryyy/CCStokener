package com.induslogic.uddi.server.util;

import java.util.Properties;
import java.net.URL;
import java.io.IOException;

/**
 * This class is the image of the config file. So instead of reading variables given in the config file directly, other classes should ask the values from this class.
 *
 *
 * @author Tarun Garg (tarung@induslogic.com)
 */
public class GlobalProperties {

    static String url;

    static String className;

    static String user;

    static String password;

    static String operatorName;

    static String authorisedName;

    static boolean isUpdated = false;

    static ClassLoader cl;

    public static void setClassLoader(ClassLoader loader) {
        cl = loader;
    }

    public static void readProperties() throws IOException {
        URL url1 = cl.getResource("conf/soapuddi.config");
        Properties props = new Properties();
        if (url1 == null) throw new IOException("soapuddi.config not found");
        props.load(url1.openStream());
        className = props.getProperty("Class");
        url = props.getProperty("URL");
        user = props.getProperty("user");
        password = props.getProperty("passwd");
        operatorName = props.getProperty("operator");
        authorisedName = props.getProperty("authorisedName");
        isUpdated = true;
    }

    public static void reReadFromFile() {
        isUpdated = false;
    }

    public static String getURL() throws IOException {
        if (isUpdated) {
            return url;
        }
        readProperties();
        return url;
    }

    public static String getClassName() throws IOException {
        if (isUpdated) {
            return className;
        }
        readProperties();
        return className;
    }

    public static String getUser() throws IOException {
        if (isUpdated) {
            return user;
        }
        readProperties();
        return user;
    }

    public static String getPassword() throws IOException {
        if (isUpdated) {
            return password;
        }
        readProperties();
        return password;
    }

    public static String getOperatorName() throws IOException {
        if (isUpdated) {
            return operatorName;
        }
        readProperties();
        return operatorName;
    }

    public static String getAuthorisedName() throws IOException {
        if (isUpdated) {
            return authorisedName;
        }
        readProperties();
        return authorisedName;
    }
}
