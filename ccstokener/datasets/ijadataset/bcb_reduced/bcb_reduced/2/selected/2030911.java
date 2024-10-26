package it.mozzicato.apkwizard.utils;

import java.io.*;
import java.net.*;
import java.util.*;

public class PropsUtils {

    private PropsUtils() {
    }

    /**
     * Load a properties file from the classpath
     * @param propsName
     * @return Properties
     * @throws Exception
     */
    public static Properties load(String propsName) {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource(propsName);
        try {
            props.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    /**
     * Load a Properties File
     * @param propsFile
     * @return Properties
     * @throws IOException
     */
    public static Properties load(File propsFile) {
        Properties props = new Properties();
        FileInputStream fis;
        try {
            fis = new FileInputStream(propsFile);
            props.load(fis);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return props;
    }

    public static void save(Properties props, File propsFile) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(propsFile);
            props.store(fos, null);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
